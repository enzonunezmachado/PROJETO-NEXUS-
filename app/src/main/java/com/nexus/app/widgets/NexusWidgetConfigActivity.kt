package com.nexus.app.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexus.app.NexusApplication
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.theme.LocalNexusPalette
import com.nexus.app.ui.theme.NexusTheme
import kotlinx.coroutines.launch

class NexusWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            Activity.RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val widgetType = resolveWidgetType(appWidgetId)
        val initialConfig = NexusWidgetConfigStore.load(this, appWidgetId, widgetType)
        val appContainer = (application as NexusApplication).container

        setContent {
            val settings by appContainer.repository.observeSettings()
                .collectAsStateWithLifecycle(
                    initialValue = SettingsEntity(
                        createdAt = 0L,
                        updatedAt = 0L
                    )
                )

            NexusTheme(darkTheme = settings.darkTheme) {
                WidgetConfigScreen(
                    widgetType = widgetType,
                    initialConfig = initialConfig,
                    onCancel = { finish() },
                    onSave = { updated ->
                        NexusWidgetConfigStore.save(this, updated)
                        val resultValue = Intent().putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            appWidgetId
                        )
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }

    private fun resolveWidgetType(appWidgetId: Int): NexusWidgetType {
        val providerClassName = runCatching {
            AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className
        }.getOrNull()
        return when (providerClassName) {
            MyDayWidgetReceiver::class.java.name -> NexusWidgetType.MY_DAY
            MyTasksWidgetReceiver::class.java.name -> NexusWidgetType.MY_TASKS
            FinanceWidgetReceiver::class.java.name -> NexusWidgetType.FINANCE
            NexusSummaryWidgetReceiver::class.java.name -> NexusWidgetType.NEXUS_SUMMARY
            else -> NexusWidgetType.MY_DAY
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    widgetType: NexusWidgetType,
    initialConfig: NexusWidgetInstanceConfig,
    onCancel: () -> Unit,
    onSave: suspend (NexusWidgetInstanceConfig) -> Unit
) {
    val palette = LocalNexusPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(initialConfig) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = palette.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Configurar widget • ${widgetType.title}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.textPrimary
            )
            Text(
                text = "Instância #${config.appWidgetId}. As alterações aplicam somente a este widget na tela inicial.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Text(
                    text = "Estilo e densidade",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                ConfigSwitchRow(
                    title = "Visual compacto",
                    subtitle = "Reduz metadados extras para caber melhor em espaços menores.",
                    checked = config.compactMode,
                    onCheckedChange = { config = config.copy(compactMode = it) }
                )
                HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Quantidade de itens exibidos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4, 5, 6).forEach { count ->
                        FilterChip(
                            selected = config.maxItems == count,
                            onClick = { config = config.copy(maxItems = count) },
                            label = { Text("$count") }
                        )
                    }
                }
            }

            NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Text(
                    text = "Seções visíveis",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (widgetType) {
                    NexusWidgetType.MY_DAY -> {
                        ConfigSwitchRow(
                            title = "Contadores de tarefas",
                            subtitle = "Exibe total de pendentes e concluídas.",
                            checked = config.showTaskCounters,
                            onCheckedChange = { config = config.copy(showTaskCounters = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Compromissos de hoje",
                            subtitle = "Lista compromissos e horários da agenda unificada.",
                            checked = config.showAppointments,
                            onCheckedChange = { config = config.copy(showAppointments = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Tarefas com prazo hoje",
                            subtitle = "Mostra tarefas vencendo na data atual.",
                            checked = config.showTasksDueToday,
                            onCheckedChange = { config = config.copy(showTasksDueToday = it) }
                        )
                    }

                    NexusWidgetType.MY_TASKS -> {
                        ConfigSwitchRow(
                            title = "Contador de pendentes",
                            subtitle = "Mostra totais de pendentes e prioritárias no cabeçalho.",
                            checked = config.showPendingCounter,
                            onCheckedChange = { config = config.copy(showPendingCounter = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Projeto relacionado",
                            subtitle = "Exibe o nome do projeto quando existir.",
                            checked = config.showProjectName,
                            onCheckedChange = { config = config.copy(showProjectName = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Prazo, prioridade e status",
                            subtitle = "Exibe prioridade, status bloqueado e data limite.",
                            checked = config.showDueAndStatus,
                            onCheckedChange = { config = config.copy(showDueAndStatus = it) }
                        )
                    }

                    NexusWidgetType.FINANCE -> {
                        ConfigSwitchRow(
                            title = "Saldo informado",
                            subtitle = "Exibe o saldo informado manualmente sem misturar com cálculos.",
                            checked = config.showDeclaredBalance,
                            onCheckedChange = { config = config.copy(showDeclaredBalance = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Receitas e despesas previstas/pagas",
                            subtitle = "Diferencia valores previstos e efetivamente realizados.",
                            checked = config.showPlannedTotals,
                            onCheckedChange = { config = config.copy(showPlannedTotals = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Contas atrasadas e faturas em aberto",
                            subtitle = "Destaca vencimentos em atraso e faturas abertas.",
                            checked = config.showOverdueAndOpenStatements,
                            onCheckedChange = { config = config.copy(showOverdueAndOpenStatements = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Próximos vencimentos",
                            subtitle = "Lista os próximos lançamentos com acesso direto ao registro.",
                            checked = config.showNextDue,
                            onCheckedChange = { config = config.copy(showNextDue = it) }
                        )
                    }

                    NexusWidgetType.NEXUS_SUMMARY -> {
                        ConfigSwitchRow(
                            title = "Tarefas pendentes",
                            subtitle = "Resumo e acesso ao módulo de tarefas.",
                            checked = config.showTasksSection,
                            onCheckedChange = { config = config.copy(showTasksSection = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Compromissos de hoje",
                            subtitle = "Quantidade de compromissos e próximo horário.",
                            checked = config.showTodayAppointmentsSection,
                            onCheckedChange = { config = config.copy(showTodayAppointmentsSection = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Próximos eventos",
                            subtitle = "Contagem e data do próximo evento.",
                            checked = config.showEventsSection,
                            onCheckedChange = { config = config.copy(showEventsSection = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Vencimentos financeiros",
                            subtitle = "Próximo vencimento ou pendência em atraso.",
                            checked = config.showFinanceSection,
                            onCheckedChange = { config = config.copy(showFinanceSection = it) }
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                        ConfigSwitchRow(
                            title = "Resumo de metas financeiras",
                            subtitle = "Exibido quando houver metas cadastradas no banco.",
                            checked = config.showGoalsSection,
                            onCheckedChange = { config = config.copy(showGoalsSection = it) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            NexusWidgetConfigStore.save(context, config)
                            NexusWidgetUpdater.updateWidgetById(context, config.appWidgetId)
                            onSave(config)
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSaving) "Aplicando..." else "Aplicar widget")
                }
            }
        }
    }
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
