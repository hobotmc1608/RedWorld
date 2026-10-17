package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val RedWorldDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonContainer,
    onPrimaryContainer = OnCrimsonContainer,
    secondary = AmberGoldSecondary,
    onSecondary = Color(0xFF1F1200),
    secondaryContainer = AmberGoldContainer,
    onSecondaryContainer = OnAmberGoldContainer,
    tertiary = RoseTertiary,
    onTertiary = Color(0xFF28050D),
    tertiaryContainer = RoseTertiaryContainer,
    onTertiaryContainer = OnRoseTertiaryContainer,
    background = ObsidianBackground,
    onBackground = TextIvoryPrimary,
    surface = ObsidianSurface,
    onSurface = TextIvoryPrimary,
    surfaceVariant = BurgundySurfaceContainer,
    onSurfaceVariant = TextMutedRose,
    surfaceContainer = BurgundySurfaceContainer,
    surfaceContainerHigh = BurgundySurfaceElevated,
    outline = BurgundyCardBorder,
    error = Color(0xFFF87171),
    onError = Color(0xFF350606)
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
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RedWorldDarkColorScheme,
        typography = Typography,
        shapes = RedWorldShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    RedWorldTheme(content = content)
}
