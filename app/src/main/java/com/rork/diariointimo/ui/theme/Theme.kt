package com.rork.diariointimo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightScheme = lightColorScheme(
    primary = Accent.gold,
    onPrimary = Paper.creamLight,
    primaryContainer = Accent.goldLight,
    onPrimaryContainer = Ink.black,
    secondary = Accent.wax,
    onSecondary = Paper.creamLight,
    background = Paper.cream,
    onBackground = Ink.black,
    surface = Paper.creamLight,
    onSurface = Ink.black,
    surfaceVariant = Paper.creamDeep,
    onSurfaceVariant = Ink.faded,
    outline = Paper.edge,
    outlineVariant = Paper.edge,
    error = Accent.wax,
    onError = Paper.creamLight
)

/** Material mirror of «Noche de tinta», so native components keep the mood too. */
private val DarkScheme = darkDiaryColors().let { c ->
    darkColorScheme(
        primary = c.gold,
        onPrimary = c.deskDeep,
        primaryContainer = c.gold,
        onPrimaryContainer = c.deskDeep,
        secondary = c.wax,
        onSecondary = c.ink,
        background = c.paper,
        onBackground = c.ink,
        surface = c.paperLight,
        onSurface = c.ink,
        surfaceVariant = c.paperDeep,
        onSurfaceVariant = c.inkFaded,
        outline = c.edge,
        outlineVariant = c.edge,
        error = c.wax,
        onError = c.ink
    )
}

/**
 * The diary's theme. The palette is never inverted: each mode has its own
 * curated set — warm daylight paper, or «Noche de tinta» at night.
 */
@Composable
fun AppTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (dark) darkDiaryColors() else lightDiaryColors()
    CompositionLocalProvider(LocalDiaryColors provides colors) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content
        )
    }
}
