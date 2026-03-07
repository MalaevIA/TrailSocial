package com.trail2.ai_route

import com.trail2.data.GeoJsonLineString
import com.trail2.data.remote.dto.AiGenerateRequestDto

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

data class RouteBuilderForm(
    val purpose: TripPurpose? = null,
    val groupType: GroupType? = null,
    val duration: TripDuration? = null,
    val distance: TripDistance? = null,
    val pace: Pace? = null,
    val terrains: Set<Terrain> = emptySet(),
    val startPoint: String = "",
    val mustSeeWishes: String = "",
    val avoidWishes: String = "",
    val hasPets: Boolean = false,
    val needsCafe: Boolean = false,
    val needsWC: Boolean = false,
    val accessible: Boolean = false
)

fun RouteBuilderForm.toApiRequest(): AiGenerateRequestDto {
    return AiGenerateRequestDto(
        region = startPoint.ifBlank { null },
        difficulty = deriveDifficulty(),
        durationMinutes = duration?.toMinutes(),
        distanceKm = distance?.toKm(),
        interests = buildInterestsList().ifEmpty { null },
        description = buildDescription().ifBlank { null }
    )
}

private fun TripDuration.toMinutes(): Int = when (this) {
    TripDuration.UNDER_1H -> 45
    TripDuration.ONE_TWO_H -> 90
    TripDuration.TWO_FOUR_H -> 180
    TripDuration.HALF_DAY -> 300
    TripDuration.FULL_DAY -> 420
    TripDuration.MULTI_DAY -> 960
}

private fun TripDistance.toKm(): Double = when (this) {
    TripDistance.SHORT -> 2.0
    TripDistance.MEDIUM -> 5.5
    TripDistance.LONG -> 11.0
    TripDistance.VERY_LONG -> 20.0
}

private fun RouteBuilderForm.deriveDifficulty(): String? = when {
    pace == Pace.BRISK && terrains.contains(Terrain.HILLS) -> "hard"
    purpose == TripPurpose.SPORT -> "moderate"
    purpose == TripPurpose.FAMILY || purpose == TripPurpose.WALK -> "easy"
    terrains.any { it == Terrain.HILLS } -> "moderate"
    else -> null
}

private fun RouteBuilderForm.buildInterestsList(): List<String> {
    val list = mutableListOf<String>()
    purpose?.let { list.add(it.label) }
    terrains.forEach { list.add(it.label) }
    groupType?.let { list.add(it.label) }
    return list
}

private fun RouteBuilderForm.buildDescription(): String {
    val parts = mutableListOf<String>()
    if (mustSeeWishes.isNotBlank()) parts.add("Хочу увидеть: $mustSeeWishes")
    if (avoidWishes.isNotBlank()) parts.add("Избегать: $avoidWishes")
    pace?.let { parts.add("Темп: ${it.label}") }
    val extras = buildList {
        if (hasPets) add("маршрут подходит для собак")
        if (needsCafe) add("желательно кафе/кофейня рядом")
        if (needsWC) add("наличие туалетов")
        if (accessible) add("без ступеней и барьеров")
    }
    if (extras.isNotEmpty()) parts.add("Доп: ${extras.joinToString(", ")}")
    return parts.joinToString(". ")
}

data class GeneratedRoute(
    val title: String,
    val description: String,
    val region: String = "",
    val distanceKm: Double,
    val elevationGainM: Int = 0,
    val durationMin: Int,
    val difficulty: String,
    val points: List<RoutePoint>,
    val tips: List<String>,
    val tags: List<String>,
    val highlights: List<String> = emptyList(),
    val geometry: GeoJsonLineString? = null
)

data class RoutePoint(
    val lat: Double,
    val lon: Double,
    val title: String,
    val description: String,
    val type: String
)

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
