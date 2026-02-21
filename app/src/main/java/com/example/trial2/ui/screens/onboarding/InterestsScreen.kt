package com.trail2.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.onboarding.OnboardingData
import com.trail2.ui.components.onboarding.OnboardingButton
import com.trail2.ui.components.onboarding.OnboardingTopBar
import com.trail2.ui.components.onboarding.SelectableChip
import com.trail2.ui.components.onboarding.StepTitle

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/InterestsScreen.kt
// ─────────────────────────────────────────────

@Composable
fun InterestsScreen(
    selectedInterestIds: List<String>,
    progress: Float,
    onToggleInterest: (String) -> Unit,
    onNext: () -> Unit,
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
                emoji = "✨",
                title = "Что вас интересует?",
                subtitle = "Отметьте темы — будем показывать подходящие маршруты"
            )

            Spacer(Modifier.height(16.dp))

            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OnboardingData.interests.forEach { interest ->
                    SelectableChip(
                        label = interest.label,
                        selected = interest.id in selectedInterestIds,
                        onClick = { onToggleInterest(interest.id) },
                        leadingEmoji = interest.emoji
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Подсказка
            if (selectedInterestIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Отлично! Выбрано ${selectedInterestIds.size} интереса. " +
                                "На основе этого подберём лучшие маршруты.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (selectedInterestIds.isNotEmpty()) {
                    Text(
                        "Выбрано: ${selectedInterestIds.size}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OnboardingButton(
                    text = if (selectedInterestIds.isEmpty()) "Пропустить" else "Продолжить",
                    onClick = onNext
                )
            }
        }
    }
}
