package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = WormGptRedAccent,
    onPrimary = Color.White,
    primaryContainer = WormGptRedDark,
    onPrimaryContainer = Color.White,
    secondary = WormGptRedGlow,
    onSecondary = Color.Black,
    background = WormGptBackground,
    onBackground = WormGptTextPrimary,
    surface = WormGptSurface,
    onSurface = WormGptTextPrimary,
    surfaceVariant = WormGptSurfaceVariant,
    onSurfaceVariant = WormGptTextSecondary,
    outline = WormGptBorderRed
)

@Composable
fun NovaAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun WormGptTheme(
    content: @Composable () -> Unit
) {
    NovaAiTheme(content)
}

