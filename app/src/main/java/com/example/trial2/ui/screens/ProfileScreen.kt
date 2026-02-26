package com.trail2.ui.screens

// ══════════════════════════════════════════════════════════════
// Файл: ui/screens/ProfileScreen.kt  (ПОЛНАЯ ЗАМЕНА)
//
// Читает данные пользователя из OnboardingRepository (DataStore).
// ══════════════════════════════════════════════════════════════
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.onboarding.FitnessLevel
import com.trail2.onboarding.OnboardingData
import com.trail2.onboarding.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    vm: OnboardingViewModel = hiltViewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val answers by vm.savedAnswers.collectAsStateWithLifecycle()

    // Разрешаем имена городов и интересов из ID
    val selectedCities = remember(answers.selectedCityIds) {
        OnboardingData.cities.filter { it.id in answers.selectedCityIds }
    }
    val selectedInterests = remember(answers.selectedInterestIds) {
        OnboardingData.interests.filter { it.id in answers.selectedInterestIds }
    }

    val displayName = answers.displayName.ifBlank { "Путешественник" }
    val email = answers.email

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

        // ── Шапка ────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF1B4332), Color(0xFF52B788))))
        ) {
            IconButton(
                onClick = { /* открыть настройки */ },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "Настройки", tint = Color.White)
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {

            // ── Аватар (перекрывает шапку) ───────────────────
            Box(modifier = Modifier.offset(y = (-44).dp)) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2D6A4F))
                        .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // ── Кнопка редактирования ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-36).dp),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { showLogoutDialog = true },  // ← сначала показываем диалог
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти из аккаунта")
                }
            }
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Выйти из аккаунта?") },
                    text = { Text("Все данные профиля и настройки будут сброшены. Вы снова увидите экран приветствия.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                vm.logout()
                                showLogoutDialog = false
                            }
                        ) {
                            Text("Выйти", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Отмена")
                        }
                    }
                )
            }
            // ── Имя и email ──────────────────────────────────
            Text(
                displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.offset(y = (-28).dp)
            )
            if (email.isNotBlank()) {
                Text(
                    email,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.offset(y = (-24).dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // ── Уровень подготовки ───────────────────────────
            answers.fitnessLevel?.let { level ->
                FitnessLevelBadge(level)
                Spacer(Modifier.height(12.dp))
            }

            // ── Статистика ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatItem("Маршруты", "0")
                VerticalDivider(Modifier.height(36.dp))
                ProfileStatItem("Подписчики", "0")
                VerticalDivider(Modifier.height(36.dp))
                ProfileStatItem("Подписки", "0")
            }

            Spacer(Modifier.height(20.dp))

            // ── Города ───────────────────────────────────────
            if (selectedCities.isNotEmpty()) {
                SectionTitle("📍 Мои города")
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedCities.forEach { city ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    city.name,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Интересы ─────────────────────────────────────
            if (selectedInterests.isNotEmpty()) {
                SectionTitle("✨ Интересы")
                Spacer(Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedInterests.forEach { interest ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                "${interest.emoji} ${interest.label}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // ── Заглушка если ничего нет ─────────────────────
            if (selectedCities.isEmpty() && selectedInterests.isEmpty() && answers.fitnessLevel == null) {
                EmptyProfileHint()
            }

            // ── Настройки / кнопка выхода ────────────────────
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { /* сброс онбординга / выход */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Выйти из аккаунта")
            }

            Spacer(Modifier.height(80.dp)) // отступ под bottom nav
        }
    }
}

@Composable
private fun FitnessLevelBadge(level: FitnessLevel) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(level.emoji, fontSize = 16.sp)
            Text(
                level.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "· ${level.description}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.75f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
private fun EmptyProfileHint() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌿", fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text("Профиль почти пуст", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Заполните профиль при следующем запуске или в настройках",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}