package com.trail2.data.remote.api

import com.trail2.data.remote.dto.PaginatedResponseDto
import com.trail2.data.remote.dto.RouteCreateDto
import com.trail2.data.remote.dto.RouteResponseDto
import com.trail2.data.remote.dto.RouteUpdateDto
import com.trail2.data.remote.dto.UserProfileDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RouteApi {

    @GET("routes")
    suspend fun getRoutes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("region") region: String? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("sort") sort: String = "recent",
        @Query("tags") tags: List<String>? = null,
        @Query("distance_min") distanceMin: Double? = null,
        @Query("distance_max") distanceMax: Double? = null
    ): PaginatedResponseDto<RouteResponseDto>

    @POST("routes")
    suspend fun createRoute(@Body request: RouteCreateDto): RouteResponseDto

    @GET("routes/{route_id}")
    suspend fun getRoute(@Path("route_id") routeId: String): RouteResponseDto

    @PUT("routes/{route_id}")
    suspend fun updateRoute(
        @Path("route_id") routeId: String,
        @Body request: RouteUpdateDto
    ): RouteResponseDto

    @DELETE("routes/{route_id}")
    suspend fun deleteRoute(@Path("route_id") routeId: String): Response<Unit>

    @POST("routes/{route_id}/like")
    suspend fun likeRoute(@Path("route_id") routeId: String): Response<Unit>

    @DELETE("routes/{route_id}/like")
    suspend fun unlikeRoute(@Path("route_id") routeId: String): Response<Unit>

    @POST("routes/{route_id}/save")
    suspend fun saveRoute(@Path("route_id") routeId: String): Response<Unit>

    @DELETE("routes/{route_id}/save")
    suspend fun unsaveRoute(@Path("route_id") routeId: String): Response<Unit>

    @GET("feed")
    suspend fun getFeed(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<RouteResponseDto>

    @GET("search")
    suspend fun searchRoutes(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<RouteResponseDto>

    @GET("search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<UserProfileDto>

    @GET("regions")
    suspend fun getRegions(): List<String>
}
