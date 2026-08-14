package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow

class GetAllCommentsUseCase(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(): Flow<List<Comment>> {
        return commentRepository.getAllComments()
    }
}
