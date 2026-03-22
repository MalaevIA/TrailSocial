package com.trail2.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
import com.trail2.data.TrailRoute
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AuthRepository
import com.trail2.data.repository.UploadRepository
import com.trail2.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val myRoutes: List<TrailRoute> = emptyList(),
    val savedRoutes: List<TrailRoute> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isUploadingAvatar: Boolean = false,
    val avatarError: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tokenManager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    loadProfile()
                } else {
                    _uiState.value = ProfileUiState()
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = userRepository.getMe()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(user = result.data, isLoading = false) }
                    loadMyRoutes(result.data.id)
                    loadSavedRoutes()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isLoading = false, error = "Нет подключения") }
            }
        }
    }

    private fun loadMyRoutes(userId: String) {
        viewModelScope.launch {
            when (val result = userRepository.getUserRoutes(userId)) {
                is ApiResult.Success -> _uiState.update { it.copy(myRoutes = result.data.items) }
                else -> {}
            }
        }
    }

    fun loadSavedRoutes() {
        viewModelScope.launch {
            when (val result = userRepository.getSavedRoutes()) {
                is ApiResult.Success -> _uiState.update { it.copy(savedRoutes = result.data.items) }
                else -> {}
            }
        }
    }

    fun uploadAvatar(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, avatarError = null) }
            when (val uploadResult = uploadRepository.uploadPhoto(uri, context)) {
                is ApiResult.Success -> {
                    val avatarPath = uploadResult.data
                    when (val updateResult = userRepository.updateMe(avatarUrl = avatarPath)) {
                        is ApiResult.Success -> _uiState.update {
                            it.copy(isUploadingAvatar = false, user = updateResult.data)
                        }
                        is ApiResult.Error -> _uiState.update { it.copy(isUploadingAvatar = false, avatarError = updateResult.message) }
                        is ApiResult.NetworkError -> _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Нет подключения к интернету") }
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isUploadingAvatar = false, avatarError = uploadResult.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isUploadingAvatar = false, avatarError = "Нет подключения к интернету") }
            }
        }
    }

    fun clearAvatarError() = _uiState.update { it.copy(avatarError = null) }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
