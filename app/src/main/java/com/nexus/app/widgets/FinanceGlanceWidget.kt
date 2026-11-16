package com.nexus.app.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.domain.model.TransactionType

class FinanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanceGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        NexusWidgetConfigStore.delete(context, appWidgetIds)
    }
}

class FinanceGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(NexusWidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = runCatching {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        }.getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)
        val config = NexusWidgetConfigStore.load(context, appWidgetId, NexusWidgetType.FINANCE)
        val state = NexusWidgetUpdater.loadSafeSnapshots(context)

        provideContent {
            val tier = NexusWidgetSizes.tierFor(LocalSize.current)
            when (state) {
                is WidgetDataState.Error -> NexusWidgetErrorState(
                    context = context,
                    appWidgetId = appWidgetId,
                    widgetTitle = "FINANCEIRO",
                    errorMessage = state.message,
                    tier = tier
                )

                is WidgetDataState.Ready -> FinanceWidgetContent(
                    context = context,
                    appWidgetId = appWidgetId,
                    snapshot = state.data.finance,
                    config = config,
                    tier = tier
                )
            }
        }
    }
}

@Composable
private fun FinanceWidgetContent(
    context: Context,
    appWidgetId: Int,
    snapshot: FinanceWidgetSnapshot,
    config: NexusWidgetInstanceConfig,
    tier: WidgetSizeTier
) {
    val financeAction = actionStartActivity(NexusWidgetNavigation.openFinanceIntent(context))
    val cardsAction = actionStartActivity(NexusWidgetNavigation.openFinanceCardsIntent(context))
    val maxDueRows = when {
        tier == WidgetSizeTier.SMALL -> 1
        config.compactMode -> 1
        tier == WidgetSizeTier.MEDIUM -> config.maxItems.coerceAtMost(2)
        else -> config.maxItems.coerceAtMost(3)
    }

    NexusWidgetShell {
        NexusWidgetHeader(
            context = context,
            appWidgetId = appWidgetId,
            title = "FINANCEIRO",
            subtitle = if (snapshot.overdueCount > 0) "${snapshot.overdueCount} vencida(s)" else "Visão local",
            tier = tier,
            onHeaderAction = financeAction
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        if (snapshot.isEmpty) {
            NexusWidgetEmptyState(
                title = "Nenhum registro financeiro",
                subtitle = if (tier == WidgetSizeTier.SMALL) null else "Toque para registrar saldo, contas ou cartões.",
                onAction = financeAction
            )
        } else {
            if (config.showDeclaredBalance) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .background(NexusWidgetColors.Surface)
                        .cornerRadius(8.dp)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .clickable(financeAction)
                ) {
                    Text(
                        text = "Saldo informado",
                        style = TextStyle(color = NexusWidgetColors.TextMuted, fontSize = 10.sp)
                    )
                    Text(
                        text = DateTimeUtils.formatBrl(snapshot.declaredBalanceCents),
                        style = TextStyle(
                            color = if (snapshot.declaredBalanceCents >= 0L) NexusWidgetColors.Highlight else NexusWidgetColors.Danger,
                            fontSize = if (tier == WidgetSizeTier.SMALL) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
            }

            if (config.showPlannedTotals && tier != WidgetSizeTier.SMALL) {
                Spacer(modifier = GlanceModifier.height(5.dp))
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .clickable(financeAction),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(NexusWidgetColors.Surface)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Receitas prev. (mês)",
                            style = TextStyle(color = NexusWidgetColors.TextMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = DateTimeUtils.formatBrl(snapshot.plannedIncomeCents),
                            style = TextStyle(
                                color = NexusWidgetColors.Positive,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (!config.compactMode) {
                            Text(
                                text = "Real. mês: ${DateTimeUtils.formatBrl(snapshot.receivedIncomeCents)}",
                                style = TextStyle(color = NexusWidgetColors.TextSecondary, fontSize = 9.sp),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .background(NexusWidgetColors.Surface)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Despesas prev. (mês)",
                            style = TextStyle(color = NexusWidgetColors.TextMuted, fontSize = 9.sp)
                        )
                        Text(
                            text = DateTimeUtils.formatBrl(snapshot.plannedExpenseCents),
                            style = TextStyle(
                                color = NexusWidgetColors.Warning,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        if (!config.compactMode) {
                            Text(
                                text = "Pagas mês: ${DateTimeUtils.formatBrl(snapshot.paidExpenseCents)}",
                                style = TextStyle(color = NexusWidgetColors.TextSecondary, fontSize = 9.sp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (config.showOverdueAndOpenStatements && (snapshot.overdueCount > 0 || snapshot.openStatementsCount > 0)) {
                Spacer(modifier = GlanceModifier.height(5.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (snapshot.overdueCount > 0) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(NexusWidgetColors.SurfaceElevated)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .clickable(financeAction)
                        ) {
                            Text(
                                text = "Atrasadas (${snapshot.overdueCount})",
                                style = TextStyle(color = NexusWidgetColors.Danger, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = DateTimeUtils.formatBrl(snapshot.overdueExpenseCents),
                                style = TextStyle(color = NexusWidgetColors.TextPrimary, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }
                    if (snapshot.overdueCount > 0 && snapshot.openStatementsCount > 0) {
                        Spacer(modifier = GlanceModifier.width(6.dp))
                    }
                    if (snapshot.openStatementsCount > 0) {
                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .background(NexusWidgetColors.SurfaceElevated)
                                .cornerRadius(8.dp)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .clickable(cardsAction)
                        ) {
                            Text(
                                text = "Faturas (${snapshot.openStatementsCount})",
                                style = TextStyle(
                                    color = if (snapshot.overdueStatementsCount > 0) NexusWidgetColors.Danger else NexusWidgetColors.Info,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = DateTimeUtils.formatBrl(snapshot.openStatementsTotalCents),
                                style = TextStyle(color = NexusWidgetColors.TextPrimary, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (config.showNextDue && snapshot.upcomingDueTransactions.isNotEmpty() && tier != WidgetSizeTier.SMALL) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = "PRÓXIMOS VENCIMENTOS",
                    style = TextStyle(
                        color = NexusWidgetColors.TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                snapshot.upcomingDueTransactions.take(maxDueRows).forEachIndexed { idx, row ->
                    if (idx > 0) Spacer(modifier = GlanceModifier.height(4.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(NexusWidgetColors.Surface)
                            .cornerRadius(8.dp)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                            .clickable(
                                actionStartActivity(
                                    NexusWidgetNavigation.openTransactionIntent(context, row.id)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = row.description,
                                style = TextStyle(color = NexusWidgetColors.TextPrimary, fontSize = 11.sp),
                                maxLines = 1
                            )
                            Text(
                                text = DateTimeUtils.formatDate(row.dueAt),
                                style = TextStyle(
                                    color = if (row.isOverdue) NexusWidgetColors.Danger else NexusWidgetColors.TextMuted,
                                    fontSize = 9.sp
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = DateTimeUtils.formatBrl(row.amountCents),
                            style = TextStyle(
                                color = if (row.type == TransactionType.INCOME) NexusWidgetColors.Positive else NexusWidgetColors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
