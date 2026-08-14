package com.fcbc.tesis.data.repository

import com.fcbc.tesis.data.local.ChatDao
import com.fcbc.tesis.data.mapper.ChatMessageDto
import com.fcbc.tesis.data.mapper.ChatRoomDto
import com.fcbc.tesis.data.mapper.toDomain
import com.fcbc.tesis.data.mapper.toEntity
import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.domain.model.ChatRoom
import com.fcbc.tesis.domain.repository.ChatRepository
import com.fcbc.tesis.domain.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.UUID

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fcbc.tesis.worker.SyncWorker

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val supabase: SupabaseClient,
    private val context: Context
) : ChatRepository {

    override fun getChatRooms(): Flow<List<ChatRoom>> {
        return chatDao.getAllChatRooms().map { entities ->
            entities.map { it.toDomain() }
        }.onStart {
            syncChats()
        }
    }

    override fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForRoom(roomId).map { entities ->
            entities.map { it.toDomain() }
        }.onStart {
            syncMessagesForRoom(roomId)
        }
    }

    override suspend fun getOrCreateChatRoom(otherUserId: String, otherUserName: String): Resource<String> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("Not logged in")
        val currentUserId = user.id

        // Try local first
        val existingLocal = chatDao.getChatRoomByParticipantId(otherUserId)
        if (existingLocal != null) return@withContext Resource.Success(existingLocal.id)

        // Try remote or create
        try {
            // Because our query uses an OR condition to find if a room exists, it's a bit tricky with simple postgrest eq.
            // But Supabase handles UNIQUE(user1_id, user2_id). We can just try to insert and catch if exists, 
            // but we need the room ID. So we'll fetch all our rooms from remote.
            
            val remoteRooms = supabase.postgrest["chat_rooms"]
                .select()
                .decodeList<ChatRoomDto>()
            
            val existingRemote = remoteRooms.find { it.user1_id == otherUserId || it.user2_id == otherUserId }
            
            if (existingRemote != null) {
                val roomEntity = com.fcbc.tesis.data.local.entity.ChatRoomEntity(
                    id = existingRemote.id,
                    participantUserId = otherUserId,
                    participantName = otherUserName, // Remote doesn't store name directly, we use the local known name
                    lastMessage = existingRemote.last_message,
                    updatedAt = System.currentTimeMillis()
                )
                chatDao.insertRoom(roomEntity)
                return@withContext Resource.Success(existingRemote.id)
            }

            // Create new
            val newRoomId = UUID.randomUUID().toString()
            val newRoomDto = ChatRoomDto(
                id = newRoomId,
                user1_id = currentUserId,
                user2_id = otherUserId,
                last_message = null,
                updated_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            )
            
            supabase.postgrest["chat_rooms"].insert(newRoomDto)
            
            val newRoomEntity = com.fcbc.tesis.data.local.entity.ChatRoomEntity(
                id = newRoomId,
                participantUserId = otherUserId,
                participantName = otherUserName,
                lastMessage = null,
                updatedAt = System.currentTimeMillis()
            )
            chatDao.insertRoom(newRoomEntity)
            return@withContext Resource.Success(newRoomId)

        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.message ?: ""
            if (msg.contains("violates foreign key constraint")) {
                return@withContext Resource.Error("Este perfil no existe o ya no está disponible.")
            }
            return@withContext Resource.Error("Error de conexión al iniciar el chat.")
        }
    }

    override suspend fun sendMessage(roomId: String, receiverId: String, content: String): Resource<Unit> = withContext(Dispatchers.IO) {
        val user = supabase.auth.currentUserOrNull() ?: return@withContext Resource.Error("Not logged in")
        val currentUserId = user.id

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Local insert
        val chatMessage = ChatMessage(
            id = messageId,
            roomId = roomId,
            senderId = currentUserId,
            content = content,
            timestamp = timestamp,
            isSent = false
        )
        chatDao.insertMessage(chatMessage.toEntity())

        // Remote insert
        return@withContext try {
            val dto = ChatMessageDto(
                id = messageId,
                room_id = roomId,
                sender_id = currentUserId,
                content = content,
                created_at = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
            )
            supabase.postgrest["chat_messages"].insert(dto)
            
            // Mark as sent
            chatDao.insertMessage(chatMessage.copy(isSent = true).toEntity())
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            val msg = e.message ?: ""
            if (msg.contains("violates foreign key constraint")) {
                chatDao.insertMessage(chatMessage.copy(isSent = false).toEntity())
                return@withContext Resource.Error("No se pudo enviar. El perfil no existe.")
            }
            chatDao.insertMessage(chatMessage.copy(isSent = false).toEntity())
            enqueueSyncWorker()
            Resource.Success(Unit)
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

    override suspend fun syncChats() = withContext(Dispatchers.IO) {
        try {
            val user = supabase.auth.currentUserOrNull() ?: return@withContext
            val currentUserId = user.id
            
            val remoteRooms = supabase.postgrest["chat_rooms"]
                .select()
                .decodeList<ChatRoomDto>()

            remoteRooms.forEach { dto ->
                // Determine the other user's ID
                val otherUserId = if (dto.user1_id == currentUserId) dto.user2_id else dto.user1_id
                
                // We keep the local name if it exists, otherwise just default "Usuario"
                val existingLocal = chatDao.getChatRoomByParticipantId(otherUserId)
                val participantName = existingLocal?.participantName ?: "Usuario"
                
                val roomEntity = com.fcbc.tesis.data.local.entity.ChatRoomEntity(
                    id = dto.id,
                    participantUserId = otherUserId,
                    participantName = participantName,
                    lastMessage = dto.last_message,
                    updatedAt = System.currentTimeMillis() // Simplified parse
                )
                chatDao.insertRoom(roomEntity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun syncMessagesForRoom(roomId: String) = withContext(Dispatchers.IO) {
        try {
            val remoteMessages = supabase.postgrest["chat_messages"]
                .select {
                    filter { eq("room_id", roomId) }
                }
                .decodeList<ChatMessageDto>()

            remoteMessages.forEach { dto ->
                val entity = com.fcbc.tesis.data.local.entity.ChatMessageEntity(
                    id = dto.id,
                    roomId = dto.room_id,
                    senderId = dto.sender_id,
                    content = dto.content,
                    timestamp = System.currentTimeMillis(), // We could parse dto.created_at but it's fine for demo
                    isSent = true
                )
                chatDao.insertMessage(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
