package com.trail2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoJsonLineStringDto(
    val type: String = "LineString",
    val coordinates: List<List<Double>> = emptyList()
)

@Serializable
data class WaypointDto(
    val lat: Double,
    val lng: Double,
    val name: String,
    val description: String? = null
)

@Serializable
data class RouteResponseDto(
    val id: String,
    val title: String,
    val status: String = "published",
    val description: String? = null,
    val region: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val difficulty: String? = null,
    val photos: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("start_lat") val startLat: Double? = null,
    @SerialName("start_lng") val startLng: Double? = null,
    @SerialName("end_lat") val endLat: Double? = null,
    @SerialName("end_lng") val endLng: Double? = null,
    val geometry: GeoJsonLineStringDto? = null,
    val waypoints: List<WaypointDto>? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("saves_count") val savesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val author: UserPublicDto? = null,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_saved") val isSaved: Boolean = false
)

@Serializable
data class RouteCreateDto(
    val title: String,
    val status: String = "published",
    val description: String? = null,
    val region: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val difficulty: String? = null,
    val photos: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("start_lat") val startLat: Double? = null,
    @SerialName("start_lng") val startLng: Double? = null,
    @SerialName("end_lat") val endLat: Double? = null,
    @SerialName("end_lng") val endLng: Double? = null,
    val geometry: GeoJsonLineStringDto? = null,
    val waypoints: List<WaypointDto>? = null
)

@Serializable
data class RouteUpdateDto(
    val title: String? = null,
    val status: String? = null,
    val description: String? = null,
    val region: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("elevation_gain_m") val elevationGainM: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val difficulty: String? = null,
    val photos: List<String>? = null,
    val tags: List<String>? = null,
    @SerialName("start_lat") val startLat: Double? = null,
    @SerialName("start_lng") val startLng: Double? = null,
    @SerialName("end_lat") val endLat: Double? = null,
    @SerialName("end_lng") val endLng: Double? = null,
    val geometry: GeoJsonLineStringDto? = null,
    val waypoints: List<WaypointDto>? = null
)
