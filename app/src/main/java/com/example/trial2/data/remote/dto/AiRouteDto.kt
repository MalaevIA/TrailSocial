package com.trail2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AiGenerateRequestDto(
    val region: String? = null,
    val difficulty: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    val interests: List<String>? = null,
    val description: String? = null
)

@Serializable
data class GeneratedRouteDto(
    val title: String,
    val description: String,
    val region: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val difficulty: String? = null,
    val tags: List<String> = emptyList(),
    val highlights: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val geometry: GeoJsonLineStringDto? = null,
    val waypoints: List<WaypointDto>? = null
)
