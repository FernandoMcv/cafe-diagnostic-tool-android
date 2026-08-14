package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.ForumRepository
import com.fcbc.tesis.domain.util.Resource

class ToggleLikeUseCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(postId: String): Resource<Unit> {
        return forumRepository.toggleLike(postId)
    }
}
