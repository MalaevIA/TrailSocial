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

val TrailColorScheme = lightColorScheme(
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

@Composable
fun TrailSocialTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TrailColorScheme, content = content)
}
