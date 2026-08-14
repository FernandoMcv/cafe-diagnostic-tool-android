package com.fcbc.tesis.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fcbc.tesis.data.local.ChatDao
import com.fcbc.tesis.data.local.CommentDao
import com.fcbc.tesis.data.local.ForumDao
import com.fcbc.tesis.data.local.InteractionDao
import com.fcbc.tesis.data.mapper.ChatMessageDto
import com.fcbc.tesis.data.mapper.toDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.fcbc.tesis.domain.repository.StorageRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val supabase: SupabaseClient by inject()
    private val forumDao: ForumDao by inject()
    private val chatDao: ChatDao by inject()
    private val commentDao: CommentDao by inject()
    private val historyDao: com.fcbc.tesis.data.local.HistoryDao by inject()
    private val interactionDao: InteractionDao by inject()
    private val storageRepository: StorageRepository by inject()

    override suspend fun doWork(): Result {
        val user = supabase.auth.currentUserOrNull()
        if (user == null) {
            // No hay sesión activa. Detenemos el worker sin reintentos innecesarios.
            return Result.success()
        }

        var hasErrors = false

        try { syncForumPosts() } catch (e: Exception) { e.printStackTrace(); hasErrors = true }
        try { syncComments() } catch (e: Exception) { e.printStackTrace(); hasErrors = true }
        try { syncChatMessages() } catch (e: Exception) { e.printStackTrace(); hasErrors = true }
        try { syncInteractions() } catch (e: Exception) { e.printStackTrace(); hasErrors = true }
        try { syncHistory() } catch (e: Exception) { e.printStackTrace(); hasErrors = true }

        return if (hasErrors) Result.retry() else Result.success()
    }

    private suspend fun syncForumPosts() {
        val unsyncedPosts = forumDao.getUnsyncedPosts()
        for (post in unsyncedPosts) {
            val userId = post.userId ?: continue
            var finalImageUrl = post.imageUrl

            if (finalImageUrl != null && !finalImageUrl.startsWith("http")) {
                try {
                    val file = java.io.File(finalImageUrl)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val fileName = "post_${System.currentTimeMillis()}_${file.name}"
                        val result = storageRepository.uploadImage(fileName, bytes)
                        if (result is com.fcbc.tesis.domain.util.Resource.Success) {
                            finalImageUrl = result.data
                            // Conservamos el archivo local para la vista offline del historial y re-análisis
                        } else {
                            throw Exception("Fallo al subir imagen offline")
                        }
                    }
                } catch (e: Exception) {
                    throw e // Relanza para que WorkManager reintente
                }
            }

            val dto = post.toDto(userId).copy(image_url = finalImageUrl)
            supabase.postgrest["forum_posts"].insert(dto)
            forumDao.update(post.copy(imageUrl = finalImageUrl, isSynced = true))
        }
    }

    private suspend fun syncComments() {
        val unsyncedComments = commentDao.getUnsyncedComments()
        for (comment in unsyncedComments) {
            // Asumimos que los comentarios offline también pueden ser subidos
            // Pero en CommentDto necesitamos userId, por ahora la implementación actual de CommentDto puede variar.
            // Para simplificar, obtenemos currentUser o intentamos.
            val user = supabase.auth.currentUserOrNull()
            val userId = user?.id
            supabase.postgrest["forum_comments"].insert(comment.toDto(userId))
            commentDao.update(comment.copy(isSynced = true))
        }
    }

    private suspend fun syncChatMessages() {
        val unsentMessages = chatDao.getUnsentMessages()
        for (msg in unsentMessages) {
            val dto = ChatMessageDto(
                id = msg.id,
                room_id = msg.roomId,
                sender_id = msg.senderId,
                content = msg.content,
                created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date(msg.timestamp))
            )
            supabase.postgrest["chat_messages"].insert(dto)
            chatDao.insertMessage(msg.copy(isSent = true))
        }
    }

    private suspend fun syncInteractions() {
        val unsyncedSaves = interactionDao.getUnsyncedSavedPosts()
        for (save in unsyncedSaves) {
            if (save.isDeletedOffline) {
                supabase.postgrest["saved_posts"].delete {
                    filter {
                        eq("user_id", save.userId)
                        eq("post_id", save.postId)
                    }
                }
                interactionDao.deleteSavedPost(save.userId, save.postId)
            } else if (!save.isSynced) {
                supabase.postgrest["saved_posts"].insert(com.fcbc.tesis.data.repository.SavedPostDto(save.userId, save.postId))
                interactionDao.insertSavedPost(save.copy(isSynced = true))
            }
        }

        val unsyncedFollows = interactionDao.getUnsyncedFollows()
        for (follow in unsyncedFollows) {
            if (follow.isDeletedOffline) {
                supabase.postgrest["user_follows"].delete {
                    filter {
                        eq("follower_id", follow.followerId)
                        eq("following_id", follow.followingId)
                    }
                }
                interactionDao.deleteFollow(follow.followerId, follow.followingId)
            } else if (!follow.isSynced) {
                supabase.postgrest["user_follows"].insert(com.fcbc.tesis.data.repository.FollowDto(follow.followerId, follow.followingId))
                interactionDao.insertFollow(follow.copy(isSynced = true))
            }
        }
    }

    private suspend fun syncHistory() {
        val user = supabase.auth.currentUserOrNull() ?: return
        val unsyncedHistory = historyDao.getUnsyncedHistory()
        for (item in unsyncedHistory) {
            var remoteUrl = item.thumbnailPath
            if (remoteUrl.isNotBlank() && !remoteUrl.startsWith("http")) {
                try {
                    val file = java.io.File(remoteUrl)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val fileName = "history_${System.currentTimeMillis()}_${file.name}"
                        val uploadRes = storageRepository.uploadImage(fileName, bytes)
                        if (uploadRes is com.fcbc.tesis.domain.util.Resource.Success && uploadRes.data != null) {
                            remoteUrl =  uploadRes.data!!
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val dto = item.toDto(user.id).copy(thumbnail_path = remoteUrl)
            supabase.postgrest["history"].insert(dto)
            historyDao.update(item.copy(thumbnailPath = remoteUrl, isSynced = true))
        }
    }
}
