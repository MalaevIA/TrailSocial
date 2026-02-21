package com.trail2.ai_route

// ══════════════════════════════════════════════════════════════
// Файл: ai_route/RouteBuilderRepository.kt
// ══════════════════════════════════════════════════════════════

import kotlinx.coroutines.delay

class RouteBuilderRepository {

    /**
     * Генерирует маршрут через AI.
     * Пока эндпоинта нет — возвращает DEMO_ROUTE с искусственной задержкой.
     * Когда сервер появится — раскомментируйте блок ниже.
     */
    suspend fun generateRoute(form: RouteBuilderForm): Result<GeneratedRoute> {
        return try {

            // ── ЗАГЛУШКА (убрать когда появится сервер) ──────
            delay(2000) // имитируем сетевой запрос
            Result.success(DEMO_ROUTE)
            // ─────────────────────────────────────────────────

            /*
            ── РЕАЛЬНЫЙ ЗАПРОС К AI-ЭНДПОИНТУ ──────────────────

            val prompt = form.buildPrompt()

            val response = ApiClient.service.generateRoute(
                AiRouteRequest(prompt = prompt)
            )

            if (!response.isSuccessful) {
                return Result.failure(Exception("Ошибка сервера: ${response.code()}"))
            }

            val body = response.body()
                ?: return Result.failure(Exception("Пустой ответ от сервера"))

            // Парсим JSON из поля ответа (структура зависит от вашего бэкенда)
            val routeJson = body.result   // или body.choices[0].message.content и т.д.
            val route = parseRouteJson(routeJson)
            Result.success(route)
            ────────────────────────────────────────────────────

            ── Retrofit-клиент (создать в отдельном файле ApiClient.kt) ──

            interface AiApiService {
                @POST("api/route/generate")         // ← ваш эндпоинт
                suspend fun generateRoute(@Body body: AiRouteRequest): Response<AiApiResponse>
            }

            data class AiApiResponse(
                val result: String    // JSON-строка с маршрутом, либо уже распакованный объект
            )

            object ApiClient {
                val service: AiApiService by lazy {
                    Retrofit.Builder()
                        .baseUrl("https://your-backend.com/")
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AiApiService::class.java)
                }
            }

            ── Парсер JSON-строки в GeneratedRoute ──

            fun parseRouteJson(json: String): GeneratedRoute {
                val obj = JSONObject(json)
                val pointsArr = obj.getJSONArray("points")
                val points = (0 until pointsArr.length()).map { i ->
                    val p = pointsArr.getJSONObject(i)
                    RoutePoint(
                        lat = p.getDouble("lat"),
                        lon = p.getDouble("lon"),
                        title = p.getString("title"),
                        description = p.getString("description"),
                        type = p.getString("type")
                    )
                }
                val tipsArr = obj.getJSONArray("tips")
                val tagsArr = obj.getJSONArray("tags")
                return GeneratedRoute(
                    title = obj.getString("title"),
                    description = obj.getString("description"),
                    distanceKm = obj.getDouble("distance_km"),
                    durationMin = obj.getInt("duration_min"),
                    difficulty = obj.getString("difficulty"),
                    points = points,
                    tips = (0 until tipsArr.length()).map { tipsArr.getString(it) },
                    tags = (0 until tagsArr.length()).map { tagsArr.getString(it) }
                )
            }
            ────────────────────────────────────────────────────
            */

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
