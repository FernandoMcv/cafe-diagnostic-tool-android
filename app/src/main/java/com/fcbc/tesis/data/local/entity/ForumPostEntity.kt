package com.fcbc.tesis.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Publicaciones del Foro.
 */
@Entity(tableName = "forum_posts")
data class ForumPostEntity(
    @PrimaryKey val id: String,
    val author: String,
    val cropContext: String,
    val content: String,
    val timestamp: String,
    val diseaseTag: String? = null,
    val imageUrl: String? = null,
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val userId: String? = null,
    val isSynced: Boolean = true,
    val location: String? = null,
    val authorAvatarUrl: String? = null,
    val aiConfidence: Float? = null
)
