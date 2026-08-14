package com.fcbc.tesis.domain.usecase

import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.repository.ForumRepository
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

class GetForumPostsUseCase(
    private val forumRepository: ForumRepository
) {
    operator fun invoke(filter: String? = null): Flow<PagingData<ForumPost>> {
        return forumRepository.getPagedPosts(filter)
    }
}
