package com.trail2.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.remote.ApiResult
import com.trail2.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME, CITIES, FITNESS, INTERESTS, PROFILE
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val usernameInput: String = "",
    val passwordVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val usernameError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: OnboardingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow<Boolean?>(null)
    val isOnboardingCompleted: StateFlow<Boolean?> = _isOnboardingCompleted.asStateFlow()

    val savedAnswers: StateFlow<OnboardingAnswers> = repo.savedAnswers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingAnswers())

    init {
        viewModelScope.launch {
            repo.isOnboardingCompleted.collect { completed ->
                _isOnboardingCompleted.value = completed
                if (completed) _uiState.update { it.copy(isCompleted = true) }
            }
        }
    }

    fun nextStep() {
        val next = when (_uiState.value.step) {
            OnboardingStep.WELCOME   -> OnboardingStep.CITIES
            OnboardingStep.CITIES    -> OnboardingStep.FITNESS
            OnboardingStep.FITNESS   -> OnboardingStep.INTERESTS
            OnboardingStep.INTERESTS -> OnboardingStep.PROFILE
            OnboardingStep.PROFILE   -> return
        }
        _uiState.update { it.copy(step = next, error = null) }
    }

    fun prevStep() {
        val prev = when (_uiState.value.step) {
            OnboardingStep.WELCOME   -> return
            OnboardingStep.CITIES    -> OnboardingStep.WELCOME
            OnboardingStep.FITNESS   -> OnboardingStep.CITIES
            OnboardingStep.INTERESTS -> OnboardingStep.FITNESS
            OnboardingStep.PROFILE   -> OnboardingStep.INTERESTS
        }
        _uiState.update { it.copy(step = prev, error = null) }
    }

    fun toggleCity(cityId: String) {
        val current = _uiState.value.answers.selectedCityIds.toMutableList()
        if (cityId in current) current.remove(cityId) else current.add(cityId)
        _uiState.update { it.copy(answers = it.answers.copy(selectedCityIds = current)) }
    }

    fun selectFitnessLevel(level: FitnessLevel) {
        _uiState.update { it.copy(answers = it.answers.copy(fitnessLevel = level)) }
    }

    fun toggleInterest(id: String) {
        val current = _uiState.value.answers.selectedInterestIds.toMutableList()
        if (id in current) current.remove(id) else current.add(id)
        _uiState.update { it.copy(answers = it.answers.copy(selectedInterestIds = current)) }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(nameInput = v, nameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(emailInput = v, emailError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(passwordInput = v, passwordError = null) }
    fun onUsernameChange(v: String) = _uiState.update { it.copy(usernameInput = v, usernameError = null) }
    fun togglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun finishOnboarding() {
        if (!validateProfile()) return
        val state = _uiState.value
        val finalAnswers = state.answers.copy(
            displayName = state.nameInput.trim(),
            email = state.emailInput.trim(),
            password = state.passwordInput
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repo.saveAnswers(finalAnswers)

            val username = state.usernameInput.trim().ifBlank {
                state.nameInput.trim().lowercase().replace(" ", "_")
            }
            when (val result = authRepository.signup(
                username = username,
                email = state.emailInput.trim(),
                password = state.passwordInput,
                displayName = state.nameInput.trim()
            )) {
                is ApiResult.Success -> {
                    repo.markCompleted()
                    _isOnboardingCompleted.value = true
                    _uiState.update { it.copy(isLoading = false, isCompleted = true) }
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

    fun skipOnboarding() {
        viewModelScope.launch {
            repo.markCompleted()
            _isOnboardingCompleted.value = true
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            repo.clearAll()
            _isOnboardingCompleted.value = false
            _uiState.update { OnboardingUiState() }
        }
    }

    private fun validateProfile(): Boolean {
        val s = _uiState.value
        val nameError = when {
            s.nameInput.isBlank() -> "Введите имя"
            s.nameInput.trim().length < 2 -> "Имя слишком короткое"
            else -> null
        }
        val emailError = when {
            s.emailInput.isBlank() -> "Введите email"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(s.emailInput.trim()).matches() -> "Некорректный email"
            else -> null
        }
        val passwordError = when {
            s.passwordInput.length < 6 -> "Пароль — минимум 6 символов"
            else -> null
        }
        if (nameError != null || emailError != null || passwordError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError, passwordError = passwordError) }
            return false
        }
        return true
    }

    val progress: Float get() = when (_uiState.value.step) {
        OnboardingStep.WELCOME   -> 0f
        OnboardingStep.CITIES    -> 0.25f
        OnboardingStep.FITNESS   -> 0.5f
        OnboardingStep.INTERESTS -> 0.75f
        OnboardingStep.PROFILE   -> 1f
    }
}
