package com.trail2.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponseDto(
    val id: String,
    @SerialName("route_id") val routeId: String,
    val text: String,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val author: UserPublicDto? = null,
    @SerialName("is_liked") val isLiked: Boolean = false
)

@Serializable
data class CommentCreateDto(
    val text: String
)
