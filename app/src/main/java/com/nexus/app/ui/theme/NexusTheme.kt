package com.nexus.app.ui.theme

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

enum class ColoredBorderRole {
    /** Grandes cartoes/banners de alerta de largura total. */
    ALERT_BANNER_LARGE,
    /** Cartao selecionado na visao semanal da Agenda (referencia visual principal). */
    DAY_CARD_SELECTED,
    /** Cartao representando o dia atual na visao semanal da Agenda. */
    DAY_CARD_TODAY,
    /** Faixas medias (evento dia inteiro, banner de permissao) e filtros coloridos ativos. */
    MEDIUM_ACCENT,
    /** Botoes de acao com destaque semantico explicito. */
    HIGHLIGHTED_BUTTON,
    /** Itens compactos em listas estreitas ou celulas pequenas (conflito semanal, hoje no calendario). */
    COMPACT_ITEM,
    /** Miniaturas funcionais pequenas que exigem definicao clara (checkbox 22dp). */
    SMALL_BADGE_DEFINED,
    /** Chips/badges inline reduzidos de status ou variacao. */
    SMALL_BADGE_SUBTLE
}

fun NexusPalette.coloredBorderWidth(role: ColoredBorderRole): Dp = when (role) {
    ColoredBorderRole.ALERT_BANNER_LARGE -> 1.5.dp
    ColoredBorderRole.DAY_CARD_SELECTED -> 1.75.dp
    ColoredBorderRole.DAY_CARD_TODAY -> 1.25.dp
    ColoredBorderRole.MEDIUM_ACCENT -> 1.25.dp
    ColoredBorderRole.HIGHLIGHTED_BUTTON -> 1.5.dp
    ColoredBorderRole.COMPACT_ITEM -> 1.15.dp
    ColoredBorderRole.SMALL_BADGE_DEFINED -> 1.25.dp
    ColoredBorderRole.SMALL_BADGE_SUBTLE -> 1.dp
}

fun NexusPalette.coloredBorderColor(baseColor: Color, role: ColoredBorderRole): Color {
    val alpha = if (isDark) {
        when (role) {
            ColoredBorderRole.ALERT_BANNER_LARGE -> 0.68f
            ColoredBorderRole.DAY_CARD_SELECTED -> 0.80f
            ColoredBorderRole.DAY_CARD_TODAY -> 0.72f
            ColoredBorderRole.MEDIUM_ACCENT -> 0.68f
            ColoredBorderRole.HIGHLIGHTED_BUTTON -> 0.78f
            ColoredBorderRole.COMPACT_ITEM -> 0.60f
            ColoredBorderRole.SMALL_BADGE_DEFINED -> 0.85f
            ColoredBorderRole.SMALL_BADGE_SUBTLE -> 0.46f
        }
    } else {
        when (role) {
            ColoredBorderRole.ALERT_BANNER_LARGE -> 0.86f
            ColoredBorderRole.DAY_CARD_SELECTED -> 0.94f
            ColoredBorderRole.DAY_CARD_TODAY -> 0.86f
            ColoredBorderRole.MEDIUM_ACCENT -> 0.84f
            ColoredBorderRole.HIGHLIGHTED_BUTTON -> 0.90f
            ColoredBorderRole.COMPACT_ITEM -> 0.80f
            ColoredBorderRole.SMALL_BADGE_DEFINED -> 0.94f
            ColoredBorderRole.SMALL_BADGE_SUBTLE -> 0.62f
        }
    }
    return baseColor.copy(alpha = alpha)
}

fun NexusPalette.coloredBorder(baseColor: Color, role: ColoredBorderRole): BorderStroke =
    BorderStroke(width = coloredBorderWidth(role), color = coloredBorderColor(baseColor, role))

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
    background = Color(0xFFD4D7DC),
    surface = Color(0xFFE3E5E8),
    surfaceElevated = Color(0xFFC9CED4),
    border = Color(0xFFB0B6BF),
    textPrimary = Color(0xFF121417),
    textSecondary = Color(0xFF424852),
    highlight = Color(0xFF121417),
    positive = Color(0xFF136B31),
    danger = Color(0xFFAD1E1E),
    warning = Color(0xFF874E00),
    info = Color(0xFF1353A6),
    accentNeon = Color(0xFF136B31),
    onAccentNeon = Color(0xFFFFFFFF),
    chartSecondary = Color(0xFF4E5763),
    chartTrack = Color(0xFFBCC1C9),
    chartPalette = listOf(
        Color(0xFF136B31),
        Color(0xFF1353A6),
        Color(0xFF5D3AA8),
        Color(0xFF874E00),
        Color(0xFFAD1E1E),
        Color(0xFF9C4716),
        Color(0xFF0E695A),
        Color(0xFF4E5763)
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
