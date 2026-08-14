package com.fcbc.tesis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.ForumPost
import com.fcbc.tesis.domain.model.UserProfile
import com.fcbc.tesis.domain.usecase.GetFollowingListUseCase
import com.fcbc.tesis.domain.usecase.GetSavedPostsUseCase
import com.fcbc.tesis.domain.usecase.UpdateProfileDataUseCase
import com.fcbc.tesis.domain.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserInteractionsViewModel(
    private val getFollowingListUseCase: GetFollowingListUseCase,
    private val getSavedPostsUseCase: GetSavedPostsUseCase,
    private val updateProfileDataUseCase: UpdateProfileDataUseCase
) : ViewModel() {
    private val _followingList = MutableStateFlow<List<UserProfile>>(emptyList())
    val followingList = _followingList.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<ForumPost>>(emptyList())
    val savedPosts = _savedPosts.asStateFlow()

    fun loadFollowingList() {
        viewModelScope.launch {
            val result = getFollowingListUseCase()
            if (result is Resource.Success) _followingList.value = result.data ?: emptyList()
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            val result = getSavedPostsUseCase()
            if (result is Resource.Success) _savedPosts.value = result.data ?: emptyList()
        }
    }

    fun updateProfileData(newName: String, newLocation: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (updateProfileDataUseCase(newName, newLocation.takeIf { it.isNotBlank() }) is Resource.Success<*>) {
                onSuccess()
            }
        }
    }
}
