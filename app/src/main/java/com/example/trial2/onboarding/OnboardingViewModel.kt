package com.trail2.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────
// Шаги онбординга
// ─────────────────────────────────────────────
enum class OnboardingStep {
    WELCOME,
    CITIES,
    FITNESS,
    INTERESTS,
    PROFILE
}

// ─────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val isLoading: Boolean = false,
    val isCompleted: Boolean = false,
    val error: String? = null,

    // Промежуточные поля формы профиля (не попадают в answers до финального сохранения)
    val nameInput: String = "",
    val emailInput: String = "",
    val passwordInput: String = "",
    val passwordVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────
class OnboardingViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = OnboardingRepository(app)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Проверяем при запуске — может онбординг уже пройден
    init {
        viewModelScope.launch {
            repo.isOnboardingCompleted.first().let { completed ->
                if (completed) _uiState.update { it.copy(isCompleted = true) }
            }
        }
    }

    // ── Навигация по шагам ──────────────────

    fun nextStep() {
        val current = _uiState.value.step
        val next = when (current) {
            OnboardingStep.WELCOME   -> OnboardingStep.CITIES
            OnboardingStep.CITIES    -> OnboardingStep.FITNESS
            OnboardingStep.FITNESS   -> OnboardingStep.INTERESTS
            OnboardingStep.INTERESTS -> OnboardingStep.PROFILE
            OnboardingStep.PROFILE   -> return // финал — handled by finishOnboarding()
        }
        _uiState.update { it.copy(step = next, error = null) }
    }

    fun prevStep() {
        val current = _uiState.value.step
        val prev = when (current) {
            OnboardingStep.WELCOME   -> return
            OnboardingStep.CITIES    -> OnboardingStep.WELCOME
            OnboardingStep.FITNESS   -> OnboardingStep.CITIES
            OnboardingStep.INTERESTS -> OnboardingStep.FITNESS
            OnboardingStep.PROFILE   -> OnboardingStep.INTERESTS
        }
        _uiState.update { it.copy(step = prev, error = null) }
    }

    // ── Города ──────────────────────────────

    fun toggleCity(cityId: String) {
        val current = _uiState.value.answers.selectedCityIds.toMutableList()
        if (cityId in current) current.remove(cityId) else current.add(cityId)
        _uiState.update { it.copy(answers = it.answers.copy(selectedCityIds = current)) }
    }

    // ── Уровень подготовки ──────────────────

    fun selectFitnessLevel(level: FitnessLevel) {
        _uiState.update { it.copy(answers = it.answers.copy(fitnessLevel = level)) }
    }

    // ── Интересы ────────────────────────────

    fun toggleInterest(id: String) {
        val current = _uiState.value.answers.selectedInterestIds.toMutableList()
        if (id in current) current.remove(id) else current.add(id)
        _uiState.update { it.copy(answers = it.answers.copy(selectedInterestIds = current)) }
    }

    // ── Поле профиля ────────────────────────

    fun onNameChange(v: String) = _uiState.update { it.copy(nameInput = v, nameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(emailInput = v, emailError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(passwordInput = v, passwordError = null) }
    fun togglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    // ── Финал: сохранить и завершить ────────

    fun finishOnboarding() {
        if (!validateProfile()) return

        val state = _uiState.value
        val finalAnswers = state.answers.copy(
            displayName = state.nameInput.trim(),
            email       = state.emailInput.trim(),
            password    = state.passwordInput     // только в памяти
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Сохранить локально
            repo.saveAnswers(finalAnswers)

            // 2. Отправить на сервер (ЗАКОММЕНТИРОВАНО — нет бэкенда)
            /*
            val serverResult = repo.sendToServer(finalAnswers)
            if (serverResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Не удалось зарегистрироваться: ${serverResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }
            */

            // 3. Пометить онбординг завершённым
            repo.markCompleted()

            _uiState.update { it.copy(isLoading = false, isCompleted = true) }
        }
    }

    /** Пропустить регистрацию (гостевой режим) */
    fun skipOnboarding() {
        viewModelScope.launch {
            repo.markCompleted()
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    // ── Валидация ────────────────────────────

    private fun validateProfile(): Boolean {
        val s = _uiState.value
        var valid = true

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
            valid = false
        }
        return valid
    }

    // Прогресс 0..1 для индикатора
    val progress: Float get() = when (_uiState.value.step) {
        OnboardingStep.WELCOME   -> 0f
        OnboardingStep.CITIES    -> 0.25f
        OnboardingStep.FITNESS   -> 0.5f
        OnboardingStep.INTERESTS -> 0.75f
        OnboardingStep.PROFILE   -> 1f
    }
}
