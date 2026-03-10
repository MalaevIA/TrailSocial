package com.trail2.data.remote.api

import com.trail2.data.remote.dto.PaginatedResponseDto
import com.trail2.data.remote.dto.ReportCreateDto
import com.trail2.data.remote.dto.ReportResponseDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportApi {

    @POST("reports")
    suspend fun createReport(@Body request: ReportCreateDto): ReportResponseDto

    @GET("reports")
    suspend fun getReports(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("status") status: String? = null,
        @Query("target_type") targetType: String? = null
    ): PaginatedResponseDto<ReportResponseDto>

    @PATCH("reports/{report_id}")
    suspend fun updateReportStatus(
        @Path("report_id") reportId: String,
        @Query("status") status: String
    ): ReportResponseDto
}
