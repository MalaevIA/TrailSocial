package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.ai_route.GeneratedRoute
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.dto.GeoJsonLineStringDto
import com.trail2.data.remote.dto.WaypointDto
import com.trail2.data.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteResultUiState(
    val isPublishing: Boolean = false,
    val publishedRouteId: String? = null,
    val publishError: String? = null,
    val isSaved: Boolean = false,
    val draftRouteId: String? = null
)

@HiltViewModel
class RouteResultViewModel @Inject constructor(
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteResultUiState())
    val uiState: StateFlow<RouteResultUiState> = _uiState.asStateFlow()

    fun publishRoute(route: GeneratedRoute) {
        if (_uiState.value.isPublishing || _uiState.value.publishedRouteId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, publishError = null) }

            val startPoint = route.points.firstOrNull()
            val endPoint = route.points.lastOrNull()

            val waypoints = route.points.map { point ->
                WaypointDto(
                    lat = point.lat,
                    lng = point.lon,
                    name = point.title,
                    description = point.description.ifBlank { null }
                )
            }

            val geometry = route.geometry?.let { geo ->
                GeoJsonLineStringDto(
                    type = geo.type,
                    coordinates = geo.coordinates
                )
            } ?: GeoJsonLineStringDto(
                coordinates = route.points.map { listOf(it.lon, it.lat) }
            )

            val result = routeRepository.createRoute(
                title = route.title,
                description = route.description,
                region = route.region.ifBlank { null },
                distanceKm = route.distanceKm,
                elevationGainM = route.elevationGainM.toDouble(),
                durationMinutes = route.durationMin,
                difficulty = route.difficulty.lowercase(),
                tags = route.tags.ifEmpty { null },
                startLat = startPoint?.lat,
                startLng = startPoint?.lon,
                endLat = endPoint?.lat,
                endLng = endPoint?.lon,
                geometry = geometry,
                waypoints = waypoints
            )

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isPublishing = false, publishedRouteId = result.data.id)
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(isPublishing = false, publishError = result.message)
                    }
                }
                is ApiResult.NetworkError -> {
                    _uiState.update {
                        it.copy(isPublishing = false, publishError = "Нет подключения к сети")
                    }
                }
            }
        }
    }

    fun saveDraft(route: GeneratedRoute) {
        if (_uiState.value.isPublishing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true, publishError = null) }
            val startPoint = route.points.firstOrNull()
            val endPoint = route.points.lastOrNull()
            val waypoints = route.points.map { point ->
                WaypointDto(lat = point.lat, lng = point.lon, name = point.title, description = point.description.ifBlank { null })
            }
            val geometry = route.geometry?.let { GeoJsonLineStringDto(it.type, it.coordinates) }
                ?: GeoJsonLineStringDto(coordinates = route.points.map { listOf(it.lon, it.lat) })

            val result = routeRepository.createRoute(
                title = route.title, description = route.description,
                region = route.region.ifBlank { null }, distanceKm = route.distanceKm,
                elevationGainM = route.elevationGainM.toDouble(), durationMinutes = route.durationMin,
                difficulty = route.difficulty.lowercase(), tags = route.tags.ifEmpty { null },
                status = "draft",
                startLat = startPoint?.lat, startLng = startPoint?.lon,
                endLat = endPoint?.lat, endLng = endPoint?.lon,
                geometry = geometry, waypoints = waypoints
            )
            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(isPublishing = false, draftRouteId = result.data.id) }
                is ApiResult.Error -> _uiState.update { it.copy(isPublishing = false, publishError = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isPublishing = false, publishError = "Нет подключения") }
            }
        }
    }

    fun toggleSave() {
        val routeId = _uiState.value.publishedRouteId ?: return
        val currentlySaved = _uiState.value.isSaved

        _uiState.update { it.copy(isSaved = !currentlySaved) }

        viewModelScope.launch {
            val result = if (currentlySaved) {
                routeRepository.unsaveRoute(routeId)
            } else {
                routeRepository.saveRoute(routeId)
            }
            if (result is ApiResult.Error || result is ApiResult.NetworkError) {
                _uiState.update { it.copy(isSaved = currentlySaved) }
            }
        }
    }
}
