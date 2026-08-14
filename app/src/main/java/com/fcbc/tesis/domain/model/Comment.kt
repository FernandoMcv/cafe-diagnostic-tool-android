package com.fcbc.tesis.domain.model

/**
 * Modelo de Dominio para Comentarios con soporte para hilos anidados.
 */
data class Comment(
    val id: String = "",
    val postId: String,
    val parentId: String? = null, // ID del comentario al que se responde
    val author: String,
    val content: String,
    val timestamp: String,
    val authorAvatarUrl: String? = null
)
