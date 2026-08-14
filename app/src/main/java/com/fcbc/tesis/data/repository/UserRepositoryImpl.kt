package com.fcbc.tesis.data.repository

import com.fcbc.tesis.domain.model.UserProfile
import com.fcbc.tesis.domain.repository.UserRepository
import com.fcbc.tesis.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val full_name: String,
    val avatar_url: String? = null,
    val location: String? = null
)

@Serializable
data class FollowDto(
    val follower_id: String,
    val following_id: String,
    val created_at: String? = null
)

class UserRepositoryImpl(
    private val supabase: SupabaseClient,
    private val interactionDao: com.fcbc.tesis.data.local.InteractionDao,
    private val context: android.content.Context
) : UserRepository {
    
    private fun enqueueSyncWorker() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = androidx.work.OneTimeWorkRequestBuilder<com.fcbc.tesis.worker.SyncWorker>().setConstraints(constraints).build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("sync_worker", androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    override suspend fun getPublicProfile(userId: String): Resource<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val dto = supabase.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<ProfileDto>()
            Resource.Success(UserProfile(dto.id, dto.full_name, dto.avatar_url, dto.location))
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al obtener perfil")
        }
    }

    override suspend fun followUser(userId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        
        val newEntity = com.fcbc.tesis.data.local.entity.FollowEntity(currentUser.id, userId, isSynced = false, isDeletedOffline = false)
        interactionDao.insertFollow(newEntity)

        try {
            val dto = FollowDto(currentUser.id, userId)
            supabase.postgrest["user_follows"].insert(dto)
            interactionDao.insertFollow(newEntity.copy(isSynced = true))
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            enqueueSyncWorker()
            Resource.Success(Unit)
        }
    }

    override suspend fun unfollowUser(userId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        
        val updatedEntity = com.fcbc.tesis.data.local.entity.FollowEntity(currentUser.id, userId, isSynced = false, isDeletedOffline = true)
        interactionDao.insertFollow(updatedEntity)

        try {
            supabase.postgrest["user_follows"].delete {
                filter {
                    eq("follower_id", currentUser.id)
                    eq("following_id", userId)
                }
            }
            interactionDao.deleteFollow(currentUser.id, userId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            enqueueSyncWorker()
            Resource.Success(Unit)
        }
    }

    override suspend fun isFollowing(userId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        try {
            val localFollow = interactionDao.getFollow(currentUser.id, userId)
            if (localFollow != null && !localFollow.isDeletedOffline) {
                return@withContext Resource.Success(true)
            }
            
            // Try fetch from remote
            try {
                val follows = supabase.postgrest["user_follows"]
                    .select {
                        filter {
                            eq("follower_id", currentUser.id)
                            eq("following_id", userId)
                        }
                    }.decodeList<FollowDto>()
                
                if (follows.isNotEmpty()) {
                    interactionDao.insertFollow(com.fcbc.tesis.data.local.entity.FollowEntity(currentUser.id, userId, isSynced = true))
                    return@withContext Resource.Success(true)
                } else {
                    // Only delete local if it was synced (we don't want to delete an unsynced offline save)
                    val existing = interactionDao.getFollow(currentUser.id, userId)
                    if (existing != null && existing.isSynced) {
                        interactionDao.deleteFollow(currentUser.id, userId)
                    }
                    return@withContext Resource.Success(false)
                }
            } catch (e: Exception) {
                // If offline, rely on local result
                return@withContext Resource.Success(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error")
        }
    }

    override suspend fun getFollowingList(): Resource<List<UserProfile>> = withContext(Dispatchers.IO) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        try {
            val localFollows = interactionDao.getFollowingForUser(currentUser.id)
            var followingIds = localFollows.map { it.followingId }
            
            try {
                val remoteFollows = supabase.postgrest["user_follows"]
                    .select {
                        filter { eq("follower_id", currentUser.id) }
                    }.decodeList<FollowDto>()
                    
                remoteFollows.forEach { dto ->
                    if (interactionDao.getFollow(currentUser.id, dto.following_id) == null) {
                        interactionDao.insertFollow(com.fcbc.tesis.data.local.entity.FollowEntity(currentUser.id, dto.following_id, isSynced = true))
                    }
                }
                followingIds = remoteFollows.map { it.following_id }
            } catch (e: Exception) {}

            if (followingIds.isEmpty()) return@withContext Resource.Success(emptyList())

            val profilesDto = supabase.postgrest["profiles"]
                .select {
                    filter { isIn("id", followingIds) }
                }.decodeList<ProfileDto>()

            val profiles = profilesDto.map { UserProfile(it.id, it.full_name, it.avatar_url, it.location) }
            Resource.Success(profiles)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al obtener lista de seguidos")
        }
    }

    override suspend fun updateProfileData(newName: String, newLocation: String?): Resource<Unit> = withContext(Dispatchers.IO) {
        val currentUser = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("No auth")
        try {
            val updateMap = mutableMapOf<String, String>()
            updateMap["full_name"] = newName
            if (newLocation != null) updateMap["location"] = newLocation

            supabase.postgrest["profiles"].update(
                updateMap
            ) {
                filter { eq("id", currentUser.id) }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Error al actualizar nombre")
        }
    }
}
