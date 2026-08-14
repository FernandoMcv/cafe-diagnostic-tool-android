package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.domain.model.ChatRoom
import com.fcbc.tesis.domain.repository.ChatRepository
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.Flow

class GetChatRoomsUseCase(private val repository: ChatRepository) {
    operator fun invoke(): Flow<List<ChatRoom>> = repository.getChatRooms()
}

class GetMessagesUseCase(private val repository: ChatRepository) {
    operator fun invoke(roomId: String): Flow<List<ChatMessage>> = repository.getMessagesForRoom(roomId)
}

class GetOrCreateChatRoomUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(otherUserId: String, otherUserName: String): Resource<String> {
        return repository.getOrCreateChatRoom(otherUserId, otherUserName)
    }
}

class SendMessageUseCase(private val repository: ChatRepository) {
    suspend operator fun invoke(roomId: String, receiverId: String, content: String): Resource<Unit> {
        return repository.sendMessage(roomId, receiverId, content)
    }
}
