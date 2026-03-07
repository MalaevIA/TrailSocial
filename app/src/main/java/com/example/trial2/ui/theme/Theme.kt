package com.trail2.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ForestGreen = Color(0xFF2D6A4F)
val MossGreen = Color(0xFF52B788)
val SageGreen = Color(0xFF95D5B2)
val PaleGreen = Color(0xFFD8F3DC)
val EarthBrown = Color(0xFF774936)
val WarmSand = Color(0xFFE9C46A)
val StoneGray = Color(0xFF495057)
val DarkForest = Color(0xFF1B4332)
val CreamWhite = Color(0xFFFFFBF5)

val TrailLightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = PaleGreen,
    onPrimaryContainer = DarkForest,
    secondary = EarthBrown,
    onSecondary = Color.White,
    tertiary = Color(0xFF457B9D),
    surface = CreamWhite,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFEEF1EE),
    onSurfaceVariant = StoneGray,
    background = Color(0xFFF5F7F5),
    outline = Color(0xFFB0BEC5),
    error = Color(0xFFE63946)
)

val TrailDarkColorScheme = darkColorScheme(
    primary = MossGreen,
    onPrimary = DarkForest,
    primaryContainer = Color(0xFF1B4332),
    onPrimaryContainer = PaleGreen,
    secondary = Color(0xFFD4A373),
    onSecondary = Color(0xFF3E2723),
    tertiary = Color(0xFF7EB8DA),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E3DF),
    surfaceVariant = Color(0xFF2C2F2C),
    onSurfaceVariant = Color(0xFFA0A4A0),
    background = Color(0xFF111311),
    outline = Color(0xFF5A5E5A),
    error = Color(0xFFFF6B6B)
)

// Keep backward-compatible alias
val TrailColorScheme = TrailLightColorScheme

@Composable
fun TrailSocialTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TrailDarkColorScheme else TrailLightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}
