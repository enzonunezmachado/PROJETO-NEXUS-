package com.nexus.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nexus.app.domain.model.MarketAlertCondition
import com.nexus.app.domain.model.MarketAlertRules
import com.nexus.app.domain.model.MarketAlertStatus
import com.nexus.app.domain.model.MarketAssetCatalog
import com.nexus.app.domain.model.MarketPriceAlert
import com.nexus.app.domain.model.MarketQuoteCalculations
import com.nexus.app.domain.model.MarketQuoteUiState
import com.nexus.app.notifications.NexusNotificationScheduler
import com.nexus.app.ui.components.NexusCard
import com.nexus.app.ui.market.MarketAlertRowPresentation
import com.nexus.app.ui.theme.LocalNexusPalette

data class MarketAlertEditorState(
    val existingAlert: MarketPriceAlert? = null,
    val initialAssetCode: String = MarketAssetCatalog.USD_BRL_CODE,
    val lockAssetSelection: Boolean = false
)

@Composable
fun rememberMarketNotificationPermissionState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scheduler = remember(context) { NexusNotificationScheduler(context) }
    var hasPermission by remember { mutableStateOf(scheduler.hasNotificationPermission()) }

    DisposableEffect(lifecycleOwner, scheduler) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = scheduler.hasNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || scheduler.hasNotificationPermission()
    }

    val requestPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    return hasPermission to requestPermission
}

@Composable
fun MarketAlertsSectionCard(
    alertRows: List<MarketAlertRowPresentation>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onCreateAlert: () -> Unit,
    onEditAlert: (MarketPriceAlert) -> Unit,
    onToggleAlertActive: (MarketPriceAlert, Boolean) -> Unit,
    onDeleteAlert: (MarketPriceAlert) -> Unit,
    onOpenAssetDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    NexusCard(modifier = modifier.fillMaxWidth()) {
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MarketNotificationPermissionBanner(
                onAllowClick = onRequestNotificationPermission
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (alertRows.isEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Crie alertas de preço acima ou abaixo do valor-alvo para acompanhar seus ativos na moeda ou unidade original.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
                OutlinedButton(
                    onClick = onCreateAlert,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddAlert,
                        contentDescription = null,
                        tint = palette.highlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Criar primeiro alerta",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                }
            }
        } else {
            alertRows.forEachIndexed { index, row ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                MarketAlertRowItem(
                    row = row,
                    showAssetHeader = true,
                    onClick = { onOpenAssetDetail(row.descriptor.code) },
                    onToggleActive = { active -> onToggleAlertActive(row.alert, active) },
                    onEdit = { onEditAlert(row.alert) },
                    onDelete = { onDeleteAlert(row.alert) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Monitoramento local vinculado às atualizações de cotação (~30 min em segundo plano ou atualização manual). Não é instantâneo e respeita restrições de rede e bateria do Android.",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

@Composable
fun MarketAssetAlertsCard(
    descriptor: MarketAssetCatalog.AssetDescriptor,
    alertRows: List<MarketAlertRowPresentation>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onCreateAlertForAsset: () -> Unit,
    onEditAlert: (MarketPriceAlert) -> Unit,
    onToggleAlertActive: (MarketPriceAlert, Boolean) -> Unit,
    onDeleteAlert: (MarketPriceAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current

    NexusCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Alertas de preço",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary
                )
                Text(
                    text = "${descriptor.ticker} · Unidade: ${MarketAlertRules.unitLabelFor(descriptor)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
                )
            }
            OutlinedButton(
                onClick = onCreateAlertForAsset,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, palette.border),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddAlert,
                    contentDescription = null,
                    tint = palette.highlight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Novo alerta",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textPrimary
                )
            }
        }

        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(10.dp))
            MarketNotificationPermissionBanner(
                onAllowClick = onRequestNotificationPermission
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = palette.border)
        Spacer(modifier = Modifier.height(10.dp))

        if (alertRows.isEmpty()) {
            Text(
                text = "Nenhum alerta configurado para ${descriptor.displayName}.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        } else {
            alertRows.forEachIndexed { index, row ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = palette.border)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                MarketAlertRowItem(
                    row = row,
                    showAssetHeader = false,
                    onClick = null,
                    onToggleActive = { active -> onToggleAlertActive(row.alert, active) },
                    onEdit = { onEditAlert(row.alert) },
                    onDelete = { onDeleteAlert(row.alert) }
                )
            }
        }
    }
}

@Composable
private fun MarketNotificationPermissionBanner(
    onAllowClick: () -> Unit
) {
    val palette = LocalNexusPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = palette.warning.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, palette.warning.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsOff,
                    contentDescription = null,
                    tint = palette.warning,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Permissão de notificações pendente para exibir alertas no Android.",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onAllowClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Permitir",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.warning
                )
            }
        }
    }
}

@Composable
private fun MarketAlertRowItem(
    row: MarketAlertRowPresentation,
    showAssetHeader: Boolean,
    onClick: (() -> Unit)?,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val alert = row.alert
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }

    Column(
        modifier = rowModifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (showAssetHeader) {
                    Text(
                        text = "${row.descriptor.displayName} (${row.descriptor.ticker})",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = row.conditionSummary,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (alert.status == MarketAlertStatus.PAUSED) {
                        palette.textSecondary
                    } else {
                        palette.textPrimary
                    }
                )
            }
            MarketAlertStatusBadge(status = alert.status)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ref. atual: ${row.formattedCurrentPrice} · ${row.unitOrCurrencyLabel}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Criado em ${row.formattedCreatedAt}",
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textSecondary
            )
        }

        row.triggeredSummary?.let { summary ->
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = palette.positive
            )
        }

        row.awaitingResetNote?.let { resetNote ->
            Text(
                text = resetNote,
                style = MaterialTheme.typography.labelSmall,
                color = palette.warning
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (alert.status == MarketAlertStatus.TRIGGERED) {
                TextButton(
                    onClick = { onToggleActive(true) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = palette.highlight,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Reativar",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.highlight
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = alert.isActive,
                        onCheckedChange = onToggleActive
                    )
                    Text(
                        text = if (alert.isActive) "Ativo" else "Pausado",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (alert.isActive) palette.textPrimary else palette.textSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar alerta",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Excluir alerta",
                        tint = palette.danger,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketAlertStatusBadge(
    status: MarketAlertStatus,
    modifier: Modifier = Modifier
) {
    val palette = LocalNexusPalette.current
    val tint = when (status) {
        MarketAlertStatus.WAITING -> palette.info
        MarketAlertStatus.TRIGGERED -> palette.positive
        MarketAlertStatus.PAUSED -> palette.textSecondary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketAlertEditorSheet(
    editorState: MarketAlertEditorState,
    quoteStates: Map<String, MarketQuoteUiState>,
    hasNotificationPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onSaveCreate: (assetCode: String, condition: MarketAlertCondition, targetPrice: Double, isActive: Boolean) -> Unit,
    onSaveUpdate: (alertId: String, assetCode: String, condition: MarketAlertCondition, targetPrice: Double, isActive: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalNexusPalette.current
    val existing = editorState.existingAlert
    val isEditing = existing != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedAssetCode by remember(editorState) {
        mutableStateOf(existing?.assetCode ?: editorState.initialAssetCode)
    }
    var selectedCondition by remember(editorState) {
        mutableStateOf(existing?.condition ?: MarketAlertCondition.PRICE_ABOVE)
    }
    var targetInput by remember(editorState) {
        mutableStateOf(
            if (existing != null) {
                val desc = MarketAssetCatalog.findByCode(existing.assetCode) ?: MarketAssetCatalog.USD_BRL
                MarketAlertRules.formatEditableTarget(existing.targetPrice, desc.category)
            } else {
                ""
            }
        )
    }
    var isActive by remember(editorState) {
        mutableStateOf(existing?.isActive ?: true)
    }
    var validationError by remember(editorState) { mutableStateOf<String?>(null) }
    var assetDropdownExpanded by remember { mutableStateOf(false) }

    val descriptor = MarketAssetCatalog.findByCode(selectedAssetCode) ?: MarketAssetCatalog.USD_BRL
    val quoteState = quoteStates[descriptor.code]
    val currentQuote = quoteState?.quote
    val currentPriceFormatted = currentQuote?.let {
        MarketAlertRules.formatValueForAsset(it.price, descriptor)
    } ?: MarketQuoteCalculations.formatEmptyPlaceholder(
        category = descriptor.category,
        currencyCode = descriptor.quoteSymbol
    )
    val unitLabel = MarketAlertRules.unitLabelFor(descriptor)
    val (inputPrefix, inputSuffix) = MarketAlertRules.inputPrefixAndSuffix(descriptor)
    val parsedPreview = remember(targetInput) { MarketAlertRules.parseTargetInput(targetInput) }
    val alreadySatisfiesCurrent = remember(parsedPreview, selectedCondition, currentQuote) {
        parsedPreview != null &&
            currentQuote != null &&
            MarketAlertRules.matchesCondition(selectedCondition, currentQuote.price, parsedPreview)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEditing) "Editar alerta de preço" else "Novo alerta de preço",
                style = MaterialTheme.typography.titleLarge,
                color = palette.textPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ativo",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                if (isEditing || editorState.lockAssetSelection) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = palette.surfaceElevated,
                        border = BorderStroke(1.dp, palette.border)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${descriptor.displayName} (${descriptor.ticker})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = unitLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { assetDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            border = BorderStroke(1.dp, palette.border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${descriptor.displayName} (${descriptor.ticker})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = "${descriptor.category.displayLabel} · Unidade: $unitLabel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = "Selecionar ativo",
                                    tint = palette.textSecondary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = assetDropdownExpanded,
                            onDismissRequest = { assetDropdownExpanded = false }
                        ) {
                            MarketAssetCatalog.SUPPORTED_ASSETS.forEach { asset ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${asset.displayName} (${asset.ticker}) · ${MarketAlertRules.unitLabelFor(asset)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        if (asset.code != selectedAssetCode) {
                                            selectedAssetCode = asset.code
                                            targetInput = ""
                                            validationError = null
                                        }
                                        assetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = palette.surfaceElevated,
                border = BorderStroke(1.dp, palette.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cotação atual (referência)",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                        Text(
                            text = currentPriceFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (currentQuote != null) palette.textPrimary else palette.textSecondary
                        )
                    }
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.highlight
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Condição",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MarketAlertCondition.entries.forEach { cond ->
                        val selected = selectedCondition == cond
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCondition = cond },
                            label = { Text(cond.label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.surfaceElevated,
                                selectedLabelColor = palette.highlight,
                                containerColor = palette.surface,
                                labelColor = palette.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = palette.border,
                                selectedBorderColor = palette.highlight
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = targetInput,
                onValueChange = {
                    targetInput = it
                    validationError = null
                },
                label = { Text("Valor-alvo em $unitLabel") },
                placeholder = { Text("Digite o valor desejado") },
                prefix = inputPrefix?.let { prefix -> { Text(prefix) } },
                suffix = inputSuffix?.let { suffix -> { Text(suffix) } },
                isError = validationError != null,
                supportingText = {
                    when {
                        validationError != null -> Text(validationError!!, color = palette.danger)
                        alreadySatisfiesCurrent -> Text(
                            text = "A cotação atual já atende esta condição; o alerta será avaliado na próxima atualização válida.",
                            color = palette.warning
                        )
                        else -> Text(
                            text = "O valor deve corresponder exatamente à unidade original ($unitLabel).",
                            color = palette.textSecondary
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alerta ativo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (isActive) "Estado inicial: Aguardando" else "Estado inicial: Pausado",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it }
                )
            }

            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MarketNotificationPermissionBanner(
                    onAllowClick = onRequestNotificationPermission
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, palette.border)
                ) {
                    Text("Cancelar", color = palette.textSecondary)
                }
                Button(
                    onClick = {
                        val parsed = MarketAlertRules.parseTargetInput(targetInput)
                        if (parsed == null) {
                            validationError = "Informe um valor numérico positivo válido."
                        } else if (existing != null) {
                            onSaveUpdate(
                                existing.id,
                                descriptor.code,
                                selectedCondition,
                                parsed,
                                isActive
                            )
                            onDismiss()
                        } else {
                            onSaveCreate(
                                descriptor.code,
                                selectedCondition,
                                parsed,
                                isActive
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.highlight,
                        contentColor = palette.background
                    )
                ) {
                    Text(if (isEditing) "Salvar" else "Criar alerta")
                }
            }
        }
    }
}
