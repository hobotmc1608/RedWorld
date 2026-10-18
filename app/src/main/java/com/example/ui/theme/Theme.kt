package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 4. ТЕМЫ ОФОРМЛЕНИЯ:
 * 1. Светлая тема (LIGHT)
 * 2. Тёмная тема (DARK)
 * 3. Как в системе (SYSTEM)
 * 4. RedWorld (REDWORLD — фирменная тёмная тема с красными и бордовыми акцентами)
 * + архитектура пресетов (ThemePreset) для будущих пользовательских тем.
 */

data class CustomThemePreset(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val colorScheme: ColorScheme
)

private val RedWorldSignatureColorScheme = darkColorScheme(
    primary = Color(0xFFE11D48),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5A0E23),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = Color(0xFF3E2406),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color(0xFF28050D),
    tertiaryContainer = Color(0xFF471220),
    onTertiaryContainer = Color(0xFFFFE4E6),
    background = Color(0xFF0D0608),
    onBackground = Color(0xFFF9F1F3),
    surface = Color(0xFF160B0F),
    onSurface = Color(0xFFF9F1F3),
    surfaceVariant = Color(0xFF211016),
    onSurfaceVariant = Color(0xFFC8AEB5),
    surfaceContainer = Color(0xFF211016),
    surfaceContainerHigh = Color(0xFF2D151E),
    outline = Color(0xFF4C1D2E),
    error = Color(0xFFF87171),
    onError = Color(0xFF350606)
)

private val StudioNeutralDarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0A192F),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFF59E0B),
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = Color(0xFF3E2406),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF161920),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1F242D),
    onSurfaceVariant = Color(0xFF9CA3AF),
    surfaceContainer = Color(0xFF1C2029),
    surfaceContainerHigh = Color(0xFF262B36),
    outline = Color(0xFF374151),
    error = Color(0xFFF87171),
    onError = Color(0xFF350606)
)

private val StudioLightColorScheme = lightColorScheme(
    primary = Color(0xFFBE123C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF6D28D9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = Color(0xFFFAF8F9),
    onBackground = Color(0xFF1C1417),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1417),
    surfaceVariant = Color(0xFFF3ECEE),
    onSurfaceVariant = Color(0xFF5C474E),
    surfaceContainer = Color(0xFFF6F0F2),
    surfaceContainerHigh = Color(0xFFEDE4E7),
    outline = Color(0xFFD6C4C9),
    error = Color(0xFFDC2626),
    onError = Color.White
)

val BuiltInThemePresets: List<CustomThemePreset> = listOf(
    CustomThemePreset("REDWORLD", "RedWorld (Фирменная)", true, RedWorldSignatureColorScheme),
    CustomThemePreset("DARK", "Тёмная графитовая", true, StudioNeutralDarkColorScheme),
    CustomThemePreset("LIGHT", "Светлая студийная", false, StudioLightColorScheme)
)

val RedWorldShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun RedWorldTheme(
    themeMode: String = AppThemeMode.REDWORLD.code,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val mode = AppThemeMode.fromCode(themeMode)
    val palette = when (mode) {
        AppThemeMode.REDWORLD -> RedWorldStudioPalette
        AppThemeMode.DARK -> ModernDarkStudioPalette
        AppThemeMode.LIGHT -> CleanLightStudioPalette
        AppThemeMode.SYSTEM -> if (systemDark) ModernDarkStudioPalette else CleanLightStudioPalette
    }
    val scheme = when (mode) {
        AppThemeMode.REDWORLD -> RedWorldSignatureColorScheme
        AppThemeMode.DARK -> StudioNeutralDarkColorScheme
        AppThemeMode.LIGHT -> StudioLightColorScheme
        AppThemeMode.SYSTEM -> if (systemDark) StudioNeutralDarkColorScheme else StudioLightColorScheme
    }

    currentStudioPalette = palette
    SideEffect {
        currentStudioPalette = palette
    }

    CompositionLocalProvider(LocalStudioPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = RedWorldShapes,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RedWorldTheme(
        themeMode = if (darkTheme) AppThemeMode.REDWORLD.code else AppThemeMode.LIGHT.code,
        content = content
    )
}
