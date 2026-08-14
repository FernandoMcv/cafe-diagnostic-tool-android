package com.fcbc.tesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Salas de Chat (Conversaciones 1 a 1).
 */
@Entity(tableName = "chat_rooms")
data class ChatRoomEntity(
    @PrimaryKey val id: String,
    val participantUserId: String,
    val participantName: String,
    val lastMessage: String?,
    val updatedAt: Long
)
