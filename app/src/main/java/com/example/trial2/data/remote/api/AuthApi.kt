package com.trail2.data.remote.api

import com.trail2.data.remote.dto.LoginRequest
import com.trail2.data.remote.dto.LogoutRequest
import com.trail2.data.remote.dto.RefreshRequest
import com.trail2.data.remote.dto.SignupRequest
import com.trail2.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/signup")
    suspend fun signup(@Body request: SignupRequest): TokenResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>
}
