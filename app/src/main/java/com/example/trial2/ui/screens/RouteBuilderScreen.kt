package com.trail2.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
import com.trail2.ai_route.*
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.theme.MossGreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteBuilderScreen(
    onRouteReady: (GeneratedRoute) -> Unit,
    vm: RouteBuilderViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    when (val screen = state.screenState) {
        is RouteBuilderScreenState.Loading -> LoadingScreen(pollCount = screen.pollCount)
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
                        stringResource(R.string.builder_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        stringResource(R.string.builder_step_format, state.currentStep.stepNum, vm.totalSteps, state.currentStep.title),
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
    val history by vm.history.collectAsStateWithLifecycle()
    StepScroll {
        if (history.isNotEmpty()) {
            Text(
                stringResource(R.string.builder_history),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(end = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                items(history) { route ->
                    HistoryRouteCard(route = route, onClick = { vm.replayFromHistory(route) })
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 20.dp))
        }
        StepHeader("🎯", stringResource(R.string.builder_goal_title), stringResource(R.string.builder_goal_subtitle))
        EnumRadioGroup(
            items = TripPurpose.entries,
            selected = form.purpose,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setPurpose(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("👥", stringResource(R.string.builder_company_title), "")
        EnumRadioGroup(
            items = GroupType.entries,
            selected = form.groupType,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setGroupType(it) }
        )
    }
}

@Composable
private fun HistoryRouteCard(route: GeneratedRoute, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreen.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
        modifier = Modifier.width(160.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                route.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "%.1f км · %d мин".format(route.distanceKm, route.durationMin),
                fontSize = 11.sp,
                color = MossGreen
            )
            Spacer(Modifier.height(4.dp))
            Text(
                route.difficulty,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Шаг 2: Параметры ─────────────────────────────────────────

@Composable
private fun ParamsStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("⏱️", stringResource(R.string.builder_time_title), "")
        EnumRadioGroup(
            items = TripDuration.entries,
            selected = form.duration,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setDuration(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("📏", stringResource(R.string.builder_distance_title), stringResource(R.string.builder_distance_subtitle))
        EnumRadioGroup(
            items = TripDistance.entries,
            selected = form.distance,
            label = { it.label },
            emoji = { it.emoji },
            onSelect = { vm.setDistance(it) }
        )
        Spacer(Modifier.height(24.dp))
        StepHeader("🏃", stringResource(R.string.builder_pace_title), "")
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
        StepHeader("🗺️", stringResource(R.string.builder_terrain_title), stringResource(R.string.builder_terrain_subtitle))
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
                    stringResource(R.string.builder_terrain_selected, form.terrains.joinToString { it.label }),
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
        StepHeader("📍", stringResource(R.string.builder_details_title), stringResource(R.string.builder_details_subtitle))
        FormTextField(
            value = form.startPoint,
            onValueChange = { vm.setStartPoint(it) },
            label = stringResource(R.string.builder_start_point),
            placeholder = stringResource(R.string.builder_start_hint),
            icon = Icons.Outlined.LocationOn,
            required = true
        )
        Spacer(Modifier.height(14.dp))
        FormTextField(
            value = form.mustSeeWishes,
            onValueChange = { vm.setMustSee(it) },
            label = stringResource(R.string.builder_want_to_see),
            placeholder = stringResource(R.string.builder_want_to_see_hint),
            icon = Icons.Outlined.Favorite,
            singleLine = false
        )
        Spacer(Modifier.height(14.dp))
        FormTextField(
            value = form.avoidWishes,
            onValueChange = { vm.setAvoid(it) },
            label = stringResource(R.string.builder_avoid),
            placeholder = stringResource(R.string.builder_avoid_hint),
            icon = Icons.Outlined.Block,
            singleLine = false
        )
    }
}

// ── Шаг 5: Доп. опции ────────────────────────────────────────

@Composable
private fun ExtrasStep(form: RouteBuilderForm, vm: RouteBuilderViewModel) {
    StepScroll {
        StepHeader("✨", stringResource(R.string.builder_extra_title), stringResource(R.string.builder_extra_subtitle))
        ToggleOption("🐕", stringResource(R.string.builder_dog_friendly), form.hasPets) { vm.togglePets() }
        ToggleOption("☕", stringResource(R.string.builder_cafe_nearby), form.needsCafe) { vm.toggleCafe() }
        ToggleOption("🚻", stringResource(R.string.builder_toilets), form.needsWC) { vm.toggleWC() }
        ToggleOption("♿", stringResource(R.string.builder_accessible), form.accessible) { vm.toggleAccessible() }
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
                    stringResource(R.string.builder_ai_hint),
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
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 130.dp),
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
                        Text("← " + stringResource(R.string.builder_back))
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(if (step.stepNum > 1) 2f else 1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (step.stepNum == totalSteps) stringResource(R.string.builder_generate)
                        else stringResource(R.string.builder_next) + " →",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Экран загрузки ────────────────────────────────────────────

@Composable
private fun LoadingScreen(pollCount: Int = 0) {
    val expectedPolls = 15
    val rawProgress = if (pollCount == 0) 0f
                      else (pollCount.toFloat() / expectedPolls).coerceAtMost(0.95f)

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 800),
        label = "loadingProgress"
    )

    val stages = listOf(
        stringResource(R.string.builder_stage_analysis),
        stringResource(R.string.builder_stage_generation),
        stringResource(R.string.builder_stage_points),
        stringResource(R.string.builder_stage_assembly)
    )
    val currentStage = (animatedProgress * stages.size).toInt().coerceIn(0, stages.lastIndex)

    val infiniteTransition = rememberInfiniteTransition(label = "loaderAnim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            val lottieComposition by rememberLottieComposition(
                LottieCompositionSpec.Asset("robot_loading.lottie")
            )
            val lottieProgress by animateLottieCompositionAsState(
                lottieComposition,
                iterations = LottieConstants.IterateForever
            )
            LottieAnimation(
                composition = lottieComposition,
                progress = { lottieProgress },
                modifier = Modifier.size(160.dp)
            )
            Text(
                stringResource(R.string.builder_generating),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.builder_generating_wait),
                fontSize = 13.sp,
                color = onSurfaceVariantColor
            )
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                    val r = 18.dp.toPx()
                    val lineY = r
                    val lineStart = r
                    val lineEnd = size.width - r
                    val progressEnd = if (currentStage > 0)
                        lineStart + (lineEnd - lineStart) * (currentStage.toFloat() / stages.lastIndex)
                    else lineStart

                    drawLine(
                        color = surfaceVariantColor,
                        start = Offset(lineStart, lineY),
                        end = Offset(lineEnd, lineY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    if (progressEnd > lineStart) {
                        drawLine(
                            color = ForestGreen,
                            start = Offset(lineStart, lineY),
                            end = Offset(progressEnd, lineY),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            stages.forEachIndexed { index, label ->
                val isCompleted = index < currentStage
                val isActive = index == currentStage

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(if (isActive) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted || isActive -> ForestGreen
                                    else -> surfaceVariantColor
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text(
                                "${index + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else onSurfaceVariantColor
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = if (isCompleted || isActive) ForestGreen else onSurfaceVariantColor,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
        } // Column
    } // Box
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
            Text(stringResource(R.string.builder_error), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.builder_try_again))
            }
        }
    }
}