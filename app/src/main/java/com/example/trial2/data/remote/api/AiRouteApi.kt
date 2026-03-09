package com.trail2.data.remote.api

import com.trail2.data.remote.dto.AiGenerateRequestDto
import com.trail2.data.remote.dto.AiTaskCreatedDto
import com.trail2.data.remote.dto.AiTaskStatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AiRouteApi {

    @POST("ai/generate-route")
    suspend fun generateRoute(@Body request: AiGenerateRequestDto): AiTaskCreatedDto

    @GET("ai/tasks/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): AiTaskStatusDto
}
