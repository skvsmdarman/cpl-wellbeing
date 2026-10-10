package com.example.cplswellbeingbynovadevs.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryVioletDark,
    onPrimary = CharcoalBackground,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = PrimaryVioletDark,
    secondary = RomanticRosePink,
    onSecondary = CharcoalBackground,
    background = CharcoalBackground,
    onBackground = OnSurfaceWhite,
    surface = SoftMidnightSurface,
    onSurface = OnSurfaceWhite,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMuted
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryVioletLight,
    onPrimary = LightSurface,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = PrimaryVioletLight,
    secondary = RomanticRoseLight,
    onSecondary = LightSurface,
    background = WarmPearlBackground,
    onBackground = CharcoalBackground,
    surface = LightSurface,
    onSurface = CharcoalBackground,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceMuted
)

@Composable
fun CplsWellbeingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
