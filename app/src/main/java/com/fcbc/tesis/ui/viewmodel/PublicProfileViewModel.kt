package com.fcbc.tesis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.model.UserProfile
import com.fcbc.tesis.domain.usecase.*
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PublicProfileViewModel(
    private val getPublicProfileUseCase: GetPublicProfileUseCase,
    private val isFollowingUseCase: IsFollowingUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val getPostsByUserIdUseCase: GetPostsByUserIdUseCase
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing = _isFollowing.asStateFlow()

    private val _userPosts = MutableStateFlow<List<ForumPost>>(emptyList())
    val userPosts = _userPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var postsJob: kotlinx.coroutines.Job? = null

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Get profile
            val profileResult = getPublicProfileUseCase(userId)
            if (profileResult is Resource.Success) {
                _profile.value = profileResult.data
            }

            // Get follow status
            val followResult = isFollowingUseCase(userId)
            if (followResult is Resource.Success) {
                _isFollowing.value = followResult.data ?: false
            }
            _isLoading.value = false

            // Prevenir fuga de corrutina (Memory Leak)
            postsJob?.cancel()
            postsJob = viewModelScope.launch {
                getPostsByUserIdUseCase(userId).collect { posts ->
                    _userPosts.value = posts
                }
            }
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            if (_isFollowing.value) {
                if (unfollowUserUseCase(userId) is Resource.Success) {
                    _isFollowing.value = false
                }
            } else {
                if (followUserUseCase(userId) is Resource.Success) {
                    _isFollowing.value = true
                }
            }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            followUserUseCase(userId)
        }
    }
}
