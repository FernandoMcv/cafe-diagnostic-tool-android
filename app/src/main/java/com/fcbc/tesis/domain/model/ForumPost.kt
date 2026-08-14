package com.fcbc.tesis.domain.model

/**
 * Modelo de Dominio puro para el Foro.
 */
data class ForumPost(
    val id: String = "",
    val author: String,
    val cropContext: String,
    val content: String,
    val timestamp: String,
    val diseaseTags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val likes: Int = 0,
    val commentsCount: Int = 0,
    val lastCommentAuthor: String? = null,
    val lastCommentPreview: String? = null,
    val userId: String? = null,
    val location: String? = null,
    val authorAvatarUrl: String? = null,
    val aiConfidence: Float? = null
)
