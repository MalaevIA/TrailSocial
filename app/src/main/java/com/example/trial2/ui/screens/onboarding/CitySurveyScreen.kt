package com.trail2.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trail2.R
import com.trail2.onboarding.OnboardingData
import com.trail2.ui.components.onboarding.OnboardingButton
import com.trail2.ui.components.onboarding.OnboardingTopBar
import com.trail2.ui.components.onboarding.SelectableChip
import com.trail2.ui.components.onboarding.StepTitle
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SuggestOptions
import com.yandex.mapkit.search.SuggestResponse
import com.yandex.mapkit.search.SuggestSession
import com.yandex.mapkit.search.SuggestType
import com.yandex.runtime.Error

@Composable
fun CitySurveyScreen(
    selectedCityIds: List<String>,
    progress: Float,
    onToggleCity: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // MapKit must be started for search to work
    DisposableEffect(Unit) {
        MapKitFactory.getInstance().onStart()
        onDispose {
            MapKitFactory.getInstance().onStop()
        }
    }

    val searchManager = remember {
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    }
    val suggestSession = remember { searchManager.createSuggestSession() }
    val suggestOptions = remember {
        SuggestOptions().setSuggestTypes(SuggestType.GEO.value)
    }
    // Bounding box covering the whole world
    val worldBounds = remember {
        BoundingBox(Point(-90.0, -180.0), Point(90.0, 180.0))
    }

    // Run suggest when query changes
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 2) {
            suggestions = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        suggestSession.suggest(
            searchQuery,
            worldBounds,
            suggestOptions,
            object : SuggestSession.SuggestListener {
                override fun onResponse(response: SuggestResponse) {
                    suggestions = response.items
                        .filter { it.displayText != null }
                        .map { it.displayText!! }
                        .distinct()
                        .take(8)
                    isSearching = false
                }

                override fun onError(error: Error) {
                    suggestions = emptyList()
                    isSearching = false
                }
            }
        )
    }

    // Map from static city id -> name for display of selected cities
    val selectedCityNames = remember(selectedCityIds) {
        selectedCityIds.map { id ->
            OnboardingData.cities.find { it.id == id }?.name ?: id
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingTopBar(progress = progress, onBack = onBack)

        StepTitle(
            emoji = "📍",
            title = stringResource(R.string.city_title),
            subtitle = stringResource(R.string.city_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { Text(stringResource(R.string.city_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = ""; suggestions = emptyList() }) {
                        Icon(Icons.Filled.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Selected cities chips
        if (selectedCityNames.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedCityNames.forEach { name ->
                    val cityId = OnboardingData.cities.find { it.name == name }?.id ?: name
                    InputChip(
                        selected = true,
                        onClick = { onToggleCity(cityId) },
                        label = { Text(name, fontSize = 13.sp) },
                        trailingIcon = {
                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Content: suggestions or popular cities
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            if (searchQuery.length >= 2) {
                // Show search suggestions
                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else if (suggestions.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.city_no_results),
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(suggestions) { suggestion ->
                        val alreadySelected = suggestion in selectedCityIds ||
                                OnboardingData.cities.find { it.name == suggestion }?.id in selectedCityIds
                        SuggestItem(
                            text = suggestion,
                            isSelected = alreadySelected,
                            onClick = {
                                // Try to match to known city, otherwise use name as id
                                val knownCity = OnboardingData.cities.find { it.name == suggestion }
                                val id = knownCity?.id ?: suggestion
                                onToggleCity(id)
                                searchQuery = ""
                                suggestions = emptyList()
                            }
                        )
                    }
                }
            } else {
                // Show popular cities grouped by region
                val cityGroups = OnboardingData.cities.groupBy { it.region }
                cityGroups.forEach { (region, cities) ->
                    item {
                        Text(
                            text = region,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cities.forEach { city ->
                                SelectableChip(
                                    label = city.name,
                                    selected = city.id in selectedCityIds,
                                    onClick = { onToggleCity(city.id) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }

        // Bottom button
        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                if (selectedCityIds.isNotEmpty()) {
                    Text(
                        stringResource(R.string.city_selected, selectedCityIds.size, pluralCity(selectedCityIds.size)),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OnboardingButton(
                    text = if (selectedCityIds.isEmpty()) stringResource(R.string.skip) else stringResource(R.string.continue_btn),
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun SuggestItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSelected) { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            if (isSelected) {
                Text(
                    "✓",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun pluralCity(n: Int): String = pluralStringResource(R.plurals.city_plural, n)
