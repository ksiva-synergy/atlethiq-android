package com.example.atlethiq.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SignalLime,
    onPrimary = Base,
    background = Base,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = SurfaceDeep,
    onSurfaceVariant = Muted,
    outline = Line
)

@Composable
fun AtlethiqTheme(
    content: @Composable () -> Unit
) {
    // Force Dark Theme only per design spec. Ignore dynamic coloring.
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
