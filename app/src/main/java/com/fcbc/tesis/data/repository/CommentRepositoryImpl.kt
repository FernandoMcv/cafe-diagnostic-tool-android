package com.fcbc.tesis.data.repository

import com.fcbc.tesis.data.local.CommentDao
import com.fcbc.tesis.data.mapper.CommentDto
import com.fcbc.tesis.data.mapper.toDomain
import com.fcbc.tesis.data.mapper.toDto
import com.fcbc.tesis.data.mapper.toEntity
import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.repository.CommentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fcbc.tesis.worker.SyncWorker

class CommentRepositoryImpl(
    private val commentDao: CommentDao,
    private val supabase: SupabaseClient,
    private val context: Context
) : CommentRepository {

    override fun getAllComments(): Flow<List<Comment>> {
        return commentDao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCommentsForPost(postId: String): Flow<List<Comment>> {
        return commentDao.getCommentsForPost(postId).map { entities ->
            entities.map { it.toDomain() }
        }.onStart {
            syncCommentsFromRemote(postId)
        }
    }

    override suspend fun addComment(comment: Comment): String = withContext(Dispatchers.IO) {
        val localId = if (comment.id.isEmpty()) java.util.UUID.randomUUID().toString() else comment.id
        val commentToInsert = comment.toEntity().copy(id = localId)
        commentDao.insert(commentToInsert)
        
        try {
            val user = supabase.auth.currentUserOrNull()
            val dto = commentToInsert.toDto(user?.id)
            supabase.postgrest["forum_comments"].insert(dto)
        } catch (e: Exception) {
            e.printStackTrace()
            val localComment = commentDao.getCommentById(localId)
            if (localComment != null) {
                commentDao.update(localComment.copy(isSynced = false))
                enqueueSyncWorker()
            }
        }
        
        localId
    }

    private fun enqueueSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniqueWork("sync_worker", ExistingWorkPolicy.KEEP, request)
    }

    override suspend fun deleteCommentsForPost(postId: String) {
        withContext(Dispatchers.IO) {
            commentDao.deleteByPostId(postId)
        }
    }

    private suspend fun syncCommentsFromRemote(postId: String) {
        withContext(Dispatchers.IO) {
            try {
                val remoteComments = supabase.postgrest["forum_comments"]
                    .select {
                        filter {
                            eq("post_id", postId)
                        }
                    }
                    .decodeList<CommentDto>()

                val remoteEntities = remoteComments.map { it.toEntity() }
                val remoteIds = remoteEntities.map { it.id }.toSet()

                remoteEntities.forEach { entity ->
                    commentDao.insert(entity)
                }

                val localComments = commentDao.getCommentsForPost(postId).first()
                localComments.forEach { local ->
                    if (local.isSynced && local.id !in remoteIds) {
                        commentDao.delete(local)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
