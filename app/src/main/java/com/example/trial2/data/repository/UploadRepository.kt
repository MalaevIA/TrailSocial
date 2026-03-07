package com.trail2.data.repository

import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.UploadApi
import com.trail2.data.remote.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRepository @Inject constructor(
    private val uploadApi: UploadApi
) {
    suspend fun uploadImage(file: File): ApiResult<String> = safeApiCall {
        val mediaType = when (file.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }.toMediaTypeOrNull()

        val requestBody = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, requestBody)
        uploadApi.uploadImage(part).url
    }
}
