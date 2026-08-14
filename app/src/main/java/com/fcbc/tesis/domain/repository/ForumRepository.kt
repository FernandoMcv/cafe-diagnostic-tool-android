package com.fcbc.tesis.domain.repository

import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface ForumRepository {
    fun getAllPosts(): Flow<List<ForumPost>>
    fun getPagedPosts(filter: String?): Flow<PagingData<ForumPost>>
    fun getPostsByUserId(userId: String): Flow<List<ForumPost>>
    suspend fun getSavedPosts(): Resource<List<ForumPost>>
    suspend fun toggleSavePost(postId: String): Resource<Boolean>
    suspend fun addPost(forumPost: ForumPost): Resource<String>
    suspend fun updatePost(forumPost: ForumPost): Resource<Unit>
    suspend fun toggleLike(postId: String): Resource<Unit>
    suspend fun deletePost(postId: String): Resource<Unit>
    suspend fun clearAll()
    suspend fun syncRemotePosts(limit: Int = 50, offset: Int = 0): Int
}
