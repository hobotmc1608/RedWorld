package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppThemeMode(
    val id: String,
    val titleRu: String,
    val subtitleRu: String
) {
    LIGHT(
        id = "LIGHT",
        titleRu = "Светлая тема",
        subtitleRu = "Дневная студийная палитра с высокой читаемостью и мягкими контрастами"
    ),
    DARK(
        id = "DARK",
        titleRu = "Тёмная тема",
        subtitleRu = "Современная графитово-сланцевая тёмная тема для комфортной работы"
    ),
    SYSTEM(
        id = "SYSTEM",
        titleRu = "Как в системе",
        subtitleRu = "Автоматически переключается между светлой и тёмной темой Android"
    ),
    REDWORLD(
        id = "REDWORLD",
        titleRu = "RedWorld",
        subtitleRu = "Фирменная тёмная тема с глубокими бордовыми и алыми акцентами"
    );

    val code: String get() = id
    val descriptionRu: String get() = subtitleRu

    companion object {
        fun fromId(value: String?): AppThemeMode {
            return entries.firstOrNull { it.id.equals(value, ignoreCase = true) } ?: REDWORLD
        }

        fun fromCode(code: String?): AppThemeMode = fromId(code)
    }
}

val LocalStudioPalette = staticCompositionLocalOf { RedWorldStudioPalette }

data class StudioColorPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceElevated: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryBright: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val statusCanonGreen: Color,
    val statusAssumptionAmber: Color,
    val statusDraftIndigo: Color,
    val statusReviewRed: Color
)

val RedWorldStudioPalette = StudioColorPalette(
    isDark = true,
    background = Color(0xFF0D0608),
    surface = Color(0xFF160B0F),
    surfaceContainer = Color(0xFF211016),
    surfaceElevated = Color(0xFF2D151E),
    cardBorder = Color(0xFF4C1D2E),
    primary = Color(0xFFE11D48),
    primaryBright = Color(0xFFF43F5E),
    primaryContainer = Color(0xFF5A0E23),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFF59E0B),
    secondaryContainer = Color(0xFF3E2406),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFFFB7185),
    tertiaryContainer = Color(0xFF471220),
    onTertiaryContainer = Color(0xFFFFE4E6),
    textPrimary = Color(0xFFF9F1F3),
    textSecondary = Color(0xFFC8AEB5),
    statusCanonGreen = Color(0xFF10B981),
    statusAssumptionAmber = Color(0xFFF59E0B),
    statusDraftIndigo = Color(0xFF818CF8),
    statusReviewRed = Color(0xFFEF4444)
)

val ModernDarkStudioPalette = StudioColorPalette(
    isDark = true,
    background = Color(0xFF0B0F17),
    surface = Color(0xFF111827),
    surfaceContainer = Color(0xFF1A2234),
    surfaceElevated = Color(0xFF243047),
    cardBorder = Color(0xFF334155),
    primary = Color(0xFF6366F1),
    primaryBright = Color(0xFF818CF8),
    primaryContainer = Color(0xFF1E1B4B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF14B8A6),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFF38BDF8),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    statusCanonGreen = Color(0xFF10B981),
    statusAssumptionAmber = Color(0xFFF59E0B),
    statusDraftIndigo = Color(0xFF818CF8),
    statusReviewRed = Color(0xFFEF4444)
)

val CleanLightStudioPalette = StudioColorPalette(
    isDark = false,
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEEF2F6),
    cardBorder = Color(0xFFCBD5E1),
    primary = Color(0xFFBE123C),
    primaryBright = Color(0xFFE11D48),
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = Color(0xFF881337),
    secondary = Color(0xFFB45309),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF4338CA),
    tertiaryContainer = Color(0xFFE0E7FF),
    onTertiaryContainer = Color(0xFF312E81),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    statusCanonGreen = Color(0xFF059669),
    statusAssumptionAmber = Color(0xFFD97706),
    statusDraftIndigo = Color(0xFF4F46E5),
    statusReviewRed = Color(0xFFDC2626)
)

internal var currentStudioPalette by mutableStateOf(RedWorldStudioPalette)

// Dynamic semantic color tokens that automatically adapt to Light, Dark, System, and RedWorld themes
val ObsidianBackground: Color
    get() = currentStudioPalette.background

val ObsidianSurface: Color
    get() = currentStudioPalette.surface

val BurgundySurfaceContainer: Color
    get() = currentStudioPalette.surfaceContainer

val BurgundySurfaceElevated: Color
    get() = currentStudioPalette.surfaceElevated

val BurgundyCardBorder: Color
    get() = currentStudioPalette.cardBorder

val CrimsonPrimary: Color
    get() = currentStudioPalette.primary

val CrimsonPrimaryBright: Color
    get() = currentStudioPalette.primaryBright

val CrimsonContainer: Color
    get() = currentStudioPalette.primaryContainer

val OnCrimsonContainer: Color
    get() = currentStudioPalette.onPrimaryContainer

val AmberGoldSecondary: Color
    get() = currentStudioPalette.secondary

val AmberGoldContainer: Color
    get() = currentStudioPalette.secondaryContainer

val OnAmberGoldContainer: Color
    get() = currentStudioPalette.onSecondaryContainer

val RoseTertiary: Color
    get() = currentStudioPalette.tertiary

val RoseTertiaryContainer: Color
    get() = currentStudioPalette.tertiaryContainer

val OnRoseTertiaryContainer: Color
    get() = currentStudioPalette.onTertiaryContainer

val TextIvoryPrimary: Color
    get() = currentStudioPalette.textPrimary

val TextMutedRose: Color
    get() = currentStudioPalette.textSecondary

val StatusCanonGreen: Color
    get() = currentStudioPalette.statusCanonGreen

val CanonVerifiedGreen: Color
    get() = currentStudioPalette.statusCanonGreen

val StatusAssumptionAmber: Color
    get() = currentStudioPalette.statusAssumptionAmber

val StatusDraftIndigo: Color
    get() = currentStudioPalette.statusDraftIndigo

val StatusReviewRed: Color
    get() = currentStudioPalette.statusReviewRed
