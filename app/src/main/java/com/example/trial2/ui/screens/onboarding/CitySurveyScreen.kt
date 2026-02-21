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
import com.trail2.onboarding.City
import com.trail2.onboarding.OnboardingData
import com.trail2.ui.components.onboarding.OnboardingButton
import com.trail2.ui.components.onboarding.OnboardingTopBar
import com.trail2.ui.components.onboarding.SelectableChip
import com.trail2.ui.components.onboarding.StepTitle

// ─────────────────────────────────────────────
// Файл: ui/screens/onboarding/CitySurveyScreen.kt
// ─────────────────────────────────────────────

@Composable
fun CitySurveyScreen(
    selectedCityIds: List<String>,
    progress: Float,
    onToggleCity: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val cityGroups = OnboardingData.cities.groupBy { it.region }

    Column(modifier = Modifier.fillMaxSize()) {

        OnboardingTopBar(progress = progress, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            StepTitle(
                emoji = "📍",
                title = "Где хотите гулять?",
                subtitle = "Выберите один или несколько городов — подберём маршруты поблизости"
            )

            Spacer(Modifier.height(16.dp))

            // Группы городов
            cityGroups.forEach { (region, cities) ->
                RegionSection(
                    region = region,
                    cities = cities,
                    selectedCityIds = selectedCityIds,
                    onToggleCity = onToggleCity
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        // Нижняя кнопка
        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (selectedCityIds.isNotEmpty()) {
                    Text(
                        "Выбрано: ${selectedCityIds.size} ${pluralCity(selectedCityIds.size)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OnboardingButton(
                    text = if (selectedCityIds.isEmpty()) "Пропустить" else "Продолжить",
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun RegionSection(
    region: String,
    cities: List<City>,
    selectedCityIds: List<String>,
    onToggleCity: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = region,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Wrap-row через FlowRow (Material 3 experimental)
        // Если нет FlowRow — используем обёртку ниже
        FlowRowCompat(
            items = cities,
            key = { it.id }
        ) { city ->
            SelectableChip(
                label = city.name,
                selected = city.id in selectedCityIds,
                onClick = { onToggleCity(city.id) }
            )
        }
    }
}

// Простая замена FlowRow через Row + wrap
// Если у вас Compose >= 1.5, замените на:
// @OptIn(ExperimentalLayoutApi::class) FlowRow(horizontalArrangement = ...) { ... }
@Composable
private fun <T> FlowRowCompat(
    items: List<T>,
    key: (T) -> Any,
    content: @Composable (T) -> Unit
) {
    // Используем стандартный FlowRow из androidx.compose.foundation.layout
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item -> content(item) }
    }
}

private fun pluralCity(n: Int): String = when {
    n % 10 == 1 && n % 100 != 11 -> "город"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "города"
    else -> "городов"
}
