package com.fcbc.tesis.data.mapper

import com.fcbc.tesis.data.local.entity.ChatMessageEntity
import com.fcbc.tesis.data.local.entity.ChatRoomEntity
import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.domain.model.ChatRoom
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoomDto(
    val id: String,
    val user1_id: String,
    val user2_id: String,
    val last_message: String? = null,
    val updated_at: String
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val room_id: String,
    val sender_id: String,
    val content: String,
    val created_at: String
)

// Extension functions for mapping
fun ChatRoomEntity.toDomain() = ChatRoom(id, participantUserId, participantName, lastMessage, updatedAt)
fun ChatRoom.toEntity() = ChatRoomEntity(id, participantUserId, participantName, lastMessage, updatedAt)

fun ChatMessageEntity.toDomain() = ChatMessage(id, roomId, senderId, content, timestamp, isSent)
fun ChatMessage.toEntity() = ChatMessageEntity(id, roomId, senderId, content, timestamp, isSent)
