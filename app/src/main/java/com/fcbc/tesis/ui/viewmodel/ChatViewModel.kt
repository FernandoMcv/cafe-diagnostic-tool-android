package com.fcbc.tesis.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fcbc.tesis.domain.model.ChatMessage
import com.fcbc.tesis.domain.model.ChatRoom
import com.fcbc.tesis.domain.usecase.GetChatRoomsUseCase
import com.fcbc.tesis.domain.usecase.GetMessagesUseCase
import com.fcbc.tesis.domain.usecase.GetOrCreateChatRoomUseCase
import com.fcbc.tesis.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.fcbc.tesis.domain.util.Resource

class ChatViewModel(
    private val getChatRoomsUseCase: GetChatRoomsUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getOrCreateChatRoomUseCase: GetOrCreateChatRoomUseCase,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessage>> = _currentMessages.asStateFlow()

    private val _currentRoomId = MutableStateFlow<String?>(null)
    val currentRoomId: StateFlow<String?> = _currentRoomId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    init {
        loadChatRooms()
    }

    private fun loadChatRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            getChatRoomsUseCase()
                .catch { e -> e.printStackTrace() }
                .collect { rooms ->
                    _chatRooms.value = rooms
                    _isLoading.value = false
                }
        }
    }

    private var messagesJob: Job? = null

    fun loadMessagesForRoom(roomId: String) {
        _currentRoomId.value = roomId
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            getMessagesUseCase(roomId)
                .catch { e -> e.printStackTrace() }
                .collect { messages ->
                    _currentMessages.value = messages
                }
        }
    }

    fun startChatWithUser(otherUserId: String, otherUserName: String, onRoomReady: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = getOrCreateChatRoomUseCase(otherUserId, otherUserName)) {
                is Resource.Success -> {
                    _isLoading.value = false
                    result.data?.let { onRoomReady(it) }
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
                else -> { _isLoading.value = false }
            }
        }
    }

    fun sendMessage(roomId: String, receiverId: String, content: String) {
        viewModelScope.launch {
            val result = sendMessageUseCase(roomId, receiverId, content)
            if (result is Resource.Error) {
                _errorMessage.value = result.message
            }
        }
    }

    fun clearCurrentRoom() {
        messagesJob?.cancel()
        messagesJob = null
        _currentRoomId.value = null
        _currentMessages.value = emptyList()
    }
}
