package com.nexus.app.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.unit.ColorProvider
import com.nexus.app.R

enum class WidgetSizeTier {
    SMALL,
    MEDIUM,
    LARGE
}

object NexusWidgetColors {
    val Background: ColorProvider = DayNightColorProvider(
        day = Color(0xFFF6F6F6),
        night = Color(0xFF050505)
    )
    val Surface: ColorProvider = DayNightColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF0C0C0C)
    )
    val SurfaceElevated: ColorProvider = DayNightColorProvider(
        day = Color(0xFFEDEDED),
        night = Color(0xFF121212)
    )
    val Border: ColorProvider = DayNightColorProvider(
        day = Color(0xFFDCDCDC),
        night = Color(0xFF242424)
    )
    val TextMuted: ColorProvider = DayNightColorProvider(
        day = Color(0xFF6B6B6B),
        night = Color(0xFF777777)
    )
    val TextSecondary: ColorProvider = DayNightColorProvider(
        day = Color(0xFF5A5A5A),
        night = Color(0xFF9A9A9A)
    )
    val TextPrimary: ColorProvider = DayNightColorProvider(
        day = Color(0xFF111111),
        night = Color(0xFFF2F2F2)
    )
    val Highlight: ColorProvider = DayNightColorProvider(
        day = Color(0xFF000000),
        night = Color(0xFFFFFFFF)
    )

    val Positive: ColorProvider = DayNightColorProvider(
        day = Color(0xFF1E8E3E),
        night = Color(0xFF34C759)
    )
    val OnPositive: ColorProvider = DayNightColorProvider(
        day = Color(0xFFFFFFFF),
        night = Color(0xFF050505)
    )
    val Warning: ColorProvider = DayNightColorProvider(
        day = Color(0xFFB26A00),
        night = Color(0xFFE5A000)
    )
    val Danger: ColorProvider = DayNightColorProvider(
        day = Color(0xFFC62828),
        night = Color(0xFFE5484D)
    )
    val Info: ColorProvider = DayNightColorProvider(
        day = Color(0xFF1A73E8),
        night = Color(0xFF4C8BF5)
    )
}

object NexusWidgetSizes {
    val SMALL = DpSize(130.dp, 100.dp)
    val MEDIUM = DpSize(240.dp, 130.dp)
    val LARGE = DpSize(280.dp, 240.dp)
    val ALL = setOf(SMALL, MEDIUM, LARGE)

    fun tierFor(size: DpSize): WidgetSizeTier = when {
        size.height >= 210.dp && size.width >= 220.dp -> WidgetSizeTier.LARGE
        size.width >= 200.dp || size.height >= 145.dp -> WidgetSizeTier.MEDIUM
        else -> WidgetSizeTier.SMALL
    }
}

@Composable
fun NexusWidgetShell(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NexusWidgetColors.Border)
            .cornerRadius(16.dp)
            .padding(1.dp)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(NexusWidgetColors.Background)
                .cornerRadius(15.dp)
                .padding(12.dp),
            content = content
        )
    }
}

@Composable
fun NexusWidgetHeader(
    context: Context,
    appWidgetId: Int,
    title: String,
    subtitle: String? = null,
    tier: WidgetSizeTier,
    onHeaderAction: Action
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(onHeaderAction)
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = NexusWidgetColors.Highlight,
                    fontSize = if (tier == WidgetSizeTier.SMALL) 12.sp else 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = TextStyle(
                        color = NexusWidgetColors.TextMuted,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
        }
        if (tier != WidgetSizeTier.SMALL) {
            Box(
                modifier = GlanceModifier
                    .size(26.dp)
                    .background(NexusWidgetColors.SurfaceElevated)
                    .cornerRadius(8.dp)
                    .clickable(actionRunCallback<RefreshWidgetsAction>()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_refresh),
                    contentDescription = "Atualizar",
                    modifier = GlanceModifier.size(14.dp),
                    colorFilter = ColorFilter.tint(NexusWidgetColors.TextSecondary)
                )
            }
            Spacer(modifier = GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .size(26.dp)
                    .background(NexusWidgetColors.SurfaceElevated)
                    .cornerRadius(8.dp)
                    .clickable(
                        actionStartActivity(
                            NexusWidgetNavigation.openWidgetConfigIntent(context, appWidgetId)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_settings),
                    contentDescription = "Configurar widget",
                    modifier = GlanceModifier.size(14.dp),
                    colorFilter = ColorFilter.tint(NexusWidgetColors.TextSecondary)
                )
            }
        }
    }
}

@Composable
fun NexusWidgetDivider() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NexusWidgetColors.Border)
    ) {}
}

@Composable
fun NexusWidgetEmptyState(
    title: String,
    subtitle: String? = null,
    onAction: Action
) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(NexusWidgetColors.Surface)
            .cornerRadius(10.dp)
            .padding(10.dp)
            .clickable(onAction)
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = NexusWidgetColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = subtitle,
                style = TextStyle(
                    color = NexusWidgetColors.TextMuted,
                    fontSize = 11.sp
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
fun NexusWidgetErrorState(
    context: Context,
    appWidgetId: Int,
    widgetTitle: String,
    errorMessage: String,
    tier: WidgetSizeTier
) {
    NexusWidgetShell {
        NexusWidgetHeader(
            context = context,
            appWidgetId = appWidgetId,
            title = widgetTitle,
            subtitle = "Falha de leitura local",
            tier = tier,
            onHeaderAction = actionStartActivity(NexusWidgetNavigation.openHomeIntent(context))
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(NexusWidgetColors.Surface)
                .cornerRadius(10.dp)
                .padding(10.dp)
                .clickable(actionRunCallback<RefreshWidgetsAction>())
        ) {
            Text(
                text = errorMessage,
                style = TextStyle(
                    color = NexusWidgetColors.Warning,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 3
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Toque para tentar sincronizar novamente",
                style = TextStyle(
                    color = NexusWidgetColors.TextMuted,
                    fontSize = 10.sp
                ),
                maxLines = 1
            )
        }
    }
}
