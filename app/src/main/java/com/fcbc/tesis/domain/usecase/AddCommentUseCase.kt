package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.repository.CommentRepository

class AddCommentUseCase(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(comment: Comment): String {
        return commentRepository.addComment(comment)
    }
}
