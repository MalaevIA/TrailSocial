package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
import com.trail2.data.Notification
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    loadUnreadCount()
                } else {
                    _uiState.value = NotificationUiState()
                }
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = notificationRepository.getNotifications()) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(notifications = result.data.items, isLoading = false)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            when (val result = notificationRepository.getUnreadCount()) {
                is ApiResult.Success -> _uiState.update { it.copy(unreadCount = result.data) }
                else -> {}
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (notificationRepository.markAllRead()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications.map { n -> n.copy(isRead = true) },
                            unreadCount = 0
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun markRead(notificationId: String) {
        viewModelScope.launch {
            when (notificationRepository.markRead(notificationId)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            notifications = it.notifications.map { n ->
                                if (n.id == notificationId) n.copy(isRead = true) else n
                            },
                            unreadCount = (it.unreadCount - 1).coerceAtLeast(0)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}
