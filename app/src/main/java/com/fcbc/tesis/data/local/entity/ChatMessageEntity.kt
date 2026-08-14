package com.fcbc.tesis.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Mensajes de Chat.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatRoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roomId")]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean = true // Para Offline-First
)
