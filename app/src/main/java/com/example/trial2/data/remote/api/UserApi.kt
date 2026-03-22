package com.trail2.data.remote.api

import com.trail2.data.remote.dto.ChangeEmailRequest
import com.trail2.data.remote.dto.DeleteAccountRequest
import com.trail2.data.remote.dto.PaginatedResponseDto
import com.trail2.data.remote.dto.RouteResponseDto
import com.trail2.data.remote.dto.UpdateProfileRequest
import com.trail2.data.remote.dto.UserProfileDto
import com.trail2.data.remote.dto.UserPublicDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {

    @GET("users/me")
    suspend fun getMe(): UserPublicDto

    @PUT("users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): UserPublicDto

    @GET("users/me/saved-routes")
    suspend fun getSavedRoutes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<RouteResponseDto>

    @GET("users/{user_id}")
    suspend fun getUserProfile(@Path("user_id") userId: String): UserProfileDto

    @GET("users/{user_id}/routes")
    suspend fun getUserRoutes(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("sort") sort: String = "recent",
        @Query("difficulty") difficulty: String? = null
    ): PaginatedResponseDto<RouteResponseDto>

    @GET("users/{user_id}/followers")
    suspend fun getFollowers(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<UserProfileDto>

    @GET("users/{user_id}/following")
    suspend fun getFollowing(
        @Path("user_id") userId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<UserProfileDto>

    @POST("users/{user_id}/follow")
    suspend fun follow(@Path("user_id") userId: String): Response<Unit>

    @DELETE("users/{user_id}/follow")
    suspend fun unfollow(@Path("user_id") userId: String): Response<Unit>

    @PUT("users/me/email")
    suspend fun changeEmail(@Body request: ChangeEmailRequest): Response<Unit>

    @HTTP(method = "DELETE", path = "users/me", hasBody = true)
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>
}
