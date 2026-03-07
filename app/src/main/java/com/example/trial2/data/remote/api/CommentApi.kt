package com.trail2.data.remote.api

import com.trail2.data.remote.dto.CommentCreateDto
import com.trail2.data.remote.dto.CommentResponseDto
import com.trail2.data.remote.dto.PaginatedResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CommentApi {

    @GET("routes/{route_id}/comments")
    suspend fun getComments(
        @Path("route_id") routeId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponseDto<CommentResponseDto>

    @POST("routes/{route_id}/comments")
    suspend fun createComment(
        @Path("route_id") routeId: String,
        @Body request: CommentCreateDto
    ): CommentResponseDto

    @DELETE("comments/{comment_id}")
    suspend fun deleteComment(@Path("comment_id") commentId: String): Response<Unit>

    @POST("comments/{comment_id}/like")
    suspend fun likeComment(@Path("comment_id") commentId: String): Response<Unit>

    @DELETE("comments/{comment_id}/like")
    suspend fun unlikeComment(@Path("comment_id") commentId: String): Response<Unit>
}
