package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.repository.ForumRepository
import com.fcbc.tesis.domain.util.Resource

class AddForumPostUseCase(
    private val forumRepository: ForumRepository
) {
    suspend operator fun invoke(forumPost: ForumPost): Resource<String> {
        return forumRepository.addPost(forumPost)
    }
}
