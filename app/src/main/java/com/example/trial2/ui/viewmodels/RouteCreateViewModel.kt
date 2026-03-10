package com.trail2.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.dto.GeoJsonLineStringDto
import com.trail2.data.remote.dto.RouteUpdateDto
import com.trail2.data.remote.dto.WaypointDto
import com.trail2.data.repository.RouteRepository
import com.trail2.data.repository.UploadRepository
import com.trail2.ui.screens.WaypointEntry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class RouteCreateForm(
    val title: String = "",
    val description: String = "",
    val region: String = "",
    val difficulty: String = "easy",
    val durationMinutes: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val photoUrls: List<String> = emptyList(),
    // Coordinates — set from map picker
    val startLat: Double? = null,
    val startLng: Double? = null,
    val endLat: Double? = null,
    val endLng: Double? = null,
    val geometry: List<List<Double>> = emptyList(),
    // Auto-computed from geometry (Haversine)
    val distanceKm: Double? = null,
    val waypoints: List<WaypointEntry> = emptyList()
) {
    val hasCoordinates: Boolean
        get() = startLat != null && startLng != null && endLat != null && endLng != null && geometry.size >= 2

    val pointCount: Int
        get() = geometry.size
}

data class RouteCreateUiState(
    val form: RouteCreateForm = RouteCreateForm(),
    val isSubmitting: Boolean = false,
    val isUploading: Boolean = false,
    val error: String? = null,
    val createdRouteId: String? = null,
    val editingRouteId: String? = null
)

@HiltViewModel
class RouteCreateViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RouteCreateUiState())
    val uiState: StateFlow<RouteCreateUiState> = _uiState.asStateFlow()

    fun resetForm() {
        _uiState.value = RouteCreateUiState()
    }

    fun loadForEdit(routeId: String) {
        viewModelScope.launch {
            _uiState.value = RouteCreateUiState()
            when (val result = routeRepository.getRouteById(routeId)) {
                is ApiResult.Success -> {
                    val route = result.data
                    val wpEntries = route.waypoints?.map { wp ->
                        WaypointEntry(
                            point = com.yandex.mapkit.geometry.Point(wp.lat, wp.lng),
                            name = wp.name,
                            description = wp.description ?: ""
                        )
                    } ?: emptyList()
                    _uiState.update {
                        it.copy(
                            editingRouteId = routeId,
                            form = RouteCreateForm(
                                title = route.title,
                                description = route.description,
                                region = route.region,
                                difficulty = route.difficulty.name.lowercase(),
                                durationMinutes = if (route.durationMinutes > 0) route.durationMinutes.toString() else "",
                                tags = route.tags,
                                photoUrls = route.photos,
                                startLat = route.startLat,
                                startLng = route.startLng,
                                endLat = route.endLat,
                                endLng = route.endLng,
                                geometry = route.geometry?.coordinates ?: emptyList(),
                                distanceKm = route.distanceKm,
                                waypoints = wpEntries
                            )
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun onTitleChange(v: String) = updateForm { copy(title = v) }
    fun onDescriptionChange(v: String) = updateForm { copy(description = v) }
    fun onRegionChange(v: String) = updateForm { copy(region = v) }
    fun onDifficultyChange(v: String) = updateForm { copy(difficulty = v) }
    fun onDurationChange(v: String) = updateForm { copy(durationMinutes = v) }
    fun onTagInputChange(v: String) = updateForm { copy(tagInput = v) }

    fun addTag() {
        val tag = _uiState.value.form.tagInput.trim()
        if (tag.isNotEmpty() && tag !in _uiState.value.form.tags) {
            updateForm { copy(tags = tags + tag, tagInput = "") }
        }
    }

    fun removeTag(tag: String) = updateForm { copy(tags = tags - tag) }

    fun setRouteCoordinates(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        geometry: List<List<Double>>,
        distanceKm: Double,
        durationMinutes: Int = 0,
        waypoints: List<WaypointEntry> = emptyList()
    ) {
        updateForm {
            copy(
                startLat = startLat, startLng = startLng,
                endLat = endLat, endLng = endLng,
                geometry = geometry,
                distanceKm = distanceKm,
                durationMinutes = if (durationMinutes > 0) durationMinutes.toString() else "",
                waypoints = waypoints
            )
        }
        // Reverse-geocode start point for region
        if (_uiState.value.form.region.isBlank()) {
            reverseGeocodeRegion(startLat, startLng)
        }
    }

    fun clearRouteCoordinates() = updateForm {
        copy(
            startLat = null, startLng = null,
            endLat = null, endLng = null,
            geometry = emptyList(),
            distanceKm = null,
            waypoints = emptyList()
        )
    }

    fun uploadPhoto(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            when (val result = uploadRepository.uploadImage(file)) {
                is ApiResult.Success -> {
                    updateForm { copy(photoUrls = photoUrls + result.data) }
                    _uiState.update { it.copy(isUploading = false) }
                }
                else -> _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    fun submit(asDraft: Boolean = false) {
        val form = _uiState.value.form
        if (form.title.isBlank()) {
            _uiState.update { it.copy(error = "Введите название маршрута") }
            return
        }
        if (!form.hasCoordinates) {
            _uiState.update { it.copy(error = "Нарисуйте маршрут на карте") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            val waypointDtos = form.waypoints.takeIf { it.isNotEmpty() }?.map { wp ->
                WaypointDto(
                    lat = wp.point.latitude,
                    lng = wp.point.longitude,
                    name = wp.name,
                    description = wp.description
                )
            }

            val editId = _uiState.value.editingRouteId
            val result = if (editId != null) {
                routeRepository.updateRoute(
                    routeId = editId,
                    update = RouteUpdateDto(
                        title = form.title,
                        description = form.description.ifBlank { null },
                        region = form.region.ifBlank { null },
                        distanceKm = form.distanceKm,
                        durationMinutes = form.durationMinutes.toIntOrNull(),
                        difficulty = form.difficulty,
                        photos = form.photoUrls.ifEmpty { null },
                        tags = form.tags.ifEmpty { null },
                        status = if (asDraft) "draft" else "published",
                        startLat = form.startLat,
                        startLng = form.startLng,
                        endLat = form.endLat,
                        endLng = form.endLng,
                        geometry = GeoJsonLineStringDto("LineString", form.geometry),
                        waypoints = waypointDtos
                    )
                )
            } else {
                routeRepository.createRoute(
                    title = form.title,
                    description = form.description.ifBlank { null },
                    region = form.region.ifBlank { null },
                    distanceKm = form.distanceKm,
                    elevationGainM = null,
                    durationMinutes = form.durationMinutes.toIntOrNull(),
                    difficulty = form.difficulty,
                    photos = form.photoUrls.ifEmpty { null },
                    tags = form.tags.ifEmpty { null },
                    status = if (asDraft) "draft" else "published",
                    startLat = form.startLat,
                    startLng = form.startLng,
                    endLat = form.endLat,
                    endLng = form.endLng,
                    geometry = GeoJsonLineStringDto("LineString", form.geometry),
                    waypoints = waypointDtos
                )
            }
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false, createdRouteId = result.data.id) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                is ApiResult.NetworkError -> _uiState.update { it.copy(isSubmitting = false, error = "Нет подключения") }
            }
        }
    }

    private var geocodeSession: Session? = null

    private fun reverseGeocodeRegion(lat: Double, lng: Double) {
        val searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.ONLINE)
        val options = SearchOptions().setSearchTypes(SearchType.GEO.value)
        geocodeSession = searchManager.submit(
            Point(lat, lng),
            17,
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val geoObject = response.collection.children.firstOrNull()?.obj ?: return
                    val components = geoObject.metadataContainer
                        .getItem(com.yandex.mapkit.search.ToponymObjectMetadata::class.java)
                        ?.address?.components ?: return

                    // Find city or locality, fallback to province
                    val city = components.firstOrNull {
                        it.kinds.any { k -> k == com.yandex.mapkit.search.Address.Component.Kind.LOCALITY }
                    }?.name
                    val province = components.firstOrNull {
                        it.kinds.any { k -> k == com.yandex.mapkit.search.Address.Component.Kind.PROVINCE }
                    }?.name

                    val region = city ?: province ?: return
                    updateForm { copy(region = region) }
                }

                override fun onSearchError(error: Error) { /* ignore */ }
            }
        )
    }

    private fun updateForm(block: RouteCreateForm.() -> RouteCreateForm) {
        _uiState.update { it.copy(form = it.form.block(), error = null) }
    }
}
