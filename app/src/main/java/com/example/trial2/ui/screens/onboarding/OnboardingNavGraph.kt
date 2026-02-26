package com.trail2.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.onboarding.OnboardingStep
import com.trail2.onboarding.OnboardingViewModel

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/OnboardingNavGraph.kt
//
// Это точка входа онбординга. Вызывается из
// AppNavigation (или MainActivity) один раз.
// Когда onboardingCompleted → true, вызывает
// onFinished() и приложение переходит к основному экрану.
// ─────────────────────────────────────────────

@Composable
fun OnboardingNavGraph(
    onFinished: () -> Unit,                  // колбэк → переход к основному приложению
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Когда онбординг завершён → уведомляем родителя
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onFinished()
    }

    // Анимация смены шагов
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                // Вперёд
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
            } else {
                // Назад
                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300)) togetherWith
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            }
        },
        label = "onboarding_step"
    ) { step ->
        when (step) {
            OnboardingStep.WELCOME -> WelcomeScreen(
                onContinue = { viewModel.nextStep() }
            )

            OnboardingStep.CITIES -> CitySurveyScreen(
                selectedCityIds = state.answers.selectedCityIds,
                progress = viewModel.progress,
                onToggleCity = { viewModel.toggleCity(it) },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() }
            )

            OnboardingStep.FITNESS -> FitnessScreen(
                selectedLevel = state.answers.fitnessLevel,
                progress = viewModel.progress,
                onSelectLevel = { viewModel.selectFitnessLevel(it) },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() }
            )

            OnboardingStep.INTERESTS -> InterestsScreen(
                selectedInterestIds = state.answers.selectedInterestIds,
                progress = viewModel.progress,
                onToggleInterest = { viewModel.toggleInterest(it) },
                onNext = { viewModel.nextStep() },
                onBack = { viewModel.prevStep() }
            )

            OnboardingStep.PROFILE -> ProfileSetupScreen(
                state = state,
                progress = viewModel.progress,
                onNameChange = { viewModel.onNameChange(it) },
                onEmailChange = { viewModel.onEmailChange(it) },
                onPasswordChange = { viewModel.onPasswordChange(it) },
                onTogglePasswordVisible = { viewModel.togglePasswordVisible() },
                onFinish = { viewModel.finishOnboarding() },
                onSkip = { viewModel.skipOnboarding() },
                onBack = { viewModel.prevStep() }
            )
        }
    }
}
