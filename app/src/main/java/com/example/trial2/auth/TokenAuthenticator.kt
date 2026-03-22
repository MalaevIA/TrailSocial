package com.trail2.auth

import com.trail2.data.remote.dto.RefreshRequest
import com.trail2.data.remote.dto.TokenResponse
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val baseUrl: String
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            tokenManager.clearTokens()
            return null
        }

        val refreshToken = tokenManager.getRefreshToken() ?: run {
            tokenManager.clearTokens()
            return null
        }

        val newTokens = try {
            refreshTokens(refreshToken)
        } catch (e: TokenExpiredException) {
            // Refresh token explicitly rejected by server — clear session
            tokenManager.clearTokens()
            return null
        } catch (_: Exception) {
            // Network/server error — keep tokens, abort retry
            return null
        } ?: return null

        tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .build()
    }

    // Returns null = success but empty body (shouldn't happen),
    // throws TokenExpiredException = refresh token is invalid (401/403),
    // throws Exception = network/server error (don't clear tokens)
    private fun refreshTokens(refreshToken: String): TokenResponse? {
        val body = json.encodeToString(
            RefreshRequest.serializer(),
            RefreshRequest(refreshToken)
        )
        val request = Request.Builder()
            .url("${baseUrl}auth/refresh")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val client = OkHttpClient.Builder().build()
        val response = client.newCall(request).execute()

        if (response.code == 401 || response.code == 403) {
            throw TokenExpiredException()
        }
        if (!response.isSuccessful) {
            // Server error (5xx) or other — do NOT clear tokens, just skip retry
            return null
        }
        return response.body?.string()?.let {
            json.decodeFromString(TokenResponse.serializer(), it)
        }
    }

    private class TokenExpiredException : Exception()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
