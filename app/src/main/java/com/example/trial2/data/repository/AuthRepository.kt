package com.trail2.data.repository

import com.trail2.auth.TokenManager
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.AuthApi
import com.trail2.data.remote.dto.LoginRequest
import com.trail2.data.remote.dto.LogoutRequest
import com.trail2.data.remote.dto.SignupRequest
import com.trail2.data.remote.safeApiCall
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn

    suspend fun signup(
        username: String,
        email: String,
        password: String,
        displayName: String
    ): ApiResult<Unit> {
        val result = safeApiCall {
            authApi.signup(SignupRequest(username, email, password, displayName))
        }
        return when (result) {
            is ApiResult.Success -> {
                tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun login(email: String, password: String): ApiResult<Unit> {
        val result = safeApiCall {
            authApi.login(LoginRequest(email, password))
        }
        return when (result) {
            is ApiResult.Success -> {
                tokenManager.saveTokens(result.data.accessToken, result.data.refreshToken)
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun logout(): ApiResult<Unit> {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken != null) {
            safeApiCall { authApi.logout(LogoutRequest(refreshToken)) }
        }
        tokenManager.clearTokens()
        return ApiResult.Success(Unit)
    }
}
