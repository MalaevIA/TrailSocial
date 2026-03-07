package com.trail2.data.remote.api

import com.trail2.data.remote.dto.ReportCreateDto
import com.trail2.data.remote.dto.ReportResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface ReportApi {

    @POST("reports")
    suspend fun createReport(@Body request: ReportCreateDto): ReportResponseDto
}
