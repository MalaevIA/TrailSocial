package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.auth.TokenManager
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val emailInput: String = "",
    val passwordInput: String = "",
    val passwordVisible: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(emailInput = value, error = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(passwordInput = value, error = null) }
    fun togglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun login() {
        val state = _uiState.value
        if (state.emailInput.isBlank() || state.passwordInput.isBlank()) {
            _uiState.update { it.copy(error = "Заполните все поля") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(state.emailInput.trim(), state.passwordInput)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update { it.copy(isLoading = false, error = "Нет подключения к интернету") }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}
