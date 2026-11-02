package com.example.zazentimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZazenDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB9F6CA),
    secondary = Color(0xFFA7F3D0),
    tertiary = Color(0xFF7AD7F0),
    background = Color(0xFF101411),
    surface = Color(0xFF1A221D),
    surfaceVariant = Color(0xFF2A352F),
    onBackground = Color(0xFFEAFBEF),
    onSurface = Color(0xFFEAFBEF),
    onSurfaceVariant = Color(0xFFC7D9CF)
)

private val ZazenLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFF1565C0),
    background = Color(0xFFF5FBF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3F2E8),
    onBackground = Color(0xFF101411),
    onSurface = Color(0xFF101411),
    onSurfaceVariant = Color(0xFF47524E)
)

@Composable
fun ZazenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZazenDarkColorScheme else ZazenLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
