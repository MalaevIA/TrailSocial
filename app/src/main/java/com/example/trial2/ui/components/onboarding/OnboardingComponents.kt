package com.trail2.ui.components.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────
// Верхняя полоска прогресса с кнопкой «Назад»
// ─────────────────────────────────────────────
@Composable
fun OnboardingTopBar(
    progress: Float,             // 0f..1f
    onBack: (() -> Unit)? = null // null → скрыть кнопку «Назад»
) {
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(400),
        label = "progress"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.offset(x = (-8).dp)
            ) {
                Text("← Назад", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
        }
        LinearProgressIndicator(
            progress = { animProgress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────
// Заголовок шага
// ─────────────────────────────────────────────
@Composable
fun StepTitle(emoji: String, title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(emoji, fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
    }
}

// ─────────────────────────────────────────────
// Большая CTA-кнопка внизу
// ─────────────────────────────────────────────
@Composable
fun OnboardingButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ─────────────────────────────────────────────
// Чип для выбора (город / интерес)
// ─────────────────────────────────────────────
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    leadingEmoji: String? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (leadingEmoji != null) Text(leadingEmoji, fontSize = 14.sp)
                Text(label, fontSize = 13.sp)
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White
        )
    )
}
