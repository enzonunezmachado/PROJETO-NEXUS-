package com.nexus.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexus.app.data.db.entity.PersonalizationSettingsEntity
import com.nexus.app.data.db.entity.QuoteEntity
import com.nexus.app.domain.model.DailyQuoteSelection
import com.nexus.app.domain.model.DashboardBlockConfig
import com.nexus.app.domain.model.DashboardBlockType
import com.nexus.app.domain.model.QuoteLibraryStatus
import com.nexus.app.ui.components.ConfirmDangerDialog
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.theme.LocalNexusPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardCustomizationScreen(
    personalization: PersonalizationSettingsEntity,
    blocks: List<DashboardBlockConfig>,
    onBack: () -> Unit,
    onToggleBlock: (DashboardBlockType, Boolean) -> Unit,
    onMoveBlock: (DashboardBlockType, Int) -> Unit,
    onUpdateBlockConfig: (DashboardBlockConfig) -> Unit,
    onToggleCompactMode: (Boolean) -> Unit,
    onToggleMetricSummary: (Boolean) -> Unit,
    onOpenQuoteLibrary: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val sortedBlocks = remember(blocks) { blocks.sortedBy { it.order } }
    var expandedBlock by remember { mutableStateOf<DashboardBlockType?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Personalizar Dashboard", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Exibição modular local",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ocultar ou reordenar blocos muda apenas o que aparece na tela Início. Nenhum registro do banco de dados é apagado, e a navegação principal permanece sempre acessível.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo compacto no dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Reduz o espaçamento vertical entre os blocos da tela inicial.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = personalization.compactDashboardMode,
                            onCheckedChange = onToggleCompactMode
                        )
                    }
                    HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resumo numérico de tarefas",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Exibe os contadores de pendentes, concluídas e atrasadas no bloco de tarefas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = personalization.showMetricSummaryOnDashboard,
                            onCheckedChange = onToggleMetricSummary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenQuoteLibrary,
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.FormatQuote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gerenciar biblioteca de frases", color = palette.textPrimary)
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Blocos da tela inicial",
                    trailing = "${sortedBlocks.count { it.enabled }}/${sortedBlocks.size} ativos"
                )
            }

            itemsIndexed(sortedBlocks, key = { _, block -> block.type.name }) { index, block ->
                val isExpanded = expandedBlock == block.type
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${block.type.defaultTitle}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (block.enabled) palette.textPrimary else palette.textSecondary
                            )
                            Text(
                                text = blockSubtitle(block.type),
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        IconButton(
                            onClick = { onMoveBlock(block.type, -1) },
                            enabled = index > 0
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowUp,
                                contentDescription = "Mover ${block.type.defaultTitle} para cima",
                                tint = if (index > 0) palette.textPrimary else palette.border
                            )
                        }
                        IconButton(
                            onClick = { onMoveBlock(block.type, 1) },
                            enabled = index < sortedBlocks.lastIndex
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Mover ${block.type.defaultTitle} para baixo",
                                tint = if (index < sortedBlocks.lastIndex) palette.textPrimary else palette.border
                            )
                        }
                        IconButton(
                            onClick = {
                                expandedBlock = if (isExpanded) null else block.type
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "Configurar opções de ${block.type.defaultTitle}",
                                tint = if (isExpanded) palette.highlight else palette.textSecondary
                            )
                        }
                        Switch(
                            checked = block.enabled,
                            onCheckedChange = { checked -> onToggleBlock(block.type, checked) }
                        )
                    }

                    if (isExpanded) {
                        HorizontalDivider(color = palette.border, modifier = Modifier.padding(vertical = 10.dp))
                        BlockDetailOptionsEditor(
                            block = block,
                            onUpdate = onUpdateBlockConfig
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    border = BorderStroke(1.dp, palette.border),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restaurar organização padrão", color = palette.textPrimary)
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = palette.surfaceElevated,
            title = {
                Text("Restaurar organização padrão?", color = palette.textPrimary)
            },
            text = {
                Text(
                    "Todos os blocos voltarão para a ordem original e configurações padrão de exibição. Nenhum registro pessoal será apagado.",
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetToDefault()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    )
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar", color = palette.textPrimary)
                }
            }
        )
    }
}

@Composable
private fun BlockDetailOptionsEditor(
    block: DashboardBlockConfig,
    onUpdate: (DashboardBlockConfig) -> Unit
) {
    val palette = LocalNexusPalette.current
    val labels = detailLabelsForBlock(block.type)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (supportsMaxItems(block.type)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Máximo de itens no bloco",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            if (block.maxItems > 1) {
                                onUpdate(block.copy(maxItems = block.maxItems - 1))
                            }
                        },
                        enabled = block.maxItems > 1,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("-")
                    }
                    Text(
                        text = "${block.maxItems}",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    OutlinedButton(
                        onClick = {
                            if (block.maxItems < 8) {
                                onUpdate(block.copy(maxItems = block.maxItems + 1))
                            }
                        },
                        enabled = block.maxItems < 8,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("+")
                    }
                }
            }
        }

        labels.primary?.let { primaryLabel ->
            OptionToggleRow(
                label = primaryLabel,
                checked = block.showPrimaryDetail,
                onCheckedChange = { onUpdate(block.copy(showPrimaryDetail = it)) }
            )
        }
        labels.secondary?.let { secondaryLabel ->
            OptionToggleRow(
                label = secondaryLabel,
                checked = block.showSecondaryDetail,
                onCheckedChange = { onUpdate(block.copy(showSecondaryDetail = it)) }
            )
        }
        labels.alerts?.let { alertLabel ->
            OptionToggleRow(
                label = alertLabel,
                checked = block.showOverdueOrAlerts,
                onCheckedChange = { onUpdate(block.copy(showOverdueOrAlerts = it)) }
            )
        }
    }
}

@Composable
private fun OptionToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalNexusPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private data class BlockDetailLabels(
    val primary: String? = null,
    val secondary: String? = null,
    val alerts: String? = null
)

private fun supportsMaxItems(type: DashboardBlockType): Boolean = when (type) {
    DashboardBlockType.TASK_SUMMARY,
    DashboardBlockType.TODAY_AGENDA,
    DashboardBlockType.UPCOMING_EVENTS,
    DashboardBlockType.UPCOMING_PAYMENTS,
    DashboardBlockType.GOALS -> true
    else -> false
}

private fun blockSubtitle(type: DashboardBlockType): String = when (type) {
    DashboardBlockType.GREETING -> "Saudação personalizada, data formatada e frase integrada"
    DashboardBlockType.DAILY_QUOTE -> "Frase local do dia integrada discretamente ao cabeçalho"
    DashboardBlockType.TASK_SUMMARY -> "Anel mensal (3 estados), atrasadas, alertas e prioridades"
    DashboardBlockType.TODAY_AGENDA -> "Próximo compromisso, rotina e agenda de hoje"
    DashboardBlockType.FINANCE_SUMMARY -> "Resultado do mês, saldo informado, receitas e despesas"
    DashboardBlockType.UPCOMING_EVENTS -> "Eventos futuros e situação do ingresso"
    DashboardBlockType.UPCOMING_PAYMENTS -> "Contas a vencer e faturas de cartão abertas"
    DashboardBlockType.GOALS -> "Objetivos pessoais ativos e metas financeiras"
    DashboardBlockType.CHARTS_INDICATORS -> "Barras de 6 meses e rosca interativa por categoria"
}

private fun detailLabelsForBlock(type: DashboardBlockType): BlockDetailLabels = when (type) {
    DashboardBlockType.GREETING -> BlockDetailLabels(
        primary = "Mostrar data formatada abaixo da saudação"
    )
    DashboardBlockType.DAILY_QUOTE -> BlockDetailLabels(
        primary = "Mostrar nome da autoria (quando informada)"
    )
    DashboardBlockType.TASK_SUMMARY -> BlockDetailLabels(
        primary = "Mostrar anel mensal de tarefas (Concluídas / Em andamento / Pendentes)",
        secondary = "Mostrar lista compacta de prioridades de hoje",
        alerts = "Exibir banners de alertas e tarefas atrasadas"
    )
    DashboardBlockType.TODAY_AGENDA -> BlockDetailLabels(
        primary = "Mostrar cartão de próximo compromisso e Assistente de Rotina",
        secondary = "Listar compromissos previstos para hoje",
        alerts = "Sinalizar conflitos de horário"
    )
    DashboardBlockType.FINANCE_SUMMARY -> BlockDetailLabels(
        primary = "Mostrar receitas e despesas do mês",
        secondary = "Mostrar próximo vencimento clicável",
        alerts = "Destacar valores vencidos sem pagamento"
    )
    DashboardBlockType.UPCOMING_EVENTS -> BlockDetailLabels(
        primary = "Mostrar local e cidade no resumo",
        secondary = "Mostrar situação do ingresso"
    )
    DashboardBlockType.UPCOMING_PAYMENTS -> BlockDetailLabels(
        primary = "Mostrar valor e data de cada conta",
        secondary = "Mostrar resumo de faturas abertas",
        alerts = "Destacar contas e faturas vencidas"
    )
    DashboardBlockType.GOALS -> BlockDetailLabels(
        primary = "Mostrar objetivos pessoais com barra de progresso",
        secondary = "Mostrar também metas financeiras",
        alerts = "Destacar objetivos com prazo atrasado"
    )
    DashboardBlockType.CHARTS_INDICATORS -> BlockDetailLabels(
        primary = "Mostrar gráfico de barras (6 meses)",
        secondary = "Mostrar gráfico de rosca por categoria"
    )
}

@Composable
fun QuoteLibraryScreen(
    quotes: List<QuoteEntity>,
    dailyQuoteSelection: DailyQuoteSelection,
    showDailyQuoteOnDashboard: Boolean,
    onBack: () -> Unit,
    onToggleShowOnDashboard: (Boolean) -> Unit,
    onSaveQuote: (id: String?, text: String, author: String, isActive: Boolean) -> Unit,
    onToggleQuoteActive: (String) -> Unit,
    onDeleteQuote: (String) -> Unit,
    onImportQuotes: (String) -> Unit,
    onExportQuotesJson: suspend () -> Result<String>,
    onQuotesExported: () -> Unit,
    onLogError: (String, Throwable) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var editingQuote by remember { mutableStateOf<QuoteEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDeleteQuote by remember { mutableStateOf<QuoteEntity?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Não foi possível ler o arquivo selecionado.")
                }
            }.onSuccess { raw ->
                onImportQuotes(raw)
            }.onFailure { err ->
                onLogError("Importação de frases", err)
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            onExportQuotesJson().onSuccess { json ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("Não foi possível gravar o arquivo.")
                    }
                }.onSuccess {
                    onQuotesExported()
                }.onFailure { err ->
                    onLogError("Exportação de frases", err)
                }
            }
        }
    }

    val filteredQuotes = remember(quotes, searchQuery) {
        val clean = searchQuery.trim().lowercase()
        if (clean.isEmpty()) quotes
        else quotes.filter {
            it.text.lowercase().contains(clean) || it.author.lowercase().contains(clean)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "Biblioteca de Frases", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exibir frase diária no dashboard",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Seleção 100% local a partir da sua biblioteca abaixo. Autores em branco não são inventados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = showDailyQuoteOnDashboard,
                            onCheckedChange = onToggleShowOnDashboard
                        )
                    }
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Text(
                        text = "Seleção de hoje",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    when (dailyQuoteSelection.libraryStatus) {
                        QuoteLibraryStatus.EMPTY -> {
                            Text(
                                text = "Nenhuma frase ativa cadastrada no momento.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        QuoteLibraryStatus.SINGLE_QUOTE,
                        QuoteLibraryStatus.ROTATING -> {
                            val active = dailyQuoteSelection.quote
                            if (active != null) {
                                Text(
                                    text = "\"${active.text}\"",
                                    style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic),
                                    color = palette.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = active.author.ifBlank { "Autoria não informada" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.textSecondary
                                )
                                if (dailyQuoteSelection.libraryStatus == QuoteLibraryStatus.SINGLE_QUOTE) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Há apenas 1 frase ativa; adicione mais frases para ativar a rotação diária sem repetição.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.info
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Rotação diária offline (${dailyQuoteSelection.activeQuotesCount} ativas · não repete no dia seguinte).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.positive
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nova")
                    }
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar", color = palette.textPrimary)
                    }
                    OutlinedButton(
                        onClick = { exportLauncher.launch("nexus-frases.json") },
                        enabled = quotes.isNotEmpty(),
                        border = BorderStroke(1.dp, palette.border),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar", color = palette.textPrimary)
                    }
                }
            }

            if (quotes.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filtrar frases ou autor") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Limpar filtro")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Frases locais",
                    trailing = "${quotes.count { it.isActive }}/${quotes.size} ativas"
                )
            }

            if (filteredQuotes.isEmpty()) {
                item {
                    NexusEmptyState(
                        icon = Icons.Outlined.FormatQuote,
                        title = if (quotes.isEmpty()) "Sua biblioteca está vazia" else "Nenhuma frase encontrada",
                        subtitle = if (quotes.isEmpty()) {
                            "Cadastre suas próprias frases ou importe um arquivo .json / .txt local pelo SAF."
                        } else {
                            "Nenhuma frase corresponde ao filtro informado."
                        },
                        actionLabel = if (quotes.isEmpty()) "Adicionar frase" else null,
                        onAction = if (quotes.isEmpty()) ({ showCreateDialog = true }) else null
                    )
                }
            } else {
                items(filteredQuotes, key = { it.id }) { quote ->
                    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                        Text(
                            text = "\"${quote.text}\"",
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                            color = if (quote.isActive) palette.textPrimary else palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = quote.author.ifBlank { "Autoria não informada" },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (quote.shownCount > 0) {
                                    "Exibida ${quote.shownCount}x"
                                } else {
                                    "Ainda não exibida"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = quote.isActive,
                                    onCheckedChange = { onToggleQuoteActive(quote.id) }
                                )
                                IconButton(onClick = { editingQuote = quote }) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = "Editar frase",
                                        tint = palette.textPrimary
                                    )
                                }
                                IconButton(onClick = { pendingDeleteQuote = quote }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = "Excluir frase",
                                        tint = palette.danger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog || editingQuote != null) {
        QuoteEditorDialog(
            existing = editingQuote,
            onDismiss = {
                showCreateDialog = false
                editingQuote = null
            },
            onSave = { text, author, isActive ->
                onSaveQuote(editingQuote?.id, text, author, isActive)
                showCreateDialog = false
                editingQuote = null
            }
        )
    }

    pendingDeleteQuote?.let { target ->
        ConfirmDangerDialog(
            title = "Excluir frase?",
            message = "A frase será removida permanentemente da sua biblioteca local.",
            confirmLabel = "Excluir",
            onConfirm = {
                pendingDeleteQuote = null
                onDeleteQuote(target.id)
            },
            onDismiss = { pendingDeleteQuote = null }
        )
    }
}

@Composable
private fun QuoteEditorDialog(
    existing: QuoteEntity?,
    onDismiss: () -> Unit,
    onSave: (text: String, author: String, isActive: Boolean) -> Unit
) {
    val palette = LocalNexusPalette.current
    var text by remember(existing?.id) { mutableStateOf(existing?.text ?: "") }
    var author by remember(existing?.id) { mutableStateOf(existing?.author ?: "") }
    var isActive by remember(existing?.id) { mutableStateOf(existing?.isActive ?: true) }
    var textError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceElevated,
        title = {
            Text(
                text = if (existing == null) "Nova frase local" else "Editar frase",
                color = palette.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (it.isNotBlank()) textError = false
                    },
                    label = { Text("Texto da frase *") },
                    isError = textError,
                    supportingText = if (textError) {
                        { Text("Digite o conteúdo da frase.") }
                    } else null,
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Autoria (opcional)") },
                    supportingText = { Text("Deixe em branco caso a autoria não seja conhecida.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ativa para exibição diária", color = palette.textPrimary)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isBlank()) {
                        textError = true
                    } else {
                        onSave(text, author, isActive)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                )
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = palette.textPrimary)
            }
        }
    )
}
