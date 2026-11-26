package com.nexus.app.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nexus.app.core.DateTimeUtils
import com.nexus.app.data.inbox.InboxConfig
import com.nexus.app.data.inbox.InboxConnectionTestResult
import com.nexus.app.data.inbox.InboxPendingCheckResult
import com.nexus.app.data.inbox.InboxSummaryFormatter
import com.nexus.app.data.inbox.InboxUpdateLogEntry
import com.nexus.app.data.ingestion.StructuredEntityType
import com.nexus.app.data.ingestion.StructuredPreviewItem
import com.nexus.app.data.ingestion.StructuredPreviewOutcome
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.components.NexusEmptyState
import com.nexus.app.ui.components.SectionHeader
import com.nexus.app.ui.theme.LocalNexusPalette

@Composable
fun NexusInboxScreen(
    config: InboxConfig,
    hasSavedToken: Boolean,
    isBusy: Boolean,
    pendingCheck: InboxPendingCheckResult?,
    lastConnectionTest: InboxConnectionTestResult?,
    lastUpdateLog: InboxUpdateLogEntry?,
    updateLogs: List<InboxUpdateLogEntry>,
    onBack: () -> Unit,
    onSaveConfig: (
        owner: String,
        repo: String,
        branch: String,
        pathPrefix: String,
        autoCheckEnabled: Boolean,
        newTokenInput: String?
    ) -> Unit,
    onClearToken: () -> Unit,
    onTestConnection: (pendingTokenOverride: String?) -> Unit,
    onCheckInboxNow: () -> Unit,
    onConfirmImport: () -> Unit,
    onDiscardPreview: () -> Unit,
    onDismissLastSummary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    var owner by remember { mutableStateOf(config.owner) }
    var repo by remember { mutableStateOf(config.repo) }
    var branch by remember { mutableStateOf(config.branch) }
    var pathPrefix by remember { mutableStateOf(config.pathPrefix) }
    var autoCheckEnabled by remember { mutableStateOf(config.autoCheckEnabled) }
    var tokenInput by remember { mutableStateOf("") }
    var showTokenText by remember { mutableStateOf(false) }
    var configExpanded by remember { mutableStateOf(!config.isConfigured || !hasSavedToken) }

    LaunchedEffect(config.owner, config.repo, config.branch, config.pathPrefix, config.autoCheckEnabled) {
        owner = config.owner
        repo = config.repo
        branch = config.branch
        pathPrefix = config.pathPrefix
        autoCheckEnabled = config.autoCheckEnabled
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopSubScreenBar(title = "NEXUS-INBOX", onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = palette.highlight
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Caixa de entrada ChatGPT → NEXUS",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val statusText = if (config.isConfigured && hasSavedToken) {
                                val folder = config.pathPrefix.takeIf { it.isNotBlank() }?.let { "/$it" } ?: ""
                                "Fonte: ${config.owner}/${config.repo}$folder (${config.branch})"
                            } else {
                                "Configure abaixo seu repositório privado NEXUS-INBOX e token somente leitura."
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    if (config.lastCheckedAtEpochMs != null || config.lastImportAtEpochMs != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        config.lastCheckedAtEpochMs?.let { checkedAt ->
                            Text(
                                text = "Última verificação: ${DateTimeUtils.formatDateTime(checkedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        config.lastImportAtEpochMs?.let { importedAt ->
                            Text(
                                text = "Última importação: ${DateTimeUtils.formatDateTime(importedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }

                    if (!config.lastError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = config.lastError,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.negative
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onCheckInboxNow,
                        enabled = !isBusy && config.isConfigured && hasSavedToken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.highlight,
                            contentColor = palette.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.background
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Verificar NEXUS-INBOX")
                    }
                }
            }

            if (pendingCheck != null) {
                item {
                    PendingPreviewCard(
                        pendingCheck = pendingCheck,
                        isBusy = isBusy,
                        onConfirmImport = onConfirmImport,
                        onDiscardPreview = onDiscardPreview
                    )
                }
            }

            if (lastUpdateLog != null) {
                item {
                    NexusCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevated = true
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = if (lastUpdateLog.errorsCount > 0) palette.warning else palette.positive
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Resultado da importação",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                            }
                            TextButton(onClick = onDismissLastSummary) {
                                Text("Ocultar")
                            }
                        }
                        UpdateLogBody(entry = lastUpdateLog)
                    }
                }
            }

            item {
                NexusCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Settings, contentDescription = null, tint = palette.highlight)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conexão com GitHub (NEXUS-INBOX)",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                        }
                        TextButton(onClick = { configExpanded = !configExpanded }) {
                            Text(if (configExpanded) "Recolher" else "Editar")
                        }
                    }

                    if (configExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = owner,
                            onValueChange = { owner = it },
                            label = { Text("Usuário ou organização GitHub") },
                            placeholder = { Text("ex.: seu-usuario") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = repo,
                                onValueChange = { repo = it },
                                label = { Text("Repositório") },
                                placeholder = { Text("NEXUS-INBOX") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = branch,
                                onValueChange = { branch = it },
                                label = { Text("Branch") },
                                placeholder = { Text("main") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pathPrefix,
                            onValueChange = { pathPrefix = it },
                            label = { Text("Subpasta (opcional)") },
                            placeholder = { Text("vazio = raiz do repositório") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = {
                                Text(
                                    if (hasSavedToken) {
                                        "Substituir token somente leitura (opcional)"
                                    } else {
                                        "Token GitHub somente leitura (Contents: Read-only)"
                                    }
                                )
                            },
                            placeholder = {
                                Text(if (hasSavedToken) "Token já salvo no Android Keystore" else "github_pat_...")
                            },
                            singleLine = true,
                            visualTransformation = if (showTokenText) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showTokenText = !showTokenText }) {
                                    Icon(
                                        imageVector = if (showTokenText) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = "Mostrar ou ocultar token"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = if (hasSavedToken) palette.positive else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasSavedToken) {
                                        "Token protegido por Android Keystore"
                                    } else {
                                        "Nenhum token armazenado"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (hasSavedToken) palette.positive else palette.textSecondary
                                )
                            }
                            if (hasSavedToken) {
                                TextButton(onClick = onClearToken) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        tint = palette.negative,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remover", color = palette.negative)
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = palette.border)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Verificação periódica em segundo plano",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Avisa por notificação quando houver novidades; nunca importa sem sua confirmação.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = autoCheckEnabled,
                                onCheckedChange = { autoCheckEnabled = it }
                            )
                        }

                        if (lastConnectionTest != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            when (lastConnectionTest) {
                                is InboxConnectionTestResult.Success -> Text(
                                    text = "Conexão validada em ${lastConnectionTest.repoFullName} (${lastConnectionTest.jsonFilesFound} JSON na pasta).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.positive
                                )
                                is InboxConnectionTestResult.Failure -> Text(
                                    text = lastConnectionTest.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.negative
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onSaveConfig(owner, repo, branch, pathPrefix, autoCheckEnabled, tokenInput)
                                    if (owner.isNotBlank() && repo.isNotBlank()) {
                                        tokenInput = ""
                                        onTestConnection(null)
                                    }
                                },
                                enabled = !isBusy,
                                border = BorderStroke(1.dp, palette.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Testar conexão")
                            }
                            Button(
                                onClick = {
                                    onSaveConfig(owner, repo, branch, pathPrefix, autoCheckEnabled, tokenInput)
                                    tokenInput = ""
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.highlight,
                                    contentColor = palette.background
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Histórico de atualizações (${updateLogs.size})"
                )
            }

            if (updateLogs.isEmpty()) {
                item {
                    NexusEmptyState(
                        title = "Nenhuma importação confirmada",
                        message = "Quando você confirmar atualizações vindas da NEXUS-INBOX, o resumo real ficará registrado aqui.",
                        icon = Icons.Outlined.History
                    )
                }
            } else {
                items(updateLogs, key = { it.id }) { entry ->
                    NexusCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = entry.summaryHeadline,
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = DateTimeUtils.formatDateTime(entry.occurredAtEpochMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                        UpdateLogBody(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPreviewCard(
    pendingCheck: InboxPendingCheckResult,
    isBusy: Boolean,
    onConfirmImport: () -> Unit,
    onDiscardPreview: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val preview = pendingCheck.combinedPreview

    NexusCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
        Text(
            text = "Prévia para confirmação",
            style = MaterialTheme.typography.titleMedium,
            color = palette.highlight
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${pendingCheck.unseenFiles.size} arquivo(s) novo(s)/alterado(s) • ${preview.totalOperations} operação(ões)",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))
        val statLines = buildList {
            if (preview.willCreateCount > 0) add("Novos registros: ${preview.willCreateCount}")
            if (preview.willUpdateCount > 0) add("Atualizações: ${preview.willUpdateCount}")
            if (preview.duplicateIgnoredCount > 0) add("Duplicidades ignoradas: ${preview.duplicateIgnoredCount}")
            if (preview.conflictPreservedLocalCount > 0) add("Conflitos (local preservado): ${preview.conflictPreservedLocalCount}")
            if (preview.rejectedCount > 0) add("Operações rejeitadas: ${preview.rejectedCount}")
            if (pendingCheck.fileParseErrors.isNotEmpty()) add("Erros de leitura JSON: ${pendingCheck.fileParseErrors.size}")
        }
        if (statLines.isEmpty()) {
            Text(
                text = "Nenhuma alteração aplicável encontrada.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        } else {
            statLines.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textPrimary
                )
            }
        }

        if (pendingCheck.fileParseErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            pendingCheck.fileParseErrors.forEach { err ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = palette.negative,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.negative
                    )
                }
            }
        }

        if (preview.items.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = palette.border)
            preview.items.forEach { item ->
                PreviewItemRow(item = item)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDiscardPreview,
                enabled = !isBusy,
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.weight(1f)
            ) {
                Text("Descartar")
            }
            Button(
                onClick = onConfirmImport,
                enabled = !isBusy && pendingCheck.unseenFiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.highlight,
                    contentColor = palette.background
                ),
                modifier = Modifier.weight(1.3f)
            ) {
                Text("Confirmar importação")
            }
        }
    }
}

@Composable
private fun PreviewItemRow(item: StructuredPreviewItem) {
    val palette = LocalNexusPalette.current
    val outcomeLabel = when (item.outcome) {
        StructuredPreviewOutcome.WILL_CREATE -> "Novo"
        StructuredPreviewOutcome.WILL_UPDATE -> "Atualizar"
        StructuredPreviewOutcome.DUPLICATE_IGNORED -> "Duplicidade"
        StructuredPreviewOutcome.CONFLICT_PRESERVED_LOCAL -> "Conflito local"
        StructuredPreviewOutcome.REJECTED -> "Inválido"
    }
    val outcomeColor = when (item.outcome) {
        StructuredPreviewOutcome.WILL_CREATE -> palette.positive
        StructuredPreviewOutcome.WILL_UPDATE -> palette.highlight
        StructuredPreviewOutcome.DUPLICATE_IGNORED -> palette.textSecondary
        StructuredPreviewOutcome.CONFLICT_PRESERVED_LOCAL -> palette.warning
        StructuredPreviewOutcome.REJECTED -> palette.negative
    }
    val typeLabel = when (item.entityType) {
        StructuredEntityType.TASK -> if (item.checklistItemsCount > 0) "Tarefa + Checklist (${item.checklistItemsCount})" else "Tarefa"
        StructuredEntityType.CHECKLIST -> "Checklist (${item.checklistItemsCount})"
        StructuredEntityType.APPOINTMENT -> "Compromisso"
        StructuredEntityType.EVENT -> "Evento"
        StructuredEntityType.PROJECT -> "Projeto"
        StructuredEntityType.PERSONAL_GOAL -> "Objetivo"
        StructuredEntityType.TRANSACTION -> "Finanças"
        null -> item.entityTypeRaw
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$typeLabel · ${item.displayTitle}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = outcomeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = outcomeColor
            )
        }
        if (item.detail.isNotBlank()) {
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun UpdateLogBody(entry: InboxUpdateLogEntry) {
    val palette = LocalNexusPalette.current
    if (entry.sourceFiles.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Arquivos: ${entry.sourceFiles.joinToString(", ")}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
    if (entry.categories.isNotEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        entry.categories.forEach { category ->
            Text(
                text = "• ${InboxSummaryFormatter.formatCategorySummaryLine(category)}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary
            )
        }
    }
    if (entry.duplicatesIgnoredCount > 0) {
        Text(
            text = "• ${entry.duplicatesIgnoredCount} duplicidade(s) ignorada(s)",
            style = MaterialTheme.typography.bodySmall,
            color = palette.textSecondary
        )
    }
    if (entry.conflictsPreservedCount > 0) {
        Text(
            text = "• ${entry.conflictsPreservedCount} conflito(s) com versão local preservada",
            style = MaterialTheme.typography.bodySmall,
            color = palette.warning
        )
    }
    if (entry.errorsCount > 0) {
        entry.errorDetails.forEach { err ->
            Text(
                text = "• Erro: $err",
                style = MaterialTheme.typography.bodySmall,
                color = palette.negative
            )
        }
    }
}
