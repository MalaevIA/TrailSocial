package com.trail2.data.remote.api

import com.trail2.data.remote.dto.PaginatedResponseDto
import com.trail2.data.remote.dto.UserPublicDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {

    @GET("admin/users")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("q") query: String? = null,
        @Query("is_active") isActive: Boolean? = null
    ): PaginatedResponseDto<UserPublicDto>

    @POST("admin/users/{user_id}/ban")
    suspend fun banUser(@Path("user_id") userId: String): UserPublicDto

    @POST("admin/users/{user_id}/unban")
    suspend fun unbanUser(@Path("user_id") userId: String): UserPublicDto

    @DELETE("admin/routes/{route_id}")
    suspend fun deleteRoute(@Path("route_id") routeId: String): Response<Unit>

    @DELETE("admin/comments/{comment_id}")
    suspend fun deleteComment(@Path("comment_id") commentId: String): Response<Unit>
}
