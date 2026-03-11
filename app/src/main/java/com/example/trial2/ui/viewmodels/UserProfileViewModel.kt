package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.TrailRoute
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AdminRepository
import com.trail2.data.repository.ReportRepository
import com.trail2.data.repository.RouteRepository
import com.trail2.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val user: User? = null,
    val routes: List<TrailRoute> = emptyList(),
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCurrentUserAdmin: Boolean = false,
    val reportSent: Boolean = false,
    val reportError: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val routeRepository: RouteRepository,
    private val reportRepository: ReportRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = userRepository.getMe()) {
                is ApiResult.Success -> _uiState.update { it.copy(isCurrentUserAdmin = result.data.isAdmin) }
                else -> {}
            }
        }
    }

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = userRepository.getUserProfile(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(user = result.data, isLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
            loadUserRoutes(userId)
        }
    }

    private fun loadUserRoutes(userId: String) {
        viewModelScope.launch {
            when (val result = userRepository.getUserRoutes(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(routes = result.data.items) }
                else -> {}
            }
        }
    }

    fun toggleFollow() {
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            val result = if (user.isFollowing) {
                userRepository.unfollow(user.id)
            } else {
                userRepository.follow(user.id)
            }
            if (result is ApiResult.Success) {
                _uiState.update {
                    it.copy(user = user.copy(
                        isFollowing = !user.isFollowing,
                        followersCount = user.followersCount + if (user.isFollowing) -1 else 1
                    ))
                }
            }
        }
    }

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            when (val result = userRepository.getFollowers(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(followers = result.data.items) }
                else -> {}
            }
        }
    }

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            when (val result = userRepository.getFollowing(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(following = result.data.items) }
                else -> {}
            }
        }
    }

    fun reportUser(reason: String, description: String?) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            when (val result = reportRepository.createReport("user", userId, reason, description)) {
                is ApiResult.Success -> _uiState.update { it.copy(reportSent = true, reportError = null) }
                is ApiResult.Error -> {
                    if (result.code == 409) {
                        _uiState.update { it.copy(reportError = "already_sent") }
                    }
                }
                else -> {}
            }
        }
    }

    fun clearReportState() {
        _uiState.update { it.copy(reportSent = false, reportError = null) }
    }

    fun banUser() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            when (val result = adminRepository.banUser(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(user = result.data) }
                else -> {}
            }
        }
    }

    fun unbanUser() {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            when (val result = adminRepository.unbanUser(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(user = result.data) }
                else -> {}
            }
        }
    }
}
