package com.trail2.ai_route

// ══════════════════════════════════════════════════════════════
// Файл: ai_route/RouteBuilderModels.kt
// ══════════════════════════════════════════════════════════════

// ── Варианты ответов для формы ───────────────────────────────

enum class TripPurpose(val label: String, val emoji: String) {
    WALK("Неспешная прогулка", "🚶"),
    SPORT("Спортивная тренировка", "🏃"),
    PHOTO("Фото-маршрут", "📸"),
    EDUCATIONAL("Познавательный / исторический", "🏛️"),
    FAMILY("С детьми", "👨‍👩‍👧"),
    NATURE("На природу / за грибами", "🍄")
}

enum class TripDuration(val label: String, val emoji: String) {
    UNDER_1H("До 1 часа", "⏱️"),
    ONE_TWO_H("1–2 часа", "🕐"),
    TWO_FOUR_H("2–4 часа", "🕑"),
    HALF_DAY("Полдня (4–6 ч)", "🌤️"),
    FULL_DAY("Весь день (6+ ч)", "☀️"),
    MULTI_DAY("Многодневный", "🏕️")
}

enum class TripDistance(val label: String, val emoji: String) {
    SHORT("До 3 км", "🔵"),
    MEDIUM("3–8 км", "🟢"),
    LONG("8–15 км", "🟡"),
    VERY_LONG("15+ км", "🔴")
}

enum class Terrain(val label: String, val emoji: String) {
    FLAT("Равнина / парк", "🌿"),
    HILLS("Холмы", "⛰️"),
    WATER("Вдоль воды", "🌊"),
    FOREST("Лес", "🌲"),
    URBAN("Городские улицы", "🏙️"),
    MIXED("Смешанный", "🗺️")
}

enum class GroupType(val label: String, val emoji: String) {
    SOLO("Один", "🧍"),
    COUPLE("С партнёром", "👫"),
    FRIENDS("С друзьями", "👥"),
    KIDS("С детьми", "👶"),
    GROUP("Большая группа", "🎒")
}

enum class Pace(val label: String, val emoji: String) {
    RELAXED("Неспешный — много остановок", "🐢"),
    MODERATE("Умеренный — иногда отдыхаем", "🚶"),
    BRISK("Бодрый — двигаемся без остановок", "🏃")
}

// ── Форма ────────────────────────────────────────────────────

data class RouteBuilderForm(
    // Шаг 1: Цель и компания
    val purpose: TripPurpose? = null,
    val groupType: GroupType? = null,
    // Шаг 2: Параметры маршрута
    val duration: TripDuration? = null,
    val distance: TripDistance? = null,
    val pace: Pace? = null,
    // Шаг 3: Местность и рельеф (мульти-выбор)
    val terrains: Set<Terrain> = emptySet(),
    // Шаг 4: Текстовые поля
    val startPoint: String = "",          // адрес / ориентир
    val mustSeeWishes: String = "",       // "хочу увидеть..."
    val avoidWishes: String = "",         // "избегать..."
    // Шаг 5: Доп. пожелания
    val hasPets: Boolean = false,
    val needsCafe: Boolean = false,
    val needsWC: Boolean = false,
    val accessible: Boolean = false       // маршрут без барьеров
)

// ── Промт, который строим из формы ──────────────────────────

fun RouteBuilderForm.buildPrompt(): String {
    val sb = StringBuilder()
    sb.appendLine("Составь пеший маршрут со следующими параметрами:")
    purpose?.let   { sb.appendLine("Цель: ${it.label}") }
    groupType?.let { sb.appendLine("Участники: ${it.label}") }
    duration?.let  { sb.appendLine("Длительность: ${it.label}") }
    distance?.let  { sb.appendLine("Дистанция: ${it.label}") }
    pace?.let      { sb.appendLine("Темп: ${it.label}") }
    if (terrains.isNotEmpty()) sb.appendLine("Рельеф/местность: ${terrains.joinToString { it.label }}")
    if (startPoint.isNotBlank()) sb.appendLine("Точка старта: $startPoint")
    if (mustSeeWishes.isNotBlank()) sb.appendLine("Хочу увидеть: $mustSeeWishes")
    if (avoidWishes.isNotBlank()) sb.appendLine("Избегать: $avoidWishes")
    val extras = buildList {
        if (hasPets) add("маршрут подходит для собак")
        if (needsCafe) add("желательно кафе/кофейня рядом")
        if (needsWC) add("наличие туалетов")
        if (accessible) add("без ступеней и барьеров")
    }
    if (extras.isNotEmpty()) sb.appendLine("Доп. требования: ${extras.joinToString(", ")}")
    sb.appendLine("""
Верни ТОЛЬКО валидный JSON без markdown, строго в формате:
{
  "title": "...",
  "description": "...",
  "distance_km": 0.0,
  "duration_min": 0,
  "difficulty": "EASY|MODERATE|HARD",
  "points": [
    {"lat": 0.0, "lon": 0.0, "title": "...", "description": "...", "type": "start|waypoint|finish"}
  ],
  "tips": ["...", "..."],
  "tags": ["...", "..."]
}
    """.trimIndent())
    return sb.toString()
}

// ── Ответ сервера ────────────────────────────────────────────

data class GeneratedRoute(
    val title: String,
    val description: String,
    val distanceKm: Double,
    val durationMin: Int,
    val difficulty: String,
    val points: List<RoutePoint>,
    val tips: List<String>,
    val tags: List<String>
)

data class RoutePoint(
    val lat: Double,
    val lon: Double,
    val title: String,
    val description: String,
    val type: String          // "start" | "waypoint" | "finish"
)

// Тело запроса к AI-эндпоинту
data class AiRouteRequest(
    val prompt: String
)

// ── Демо-маршрут (показывается пока нет бэкенда) ─────────────

val DEMO_ROUTE = GeneratedRoute(
    title = "Вдоль Яузы через Сокольники",
    description = "Живописный маршрут от Преображенской площади вдоль реки Яузы через парк Сокольники с выходом к Лефортовскому парку. Минимум рельефа, много зелени и скамеек для отдыха.",
    distanceKm = 6.3,
    durationMin = 90,
    difficulty = "EASY",
    points = listOf(
        RoutePoint(55.7943, 37.7077, "Старт: Преображенская пл.", "Выход 5 метро, ориентир — храм Преображения", "start"),
        RoutePoint(55.7900, 37.6980, "Набережная Яузы", "Спуститесь к воде — красивый вид на деревянные дачи", "waypoint"),
        RoutePoint(55.7958, 37.6789, "Парк Сокольники", "Центральная аллея, фонтан, кафе у входа", "waypoint"),
        RoutePoint(55.7820, 37.6820, "Лефортовский парк", "Пруд с утками, старинная ограда XVIII века", "waypoint"),
        RoutePoint(55.7786, 37.6910, "Финиш: ст. м. Авиамоторная", "Конечная точка маршрута", "finish")
    ),
    tips = listOf(
        "Лучшее время — утро в будни: меньше людей",
        "Берите воду: питьевых кранов мало",
        "В Сокольниках есть прокат велосипедов"
    ),
    tags = listOf("Парк", "Вода", "Ровный рельеф", "Асфальт", "Москва")
)
