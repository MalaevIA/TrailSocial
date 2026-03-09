package com.trail2.ai_route

import com.trail2.data.remote.api.AiRouteApi
import com.trail2.data.remote.mappers.toDomain
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteBuilderRepository @Inject constructor(
    private val aiRouteApi: AiRouteApi
) {
    companion object {
        private const val POLL_INTERVAL_MS = 2_000L
        private const val POLL_TIMEOUT_MS = 300_000L
    }

    suspend fun generateRoute(
        form: RouteBuilderForm,
        onPollProgress: (pollNumber: Int) -> Unit = {}
    ): Result<GeneratedRoute> {
        return try {
            val request = form.toApiRequest()
            val taskCreated = aiRouteApi.generateRoute(request)
            onPollProgress(0)
            pollForResult(taskCreated.taskId, onPollProgress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun pollForResult(
        taskId: String,
        onPollProgress: (pollNumber: Int) -> Unit
    ): Result<GeneratedRoute> {
        val startTime = System.currentTimeMillis()
        var pollCount = 0

        while (true) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= POLL_TIMEOUT_MS) {
                return Result.failure(Exception("Превышено время ожидания генерации маршрута"))
            }

            delay(POLL_INTERVAL_MS)
            pollCount++
            onPollProgress(pollCount)

            val status = try {
                aiRouteApi.getTaskStatus(taskId)
            } catch (e: Exception) {
                return Result.failure(e)
            }

            when (status.status) {
                "completed" -> {
                    val result = status.result
                        ?: return Result.failure(Exception("Сервер вернул пустой результат"))
                    return Result.success(result.toDomain())
                }
                "failed" -> {
                    val errorMsg = status.error ?: "Неизвестная ошибка генерации"
                    return Result.failure(Exception(errorMsg))
                }
            }
        }
    }
}
