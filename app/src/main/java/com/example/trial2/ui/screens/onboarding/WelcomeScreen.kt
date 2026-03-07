package com.trail2.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.ui.components.onboarding.OnboardingButton

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/WelcomeScreen.kt
// ─────────────────────────────────────────────

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onGoToLogin: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // Фоновый градиент (лесной)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF52B788))
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // ── Верхняя часть (на фоне) ──────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 28.dp, end = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("🏔️", fontSize = 52.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "TrailSocial",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Открывайте пешие маршруты,\nделитесь впечатлениями,\nнаходите единомышленников.",
                    fontSize = 17.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    lineHeight = 24.sp
                )
            }

            // ── Нижняя карточка ─────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Фичи
                    FeatureRow(emoji = "🗺️", text = "Лента маршрутов от таких же туристов")
                    Spacer(Modifier.height(12.dp))
                    FeatureRow(emoji = "📍", text = "Маршруты по вашему городу и региону")
                    Spacer(Modifier.height(12.dp))
                    FeatureRow(emoji = "👥", text = "Сообщество любителей пеших прогулок")

                    Spacer(Modifier.height(28.dp))

                    OnboardingButton(
                        text = "Начать — это бесплатно",
                        onClick = onContinue
                    )

                    Spacer(Modifier.height(10.dp))

                    TextButton(onClick = onGoToLogin) {
                        Text(
                            "Уже есть аккаунт? Войти",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Политика конфиденциальности
                    Text(
                        text = buildAnnotatedString {
                            append("Нажимая «Начать», вы соглашаетесь с ")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                                append("Условиями использования")
                            }
                            append(" и ")
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                                append("Политикой конфиденциальности")
                            }
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(emoji: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}
