package com.trail2.data.repository

import com.trail2.data.PaginatedResponse
import com.trail2.data.Report
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.ReportApi
import com.trail2.data.remote.dto.ReportCreateDto
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val reportApi: ReportApi
) {
    suspend fun createReport(
        targetType: String,
        targetId: String,
        reason: String,
        description: String? = null
    ): ApiResult<Report> = safeApiCall {
        reportApi.createReport(
            ReportCreateDto(targetType, targetId, reason, description)
        ).toDomain()
    }

    suspend fun getReports(
        page: Int = 1,
        status: String? = null,
        targetType: String? = null
    ): ApiResult<PaginatedResponse<Report>> = safeApiCall {
        reportApi.getReports(page, status = status, targetType = targetType).toDomain { it.toDomain() }
    }

    suspend fun updateReportStatus(
        reportId: String,
        status: String
    ): ApiResult<Report> = safeApiCall {
        reportApi.updateReportStatus(reportId, status).toDomain()
    }
}
