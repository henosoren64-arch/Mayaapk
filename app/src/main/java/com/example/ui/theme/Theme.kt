package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MayraColorScheme = darkColorScheme(
    primary = NeonMagenta,
    onPrimary = Color.White,
    primaryContainer = DeepGlassSurface,
    onPrimaryContainer = TextPrimary,
    secondary = ElectricCyan,
    onSecondary = MidnightObsidian,
    secondaryContainer = DarkVioletSurface,
    onSecondaryContainer = ElectricCyan,
    tertiary = ElectricPurple,
    onTertiary = Color.White,
    background = MidnightObsidian,
    onBackground = TextPrimary,
    surface = DarkVioletSurface,
    onSurface = TextPrimary,
    surfaceVariant = DeepGlassSurface,
    onSurfaceVariant = TextSecondary,
    error = StatusError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MayraColorScheme,
        typography = Typography,
        content = content
    )
}
