package com.digitalwellness.coach.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Color Palette ─────────────────────────────────────────────────────────────
// Brand: deep indigo + wellness teal + warm amber accent

val Indigo900 = Color(0xFF1A0F4E)
val Indigo700 = Color(0xFF3D2ABA)
val Indigo500 = Color(0xFF6B52F5)
val Indigo200 = Color(0xFFBDB4FA)

val Teal500 = Color(0xFF00BFA5)
val Teal200 = Color(0xFF69F0AE)

val Amber500 = Color(0xFFFFAB00)
val Amber200 = Color(0xFFFFE57F)

val ErrorRed = Color(0xFFCF6679)
val SuccessGreen = Color(0xFF4CAF50)
val WarnOrange = Color(0xFFFF9800)
val DangerRed = Color(0xFFF44336)

// ─── Light Scheme ─────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary = Indigo700,
    onPrimary = Color.White,
    primaryContainer = Indigo200,
    onPrimaryContainer = Indigo900,
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBE5),
    onSecondaryContainer = Color(0xFF00332D),
    tertiary = Amber500,
    onTertiary = Color.Black,
    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF1B1B2E),
    surface = Color.White,
    onSurface = Color(0xFF1B1B2E),
    surfaceVariant = Color(0xFFEAE7FF),
    error = ErrorRed,
    outline = Color(0xFFB8B5CF)
)

// ─── Dark Scheme ──────────────────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = Indigo200,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo200,
    secondary = Teal200,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF00534A),
    onSecondaryContainer = Teal200,
    tertiary = Amber200,
    onTertiary = Color(0xFF3D2E00),
    background = Color(0xFF0F0C1E),
    onBackground = Color(0xFFE5E1FF),
    surface = Color(0xFF1B1830),
    onSurface = Color(0xFFE5E1FF),
    surfaceVariant = Color(0xFF2C2845),
    error = Color(0xFFFFB3C1),
    outline = Color(0xFF685F7A)
)

// ─── Typography ───────────────────────────────────────────────────────────────

val WellnessTypography = Typography()

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun DigitalWellnessTheme(
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
        typography = WellnessTypography,
        content = content
    )
}
