package com.trail2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponseDto(
    val id: String,
    val type: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    val actor: UserPublicDto,
    @SerialName("route_id") val routeId: String? = null,
    @SerialName("route_title") val routeTitle: String? = null,
    @SerialName("comment_id") val commentId: String? = null,
    @SerialName("comment_text") val commentText: String? = null
)

@Serializable
data class UnreadCountDto(
    val count: Int
)
