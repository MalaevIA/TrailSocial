package com.trail2.data.remote.api

import com.trail2.data.remote.dto.AiGenerateRequestDto
import com.trail2.data.remote.dto.GeneratedRouteDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AiRouteApi {

    @POST("ai/generate-route")
    suspend fun generateRoute(@Body request: AiGenerateRequestDto): GeneratedRouteDto
}
