package com.trail2.data.repository

import com.trail2.data.Difficulty
import com.trail2.data.PaginatedResponse
import com.trail2.data.RegionInfo
import com.trail2.data.TrailRoute
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.RouteApi
import com.trail2.data.remote.dto.GeoJsonLineStringDto
import com.trail2.data.remote.dto.RouteCreateDto
import com.trail2.data.remote.dto.RouteUpdateDto
import com.trail2.data.remote.dto.WaypointDto
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val routeApi: RouteApi
) {
    suspend fun getRoutes(
        page: Int = 1,
        region: String? = null,
        difficulty: Difficulty? = null,
        sort: String = "recent",
        tags: List<String>? = null
    ): ApiResult<PaginatedResponse<TrailRoute>> = safeApiCall {
        routeApi.getRoutes(
            page = page,
            region = region,
            difficulty = difficulty?.name?.lowercase(),
            sort = sort,
            tags = tags
        ).toDomain { it.toDomain() }
    }

    suspend fun getFeed(page: Int = 1): ApiResult<PaginatedResponse<TrailRoute>> = safeApiCall {
        routeApi.getFeed(page).toDomain { it.toDomain() }
    }

    suspend fun getRouteById(routeId: String): ApiResult<TrailRoute> = safeApiCall {
        routeApi.getRoute(routeId).toDomain()
    }

    suspend fun createRoute(
        title: String,
        description: String? = null,
        region: String? = null,
        distanceKm: Double? = null,
        elevationGainM: Double? = null,
        durationMinutes: Int? = null,
        difficulty: String? = null,
        photos: List<String>? = null,
        tags: List<String>? = null,
        status: String = "published",
        startLat: Double? = null,
        startLng: Double? = null,
        endLat: Double? = null,
        endLng: Double? = null,
        geometry: GeoJsonLineStringDto? = null,
        waypoints: List<WaypointDto>? = null
    ): ApiResult<TrailRoute> = safeApiCall {
        routeApi.createRoute(
            RouteCreateDto(
                title = title,
                status = status,
                description = description,
                region = region,
                distanceKm = distanceKm,
                elevationGainM = elevationGainM,
                durationMinutes = durationMinutes,
                difficulty = difficulty,
                photos = photos,
                tags = tags,
                startLat = startLat,
                startLng = startLng,
                endLat = endLat,
                endLng = endLng,
                geometry = geometry,
                waypoints = waypoints
            )
        ).toDomain()
    }

    suspend fun updateRoute(
        routeId: String,
        update: RouteUpdateDto
    ): ApiResult<TrailRoute> = safeApiCall {
        routeApi.updateRoute(routeId, update).toDomain()
    }

    suspend fun deleteRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        routeApi.deleteRoute(routeId)
    }

    suspend fun likeRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        routeApi.likeRoute(routeId)
    }

    suspend fun unlikeRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        routeApi.unlikeRoute(routeId)
    }

    suspend fun saveRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        routeApi.saveRoute(routeId)
    }

    suspend fun unsaveRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        routeApi.unsaveRoute(routeId)
    }

    suspend fun searchRoutes(
        query: String,
        page: Int = 1
    ): ApiResult<PaginatedResponse<TrailRoute>> = safeApiCall {
        routeApi.searchRoutes(query, page).toDomain { it.toDomain() }
    }

    suspend fun searchUsers(
        query: String,
        page: Int = 1
    ) = safeApiCall {
        routeApi.searchUsers(query, page).toDomain { it.toDomain() }
    }

    suspend fun getRegions(): ApiResult<List<RegionInfo>> = safeApiCall {
        routeApi.getRegions().items.map { it.toDomain() }
    }
}
