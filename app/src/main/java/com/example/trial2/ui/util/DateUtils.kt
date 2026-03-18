package com.trail2.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

fun formatDate(raw: String, locale: Locale = Locale.getDefault()): String {
    val date = try {
        LocalDateTime.parse(raw.trimEnd('Z'), DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate()
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: DateTimeParseException) {
            return raw
        }
    }

    val today = LocalDate.now()
    val days = ChronoUnit.DAYS.between(date, today)

    val isRu = locale.language == "ru"

    return when {
        days == 0L -> if (isRu) "сегодня" else "today"
        days == 1L -> if (isRu) "вчера" else "yesterday"
        days < 7L -> if (isRu) "$days дн. назад" else "$days days ago"
        else -> {
            val pattern = if (date.year == today.year) "d MMM" else "d MMM yyyy"
            date.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
}
