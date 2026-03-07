package com.trail2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trail2.ui.components.RouteMapView
import com.trail2.ui.theme.ForestGreen
import com.trail2.ui.viewmodels.RouteCreateViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouteCreateScreen(
    onBack: () -> Unit,
    onRouteCreated: (String) -> Unit,
    onPickRoute: () -> Unit,
    vm: RouteCreateViewModel = hiltViewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val form = uiState.form

    LaunchedEffect(uiState.createdRouteId) {
        uiState.createdRouteId?.let { onRouteCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый маршрут") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.title,
                onValueChange = vm::onTitleChange,
                label = { Text("Название *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text("Описание") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.region,
                onValueChange = vm::onRegionChange,
                label = { Text("Регион") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Map route section ──
            Text("Маршрут на карте *", style = MaterialTheme.typography.labelLarge)
            if (form.hasCoordinates) {
                RouteMapView(
                    geometry = form.geometry,
                    startLat = form.startLat!!,
                    startLng = form.startLng!!,
                    endLat = form.endLat!!,
                    endLng = form.endLng!!,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                // Read-only stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${"%.2f".format(form.distanceKm ?: 0.0)} км · ${form.pointCount} точек",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onPickRoute) {
                        Text("Изменить")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickRoute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Нарисовать маршрут на карте")
                }
            }

            // ── Duration ──
            OutlinedTextField(
                value = form.durationMinutes,
                onValueChange = vm::onDurationChange,
                label = { Text("Время (мин)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Difficulty ──
            Text("Сложность", style = MaterialTheme.typography.labelLarge)
            val difficulties = listOf(
                "easy" to "Лёгкий",
                "moderate" to "Средний",
                "hard" to "Сложный",
                "expert" to "Эксперт"
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                difficulties.forEach { (value, label) ->
                    FilterChip(
                        selected = form.difficulty == value,
                        onClick = { vm.onDifficultyChange(value) },
                        label = { Text(label) }
                    )
                }
            }

            // ── Tags ──
            Text("Теги", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                form.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, "Удалить", Modifier.size(16.dp)) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.tagInput,
                    onValueChange = vm::onTagInputChange,
                    label = { Text("Новый тег") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::addTag) {
                    Icon(Icons.Default.Add, "Добавить")
                }
            }

            // ── Error ──
            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Submit ──
            val canSubmit = form.title.isNotBlank() && form.hasCoordinates && !uiState.isSubmitting
            Button(
                onClick = { vm.submit(asDraft = false) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Создать", fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = { vm.submit(asDraft = true) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить черновик")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
