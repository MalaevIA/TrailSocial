package com.trail2.data.repository

import com.trail2.data.Notification
import com.trail2.data.PaginatedResponse
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.NotificationApi
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi
) {
    suspend fun getNotifications(
        page: Int = 1,
        unreadOnly: Boolean = false
    ): ApiResult<PaginatedResponse<Notification>> = safeApiCall {
        notificationApi.getNotifications(page, unreadOnly = unreadOnly).toDomain { it.toDomain() }
    }

    suspend fun getUnreadCount(): ApiResult<Int> = safeApiCall {
        notificationApi.getUnreadCount().count
    }

    suspend fun markAllRead(): ApiResult<Unit> = safeApiCall {
        notificationApi.markAllRead()
    }

    suspend fun markRead(notificationId: String): ApiResult<Unit> = safeApiCall {
        notificationApi.markRead(notificationId)
    }
}
