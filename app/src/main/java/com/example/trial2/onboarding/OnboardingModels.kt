package com.trail2.onboarding

// ─────────────────────────────────────────────
// Модели данных онбординга
// ─────────────────────────────────────────────

data class City(
    val id: String,
    val name: String,
    val region: String
)

enum class FitnessLevel(val label: String, val description: String, val emoji: String) {
    BEGINNER(
        label = "Начинающий",
        description = "Редко хожу пешком, хочу начать",
        emoji = "🌱"
    ),
    LIGHT(
        label = "Любитель",
        description = "Прогулки до 5–10 км пару раз в неделю",
        emoji = "🚶"
    ),
    MODERATE(
        label = "Средний",
        description = "Регулярные походы, без труда 15–20 км",
        emoji = "🥾"
    ),
    ADVANCED(
        label = "Опытный",
        description = "Многодневные маршруты, горы, перепады высот",
        emoji = "🏔️"
    )
}

data class Interest(
    val id: String,
    val label: String,
    val emoji: String
)

// Данные, собранные в ходе онбординга
data class OnboardingAnswers(
    val selectedCityIds: List<String> = emptyList(),
    val fitnessLevel: FitnessLevel? = null,
    val selectedInterestIds: List<String> = emptyList(),
    val displayName: String = "",
    val email: String = "",
    val password: String = ""
)

// ─────────────────────────────────────────────
// Справочники
// ─────────────────────────────────────────────

object OnboardingData {

    val cities: List<City> = listOf(
        // Москва
        City("moscow",       "Москва",         "Москва"),
        // Золотое кольцо
        City("sergiev",      "Сергиев Посад",  "Золотое кольцо"),
        City("pereslavl",    "Переславль-Залесский", "Золотое кольцо"),
        City("rostov",       "Ростов Великий", "Золотое кольцо"),
        City("yaroslavl",    "Ярославль",      "Золотое кольцо"),
        City("kostroma",     "Кострома",       "Золотое кольцо"),
        City("ivanovo",      "Иваново",        "Золотое кольцо"),
        City("suzdal",       "Суздаль",        "Золотое кольцо"),
        City("vladimir",     "Владимир",       "Золотое кольцо"),
        // Санкт-Петербург и окрестности
        City("spb",          "Санкт-Петербург", "Северо-Запад"),
        City("pushkin",      "Пушкин (Царское Село)", "Северо-Запад"),
        City("peterhof",     "Петергоф",       "Северо-Запад"),
        City("vyborg",       "Выборг",         "Северо-Запад"),
        City("gatchina",     "Гатчина",        "Северо-Запад"),
        City("tikhvin",      "Тихвин",         "Северо-Запад")
    )

    val interests: List<Interest> = listOf(
        Interest("nature",    "Природа",         "🌿"),
        Interest("history",   "История",         "🏛️"),
        Interest("photo",     "Фотография",      "📸"),
        Interest("birds",     "Бёрдвотчинг",     "🦅"),
        Interest("botany",    "Ботаника",        "🌸"),
        Interest("geology",   "Геология",        "🪨"),
        Interest("castles",   "Усадьбы и замки", "🏰"),
        Interest("rivers",    "Реки и озёра",    "🏞️"),
        Interest("camping",   "Кемпинг",         "⛺"),
        Interest("running",   "Трейлраннинг",    "🏃"),
        Interest("nordic",    "Скандинавская ходьба", "🥍"),
        Interest("family",    "Семейные прогулки","👨‍👩‍👧")
    )
}
