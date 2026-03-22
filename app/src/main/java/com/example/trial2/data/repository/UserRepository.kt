package com.trail2.data.repository

import com.trail2.data.PaginatedResponse
import com.trail2.data.TrailRoute
import com.trail2.data.User
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.UserApi
import com.trail2.data.remote.dto.ChangeEmailRequest
import com.trail2.data.remote.dto.DeleteAccountRequest
import com.trail2.data.remote.dto.UpdateProfileRequest
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getMe(): ApiResult<User> = safeApiCall {
        userApi.getMe().toDomain()
    }

    suspend fun updateMe(
        displayName: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): ApiResult<User> = safeApiCall {
        userApi.updateMe(UpdateProfileRequest(displayName, bio, avatarUrl)).toDomain()
    }

    suspend fun getSavedRoutes(page: Int = 1): ApiResult<PaginatedResponse<TrailRoute>> =
        safeApiCall {
            userApi.getSavedRoutes(page).toDomain { it.toDomain() }
        }

    suspend fun getUserProfile(userId: String): ApiResult<User> = safeApiCall {
        userApi.getUserProfile(userId).toDomain()
    }

    suspend fun getUserRoutes(
        userId: String,
        page: Int = 1
    ): ApiResult<PaginatedResponse<TrailRoute>> = safeApiCall {
        userApi.getUserRoutes(userId, page).toDomain { it.toDomain() }
    }

    suspend fun getFollowers(
        userId: String,
        page: Int = 1
    ): ApiResult<PaginatedResponse<User>> = safeApiCall {
        userApi.getFollowers(userId, page).toDomain { it.toDomain() }
    }

    suspend fun getFollowing(
        userId: String,
        page: Int = 1
    ): ApiResult<PaginatedResponse<User>> = safeApiCall {
        userApi.getFollowing(userId, page).toDomain { it.toDomain() }
    }

    suspend fun follow(userId: String): ApiResult<Unit> = safeApiCall {
        userApi.follow(userId)
    }

    suspend fun unfollow(userId: String): ApiResult<Unit> = safeApiCall {
        userApi.unfollow(userId)
    }

    suspend fun changeEmail(newEmail: String, password: String): ApiResult<Unit> = safeApiCall {
        userApi.changeEmail(ChangeEmailRequest(newEmail, password))
    }

    suspend fun deleteAccount(currentPassword: String): ApiResult<Unit> = safeApiCall {
        userApi.deleteAccount(DeleteAccountRequest(currentPassword))
    }
}
