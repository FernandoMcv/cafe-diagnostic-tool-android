package com.fcbc.tesis.data.repository

import com.fcbc.tesis.data.local.ForumDao
import com.fcbc.tesis.data.mapper.ForumPostDto
import com.fcbc.tesis.data.mapper.toDomain
import com.fcbc.tesis.data.mapper.toDto
import com.fcbc.tesis.data.mapper.toEntity
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.repository.ForumRepository
import com.fcbc.tesis.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fcbc.tesis.worker.SyncWorker
@Serializable
data class SavedPostDto(
    val user_id: String,
    val post_id: String
)


class ForumRepositoryImpl(
    private val forumDao: ForumDao,
    private val interactionDao: com.fcbc.tesis.data.local.InteractionDao,
    private val supabase: SupabaseClient,
    private val context: Context
) : ForumRepository {

    override fun getAllPosts(): Flow<List<ForumPost>> {
        return forumDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPagedPosts(filter: String?): Flow<PagingData<ForumPost>> {
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = {
                if (filter == null) {
                    forumDao.getPagedPosts()
                } else {
                    forumDao.getPagedPostsByFilter(filter)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getPostsByUserId(userId: String): Flow<List<ForumPost>> {
        return forumDao.getPostsByUserId(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSavedPosts(): Resource<List<ForumPost>> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        try {
            // Offline-first: Read from local cache first
            val localSaved = interactionDao.getSavedPostsForUser(user.id)
            val postIds = localSaved.map { it.postId }
            
            val localPosts = postIds.mapNotNull { forumDao.getPostById(it) }.map { it.toDomain() }
            
            // Try to sync with Supabase in background
            try {
                val remoteSaved = supabase.postgrest["saved_posts"]
                    .select { filter { eq("user_id", user.id) } }
                    .decodeList<SavedPostDto>()
                
                remoteSaved.forEach { dto ->
                    if (interactionDao.getSavedPost(dto.user_id, dto.post_id) == null) {
                        interactionDao.insertSavedPost(com.fcbc.tesis.data.local.entity.SavedPostEntity(dto.user_id, dto.post_id, isSynced = true))
                    }
                }
            } catch (e: Exception) {
                // Ignore network errors, we just return local
            }

            Resource.Success(localPosts)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al obtener guardados")
        }
    }

    override suspend fun toggleSavePost(postId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        
        val existingLocal = interactionDao.getSavedPost(user.id, postId)
        val isCurrentlySaved = existingLocal != null && !existingLocal.isDeletedOffline
        val newState = !isCurrentlySaved

        if (newState) {
            val newEntity = com.fcbc.tesis.data.local.entity.SavedPostEntity(user.id, postId, isSynced = false, isDeletedOffline = false)
            interactionDao.insertSavedPost(newEntity)
        } else {
            val updatedEntity = com.fcbc.tesis.data.local.entity.SavedPostEntity(user.id, postId, isSynced = false, isDeletedOffline = true)
            interactionDao.insertSavedPost(updatedEntity)
        }

        try {
            if (newState) {
                supabase.postgrest["saved_posts"].insert(SavedPostDto(user.id, postId))
            } else {
                supabase.postgrest["saved_posts"].delete {
                    filter {
                        eq("user_id", user.id)
                        eq("post_id", postId)
                    }
                }
            }
            // If successful, update local to synced
            if (newState) {
                interactionDao.insertSavedPost(com.fcbc.tesis.data.local.entity.SavedPostEntity(user.id, postId, isSynced = true, isDeletedOffline = false))
            } else {
                interactionDao.deleteSavedPost(user.id, postId) 
            }
            Resource.Success(newState)
        } catch (e: Exception) {
            e.printStackTrace()
            enqueueSyncWorker()
            Resource.Success(newState)
        }
    }

    override suspend fun addPost(forumPost: ForumPost): Resource<String> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull()
        var location: String? = null
        if (user != null) {
            try {
                val profileDto = supabase.postgrest["profiles"]
                    .select { filter { eq("id", user.id) } }
                    .decodeSingleOrNull<ProfileDto>()
                location = profileDto?.location
            } catch (e: Exception) {}
        }
        val localId = if (forumPost.id.isEmpty()) java.util.UUID.randomUUID().toString() else forumPost.id
        val postWithLocation = forumPost.toEntity().copy(id = localId, location = location)
        forumDao.insert(postWithLocation)
        
        return@withContext try {
            if (user != null) {
                val dto = postWithLocation.toDto(user.id)
                supabase.postgrest["forum_posts"].insert(dto)
            }
            Resource.Success(localId)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallo en red: marcar local como pendiente de sincronización y encolar Worker
            val localPost = forumDao.getPostById(localId)
            if (localPost != null) {
                forumDao.update(localPost.copy(isSynced = false))
                enqueueSyncWorker()
            }
            Resource.Success(localId)
        }
    }

    override suspend fun updatePost(forumPost: ForumPost): Resource<Unit> = withContext(Dispatchers.IO) {
        forumDao.update(forumPost.toEntity())
        
        return@withContext try {
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                val dto = forumPost.toEntity().toDto(user.id)
                supabase.postgrest["forum_posts"].update(dto) {
                    filter {
                        eq("id", forumPost.id)
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al actualizar post")
        }
    }

    override suspend fun toggleLike(postId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("post_likes", android.content.Context.MODE_PRIVATE)
            val isLiked = prefs.getBoolean("liked_$postId", false)
            
            val postEntity = forumDao.getPostById(postId) 
                ?: return@withContext Resource.Error("Post no encontrado")
            
            val updatedLikes = if (isLiked) maxOf(0, postEntity.likes - 1) else postEntity.likes + 1
            val updatedPost = postEntity.copy(likes = updatedLikes)
            forumDao.update(updatedPost)
            
            prefs.edit().putBoolean("liked_$postId", !isLiked).apply()
            
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                supabase.postgrest["forum_posts"].update(kotlinx.serialization.json.buildJsonObject {
                    put("likes", updatedPost.likes)
                }) {
                    filter { eq("id", postId) }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error al dar like")
        }
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        forumDao.deleteAll()
        Unit
    }

    override suspend fun syncRemotePosts(limit: Int, offset: Int): Int = withContext(Dispatchers.IO) {
        try {
            val remotePosts = supabase.postgrest["forum_posts"]
                .select {
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<ForumPostDto>()

            val remoteEntities = remotePosts.map { it.toEntity() }
            val remoteIds = remoteEntities.map { it.id }.toSet()

            remoteEntities.forEach { entity ->
                forumDao.insert(entity)
            }

            if (offset == 0) {
                val localSynced = forumDao.getAllSyncedPosts()
                localSynced.forEach { local ->
                    if (local.id !in remoteIds) {
                        forumDao.deletePost(local.id)
                    }
                }
            }

            remotePosts.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    override suspend fun deletePost(postId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            forumDao.deletePost(postId)
            
            val user = supabase.auth.currentUserOrNull()
            if (user != null) {
                supabase.postgrest["forum_posts"].delete {
                    filter {
                        eq("id", postId)
                        eq("user_id", user.id)
                    }
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al eliminar post")
        }
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync_worker", ExistingWorkPolicy.KEEP, request)
    }
}
