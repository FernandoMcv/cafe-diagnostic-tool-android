package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.Comment
import kotlinx.coroutines.flow.Flow

interface CommentRepository {
    fun getAllComments(): Flow<List<Comment>>
    fun getCommentsForPost(postId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment): String
    suspend fun deleteCommentsForPost(postId: String)
}
