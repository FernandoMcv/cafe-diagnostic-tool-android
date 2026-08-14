package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.domain.model.ChatRoom
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatRooms(): Flow<List<ChatRoom>>
    fun getMessagesForRoom(roomId: String): Flow<List<ChatMessage>>
    suspend fun getOrCreateChatRoom(otherUserId: String, otherUserName: String): Resource<String>
    suspend fun sendMessage(roomId: String, receiverId: String, content: String): Resource<Unit>
    suspend fun syncChats()
    suspend fun syncMessagesForRoom(roomId: String)
}
