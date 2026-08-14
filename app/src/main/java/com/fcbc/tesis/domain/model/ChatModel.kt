package com.fcbc.tesis.domain.model

data class ChatRoom(
    val id: String,
    val participantUserId: String,
    val participantName: String,
    val lastMessage: String?,
    val updatedAt: Long
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean = true
)
