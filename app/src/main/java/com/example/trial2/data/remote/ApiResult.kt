package com.trail2.data.remote

import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class NetworkError(val exception: Throwable) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        val message = parseErrorMessage(errorBody) ?: "Ошибка сервера: ${e.code()}"
        ApiResult.Error(e.code(), message)
    } catch (e: IOException) {
        ApiResult.NetworkError(e)
    } catch (e: Exception) {
        ApiResult.Error(-1, e.message ?: "Неизвестная ошибка")
    }
}

private fun parseErrorMessage(errorBody: String?): String? {
    if (errorBody.isNullOrBlank()) return null
    return try {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val element = json.parseToJsonElement(errorBody)
        val detail = element.jsonObject["detail"]
        when {
            detail is kotlinx.serialization.json.JsonPrimitive -> detail.content
            detail is kotlinx.serialization.json.JsonArray -> {
                detail.joinToString("; ") {
                    val obj = it.jsonObject
                    val msg = obj["msg"]?.jsonPrimitive?.content ?: ""
                    msg
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject
    get() = this as kotlinx.serialization.json.JsonObject

private val kotlinx.serialization.json.JsonElement.jsonPrimitive
    get() = this as kotlinx.serialization.json.JsonPrimitive

private val kotlinx.serialization.json.JsonElement.jsonArray
    get() = this as kotlinx.serialization.json.JsonArray
