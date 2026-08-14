package com.fcbc.tesis.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Comentarios.
 */
@Entity(
    tableName = "forum_comments",
    foreignKeys = [
        ForeignKey(
            entity = ForumPostEntity::class,
            parentColumns = ["id"],
            childColumns = ["postId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("postId")]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val parentId: String? = null,
    val author: String,
    val content: String,
    val timestamp: String,
    val isSynced: Boolean = true,
    val authorAvatarUrl: String? = null
)
