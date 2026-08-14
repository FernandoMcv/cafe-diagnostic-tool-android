package com.fcbc.tesis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.Comment
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.fcbc.tesis.domain.util.Resource
import com.fcbc.tesis.domain.repository.AnalyticsRepository
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers

data class CommentNode(
    val comment: Comment,
    val replies: List<CommentNode>
)

class ForumViewModel(
    private val getForumPostsUseCase: GetForumPostsUseCase,
    private val addForumPostUseCase: AddForumPostUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val getAllCommentsUseCase: GetAllCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleSavePostUseCase: ToggleSavePostUseCase,
    private val syncRemotePostsUseCase: com.fcbc.tesis.domain.usecase.SyncRemotePostsUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getSavedPostsUseCase: com.fcbc.tesis.domain.usecase.GetSavedPostsUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds = _savedPostIds.asStateFlow()

    private val _activeFilter = MutableStateFlow<String?>(null)
    val activeFilter = _activeFilter.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private val _hasReachedEnd = MutableStateFlow(false)
    val hasReachedEnd = _hasReachedEnd.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _isLoadingMore.value = true
                val fetched = syncRemotePostsUseCase()
                if (fetched == 0) _hasReachedEnd.value = true
            } catch (e: Exception) {
                // Silencioso: fallo de red al arrancar no debe cerrar la app.
                // Los posts locales de Room siguen disponibles.
                e.printStackTrace()
            } finally {
                _isLoadingMore.value = false
            }
        }
        viewModelScope.launch {
            try {
                when (val res = getSavedPostsUseCase()) {
                    is Resource.Success -> {
                        val ids = res.data?.map { it.id }?.toSet() ?: emptySet()
                        _savedPostIds.value = ids
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMorePosts(offset: Int) {
        if (_isLoadingMore.value || _hasReachedEnd.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val fetched = syncRemotePostsUseCase(offset = offset)
            if (fetched == 0) _hasReachedEnd.value = true
            _isLoadingMore.value = false
        }
    }

    fun setFilter(filter: String?) {
        _activeFilter.value = if (filter == "Todas") null else filter
    }

    // LÓGICA DE POSTS ENRIQUECIDOS Paginados (CON CONTADORES Y PREVIEWS)
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedPosts: Flow<PagingData<ForumPost>> = _activeFilter.flatMapLatest { filter ->
        getForumPostsUseCase(filter)
    }.cachedIn(viewModelScope)

    // Ya no expondremos posts como List
    // val posts: StateFlow<List<ForumPost>> = ...

    // --- INTERACCIONES ---

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            toggleLikeUseCase(postId)
        }
    }

    fun toggleSavePost(postId: String) {
        val currentIds = _savedPostIds.value.toMutableSet()
        val isSaved = currentIds.contains(postId)
        if (isSaved) currentIds.remove(postId) else currentIds.add(postId)
        _savedPostIds.value = currentIds
        
        viewModelScope.launch {
            val result = toggleSavePostUseCase(postId)
            if (result is Resource.Error) {
                // Rollback en caso de error crítico
                val fallbackIds = _savedPostIds.value.toMutableSet()
                if (isSaved) fallbackIds.add(postId) else fallbackIds.remove(postId)
                _savedPostIds.value = fallbackIds
                _errorMessage.value = result.message
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            deletePostUseCase(postId)
        }
    }

    fun getComments(postId: String): Flow<List<Comment>> = getCommentsUseCase(postId)

    fun getOrganizedComments(postId: String): Flow<List<CommentNode>> = getCommentsUseCase(postId)
        .map { comments ->
            val nodes = comments.map { CommentNode(it, emptyList()) }.associateBy { it.comment.id }
            val roots = mutableListOf<CommentNode>()
            val parentToChildren = mutableMapOf<String, MutableList<CommentNode>>()
            
            comments.forEach { comment ->
                val node = nodes[comment.id] ?: return@forEach
                if (comment.parentId == null) {
                    roots.add(node)
                } else {
                    parentToChildren.getOrPut(comment.parentId) { mutableListOf() }.add(node)
                }
            }
            
            fun buildTree(node: CommentNode, visited: Set<String> = emptySet()): CommentNode {
                if (visited.contains(node.comment.id)) return node
                val newVisited = visited + node.comment.id
                val children = parentToChildren[node.comment.id]?.map { buildTree(it, newVisited) } ?: emptyList()
                return node.copy(replies = children)
            }
            
            roots.map { buildTree(it) }
        }
        .flowOn(Dispatchers.Default)

    fun addComment(postId: String, author: String, content: String, parentId: String? = null) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val comment = Comment(
                postId = postId,
                parentId = parentId,
                author = author,
                content = content,
                timestamp = dateFormat.format(Date())
            )
            addCommentUseCase(comment)
        }
    }

    fun addPost(author: String, context: String, content: String, diseaseTags: List<String> = emptyList(), imageUrl: String? = null, aiConfidence: Float? = null) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val newPost = ForumPost(
                author = author,
                cropContext = context,
                content = content,
                timestamp = dateFormat.format(Date()),
                diseaseTags = diseaseTags,
                imageUrl = imageUrl,
                aiConfidence = aiConfidence
            )
            val result = addForumPostUseCase(newPost)
            if (result is Resource.Error) {
                _errorMessage.value = result.message
            } else {
                analyticsRepository.trackEvent("forum_post_created", mapOf(
                    "has_image" to (imageUrl != null).toString(),
                    "has_ai" to (aiConfidence != null).toString()
                ))
            }
        }
    }
}
