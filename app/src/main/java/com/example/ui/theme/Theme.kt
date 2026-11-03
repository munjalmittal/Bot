package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = VortexCyan,
    onPrimary = VortexDarkBg,
    primaryContainer = VortexSurfaceElevated,
    onPrimaryContainer = VortexCyanGlow,
    secondary = VortexPurple,
    onSecondary = VortexDarkBg,
    secondaryContainer = VortexSurfaceVariant,
    onSecondaryContainer = VortexPurpleGlow,
    tertiary = VortexGreen,
    background = VortexDarkBg,
    onBackground = TextPrimaryDark,
    surface = VortexSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = VortexSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = VortexSurfaceElevated
)

private val LightColorScheme = lightColorScheme(
    primary = VortexPrimaryLight,
    onPrimary = VortexBgLight,
    background = VortexBgLight,
    onBackground = VortexDarkBg,
    surface = VortexSurfaceLight,
    onSurface = VortexDarkBg,
    surfaceVariant = VortexSurfaceVariantLight,
    onSurfaceVariant = TextMutedDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to futuristic dark theme for action hub
    dynamicColor: Boolean = false, // Keep branded high-tech neon palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
