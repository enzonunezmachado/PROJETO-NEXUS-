package com.nexus.app.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.BuildConfig
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.backup.BackupImportMode
import com.nexus.app.data.backup.MergeIssueKind
import com.nexus.app.data.backup.NexusBackupPayload
import com.nexus.app.data.db.NexusDatabase
import com.nexus.app.data.db.entity.CategoryEntity
import com.nexus.app.data.db.entity.ErrorLogEntity
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.RoutineAssistantSettingsEntity
import com.nexus.app.data.db.entity.SettingsEntity
import com.nexus.app.domain.model.DatabaseTableCounts
import com.nexus.app.domain.model.GlobalSearchResults
import com.nexus.app.domain.model.NexusDateFormat
import com.nexus.app.ui.BackupUiState
import com.nexus.app.ui.FactoryResetUiState
import com.nexus.app.ui.components.ConfirmDangerDialog
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.components.TaskRowCard
import com.nexus.app.ui.components.parseHexColorOrDefault
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoreScreen(
    onOpenPlanning: () -> Unit,
    onOpenMarket: () -> Unit = {},
    onOpenCustomizeDashboard: () -> Unit,
    onOpenQuoteLibrary: () -> Unit,
    onOpenRoutineAssistant: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenInboxBridge: () -> Unit = {},
    onOpenDiagnostics: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        if (onBack != null) {
            TopSubScreenBar(title = "Mais", onBack = onBack)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack == null) {
                item {
                    Text(
                        text = "Mais",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.textPrimary
                    )
                }
            }

            item {
                MoreMenuItem(
                    icon = Icons.Outlined.Flag,
                    title = "Planejamento e Objetivos",
                    subtitle = "Objetivos pessoais, marcos, histórico de evolução e vínculo com tarefas e projetos",
                    onClick = onOpenPlanning
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.ShowChart,
                    title = "Mercado",
                    subtitle = "Moedas, cripto, índices, ações, gráficos históricos, acompanhamento e alertas de preço",
                    onClick = onOpenMarket
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.DashboardCustomize,
                    title = "Personalizar Dashboard",
                    subtitle = "Ativar, ocultar, reordenar blocos e configurar exibição da tela inicial",
                    onClick = onOpenCustomizeDashboard
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.FormatQuote,
                    title = "Biblioteca de Frases",
                    subtitle = "Frases locais do dia com autoria opcional, rotação offline e importação/exportação",
                    onClick = onOpenQuoteLibrary
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.NotificationsActive,
                    title = "Assistente de Rotina e Notificações",
                    subtitle = "Briefing diário, revisão noturna, canais offline, silêncio e fila de lembretes",
                    onClick = onOpenRoutineAssistant
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.Widgets,
                    title = "Widgets da Tela Inicial",
                    subtitle = "Fixar os 4 widgets nativos (Meu Dia, Tarefas, Financeiro, Resumo) e sincronizar",
                    onClick = onOpenWidgets
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.Search,
                    title = "Pesquisa global",
                    subtitle = "Buscar tarefas, objetivos, projetos, compromissos, eventos, transações e cartões",
                    onClick = onOpenSearch
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.Settings,
                    title = "Configurações",
                    subtitle = "Aparência, saudação, formato de data, categorias e armazenamento local",
                    onClick = onOpenSettings
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.Backup,
                    title = "Backup e Restauração",
                    subtitle = "Exportar ou restaurar banco e configurações do Mercado (v1 a v${NexusBackupPayload.CURRENT_FORMAT_VERSION}) em JSON via SAF",
                    onClick = onOpenBackup
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.CloudDownload,
                    title = "NEXUS-INBOX (Ponte ChatGPT)",
                    subtitle = "Consultar atualizações enviadas pelo ChatGPT, revisar prévia e ver histórico",
                    onClick = onOpenInboxBridge
                )
            }
            item {
                MoreMenuItem(
                    icon = Icons.Outlined.BugReport,
                    title = "Diagnóstico",
                    subtitle = "Versão do banco (v${NexusDatabase.DATABASE_VERSION}), contagem das 28 tabelas e logs",
                    onClick = onOpenDiagnostics
                )
            }
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    NexusCard(
        modifier = Modifier.fillMaxWidth(),
        elevated = true,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = palette.highlight)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

@Composable
fun GlobalSearchScreen(
    query: String,
    results: GlobalSearchResults,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onToggleTaskComplete: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onOpenGoal: (String) -> Unit,
    onOpenAppointment: (String) -> Unit,
    onOpenEvent: (String) -> Unit,
    onOpenTransaction: (String) -> Unit,
    onToggleSettleTransaction: (String) -> Unit = {},
    onOpenFinanceCards: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Buscar tarefas, objetivos, projetos, eventos ou financeiro") },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (query.isBlank()) {
                item {
                    NexusEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "Pesquisa global",
                        subtitle = "Pesquise em tarefas, objetivos pessoais, projetos, compromissos, eventos, lançamentos financeiros e cartões."
                    )
                }
            } else if (results.isEmpty) {
                item {
                    NexusEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "Nenhum resultado",
                        subtitle = "Nenhum registro encontrado para \"$query\"."
                    )
                }
            } else {
                if (results.tasks.isNotEmpty()) {
                    item { SectionHeader(title = "Tarefas", trailing = "${results.tasks.size}") }
                    items(results.tasks, key = { "gs_t_${it.task.id}" }) { task ->
                        TaskRowCard(
                            item = task,
                            onClick = { onOpenTask(task.task.id) },
                            onToggleComplete = { onToggleTaskComplete(task.task.id) }
                        )
                    }
                }
                if (results.events.isNotEmpty()) {
                    item { SectionHeader(title = "Eventos", trailing = "${results.events.size}") }
                    items(results.events, key = { "gs_ev_${it.event.id}" }) { ew ->
                        EventSummaryCard(
                            item = ew,
                            onClick = { onOpenEvent(ew.event.id) }
                        )
                    }
                }
                if (results.transactions.isNotEmpty()) {
                    item { SectionHeader(title = "Lançamentos financeiros", trailing = "${results.transactions.size}") }
                    items(results.transactions, key = { "gs_tx_${it.transaction.id}" }) { tx ->
                        TransactionRowCard(
                            item = tx,
                            onClick = { onOpenTransaction(tx.transaction.id) },
                            onToggleSettle = { onToggleSettleTransaction(tx.transaction.id) }
                        )
                    }
                }
                if (results.creditCards.isNotEmpty()) {
                    item { SectionHeader(title = "Cartões", trailing = "${results.creditCards.size}") }
                    items(results.creditCards, key = { "gs_cc_${it.id}" }) { card ->
                        NexusCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevated = true,
                            onClick = onOpenFinanceCards
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = palette.highlight)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(card.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                                    Text(
                                        text = "${card.institution.ifBlank { "Cartão" }} · Fecha dia ${card.closingDay} · Vence dia ${card.dueDay}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                if (results.projects.isNotEmpty()) {
                    item { SectionHeader(title = "Projetos", trailing = "${results.projects.size}") }
                    items(results.projects, key = { "gs_p_${it.project.id}" }) { pw ->
                        NexusCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevated = true,
                            onClick = { onOpenProject(pw.project.id) }
                        ) {
                            Text(pw.project.name, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "${pw.project.status.label} · ${pw.completedTasks}/${pw.totalTasks} concluídas (${pw.progressPercent}%)",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
                if (results.personalGoals.isNotEmpty()) {
                    item { SectionHeader(title = "Objetivos pessoais", trailing = "${results.personalGoals.size}") }
                    items(results.personalGoals, key = { "gs_pg_${it.goal.id}" }) { pg ->
                        PersonalGoalCard(
                            item = pg,
                            onClick = { onOpenGoal(pg.goal.id) }
                        )
                    }
                }
                if (results.appointments.isNotEmpty()) {
                    item { SectionHeader(title = "Compromissos", trailing = "${results.appointments.size}") }
                    items(results.appointments, key = { "gs_a_${it.appointment.id}" }) { appt ->
                        AppointmentCompactCard(
                            item = appt,
                            onClick = { onOpenAppointment(appt.appointment.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: SettingsEntity,
    personalization: PersonalizationSettingsEntity,
    routineSettings: RoutineAssistantSettingsEntity,
    categories: List<CategoryEntity>,
    factoryResetState: FactoryResetUiState = FactoryResetUiState(),
    onBack: () -> Unit,
    onSaveSettings: (userName: String, darkTheme: Boolean) -> Unit,
    onSaveGreetingAndPresentation: (
        greetingEnabled: Boolean,
        greetingTemplate: String,
        dateFormatPattern: NexusDateFormat,
        showDailyQuoteOnDashboard: Boolean,
        compactDashboardMode: Boolean,
        showMetricSummaryOnDashboard: Boolean
    ) -> Unit,
    onOpenCustomizeDashboard: () -> Unit,
    onOpenQuoteLibrary: () -> Unit,
    onResetDashboardToDefault: () -> Unit,
    onEditDeclaredBalance: () -> Unit,
    onOpenRoutineAssistant: (tabIndex: Int) -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onSaveCategory: (id: String?, name: String, colorHex: String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onGenerateBackupJson: suspend () -> Result<String> = {
        Result.failure(IllegalStateException("Exportação indisponível."))
    },
    onNotifyBackupExported: () -> Unit = {},
    onTriggerFactoryReset: () -> Unit = {},
    onDismissFactoryResetFeedback: () -> Unit = {},
    onLogError: (String, Throwable) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = DateTimeUtils.todayLocalDate()
    var userNameInput by remember(settings.userName) { mutableStateOf(settings.userName) }
    var greetingTemplateInput by remember(personalization.greetingTemplate) {
        mutableStateOf(personalization.greetingTemplate)
    }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateCategory by remember { mutableStateOf(false) }
    var factoryResetDialogStep by remember { mutableStateOf(0) }
    var factoryResetAcknowledged by remember { mutableStateOf(false) }

    val preResetExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val jsonResult = onGenerateBackupJson()
            jsonResult.onSuccess { json ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Não foi possível gravar o backup selecionado.")
                    }
                }.onSuccess {
                    onNotifyBackupExported()
                }.onFailure { err ->
                    onLogError("Backup pré-restauração (SAF)", err)
                }
            }.onFailure { err ->
                onLogError("Backup pré-restauração (JSON)", err)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Configurações", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionHeader(title = "Aparência") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema escuro", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = if (settings.darkTheme) "Escuro minimalista ativo" else "Tema claro ativo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = settings.darkTheme,
                            onCheckedChange = { dark -> onSaveSettings(settings.userName, dark) }
                        )
                    }
                }
            }

            item { SectionHeader(title = "Dashboard") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo compacto no painel", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Ajusta o espaçamento vertical entre os blocos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = personalization.compactDashboardMode,
                            onCheckedChange = { compact ->
                                onSaveGreetingAndPresentation(
                                    personalization.greetingEnabled,
                                    personalization.greetingTemplate,
                                    personalization.dateFormatPattern,
                                    personalization.showDailyQuoteOnDashboard,
                                    compact,
                                    personalization.showMetricSummaryOnDashboard
                                )
                            }
                        )
                    }
                    HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Resumo numérico de tarefas", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Exibe pendentes, concluídas e atrasadas na tela inicial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = personalization.showMetricSummaryOnDashboard,
                            onCheckedChange = { showSummary ->
                                onSaveGreetingAndPresentation(
                                    personalization.greetingEnabled,
                                    personalization.greetingTemplate,
                                    personalization.dateFormatPattern,
                                    personalization.showDailyQuoteOnDashboard,
                                    personalization.compactDashboardMode,
                                    showSummary
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenCustomizeDashboard,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Blocos e ordem", color = palette.textPrimary)
                        }
                        OutlinedButton(
                            onClick = onResetDashboardToDefault,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Restaurar padrão", color = palette.textPrimary)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Saudação e identidade") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exibir saudação no dashboard", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = DateTimeUtils.formatGreeting(userNameInput.ifBlank { settings.userName }, greetingTemplateInput),
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.info
                            )
                        }
                        Switch(
                            checked = personalization.greetingEnabled,
                            onCheckedChange = { enabled ->
                                onSaveGreetingAndPresentation(
                                    enabled,
                                    personalization.greetingTemplate,
                                    personalization.dateFormatPattern,
                                    personalization.showDailyQuoteOnDashboard,
                                    personalization.compactDashboardMode,
                                    personalization.showMetricSummaryOnDashboard
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        label = { Text("Nome de exibição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = greetingTemplateInput,
                        onValueChange = { greetingTemplateInput = it },
                        label = { Text("Texto da saudação") },
                        supportingText = {
                            Text("Use AUTO para bom dia/tarde/noite automático ou escreva um texto usando {nome}.")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onSaveSettings(userNameInput, settings.darkTheme)
                                onSaveGreetingAndPresentation(
                                    personalization.greetingEnabled,
                                    greetingTemplateInput,
                                    personalization.dateFormatPattern,
                                    personalization.showDailyQuoteOnDashboard,
                                    personalization.compactDashboardMode,
                                    personalization.showMetricSummaryOnDashboard
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.highlight,
                                contentColor = palette.background
                            )
                        ) {
                            Text("Salvar saudação")
                        }
                        if (greetingTemplateInput != "AUTO") {
                            OutlinedButton(
                                onClick = {
                                    greetingTemplateInput = "AUTO"
                                    onSaveGreetingAndPresentation(
                                        personalization.greetingEnabled,
                                        "AUTO",
                                        personalization.dateFormatPattern,
                                        personalization.showDailyQuoteOnDashboard,
                                        personalization.compactDashboardMode,
                                        personalization.showMetricSummaryOnDashboard
                                    )
                                },
                                border = BorderStroke(1.dp, palette.border),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Usar AUTO", color = palette.textPrimary)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = "Frases diárias") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Exibir frase do dia no dashboard", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                            Text(
                                text = "Usa apenas frases cadastradas ou importadas localmente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = personalization.showDailyQuoteOnDashboard,
                            onCheckedChange = { showQuote ->
                                onSaveGreetingAndPresentation(
                                    personalization.greetingEnabled,
                                    personalization.greetingTemplate,
                                    personalization.dateFormatPattern,
                                    showQuote,
                                    personalization.compactDashboardMode,
                                    personalization.showMetricSummaryOnDashboard
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenQuoteLibrary,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir biblioteca de frases", color = palette.textPrimary)
                    }
                }
            }

            item { SectionHeader(title = "Idioma e região") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Português (Brasil) · pt-BR", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    Text(
                        text = "Fuso horário configurado: America/Sao_Paulo",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            item { SectionHeader(title = "Data e horário") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text("Formato de data no dashboard", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    NexusDateFormat.entries.forEach { fmt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSaveGreetingAndPresentation(
                                        personalization.greetingEnabled,
                                        personalization.greetingTemplate,
                                        fmt,
                                        personalization.showDailyQuoteOnDashboard,
                                        personalization.compactDashboardMode,
                                        personalization.showMetricSummaryOnDashboard
                                    )
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = personalization.dateFormatPattern == fmt,
                                onClick = {
                                    onSaveGreetingAndPresentation(
                                        personalization.greetingEnabled,
                                        personalization.greetingTemplate,
                                        fmt,
                                        personalization.showDailyQuoteOnDashboard,
                                        personalization.compactDashboardMode,
                                        personalization.showMetricSummaryOnDashboard
                                    )
                                }
                            )
                            Column {
                                Text(fmt.label, style = MaterialTheme.typography.bodyMedium, color = palette.textPrimary)
                                Text(
                                    text = DateTimeUtils.formatDateByPattern(today, fmt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 8.dp))
                    Text("Padrão de horário: 24 horas (00:00–23:59)", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                }
            }

            item { SectionHeader(title = "Moeda e financeiro") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Moeda: BRL (R$)", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                            Text(
                                text = "Saldo informado: ${DateTimeUtils.formatBrl(settings.declaredBalanceCents)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onEditDeclaredBalance,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ajustar saldo", color = palette.textPrimary)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Notificações e Briefing") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = if (routineSettings.notificationsEnabled) {
                            "Notificações ativas · Briefing ${"%02d:%02d".format(routineSettings.morningBriefingHour, routineSettings.morningBriefingMinute)} · Revisão ${"%02d:%02d".format(routineSettings.nightReviewHour, routineSettings.nightReviewMinute)}"
                        } else {
                            "Notificações desativadas nas preferências locais"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenRoutineAssistant(2) },
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Notificações", color = palette.textPrimary)
                        }
                        OutlinedButton(
                            onClick = { onOpenRoutineAssistant(0) },
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Briefing", color = palette.textPrimary)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Widgets e Backup") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenWidgets,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Widgets", color = palette.textPrimary)
                        }
                        OutlinedButton(
                            onClick = onOpenBackup,
                            border = BorderStroke(1.dp, palette.border),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Backup JSON", color = palette.textPrimary)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Privacidade e armazenamento local") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "100% offline no dispositivo (SQLite / Room v${NexusDatabase.DATABASE_VERSION})",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "O NEXUS não utiliza internet, nuvem, login ou rastreamento. Ocultar blocos ou detalhes do dashboard altera somente a exibição e nunca apaga registros reais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categorias", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    OutlinedButton(onClick = { showCreateCategory = true }, shape = RoundedCornerShape(10.dp)) {
                        Text("Nova categoria", color = palette.textPrimary)
                    }
                }
            }

            items(categories, key = { it.id }) { cat ->
                val catColor = parseHexColorOrDefault(cat.colorHex, palette.textSecondary)
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = cat.scope.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(onClick = { editingCategory = cat }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar categoria")
                        }
                        IconButton(onClick = { onDeleteCategory(cat.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Excluir categoria", tint = palette.danger)
                        }
                    }
                }
            }

            item { SectionHeader(title = "Dados e armazenamento") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Restaurar banco de dados de fábrica",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.danger
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Apaga todos os dados locais do aparelho (tarefas, compromissos, eventos, checklists, projetos, metas, financeiro, lembretes, séries recorrentes, frases, favoritos/alertas/caches do Mercado e rastreamento local da NEXUS-INBOX) e recria a base inicial de fábrica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preserva: instalação do app, repositório remoto GitHub, token PAT criptografado e coordenadas configuradas da NEXUS-INBOX.",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.info
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        enabled = !factoryResetState.isResetting,
                        onClick = {
                            factoryResetAcknowledged = false
                            factoryResetDialogStep = 1
                        },
                        border = BorderStroke(1.dp, palette.danger),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (factoryResetState.isResetting) {
                                "Restaurando banco de fábrica..."
                            } else {
                                "Restaurar banco de dados de fábrica"
                            },
                            color = palette.danger
                        )
                    }
                }
            }

            item { SectionHeader(title = "Diagnóstico e versão") }
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "NEXUS ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE}) · Banco Room v${NexusDatabase.DATABASE_VERSION}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenDiagnostics,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir diagnóstico e logs", color = palette.textPrimary)
                    }
                }
            }
        }
    }

    if (showCreateCategory || editingCategory != null) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = {
                showCreateCategory = false
                editingCategory = null
            },
            onSave = { name, colorHex ->
                onSaveCategory(editingCategory?.id, name, colorHex)
                showCreateCategory = false
                editingCategory = null
            }
        )
    }

    if (factoryResetDialogStep == 1) {
        AlertDialog(
            onDismissRequest = { factoryResetDialogStep = 0 },
            containerColor = palette.surfaceElevated,
            title = {
                Text("Restaurar banco de fábrica?", color = palette.danger)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Esta operação apaga todas as 28 tabelas locais do SQLite/Room, cancela lembretes agendados, limpa caches/alertas/favoritos do Mercado e zera o rastreamento local de importações da NEXUS-INBOX.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "As configurações padrão iniciais serão recriadas. Suas credenciais (token GitHub PAT) e coordenadas da NEXUS-INBOX serão mantidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            val timestamp = DateTimeUtils.formatDate(DateTimeUtils.nowEpochMillis())
                                .replace("/", "-")
                            preResetExportLauncher.launch("nexus-backup-pre-reset-$timestamp.json")
                        },
                        border = BorderStroke(1.dp, palette.highlight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fazer backup JSON antes", color = palette.highlight)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        factoryResetAcknowledged = false
                        factoryResetDialogStep = 2
                    }
                ) {
                    Text("Continuar", color = palette.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { factoryResetDialogStep = 0 }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }

    if (factoryResetDialogStep == 2) {
        AlertDialog(
            onDismissRequest = { factoryResetDialogStep = 0 },
            containerColor = palette.surfaceElevated,
            title = {
                Text("Confirmação final e irreversível", color = palette.danger)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Todos os registros locais não exportados serão perdidos permanentemente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { factoryResetAcknowledged = !factoryResetAcknowledged },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = factoryResetAcknowledged,
                            onCheckedChange = { factoryResetAcknowledged = it },
                            colors = CheckboxDefaults.colors(checkedColor = palette.danger)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Entendo que os dados locais serão apagados e o banco será reiniciado do zero.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = factoryResetAcknowledged && !factoryResetState.isResetting,
                    onClick = {
                        factoryResetDialogStep = 0
                        factoryResetAcknowledged = false
                        onTriggerFactoryReset()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.danger,
                        contentColor = palette.background
                    )
                ) {
                    Text("APAGAR E RESTAURAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { factoryResetDialogStep = 0 }) {
                    Text("Cancelar", color = palette.textSecondary)
                }
            }
        )
    }

    if (factoryResetState.isResetting) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = palette.surfaceElevated,
            title = { Text("Restaurando banco de fábrica", color = palette.textPrimary) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = palette.highlight)
                    Text(
                        text = "Limpando tabelas e recriando dados iniciais...",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            },
            confirmButton = {}
        )
    }

    factoryResetState.lastSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = onDismissFactoryResetFeedback,
            containerColor = palette.surfaceElevated,
            title = { Text("Banco restaurado de fábrica", color = palette.positive) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Registros apagados: ${summary.deletedRowsTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Lembretes cancelados: ${summary.cancelledRemindersCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Mercado limpo: ${summary.clearedMarketFavoritesCount} favorito(s), ${summary.clearedMarketAlertsCount} alerta(s) e caches",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Categorias iniciais restauradas: ${summary.restoredDefaultCategoriesCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (summary.preservedCredentials && summary.preservedInboxConfig) {
                            "Credenciais GitHub PAT e coordenadas NEXUS-INBOX preservadas."
                        } else {
                            "Rastreamento local da NEXUS-INBOX limpo."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.info
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissFactoryResetFeedback) {
                    Text("Entendi", color = palette.highlight)
                }
            }
        )
    }

    factoryResetState.lastError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = onDismissFactoryResetFeedback,
            containerColor = palette.surfaceElevated,
            title = { Text("Falha na restauração de fábrica", color = palette.danger) },
            text = {
                Text(
                    text = "A restauração falhou e a transação foi revertida para preservar os dados anteriores.\n\nDetalhe: $errorMsg",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = onDismissFactoryResetFeedback) {
                    Text("Fechar", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun CategoryEditDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String) -> Unit
) {
    val palette = LocalNexusPalette.current
    var name by remember(category?.id) { mutableStateOf(category?.name ?: "") }
    val presetColors = listOf("#4C8BF5", "#34C759", "#E5A000", "#E5484D", "#9A9A9A", "#F2F2F2")
    var selectedHex by remember(category?.id) { mutableStateOf(category?.colorHex ?: presetColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                text = if (category == null) "Nova categoria" else "Editar categoria",
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cor discreta", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    presetColors.forEach { hex ->
                        val color = parseHexColorOrDefault(hex, palette.textSecondary)
                        val isSelected = selectedHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 30.dp else 24.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name, selectedHex)
                }
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun BackupScreen(
    backupState: BackupUiState,
    onBack: () -> Unit,
    onGenerateBackupJson: suspend () -> Result<String>,
    onNotifyExported: () -> Unit,
    onSelectImportMode: (BackupImportMode) -> Unit,
    onInspectBackupJson: (String, BackupImportMode) -> Unit,
    onConfirmRestore: () -> Unit,
    onConfirmMerge: () -> Unit,
    onDismissDialogs: () -> Unit,
    onLogError: (String, Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingLaunchMode by remember { mutableStateOf(BackupImportMode.INCREMENTAL_MERGE) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val jsonResult = onGenerateBackupJson()
            jsonResult.onSuccess { json ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Não foi possível abrir o destino selecionado.")
                    }
                }.onSuccess {
                    onNotifyExported()
                }.onFailure { err ->
                    onLogError("Backup (Gravação SAF)", err)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val launchMode = pendingLaunchMode
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Não foi possível ler o arquivo selecionado.")
                }
            }.onSuccess { rawJson ->
                onInspectBackupJson(rawJson, launchMode)
            }.onFailure { err ->
                onLogError("Backup (Leitura SAF)", err)
                onInspectBackupJson("", launchMode)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Backup e Restauração", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Exportar backup completo (JSON v${NexusBackupPayload.CURRENT_FORMAT_VERSION})",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Gera um arquivo JSON versionado com todas as 28 tabelas locais (tarefas, séries recorrentes, projetos, objetivos pessoais, personalização, frases, agenda, eventos, checklists de ocorrências, financeiro, lembretes e rotina) e suas configurações do Mercado (favoritos e alertas de preço) via seletor SAF.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        enabled = !backupState.isBusy,
                        onClick = {
                            val timestamp = DateTimeUtils.formatDate(DateTimeUtils.nowEpochMillis())
                                .replace("/", "-")
                            exportLauncher.launch("nexus-backup-$timestamp.json")
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Exportar JSON")
                    }
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Importar dados de um backup",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Escolha entre mesclar apenas registros novos preservando tudo o que já existe no aparelho, ou substituir o banco inteiro por um backup (compatível com v1 a v${NexusBackupPayload.CURRENT_FORMAT_VERSION}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        enabled = !backupState.isBusy,
                        onClick = {
                            pendingLaunchMode = BackupImportMode.INCREMENTAL_MERGE
                            onSelectImportMode(BackupImportMode.INCREMENTAL_MERGE)
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mesclar informações de um backup")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modo seguro (recomendado): analisa o arquivo antes de confirmar, adiciona apenas registros novos, preserva alterações locais e não apaga nenhum dado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        enabled = !backupState.isBusy,
                        onClick = {
                            pendingLaunchMode = BackupImportMode.FULL_RESTORE
                            onSelectImportMode(BackupImportMode.FULL_RESTORE)
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, palette.danger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restaurar backup completo (substituir tudo)", color = palette.danger)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modo destrutivo: apaga integralmente os dados atuais deste dispositivo e substitui pelo conteúdo do backup escolhido.",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }

            backupState.lastMergeSummary?.let { mergeSummary ->
                item {
                    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                        Text(
                            text = "Resultado da mesclagem incremental",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.positive
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                appendLine("Analisados no arquivo: ${mergeSummary.totalFoundInFile}")
                                appendLine("• Adicionados com sucesso: ${mergeSummary.totalInserted}")
                                appendLine("• Já existiam (iguais): ${mergeSummary.totalAlreadyExisting}")
                                appendLine("• Conflitos (versão local mantida): ${mergeSummary.totalConflicts}")
                                appendLine("• Rejeitados / inválidos: ${mergeSummary.totalInvalid}")
                                appendLine("• Precisam de revisão (vínculos ajustados): ${mergeSummary.totalNeedsReview}")
                                appendLine("• Configurações locais preservadas: ${mergeSummary.totalSingletonSkipped}")
                                appendLine("• Exclusivos deste aparelho mantidos intactos: ${mergeSummary.totalPreservedLocalOnly}")
                                if (mergeSummary.marketConfigApplied || mergeSummary.marketFavoritesFound > 0 || mergeSummary.marketAlertsFound > 0) {
                                    appendLine(
                                        "• Mercado: +${mergeSummary.marketFavoritesAdded} favorito(s) (${mergeSummary.marketFavoritesAlreadyExisting} existentes) · " +
                                            "+${mergeSummary.marketAlertsAdded} alerta(s) (${mergeSummary.marketAlertsConflicts} conflito(s), ${mergeSummary.marketAlertsInvalid} inválido(s))"
                                    )
                                }
                                val activeEntities = mergeSummary.perEntity.filter {
                                    it.foundInFile > 0 || it.preservedLocalOnly > 0
                                }
                                if (activeEntities.isNotEmpty()) {
                                    appendLine()
                                    appendLine("Resumo por categoria:")
                                    activeEntities.forEach { e ->
                                        appendLine(
                                            "• ${e.label}: +${e.inserted} novo(s) · ${e.alreadyExisting} já existente(s) · " +
                                                "${e.conflicts} conflito(s) · ${e.invalid} inválido(s) · ${e.preservedLocalOnly} locais"
                                        )
                                    }
                                }
                            }.trimEnd(),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        if (mergeSummary.issues.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = palette.border)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ocorrências e conflitos (${mergeSummary.issues.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.warning,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            mergeSummary.issues.take(15).forEach { issue ->
                                val prefix = when (issue.kind) {
                                    MergeIssueKind.CONFLICT_KEPT_LOCAL -> "[Conflito/Local mantido]"
                                    MergeIssueKind.INVALID_REJECTED -> "[Inválido]"
                                    MergeIssueKind.REFERENCE_SANITIZED_NEEDS_REVIEW -> "[Revisão]"
                                    MergeIssueKind.SINGLETON_PRESERVED -> "[Config. local]"
                                    MergeIssueKind.FINANCIAL_INTEGRITY_GUARD -> "[Proteção financeira]"
                                }
                                Text(
                                    text = "$prefix ${issue.tableName} (${issue.recordId}): ${issue.reason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            if (mergeSummary.issues.size > 15) {
                                Text(
                                    text = "... e mais ${mergeSummary.issues.size - 15} ocorrência(s).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            backupState.lastRestoreSummary?.let { summary ->
                item {
                    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                        Text(
                            text = "Resultado da última restauração completa",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.positive
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = buildString {
                                appendLine("Total restaurado: ${summary.totalRestored} registros")
                                appendLine("Configurações: ${summary.settingsRestored} · Categorias: ${summary.categoriesRestored}")
                                appendLine("Personalização: ${summary.personalizationSettingsRestored} · Frases: ${summary.quotesRestored}")
                                appendLine("Objetivos pessoais: ${summary.personalGoalsRestored} · Marcos: ${summary.goalMilestonesRestored} · Histórico: ${summary.goalProgressLogsRestored}")
                                appendLine("Projetos: ${summary.projectsRestored} · Eventos: ${summary.eventsRestored}")
                                appendLine("Tarefas: ${summary.tasksRestored} · Subtarefas: ${summary.subtasksRestored}")
                                appendLine("Séries recorrentes: ${summary.recurrencesRestored} · Exceções: ${summary.recurrenceExceptionsRestored}")
                                appendLine("Compromissos: ${summary.appointmentsRestored}")
                                appendLine("Cartões: ${summary.creditCardsRestored} · Faturas: ${summary.statementsRestored}")
                                appendLine("Parcelamentos: ${summary.installmentPurchasesRestored} · Recorrências: ${summary.recurringRulesRestored}")
                                appendLine("Transações: ${summary.transactionsRestored} · Empréstimos: ${summary.loansRestored}")
                                appendLine("Orçamentos: ${summary.budgetsRestored} · Metas: ${summary.financialGoalsRestored}")
                                appendLine("Lembretes: ${summary.remindersRestored} · Rotina: ${summary.routineSettingsRestored} · Briefings: ${summary.briefingHistoryRestored}")
                                appendLine("Auditoria: ${summary.auditHistoryRestored} · Logs de erro: ${summary.errorLogsRestored}")
                                append(
                                    if (summary.marketConfigApplied) {
                                        "Mercado: ${summary.marketFavoritesRestored} favorito(s) · ${summary.marketAlertsRestored} alerta(s)"
                                    } else {
                                        "Mercado: configurações locais preservadas (ausente no backup)"
                                    }
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }

    backupState.pendingPreview?.let { preview ->
        if (backupState.selectedMode == BackupImportMode.INCREMENTAL_MERGE) {
            val mergePlan = backupState.pendingMergePlan
            val mergeBlockedReason = backupState.mergeCompatibilityError

            AlertDialog(
                onDismissRequest = onDismissDialogs,
                containerColor = palette.surfaceElevated,
                title = {
                    Text(
                        text = "Prévia da mesclagem incremental",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Formato v${preview.formatVersion} · Banco v${preview.databaseVersion} · App ${preview.appVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "Exportado em: ${preview.exportedAt}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        preview.compatibilityNotice?.let { notice ->
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.warning
                            )
                        }
                        mergeBlockedReason?.let { blocked ->
                            Text(
                                text = blocked,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                        if (backupState.isPreparingPreview) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = palette.highlight
                                )
                                Text(
                                    text = "Comparando arquivo com os dados locais (nenhuma gravação foi feita)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        } else if (mergePlan != null) {
                            Text(
                                text = "Simulação real antes de aplicar:",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text("• Encontrados no arquivo: ${mergePlan.totalFoundInFile}", color = palette.textSecondary)
                            Text("• Novos a adicionar: ${mergePlan.totalNewRecords}", color = palette.positive, fontWeight = FontWeight.SemiBold)
                            Text("• Já existentes (iguais): ${mergePlan.totalAlreadyExisting}", color = palette.textSecondary)
                            Text(
                                text = "• Conflitos (versão local será mantida): ${mergePlan.totalConflicts}",
                                color = if (mergePlan.totalConflicts > 0) palette.warning else palette.textSecondary
                            )
                            Text(
                                text = "• Inválidos / rejeitados: ${mergePlan.totalInvalid}",
                                color = if (mergePlan.totalInvalid > 0) palette.danger else palette.textSecondary
                            )
                            Text(
                                text = "• Exigem revisão / vínculos ajustados: ${mergePlan.totalNeedsReview}",
                                color = if (mergePlan.totalNeedsReview > 0) palette.warning else palette.textSecondary
                            )
                            Text("• Exclusivos locais preservados: ${mergePlan.totalPreservedLocalOnly}", color = palette.textSecondary)
                            Text("• Configurações locais mantidas: ${mergePlan.totalSingletonSkipped}", color = palette.textSecondary)

                            if (preview.includesMarketConfig || mergePlan.marketFavoritesFound > 0 || mergePlan.marketAlertsFound > 0) {
                                Text(
                                    text = "• Mercado: +${mergePlan.marketFavoritesAdded} fav. (${mergePlan.marketFavoritesAlreadyExisting} existentes) · " +
                                        "+${mergePlan.marketAlertsAdded} alerta(s) (${mergePlan.marketAlertsConflicts} conflito(s), ${mergePlan.marketAlertsInvalid} inválido(s))",
                                    color = palette.textSecondary
                                )
                            }

                            val activeEntities = mergePlan.perEntity.filter {
                                it.newRecords > 0 || it.conflicts > 0 || it.invalid > 0 || it.needsReview > 0
                            }
                            if (activeEntities.isNotEmpty()) {
                                HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Alterações previstas por categoria:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                activeEntities.forEach { e ->
                                    Text(
                                        text = "• ${e.label}: +${e.newRecords} novo(s) · ${e.alreadyExisting} exist. · ${e.conflicts} confl. · ${e.invalid} inv.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }

                            if (mergePlan.issues.isNotEmpty()) {
                                HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Destaques de conflitos/revisão (${mergePlan.issues.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.warning,
                                    fontWeight = FontWeight.SemiBold
                                )
                                mergePlan.issues.take(8).forEach { issue ->
                                    Text(
                                        text = "• ${issue.tableName} (${issue.recordId}): ${issue.reason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                if (mergePlan.issues.size > 8) {
                                    Text(
                                        text = "... e mais ${mergePlan.issues.size - 8} ocorrência(s).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nenhum registro atual será apagado ou sobrescrito.",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.positive
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !backupState.isBusy &&
                            !backupState.isPreparingPreview &&
                            mergePlan != null &&
                            mergeBlockedReason == null,
                        onClick = onConfirmMerge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Confirmar mesclagem")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialogs) {
                        Text("Cancelar", color = palette.textPrimary)
                    }
                }
            )
        } else {
            var destructiveConfirmed by remember(preview) { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismissDialogs,
                containerColor = palette.surfaceElevated,
                title = {
                    Text(
                        text = "Restaurar backup completo?",
                        color = palette.danger,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "ATENÇÃO — AÇÃO DESTRUTIVA",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.danger,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Todos os dados atualmente salvos neste aparelho serão apagados e substituídos pelos ${preview.totalRecords} itens deste arquivo. Se você só deseja importar registros novos sem apagar nada, cancele e use 'Mesclar informações de um backup'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textPrimary
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Formato v${preview.formatVersion} · Banco v${preview.databaseVersion} · App ${preview.appVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        Text(
                            text = "Exportado em: ${preview.exportedAt}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textSecondary
                        )
                        preview.compatibilityNotice?.let { notice ->
                            Text(
                                text = notice,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.warning
                            )
                        }
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Conteúdo que substituirá o banco (${preview.totalRecords} itens):", color = palette.textPrimary)
                        Text("• Configurações: ${preview.settingsCount} · Categorias: ${preview.categoriesCount}", color = palette.textSecondary)
                        Text("• Personalização: ${preview.personalizationSettingsCount} · Frases: ${preview.quotesCount}", color = palette.textSecondary)
                        Text("• Objetivos pessoais: ${preview.personalGoalsCount} · Marcos: ${preview.goalMilestonesCount} · Evolução: ${preview.goalProgressLogsCount}", color = palette.textSecondary)
                        Text("• Projetos: ${preview.projectsCount} · Eventos: ${preview.eventsCount}", color = palette.textSecondary)
                        Text("• Tarefas: ${preview.tasksCount} · Subtarefas: ${preview.subtasksCount}", color = palette.textSecondary)
                        Text("• Séries recorrentes: ${preview.recurrencesCount} · Exceções: ${preview.recurrenceExceptionsCount}", color = palette.textSecondary)
                        Text("• Compromissos: ${preview.appointmentsCount}", color = palette.textSecondary)
                        Text("• Cartões: ${preview.creditCardsCount} · Faturas: ${preview.statementsCount}", color = palette.textSecondary)
                        Text("• Parcelamentos: ${preview.installmentPurchasesCount} · Recorrências: ${preview.recurringRulesCount}", color = palette.textSecondary)
                        Text("• Transações: ${preview.transactionsCount} · Empréstimos: ${preview.loansCount}", color = palette.textSecondary)
                        Text("• Orçamentos: ${preview.budgetsCount} · Metas: ${preview.financialGoalsCount}", color = palette.textSecondary)
                        Text("• Lembretes: ${preview.remindersCount} · Rotina: ${preview.routineSettingsCount} · Briefings: ${preview.briefingHistoryCount}", color = palette.textSecondary)
                        Text("• Auditoria: ${preview.auditHistoryCount} · Logs: ${preview.errorLogsCount}", color = palette.textSecondary)
                        Text(
                            text = if (preview.includesMarketConfig) {
                                "• Mercado: ${preview.marketFavoritesCount} favorito(s) · ${preview.marketAlertsCount} alerta(s)"
                            } else {
                                "• Mercado: não incluído neste backup (favoritos e alertas locais preservados)"
                            },
                            color = palette.textSecondary
                        )
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { destructiveConfirmed = !destructiveConfirmed }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = destructiveConfirmed,
                                onCheckedChange = { destructiveConfirmed = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = palette.danger,
                                    uncheckedColor = palette.textSecondary
                                )
                            )
                            Text(
                                text = "Entendo que todos os dados atuais deste aparelho serão apagados e substituídos.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textPrimary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !backupState.isBusy && destructiveConfirmed,
                        onClick = onConfirmRestore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.danger,
                            contentColor = palette.background
                        )
                    ) {
                        Text("Substituir tudo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDialogs) {
                        Text("Cancelar", color = palette.textPrimary)
                    }
                }
            )
        }
    }

    backupState.validationError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = onDismissDialogs,
            containerColor = palette.surfaceElevated,
            title = { Text("Operação não concluída", color = palette.danger) },
            text = { Text(errorMsg, color = palette.textPrimary) },
            confirmButton = {
                TextButton(onClick = onDismissDialogs) {
                    Text("Entendi", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
fun DiagnosticsScreen(
    tableCounts: DatabaseTableCounts,
    errorLogs: List<ErrorLogEntity>,
    onBack: () -> Unit,
    onClearErrorLogs: () -> Unit,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val clipboard = LocalClipboardManager.current
    var showClearConfirm by remember { mutableStateOf(false) }

    val tableRows = listOf(
        "settings" to tableCounts.settings,
        "categories" to tableCounts.categories,
        "projects" to tableCounts.projects,
        "events" to tableCounts.events,
        "tasks" to tableCounts.tasks,
        "subtasks" to tableCounts.subtasks,
        "appointments" to tableCounts.appointments,
        "credit_cards" to tableCounts.creditCards,
        "credit_card_statements" to tableCounts.statements,
        "installment_purchases" to tableCounts.installmentPurchases,
        "recurring_rules" to tableCounts.recurringRules,
        "transactions" to tableCounts.transactions,
        "loans" to tableCounts.loans,
        "budgets" to tableCounts.budgets,
        "financial_goals" to tableCounts.financialGoals,
        "audit_history" to tableCounts.auditHistory,
        "error_logs" to tableCounts.errorLogs,
        "reminders" to tableCounts.reminders,
        "routine_assistant_settings" to tableCounts.routineSettings,
        "briefing_history" to tableCounts.briefingHistory,
        "personalization_settings" to tableCounts.personalizationSettings,
        "quotes" to tableCounts.quotes,
        "personal_goals" to tableCounts.personalGoals,
        "goal_milestones" to tableCounts.goalMilestones,
        "goal_progress_logs" to tableCounts.goalProgressLogs,
        "recurrences" to tableCounts.recurrences,
        "recurrence_exceptions" to tableCounts.recurrenceExceptions,
        "schedule_checklist_items" to tableCounts.scheduleChecklistItems
    )

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Diagnóstico", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text("Versões", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "App: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Text(
                        text = "Banco SQLite (Room): versão ${NexusDatabase.DATABASE_VERSION}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text("Registros por tabela (${tableRows.size} tabelas)", style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    tableRows.forEach { (tableName, count) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tableName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = palette.textSecondary
                            )
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                color = palette.textPrimary
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Últimos ${errorLogs.size} erros (máx. 50)",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (errorLogs.isNotEmpty()) {
                            TextButton(onClick = { showClearConfirm = true }) {
                                Text("Limpar", color = palette.danger)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val report = buildString {
                                    appendLine("NEXUS Diagnóstico")
                                    appendLine("App: ${BuildConfig.VERSION_NAME} | DB v${NexusDatabase.DATABASE_VERSION}")
                                    appendLine("Tabelas: " + tableRows.joinToString(", ") { "${it.first}=${it.second}" })
                                    appendLine("---")
                                    if (errorLogs.isEmpty()) {
                                        appendLine("Nenhum erro registrado.")
                                    } else {
                                        errorLogs.forEach { log ->
                                            appendLine("[${DateTimeUtils.formatDateTime(log.occurredAt)}] ${log.source}: ${log.message}")
                                            appendLine(log.stackTrace)
                                            appendLine("---")
                                        }
                                    }
                                }
                                clipboard.setText(AnnotatedString(report))
                                onCopied()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copiar erros")
                        }
                    }
                }
            }

            if (errorLogs.isEmpty()) {
                item {
                    NexusCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Nenhum erro capturado até o momento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
            } else {
                items(errorLogs, key = { it.id }) { log ->
                    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                        Text(
                            text = "${DateTimeUtils.formatDateTime(log.occurredAt)} · ${log.source}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.danger
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = log.stackTrace.take(500),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = palette.textSecondary
                        )
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        ConfirmDangerDialog(
            title = "Limpar histórico de erros?",
            message = "Todos os registros da tabela error_logs serão apagados.",
            confirmLabel = "Limpar",
            onConfirm = {
                showClearConfirm = false
                onClearErrorLogs()
            },
            onDismiss = { showClearConfirm = false }
        )
    }
}

@Composable
fun TopSubScreenBar(
    title: String,
    onBack: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Voltar",
                    tint = palette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )
        }
        HorizontalDivider(color = palette.border)
    }
}

private data class WidgetCatalogSpec(
    val title: String,
    val description: String,
    val receiverClassName: String
)

@Composable
fun WidgetsHubScreen(
    onBack: () -> Unit,
    onNotifyMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }
    val supportsPinning = remember(appWidgetManager) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported
    }

    val widgetSpecs = remember {
        listOf(
            WidgetCatalogSpec(
                title = "NEXUS — Meu Dia",
                description = "Data de hoje, compromissos, tarefas com prazo hoje, contagem de pendentes/concluídas e atalhos diretos.",
                receiverClassName = "com.nexus.app.widgets.MyDayWidgetReceiver"
            ),
            WidgetCatalogSpec(
                title = "NEXUS — Minhas Tarefas",
                description = "Lista priorizada por urgência e prazo com conclusão em 2 etapas diretamente no widget e bloqueio de tarefas bloqueadas.",
                receiverClassName = "com.nexus.app.widgets.MyTasksWidgetReceiver"
            ),
            WidgetCatalogSpec(
                title = "NEXUS — Financeiro",
                description = "Saldo declarado separado dos cálculos, receitas/despesas previstas e realizadas, contas vencidas e faturas abertas.",
                receiverClassName = "com.nexus.app.widgets.FinanceWidgetReceiver"
            ),
            WidgetCatalogSpec(
                title = "NEXUS — Resumo NEXUS",
                description = "Painel consolidado de tarefas, compromissos, eventos, vencimentos e metas financeiras.",
                receiverClassName = "com.nexus.app.widgets.NexusSummaryWidgetReceiver"
            )
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Widgets da Tela Inicial", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Widgets Nativos (Jetpack Glance)",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Todos os widgets consultam diretamente o banco SQLite/Room local e são atualizados automaticamente ao alterar dados, concluir tarefas, restaurar backup ou virar o dia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                com.nexus.app.widgets.NexusWidgetUpdater.updateAllWidgets(context.applicationContext)
                                onNotifyMessage("Widgets sincronizados com o banco local")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizar widgets agora")
                    }
                }
            }

            items(widgetSpecs) { spec ->
                val component = remember(spec.receiverClassName, context) {
                    ComponentName(context.packageName, spec.receiverClassName)
                }
                val activeCount = remember(component, appWidgetManager) {
                    runCatching { appWidgetManager.getAppWidgetIds(component).size }.getOrDefault(0)
                }
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = spec.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeCount > 0) "$activeCount ativo(s)" else "Não fixado",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (activeCount > 0) palette.positive else palette.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = spec.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && supportsPinning) {
                                val requested = runCatching {
                                    appWidgetManager.requestPinAppWidget(component, null, null)
                                }.getOrDefault(false)
                                if (requested) {
                                    onNotifyMessage("Solicitação enviada ao launcher do Android")
                                } else {
                                    onNotifyMessage("Este launcher não permitiu fixar automaticamente; adicione pela tela inicial.")
                                }
                            } else {
                                onNotifyMessage("Pressione e segure na tela inicial do Android → Widgets → NEXUS.")
                            }
                        },
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (supportsPinning) "Fixar na tela inicial" else "Como adicionar na tela inicial"
                        )
                    }
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Como adicionar manualmente",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Pressione e segure uma área vazia da tela inicial do Android.\n2. Toque em Widgets e procure por NEXUS.\n3. Arraste um dos 4 widgets (Meu Dia, Minhas Tarefas, Financeiro ou Resumo NEXUS).\n4. Redimensione livremente e toque no ícone de engrenagem no widget para configurar modo compacto e seções.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

