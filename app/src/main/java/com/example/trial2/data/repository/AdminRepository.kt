package com.trail2.data.repository

import com.trail2.data.PaginatedResponse
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.AdminApi
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val adminApi: AdminApi
) {
    suspend fun getUsers(
        page: Int = 1,
        query: String? = null,
        isActive: Boolean? = null
    ): ApiResult<PaginatedResponse<User>> = safeApiCall {
        adminApi.getUsers(page = page, query = query, isActive = isActive).toDomain { it.toDomain() }
    }

    suspend fun banUser(userId: String): ApiResult<User> = safeApiCall {
        adminApi.banUser(userId).toDomain()
    }

    suspend fun unbanUser(userId: String): ApiResult<User> = safeApiCall {
        adminApi.unbanUser(userId).toDomain()
    }

    suspend fun deleteRoute(routeId: String): ApiResult<Unit> = safeApiCall {
        adminApi.deleteRoute(routeId)
    }

    suspend fun deleteComment(commentId: String): ApiResult<Unit> = safeApiCall {
        adminApi.deleteComment(commentId)
    }
}
