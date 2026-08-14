package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.repository.ForumRepository

class SyncRemotePostsUseCase(private val repository: ForumRepository) {
    suspend operator fun invoke(limit: Int = 50, offset: Int = 0): Int = repository.syncRemotePosts(limit, offset)
}
