package com.trail2.data.repository

import com.trail2.data.Comment
import com.trail2.data.PaginatedResponse
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.CommentApi
import com.trail2.data.remote.dto.CommentCreateDto
import com.trail2.data.remote.mappers.toDomain
import com.trail2.data.remote.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentRepository @Inject constructor(
    private val commentApi: CommentApi
) {
    suspend fun getComments(
        routeId: String,
        page: Int = 1
    ): ApiResult<PaginatedResponse<Comment>> = safeApiCall {
        commentApi.getComments(routeId, page).toDomain { it.toDomain() }
    }

    suspend fun createComment(routeId: String, text: String): ApiResult<Comment> = safeApiCall {
        commentApi.createComment(routeId, CommentCreateDto(text)).toDomain()
    }

    suspend fun deleteComment(commentId: String): ApiResult<Unit> = safeApiCall {
        commentApi.deleteComment(commentId)
    }

    suspend fun likeComment(commentId: String): ApiResult<Unit> = safeApiCall {
        commentApi.likeComment(commentId)
    }

    suspend fun unlikeComment(commentId: String): ApiResult<Unit> = safeApiCall {
        commentApi.unlikeComment(commentId)
    }
}
