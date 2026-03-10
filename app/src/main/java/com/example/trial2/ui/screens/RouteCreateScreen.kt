package com.trail2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trail2.R
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
                title = { Text(stringResource(R.string.create_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
                label = { Text(stringResource(R.string.create_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.description,
                onValueChange = vm::onDescriptionChange,
                label = { Text(stringResource(R.string.create_description)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = form.region,
                onValueChange = vm::onRegionChange,
                label = { Text(stringResource(R.string.create_region)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Map route section ──
            Text(stringResource(R.string.create_map_route), style = MaterialTheme.typography.labelLarge)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${"%.2f".format(form.distanceKm ?: 0.0)} км · ${form.pointCount} точек",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (form.durationMinutes.isNotBlank()) {
                            val mins = form.durationMinutes.toIntOrNull() ?: 0
                            val hours = mins / 60
                            val remMins = mins % 60
                            val timeText = if (hours > 0) "${hours} ч ${remMins} мин" else "$mins мин"
                            Text(
                                "~$timeText пешком",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    TextButton(onClick = onPickRoute) {
                        Text(stringResource(R.string.edit))
                    }
                }
                // Waypoints list
                if (form.waypoints.isNotEmpty()) {
                    Text(
                        stringResource(R.string.map_picker_waypoints),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    form.waypoints.forEachIndexed { idx, wp ->
                        val dotColor = when {
                            idx == 0 -> Color(0xFF52B788)
                            idx == form.waypoints.lastIndex && form.waypoints.size > 1 -> Color(0xFFE63946)
                            else -> Color(0xFF457B9D)
                        }
                        val icon = when {
                            idx == 0 -> "S"
                            idx == form.waypoints.lastIndex && form.waypoints.size > 1 -> "F"
                            else -> "${idx + 1}"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(icon, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(wp.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                if (wp.description.isNotBlank()) {
                                    Text(wp.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onPickRoute,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.create_draw_route))
                }
            }

            // ── Difficulty ──
            Text(stringResource(R.string.create_difficulty), style = MaterialTheme.typography.labelLarge)
            val difficulties = listOf(
                "easy" to stringResource(R.string.difficulty_easy),
                "moderate" to stringResource(R.string.difficulty_medium),
                "hard" to stringResource(R.string.difficulty_hard),
                "expert" to stringResource(R.string.difficulty_expert_short)
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
            Text(stringResource(R.string.create_tags), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                form.tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { vm.removeTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.delete), Modifier.size(16.dp)) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.tagInput,
                    onValueChange = vm::onTagInputChange,
                    label = { Text(stringResource(R.string.create_new_tag)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::addTag) {
                    Icon(Icons.Default.Add, stringResource(R.string.add))
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
                    Text(stringResource(R.string.create_submit), fontWeight = FontWeight.SemiBold)
                }
            }

            OutlinedButton(
                onClick = { vm.submit(asDraft = true) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.create_save_draft))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
