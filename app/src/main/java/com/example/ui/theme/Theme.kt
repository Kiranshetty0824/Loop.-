package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SaveLoopColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF08080A),
    primaryContainer = Color(0xFF0F3E22),
    onPrimaryContainer = NeonGreen,
    secondary = SoftMint,
    onSecondary = Color(0xFF0F0E11),
    tertiary = GlowingGreen,
    onTertiary = Color(0xFF0F0E11),
    background = MatteBlackBg,
    onBackground = TextTitanium,
    surface = DeepCharcoalSurface,
    onSurface = Color.White,
    surfaceVariant = CardGray,
    onSurfaceVariant = TextTitanium,
    outline = GlassBorder,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Always premium dark mode first, but configurable
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful neon brand identity
    content: @Composable () -> Unit
) {
    // We enforce the beautiful fintech dark theme to present the product in its best light
    val colorScheme = SaveLoopColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
