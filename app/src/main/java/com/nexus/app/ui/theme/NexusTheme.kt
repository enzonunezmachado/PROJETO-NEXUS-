package com.nexus.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class NexusPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val highlight: Color,
    val positive: Color,
    val danger: Color,
    val warning: Color,
    val info: Color,
    val accentNeon: Color,
    val onAccentNeon: Color,
    val chartSecondary: Color,
    val chartTrack: Color,
    val chartPalette: List<Color>
)

private val DarkPalette = NexusPalette(
    isDark = true,
    background = Color(0xFF050505),
    surface = Color(0xFF0C0C0C),
    surfaceElevated = Color(0xFF121212),
    border = Color(0xFF242424),
    textPrimary = Color(0xFFF2F2F2),
    textSecondary = Color(0xFF9A9A9A),
    highlight = Color(0xFFFFFFFF),
    positive = Color(0xFF34C759),
    danger = Color(0xFFE5484D),
    warning = Color(0xFFE5A000),
    info = Color(0xFF4C8BF5),
    accentNeon = Color(0xFF7CFFB2),
    onAccentNeon = Color(0xFF051A10),
    chartSecondary = Color(0xFFD4D8DF),
    chartTrack = Color(0xFF1C1F24),
    chartPalette = listOf(
        Color(0xFF7CFFB2),
        Color(0xFF5CC8FF),
        Color(0xFFC792FF),
        Color(0xFFFFD166),
        Color(0xFFFF7A90),
        Color(0xFFFF9F6B),
        Color(0xFF4DE1C1),
        Color(0xFFA3B1C6)
    )
)

private val LightPalette = NexusPalette(
    isDark = false,
    background = Color(0xFFF6F6F6),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFEDEDED),
    border = Color(0xFFDCDCDC),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF5A5A5A),
    highlight = Color(0xFF000000),
    positive = Color(0xFF1E8E3E),
    danger = Color(0xFFC62828),
    warning = Color(0xFFB26A00),
    info = Color(0xFF1A73E8),
    accentNeon = Color(0xFF0E9F63),
    onAccentNeon = Color(0xFFFFFFFF),
    chartSecondary = Color(0xFF4A5568),
    chartTrack = Color(0xFFE3E6EC),
    chartPalette = listOf(
        Color(0xFF0E9F63),
        Color(0xFF0B7FBF),
        Color(0xFF7A4FD6),
        Color(0xFFB98100),
        Color(0xFFD6334F),
        Color(0xFFD2691E),
        Color(0xFF0C9C86),
        Color(0xFF5A6B80)
    )
)

val LocalNexusPalette = staticCompositionLocalOf { DarkPalette }

private val NexusTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Medium
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    )
)

@Composable
fun NexusTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.highlight,
            onPrimary = palette.background,
            secondary = palette.textSecondary,
            onSecondary = palette.background,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = palette.highlight,
            onPrimary = palette.surface,
            secondary = palette.textSecondary,
            onSecondary = palette.surface,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceElevated,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalNexusPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NexusTypography,
            content = content
        )
    }
}
