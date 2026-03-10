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
        // Москва и Подмосковье
        City("moscow",       "Москва",                "Центральная Россия"),
        City("sergiev",      "Сергиев Посад",         "Центральная Россия"),
        City("pereslavl",    "Переславль-Залесский",  "Центральная Россия"),
        City("rostov",       "Ростов Великий",        "Центральная Россия"),
        City("yaroslavl",    "Ярославль",             "Центральная Россия"),
        City("kostroma",     "Кострома",              "Центральная Россия"),
        City("ivanovo",      "Иваново",               "Центральная Россия"),
        City("suzdal",       "Суздаль",               "Центральная Россия"),
        City("vladimir",     "Владимир",              "Центральная Россия"),
        City("tula",         "Тула",                  "Центральная Россия"),
        City("kaluga",       "Калуга",                "Центральная Россия"),
        City("ryazan",       "Рязань",                "Центральная Россия"),
        City("tver",         "Тверь",                 "Центральная Россия"),
        City("smolensk",     "Смоленск",              "Центральная Россия"),
        City("bryansk",      "Брянск",                "Центральная Россия"),
        City("orel",         "Орёл",                  "Центральная Россия"),
        City("tambov",       "Тамбов",                "Центральная Россия"),
        // Санкт-Петербург и Северо-Запад
        City("spb",          "Санкт-Петербург",       "Северо-Запад"),
        City("pushkin",      "Пушкин (Царское Село)", "Северо-Запад"),
        City("peterhof",     "Петергоф",              "Северо-Запад"),
        City("vyborg",       "Выборг",                "Северо-Запад"),
        City("gatchina",     "Гатчина",               "Северо-Запад"),
        City("tikhvin",      "Тихвин",                "Северо-Запад"),
        City("pskov",        "Псков",                 "Северо-Запад"),
        City("novgorod",     "Великий Новгород",      "Северо-Запад"),
        City("petrozavodsk", "Петрозаводск",          "Северо-Запад"),
        City("murmansk",     "Мурманск",              "Северо-Запад"),
        City("arkhangelsk",  "Архангельск",           "Северо-Запад"),
        City("vologda",      "Вологда",               "Северо-Запад"),
        City("kaliningrad",  "Калининград",           "Северо-Запад"),
        // Юг России и Кавказ
        City("sochi",        "Сочи",                  "Юг России"),
        City("krasnodar",    "Краснодар",             "Юг России"),
        City("rostov_don",   "Ростов-на-Дону",        "Юг России"),
        City("anapa",        "Анапа",                 "Юг России"),
        City("novorossiysk", "Новороссийск",          "Юг России"),
        City("stavropol",    "Ставрополь",            "Юг России"),
        City("pyatigorsk",   "Пятигорск",             "Кавказ"),
        City("kislovodsk",   "Кисловодск",            "Кавказ"),
        City("elbrus",       "Приэльбрусье",          "Кавказ"),
        City("dombay",       "Домбай",                "Кавказ"),
        City("arkhyz",       "Архыз",                 "Кавказ"),
        City("derbent",      "Дербент",               "Кавказ"),
        City("makhachkala",  "Махачкала",             "Кавказ"),
        City("vladikavkaz",  "Владикавказ",           "Кавказ"),
        // Поволжье
        City("nizhny",       "Нижний Новгород",       "Поволжье"),
        City("kazan",        "Казань",                "Поволжье"),
        City("samara",       "Самара",                "Поволжье"),
        City("saratov",      "Саратов",               "Поволжье"),
        City("volgograd",    "Волгоград",             "Поволжье"),
        City("ulyanovsk",    "Ульяновск",             "Поволжье"),
        City("penza",        "Пенза",                 "Поволжье"),
        City("cheboksary",   "Чебоксары",             "Поволжье"),
        City("yoshkar_ola",  "Йошкар-Ола",            "Поволжье"),
        City("astrakhan",    "Астрахань",             "Поволжье"),
        // Урал
        City("ekaterinburg", "Екатеринбург",          "Урал"),
        City("chelyabinsk",  "Челябинск",             "Урал"),
        City("perm",         "Пермь",                 "Урал"),
        City("ufa",          "Уфа",                   "Урал"),
        City("tyumen",       "Тюмень",                "Урал"),
        City("magnitogorsk", "Магнитогорск",          "Урал"),
        City("orenburg",     "Оренбург",              "Урал"),
        // Сибирь
        City("novosibirsk",  "Новосибирск",           "Сибирь"),
        City("krasnoyarsk",  "Красноярск",            "Сибирь"),
        City("omsk",         "Омск",                  "Сибирь"),
        City("tomsk",        "Томск",                 "Сибирь"),
        City("barnaul",      "Барнаул",               "Сибирь"),
        City("gorno_altaysk","Горно-Алтайск",         "Сибирь"),
        City("irkutsk",      "Иркутск",               "Сибирь"),
        City("baikal",       "Байкал",                "Сибирь"),
        City("kemerovo",     "Кемерово",              "Сибирь"),
        City("abakan",       "Абакан",                "Сибирь"),
        // Дальний Восток
        City("vladivostok",  "Владивосток",           "Дальний Восток"),
        City("khabarovsk",   "Хабаровск",             "Дальний Восток"),
        City("yuzhno",       "Южно-Сахалинск",        "Дальний Восток"),
        City("petropavlovsk","Петропавловск-Камчатский","Дальний Восток"),
        City("yakutsk",      "Якутск",                "Дальний Восток")
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
