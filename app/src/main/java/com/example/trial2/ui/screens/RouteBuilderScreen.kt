package com.trail2.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trail2.ai_route.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteBuilderScreen(
    onRouteReady: (GeneratedRoute) -> Unit,
    vm: RouteBuilderViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    when (val screen = state.screenState) {
        is RouteBuilderScreenState.Loading -> LoadingScreen()
        is RouteBuilderScreenState.Error   -> ErrorScreen(screen.message) { vm.resetToForm() }
        is RouteBuilderScreenState.Result  -> {
            LaunchedEffect(screen.route) { onRouteReady(screen.route) }
        }
        RouteBuilderScreenState.Form -> {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── TopBar вручную (без вложенного Scaffold) ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        "AI-маршрут",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        "Шаг ${state.currentStep.stepNum} из ${vm.totalSteps} — ${state.currentStep.title}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
                // ── BottomBar прямо в Column, не в Scaffold ──
                FormBottomBar(
                    step = state.currentStep,
                    totalSteps = vm.totalSteps,
                    progress = vm.progress,
                    error = state.stepError,
                    onBack = { vm.prevStep() },
                    onNext = { vm.nextStep() }
                )

                // ── Контент шага (weight = всё свободное место) ──
                Crossfade(
                    targetState = state.currentStep,
                    animationSpec = tween(200),
                    modifier = Modifier.weight(1f),
                    label = "step"
                ) { step ->
                    when (step) {
                        BuilderStep.GOAL    -> GoalStep(state.form, vm)
                        BuilderStep.PARAMS  -> ParamsStep(state.form, vm)
                        BuilderStep.TERRAIN -> TerrainStep(state.form, vm)
                        BuilderStep.DETAILS -> DetailsStep(state.form, vm)
                        BuilderStep.EXTRAS  -> ExtrasStep(state.form, vm)
                    }
                }


            }
        }
    }
}

// ── Шаг 1: Цель и компания ───────────────────────────────────

@Composable
private fun GoalStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("🎯", "Какова цель прогулки?", "Выберите один вариант")
        EnumRadioGroup(
            items = TripPurpose.entries,
            selected = form.purpose,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setPurpose(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("👥", "С кем идёте?", "")
        EnumRadioGroup(
            items = GroupType.entries,
            selected = form.groupType,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setGroupType(it) }
        )
    }
}

// ── Шаг 2: Параметры ─────────────────────────────────────────

@Composable
private fun ParamsStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("⏱️", "Сколько времени есть?", "")
        EnumRadioGroup(
            items = TripDuration.entries,
            selected = form.duration,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setDuration(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("📏", "Желаемая дистанция", "Сколько готовы пройти")
        EnumRadioGroup(
            items = TripDistance.entries,
            selected = form.distance,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setDistance(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("🏃", "Темп ходьбы", "")
        EnumRadioGroup(
            items = Pace.entries,
            selected = form.pace,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setPace(it) }
        )
    }
}

// ── Шаг 3: Местность ─────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TerrainStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("🗺️", "Какая местность нравится?", "Можно выбрать несколько")
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Terrain.entries.forEach { t ->
                val selected = t in form.terrains
                FilterChip(
                    selected = selected,
                    onClick = { vm.toggleTerrain(t) },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(t.emoji, fontSize = 16.sp)
                            Text(t.label, fontSize = 14.sp)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        if (form.terrains.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    "Выбрано: ${form.terrains.joinToString { it.label }}",
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ── Шаг 4: Детали ────────────────────────────────────────────

@Composable
private fun DetailsStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("📍", "Детали маршрута", "Уточните пожелания")
        FormTextField(
            value = form.startPoint,
            onValueChange = { vm.setStartPoint(it) },
            label = "Точка старта *",
            placeholder = "Название района, станции метро, адрес...",
            icon = Icons.Outlined.LocationOn,
            required = true
        )
        Spacer(Modifier.height(14.dp))
        FormTextField(
            value = form.mustSeeWishes,
            onValueChange = { vm.setMustSee(it) },
            label = "Хочу увидеть / посетить",
            placeholder = "Парк, пруд, старинная церковь, граффити...",
            icon = Icons.Outlined.Favorite,
            singleLine = false
        )
        Spacer(Modifier.height(14.dp))
        FormTextField(
            value = form.avoidWishes,
            onValueChange = { vm.setAvoid(it) },
            label = "Чего избегать",
            placeholder = "Шумные дороги, крутые подъёмы, стройки...",
            icon = Icons.Outlined.Block,
            singleLine = false
        )
    }
}

// ── Шаг 5: Доп. опции ────────────────────────────────────────

@Composable
private fun ExtrasStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("✨", "Дополнительно", "Необязательно, но поможет уточнить маршрут")
        ToggleOption("🐕", "Маршрут подходит для собак", form.hasPets) { vm.togglePets() }
        ToggleOption("☕", "Желательно кафе / кофейня рядом", form.needsCafe) { vm.toggleCafe() }
        ToggleOption("🚻", "Наличие туалетов", form.needsWC) { vm.toggleWC() }
        ToggleOption("♿", "Без ступеней и барьеров", form.accessible) { vm.toggleAccessible() }
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🤖", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "После нажатия «Сгенерировать» AI составит маршрут специально для вас на основе ваших ответов",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ── Вспомогательные composable ───────────────────────────────

@Composable
private fun StepScroll(content: @Composable ColumnScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            content = content
        )
    }
}

@Composable
private fun StepHeader(emoji: String, title: String, subtitle: String) {
    Text(emoji, fontSize = 28.sp)
    Spacer(Modifier.height(6.dp))
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    if (subtitle.isNotBlank()) {
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun <T> EnumRadioGroup(
    items: List<T>,
    selected: T?,
    label: (T) -> String,
    emoji: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val isSelected = item == selected
            val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(item) },
                shape = RoundedCornerShape(12.dp),
                color = bgColor
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(emoji(item), fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        label(item),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    required: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        leadingIcon = { Icon(icon, null) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default
        )
    )
}

@Composable
private fun ToggleOption(
    emoji: String,
    label: String,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

// ── Нижняя панель ────────────────────────────────────────────

@Composable
private fun FormBottomBar(
    step: BuilderStep,
    totalSteps: Int,
    progress: Float,
    error: String?,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Surface(shadowElevation = 12.dp) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            if (error != null) {
                Text(
                    "⚠️  $error",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step.stepNum > 1) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("← Назад")
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(if (step.stepNum > 1) 2f else 1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (step.stepNum == totalSteps) "🤖  Сгенерировать маршрут"
                        else "Далее →",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Экран загрузки ────────────────────────────────────────────

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(20.dp))
            Text("🤖", fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text("AI строит ваш маршрут...", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Это может занять несколько секунд",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Экран ошибки ──────────────────────────────────────────────

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("😕", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("Не удалось составить маршрут", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Попробовать снова")
            }
        }
    }
}