package com.example.zazentimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZazenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD8B77A),
    onPrimary = Color(0xFF211A0D),
    secondary = Color(0xFFA8B99D),
    onSecondary = Color(0xFF172017),
    tertiary = Color(0xFFB6C7D8),
    background = Color(0xFF111613),
    onBackground = Color(0xFFF0EEE6),
    surface = Color(0xFF1A211D),
    onSurface = Color(0xFFF0EEE6),
    surfaceVariant = Color(0xFF29332D),
    onSurfaceVariant = Color(0xFFC0C8BE),
    outline = Color(0xFF566057)
)

private val ZazenLightColorScheme = lightColorScheme(
    primary = Color(0xFF725A2A),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF52664F),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF526779),
    background = Color(0xFFF5F2E9),
    onBackground = Color(0xFF20251F),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF20251F),
    surfaceVariant = Color(0xFFE9E6DA),
    onSurfaceVariant = Color(0xFF545B52),
    outline = Color(0xFF7C8177)
)

@Composable
fun ZazenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ZazenDarkColorScheme else ZazenLightColorScheme,
        content = content
    )
}
