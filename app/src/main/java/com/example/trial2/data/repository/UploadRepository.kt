package com.trail2.data.repository

import android.content.Context
import android.net.Uri
import com.trail2.data.remote.ApiResult
import com.trail2.data.remote.api.UploadApi
import com.trail2.data.remote.safeApiCall
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun uploadPhoto(uri: Uri, context: Context): ApiResult<String> = safeApiCall {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        val bytes = context.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "photo.$ext", requestBody)
        uploadApi.uploadImage(part).url
    }
}
