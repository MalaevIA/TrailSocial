package com.trail2.ai_route

// ══════════════════════════════════════════════════════════════
// Файл: ai_route/RouteBuilderViewModel.kt
// ══════════════════════════════════════════════════════════════

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class BuilderStep(val title: String, val stepNum: Int) {
    GOAL("Цель и компания", 1),
    PARAMS("Параметры", 2),
    TERRAIN("Местность", 3),
    DETAILS("Детали", 4),
    EXTRAS("Доп. опции", 5)
}

sealed class RouteBuilderScreenState {
    object Form : RouteBuilderScreenState()
    object Loading : RouteBuilderScreenState()
    data class Result(val route: GeneratedRoute) : RouteBuilderScreenState()
    data class Error(val message: String) : RouteBuilderScreenState()
}

data class RouteBuilderUiState(
    val form: RouteBuilderForm = RouteBuilderForm(),
    val currentStep: BuilderStep = BuilderStep.GOAL,
    val screenState: RouteBuilderScreenState = RouteBuilderScreenState.Form,
    // Валидация
    val stepError: String? = null
)

class RouteBuilderViewModel : ViewModel() {

    private val repo = RouteBuilderRepository()
    private val _state = MutableStateFlow(RouteBuilderUiState())
    val state: StateFlow<RouteBuilderUiState> = _state.asStateFlow()

    val totalSteps = BuilderStep.entries.size
    val progress get() = (_state.value.currentStep.stepNum.toFloat()) / totalSteps

    // ── Навигация по шагам ───────────────────────────────────

    fun nextStep() {
        if (!validateCurrentStep()) return
        val steps = BuilderStep.entries
        val idx = steps.indexOf(_state.value.currentStep)
        if (idx < steps.lastIndex) {
            _state.update { it.copy(currentStep = steps[idx + 1], stepError = null) }
        } else {
            generate() // последний шаг → генерируем
        }
    }

    fun prevStep() {
        val steps = BuilderStep.entries
        val idx = steps.indexOf(_state.value.currentStep)
        if (idx > 0) _state.update { it.copy(currentStep = steps[idx - 1], stepError = null) }
    }

    fun resetToForm() {
        _state.update { it.copy(screenState = RouteBuilderScreenState.Form, stepError = null) }
    }

    // ── Обновление формы ─────────────────────────────────────

    fun setPurpose(v: TripPurpose) = updateForm { copy(purpose = v) }
    fun setGroupType(v: GroupType) = updateForm { copy(groupType = v) }
    fun setDuration(v: TripDuration) = updateForm { copy(duration = v) }
    fun setDistance(v: TripDistance) = updateForm { copy(distance = v) }
    fun setPace(v: Pace) = updateForm { copy(pace = v) }
    fun toggleTerrain(v: Terrain) = updateForm {
        val t = terrains.toMutableSet()
        if (v in t) t.remove(v) else t.add(v)
        copy(terrains = t)
    }
    fun setStartPoint(v: String) = updateForm { copy(startPoint = v) }
    fun setMustSee(v: String) = updateForm { copy(mustSeeWishes = v) }
    fun setAvoid(v: String) = updateForm { copy(avoidWishes = v) }
    fun togglePets() = updateForm { copy(hasPets = !hasPets) }
    fun toggleCafe() = updateForm { copy(needsCafe = !needsCafe) }
    fun toggleWC() = updateForm { copy(needsWC = !needsWC) }
    fun toggleAccessible() = updateForm { copy(accessible = !accessible) }

    private fun updateForm(block: RouteBuilderForm.() -> RouteBuilderForm) {
        _state.update { it.copy(form = it.form.block(), stepError = null) }
    }

    // ── Генерация маршрута ───────────────────────────────────

    private fun generate() {
        viewModelScope.launch {
            _state.update { it.copy(screenState = RouteBuilderScreenState.Loading) }
            val result = repo.generateRoute(_state.value.form)
            _state.update {
                it.copy(
                    screenState = if (result.isSuccess)
                        RouteBuilderScreenState.Result(result.getOrThrow())
                    else
                        RouteBuilderScreenState.Error(
                            result.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                        )
                )
            }
        }
    }

    // ── Валидация ────────────────────────────────────────────

    private fun validateCurrentStep(): Boolean {
        val f = _state.value.form
        val error = when (_state.value.currentStep) {
            BuilderStep.GOAL -> when {
                f.purpose == null -> "Выберите цель прогулки"
                f.groupType == null -> "Укажите, с кем идёте"
                else -> null
            }
            BuilderStep.PARAMS -> when {
                f.duration == null -> "Укажите желаемую длительность"
                f.distance == null -> "Укажите примерную дистанцию"
                else -> null
            }
            BuilderStep.TERRAIN -> if (f.terrains.isEmpty()) "Выберите хотя бы один тип местности" else null
            BuilderStep.DETAILS -> if (f.startPoint.isBlank()) "Укажите точку старта" else null
            BuilderStep.EXTRAS -> null // опционально
        }
        _state.update { it.copy(stepError = error) }
        return error == null
    }
}
