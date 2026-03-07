package com.trail2.ai_route

import com.trail2.data.remote.api.AiRouteApi
import com.trail2.data.remote.mappers.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteBuilderRepository @Inject constructor(
    private val aiRouteApi: AiRouteApi
) {
    suspend fun generateRoute(form: RouteBuilderForm): Result<GeneratedRoute> {
        return try {
            val request = form.toApiRequest()
            val response = aiRouteApi.generateRoute(request)
            Result.success(response.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
