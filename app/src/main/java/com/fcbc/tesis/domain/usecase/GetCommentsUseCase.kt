package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow

class GetCommentsUseCase(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(postId: String): Flow<List<Comment>> {
        return commentRepository.getCommentsForPost(postId)
    }
}
