package com.trail2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponseDto<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    @SerialName("page_size") val pageSize: Int,
    val pages: Int
)

@Serializable
data class UploadResponseDto(
    val url: String
)

@Serializable
data class ReportCreateDto(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val reason: String,
    val description: String? = null
)

@Serializable
data class ReportResponseDto(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val reason: String,
    val description: String? = null,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String = ""
)
