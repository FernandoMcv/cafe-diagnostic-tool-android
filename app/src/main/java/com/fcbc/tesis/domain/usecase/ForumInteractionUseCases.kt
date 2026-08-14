package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.ForumRepository

class GetPostsByUserIdUseCase(private val repository: ForumRepository) {
    operator fun invoke(userId: String) = repository.getPostsByUserId(userId)
}
class GetSavedPostsUseCase(private val repository: ForumRepository) {
    suspend operator fun invoke() = repository.getSavedPosts()
}
class ToggleSavePostUseCase(private val repository: ForumRepository) {
    suspend operator fun invoke(postId: String) = repository.toggleSavePost(postId)
}
class DeletePostUseCase(private val repository: ForumRepository) {
    suspend operator fun invoke(postId: String) = repository.deletePost(postId)
}
