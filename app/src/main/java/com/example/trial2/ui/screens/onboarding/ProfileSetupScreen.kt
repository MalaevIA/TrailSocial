package com.trail2.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import com.trail2.R

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.onboarding.OnboardingUiState
import com.trail2.ui.components.onboarding.OnboardingButton
import com.trail2.ui.components.onboarding.OnboardingTopBar
import com.trail2.ui.components.onboarding.StepTitle

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/ProfileSetupScreen.kt
// ─────────────────────────────────────────────

@Composable
fun ProfileSetupScreen(
    state: OnboardingUiState,
    progress: Float,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit = {},
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisible: () -> Unit,
    onFinish: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        OnboardingTopBar(progress = progress, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            StepTitle(
                emoji = "👤",
                title = stringResource(R.string.setup_title),
                subtitle = stringResource(R.string.setup_subtitle)
            )

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                // ── Имя ─────────────────────────
                OnboardingTextField(
                    value = state.nameInput,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.setup_name),
                    placeholder = stringResource(R.string.setup_name_hint),
                    icon = Icons.Outlined.Person,
                    error = state.nameError,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(14.dp))

                // ── Имя пользователя ─────────────
                OnboardingTextField(
                    value = state.usernameInput,
                    onValueChange = onUsernameChange,
                    label = stringResource(R.string.setup_username),
                    placeholder = stringResource(R.string.setup_username_hint),
                    icon = Icons.Outlined.AlternateEmail,
                    error = state.usernameError,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(14.dp))

                // ── Email ────────────────────────
                OnboardingTextField(
                    value = state.emailInput,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.setup_email),
                    placeholder = stringResource(R.string.setup_email_hint),
                    icon = Icons.Outlined.Email,
                    error = state.emailError,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )

                Spacer(Modifier.height(14.dp))

                // ── Пароль ───────────────────────
                OutlinedTextField(
                    value = state.passwordInput,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.setup_password)) },
                    placeholder = { Text(stringResource(R.string.setup_password_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisible) {
                            Icon(
                                if (state.passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = stringResource(R.string.setup_show_password)
                            )
                        }
                    },
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    isError = state.passwordError != null,
                    supportingText = state.passwordError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // ── Глобальная ошибка ────────────
                if (state.error != null) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            state.error,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Сводка выбранного ───────────
                SurveySummaryCard(state = state)
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── Нижние кнопки ────────────────────
        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                OnboardingButton(
                    text = if (state.isLoading) stringResource(R.string.setup_registering) else stringResource(R.string.setup_register),
                    onClick = onFinish,
                    enabled = !state.isLoading
                )

                Spacer(Modifier.height(10.dp))

                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.setup_skip),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(icon, null) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// Краткий итог выбранного в опросе
@Composable
private fun SurveySummaryCard(state: OnboardingUiState) {
    val hasSomething = state.answers.selectedCityIds.isNotEmpty()
            || state.answers.fitnessLevel != null
            || state.answers.selectedInterestIds.isNotEmpty()

    if (!hasSomething) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(stringResource(R.string.setup_preferences), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            if (state.answers.selectedCityIds.isNotEmpty()) {
                SummaryRow(emoji = "📍", text = stringResource(R.string.setup_cities_selected, state.answers.selectedCityIds.size))
            }
            if (state.answers.fitnessLevel != null) {
                SummaryRow(emoji = state.answers.fitnessLevel.emoji, text = state.answers.fitnessLevel.label)
            }
            if (state.answers.selectedInterestIds.isNotEmpty()) {
                SummaryRow(emoji = "✨", text = stringResource(R.string.setup_interests_selected, state.answers.selectedInterestIds.size))
            }
        }
    }
}

@Composable
private fun SummaryRow(emoji: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 14.sp, modifier = Modifier.width(24.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
