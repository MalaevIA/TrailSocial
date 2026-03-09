package com.trail2.ui.screens.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.R
import com.trail2.onboarding.FitnessLevel
import com.trail2.ui.components.onboarding.OnboardingButton
import com.trail2.ui.components.onboarding.OnboardingTopBar
import com.trail2.ui.components.onboarding.StepTitle

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/FitnessScreen.kt
// ─────────────────────────────────────────────

@Composable
fun FitnessScreen(
    selectedLevel: FitnessLevel?,
    progress: Float,
    onSelectLevel: (FitnessLevel) -> Unit,
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
                emoji = "💪",
                title = stringResource(R.string.fitness_title),
                subtitle = stringResource(R.string.fitness_subtitle)
            )

            Spacer(Modifier.height(20.dp))

            FitnessLevel.entries.forEach { level ->
                FitnessCard(
                    level = level,
                    isSelected = level == selectedLevel,
                    onClick = { onSelectLevel(level) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
        }

        Surface(shadowElevation = 8.dp) {
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                OnboardingButton(
                    text = if (selectedLevel == null) stringResource(R.string.skip) else stringResource(R.string.continue_btn),
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun FitnessCard(
    level: FitnessLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.padding(0.dp)
                    .let { it } // фон задаётся Surface ниже
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = bgColor
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(level.emoji, fontSize = 28.sp, modifier = Modifier.width(40.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        level.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        level.description,
                        fontSize = 13.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(0.75f)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = primary
                    ) {
                        Text(
                            "✓",
                            color = Color.White,
                            modifier = Modifier.padding(6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
