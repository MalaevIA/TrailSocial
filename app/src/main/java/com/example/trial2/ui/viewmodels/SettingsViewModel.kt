package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AuthRepository
import com.trail2.data.repository.SettingsRepository
import com.trail2.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isChangingEmail: Boolean = false,
    val changeEmailError: String? = null,
    val changeEmailSuccess: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = settingsRepository.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val language: StateFlow<String> = settingsRepository.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "ru")

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun changeEmail(newEmail: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingEmail = true, changeEmailError = null, changeEmailSuccess = false) }
            when (val result = userRepository.changeEmail(newEmail, password)) {
                is ApiResult.Success -> _uiState.update { it.copy(isChangingEmail = false, changeEmailSuccess = true) }
                is ApiResult.Error -> _uiState.update { it.copy(isChangingEmail = false, changeEmailError = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isChangingEmail = false, changeEmailError = "Нет подключения к интернету") }
            }
        }
    }

    fun resetChangeEmailState() {
        _uiState.update { it.copy(changeEmailError = null, changeEmailSuccess = false) }
    }

    fun deleteAccount(currentPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, deleteAccountError = null) }
            when (val result = userRepository.deleteAccount(currentPassword)) {
                is ApiResult.Success -> {
                    authRepository.logout()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isDeletingAccount = false, deleteAccountError = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isDeletingAccount = false, deleteAccountError = "Нет подключения к интернету") }
            }
        }
    }

    fun resetDeleteAccountError() {
        _uiState.update { it.copy(deleteAccountError = null) }
    }
}
