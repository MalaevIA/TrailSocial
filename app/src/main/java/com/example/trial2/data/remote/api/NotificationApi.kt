package com.trail2.data.remote.api

import com.trail2.data.remote.dto.NotificationResponseDto
import com.trail2.data.remote.dto.PaginatedResponseDto
import com.trail2.data.remote.dto.UnreadCountDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("unread_only") unreadOnly: Boolean = false
    ): PaginatedResponseDto<NotificationResponseDto>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): UnreadCountDto

    @POST("notifications/read-all")
    suspend fun markAllRead(): Response<Unit>

    @POST("notifications/{notification_id}/read")
    suspend fun markRead(@Path("notification_id") notificationId: String): Response<Unit>
}
