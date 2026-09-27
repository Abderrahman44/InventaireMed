package org.abdat.inventory.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import org.abdat.inventory.ui.components.AppDatePickerField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.EmptyState
import org.abdat.inventory.ui.components.MetricCard
import org.abdat.inventory.ui.components.formatDzd
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.OrdersCsvMapper
import org.abdat.inventory.util.showSaveFileDialog
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var orderToDelete by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Long?>(null) }
    var exportSuccessPath by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Dedicated Order Detail Drill-down View
    state.selectedOrderForDetail?.let { detailData ->
        OrderDetailScreen(
            data = detailData,
            onBack = { viewModel.selectOrderForDetail(null) },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Commandes & Bons d'Achat",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "Achats & Réappro",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = AppTheme.colors.textSecondary
                            )
                            AppBadge(
                                text = "Plateforme A • Quai 03",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Création séquentielle de Bons de Commande (BC) et contrôle d'exécution budgétaire en Dinars Algériens (DZD).",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val path = showSaveFileDialog(
                                    title = "Exporter Registre Commandes CSV",
                                    defaultFileName = "registre_commandes.csv",
                                    filters = listOf(FileTypeFilter("CSV Files", "csv"))
                                )
                                if (path != null) {
                                    try {
                                        val (headers, rows) = OrdersCsvMapper.toHeadersAndRows(state.filteredOrders)
                                        CsvExporter.writeCsv(path, headers, rows)
                                        exportSuccessPath = path
                                        exportError = null
                                    } catch (e: Exception) {
                                        exportError = e.message ?: "Erreur inconnue lors de l'export"
                                        exportSuccessPath = null
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("Exporter Registre CSV", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.openNewOrderDialog() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("+ Nouveau Bon de Commande", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary KPI Bar (4 cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Commandes en Cours",
                value = "${state.activeOrdersCount} BC",
                subtitle = "Registre actif",
                icon = Icons.Default.ReceiptLong,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Montant Engagé (DZD)",
                value = formatDzd(state.totalCommittedDzd),
                subtitle = "Total engagé validé",
                icon = Icons.Default.Payments,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Réceptions en Attente",
                value = "${state.pendingDeliveriesCount} BL",
                subtitle = "Livraisons programmées",
                icon = Icons.Default.LocalShipping,
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Conventions Suivies",
                value = "${state.strainedConventionsCount} Cadres",
                subtitle = "Quotas sous contrôle",
                icon = Icons.Default.Gavel,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Status Tabs Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                placeholder = "Rechercher par N° BC ou fournisseur...",
                modifier = Modifier.width(360.dp)
            )

            AppFilterTabs(
                items = OrderStatusFilter.entries,
                selectedItem = state.selectedFilter,
                onItemSelected = { viewModel.onFilterChange(it) },
                labelProvider = { it.label }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Orders Data Table
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            val tableScrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("N° BON DE COMMANDE", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("FOURNISSEUR", modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("DATE ÉMISSION", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("LIGNES", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("MONTANT TOTAL (TTC)", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("PROGRESSION RÉCEPTION", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("STATUT", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("ACTIONS", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Table Rows
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (state.filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            message = "Aucun bon de commande trouvé",
                            icon = Icons.Default.Search,
                            action = {
                                TextButton(onClick = {
                                    viewModel.onSearchQueryChange("")
                                    viewModel.onFilterChange(OrderStatusFilter.ALL)
                                }) {
                                    Text("Réinitialiser les filtres")
                                }
                            }
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredOrders, key = { it.commande.id }, contentType = { "order_row" }) { item ->
                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .horizontalScroll(tableScrollState)
                                    .clickable { viewModel.selectOrderForDetail(item.commande.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(180.dp)) {
                                    Text(
                                        text = item.commande.referenceBon,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (item.linkedDeliveriesCount > 0) {
                                        Text(
                                            text = "${item.linkedDeliveriesCount} BL • ${item.linkedDeliveryRefs.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = AppTheme.colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    text = item.supplierName,
                                    modifier = Modifier.weight(2.2f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = item.commande.date,
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${item.itemsCount} art.",
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Column(modifier = Modifier.width(160.dp)) {
                                    Text(
                                        text = formatDzd(item.totalAmountDzd),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    if (item.tvaRate > 0) {
                                        Text(
                                            text = "HT: ${formatDzd(item.subtotalHtDzd)} (${item.tvaRate}%)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = AppTheme.colors.textSecondary
                                        )
                                    }
                                }

                                // Progress
                                Column(modifier = Modifier.width(160.dp).padding(end = 16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.progressPercent}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AppTheme.colors.textPrimary
                                        )
                                        if (item.linkedDeliveriesCount > 0) {
                                            Text(
                                                text = "${item.linkedDeliveriesCount} BL",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = AppTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = when {
                                            item.progressPercent >= 100 -> AppTheme.colors.statusSuccess
                                            item.progressPercent > 0 -> AppTheme.colors.statusWarning
                                            else -> MaterialTheme.colorScheme.primary
                                        },
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }

                                // Status Badge
                                Box(modifier = Modifier.width(130.dp)) {
                                    val (badgeBg, badgeFg) = when {
                                        item.statusLabel.contains("Soldé") || item.statusLabel.contains("Clôturé") ->
                                            AppTheme.colors.statusSuccessContainer to AppTheme.colors.onStatusSuccess
                                        item.statusLabel.contains("Partiellement") ->
                                            AppTheme.colors.statusWarningContainer to AppTheme.colors.onStatusWarning
                                        item.statusLabel.contains("Dette") ->
                                            AppTheme.colors.statusUrgentContainer to AppTheme.colors.onStatusUrgent
                                        item.statusLabel.contains("Validé") ->
                                            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.primary
                                        item.statusLabel.contains("Brouillon") ->
                                            MaterialTheme.colorScheme.surfaceContainerHighest to AppTheme.colors.textSecondary
                                        else ->
                                            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.primary
                                    }
                                    AppBadge(
                                        text = item.statusLabel,
                                        containerColor = badgeBg,
                                        contentColor = badgeFg
                                    )
                                }

                                // Actions
                                Row(
                                    modifier = Modifier.width(80.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.selectOrderForDetail(item.commande.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Détails de la commande",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { orderToDelete = item.commande.id },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Supprimer la commande",
                                            modifier = Modifier.size(16.dp),
                                            tint = AppTheme.colors.statusUrgent
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        }
    }

    // New Order Dialog with Convention & Article Selection
    if (state.isNewOrderDialogOpen) {
        NewOrderDialog(
            suppliers = state.suppliers,
            conventions = state.conventionsForSupplier,
            conventionArticles = state.articlesForConvention,
            selectedSupplierId = state.selectedSupplierId,
            selectedConventionId = state.selectedConventionId,
            selectedConventionTvaRate = state.selectedConventionTvaRate,
            onSupplierChange = { viewModel.selectSupplierForNewOrder(it) },
            onConventionChange = { viewModel.selectConventionForNewOrder(it) },
            onDismiss = { viewModel.closeNewOrderDialog() },
            onConfirm = { ref, supplierId, date, isDebt, items ->
                viewModel.createOrder(ref, supplierId, date, isDebt, items)
            }
        )
    }

    orderToDelete?.let { orderId ->
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Confirmer la suppression", fontWeight = FontWeight.Bold) },
            text = { Text("Êtes-vous sûr de vouloir supprimer ce bon de commande ? Cette action est irréversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOrder(orderId)
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.statusUrgent)
                ) { Text("Confirmer") }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) { Text("Annuler") }
            }
        )
    }

    // CSV Export Success Notification
    exportSuccessPath?.let { path ->
        Dialog(onDismissRequest = { exportSuccessPath = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.surfaceWhite,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.statusSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Export Registre CSV",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = "Le fichier a été exporté avec succès (${state.filteredOrders.size} commandes).\n\nChemin : $path",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Button(
                        onClick = { exportSuccessPath = null },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    // CSV Export Error Notification
    exportError?.let { error ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            title = { Text("Erreur d'export", fontWeight = FontWeight.Bold) },
            text = { Text("L'export CSV a échoué :\n$error") },
            confirmButton = {
                Button(onClick = { exportError = null }) { Text("OK") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOrderDialog(
    suppliers: List<Fournisseur>,
    conventions: List<Convention>,
    conventionArticles: List<ConventionQuotaBalance>,
    selectedSupplierId: Long?,
    selectedConventionId: Long?,
    selectedConventionTvaRate: Double = 0.0,
    onSupplierChange: (Long) -> Unit,
    onConventionChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, Boolean, List<OrderDraftItem>) -> Unit
) {
    var ref by remember { mutableStateOf("BC-2026-${(1000..9999).random()}") }
    var expandedSupplierDropdown by remember { mutableStateOf(false) }
    var expandedConventionDropdown by remember { mutableStateOf(false) }
    var expandedArticleDropdown by remember { mutableStateOf(false) }

    var date by remember { mutableStateOf("2026-09-08") }
    var isDebt by remember { mutableStateOf(false) }

    // Multi-line items in draft
    val draftItems = remember { mutableStateListOf<OrderDraftItem>() }

    // Current article selector fields
    var selectedArticle by remember { mutableStateOf<ConventionQuotaBalance?>(null) }
    var itemQtyText by remember { mutableStateOf("10") }

    val currentSupplier = suppliers.firstOrNull { it.id == selectedSupplierId } ?: suppliers.firstOrNull()
    val currentConvention = conventions.firstOrNull { it.id == selectedConventionId } ?: conventions.firstOrNull()

    // Quota validation
    val enteredQty = itemQtyText.toDoubleOrNull() ?: 0.0
    val isOverQuota = selectedArticle != null && !isDebt && enteredQty > selectedArticle!!.remainingQuantity

    val tvaRate = currentConvention?.tvaRate ?: selectedConventionTvaRate
    val subtotalHt = draftItems.sumOf { it.subtotal }
    val tvaAmount = subtotalHt * (tvaRate / 100.0)
    val totalTtc = subtotalHt + tvaAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Créer un Bon de Commande (Sélection Convention & Articles)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header Info: Reference and Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ref,
                        onValueChange = { ref = it },
                        label = { Text("N° Référence") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    AppDatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Date Commande",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Supplier Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSupplierDropdown,
                    onExpandedChange = { expandedSupplierDropdown = it }
                ) {
                    OutlinedTextField(
                        value = currentSupplier?.fullName ?: "Sélectionner Fournisseur",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("1. Fournisseur") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplierDropdown) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSupplierDropdown,
                        onDismissRequest = { expandedSupplierDropdown = false }
                    ) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.fullName) },
                                onClick = {
                                    onSupplierChange(supplier.id)
                                    selectedArticle = null
                                    expandedSupplierDropdown = false
                                }
                            )
                        }
                    }
                }

                // Convention Dropdown
                if (conventions.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedConventionDropdown,
                        onExpandedChange = { expandedConventionDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = if (currentConvention != null) "Convention N° ${currentConvention.ordNumber} (${currentConvention.date}) - TVA ${currentConvention.tvaRate}% - Plafond ${formatDzd(currentConvention.totalMontant)}" else "Sélectionner Convention",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("2. Convention-Cadre Active") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConventionDropdown) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedConventionDropdown,
                            onDismissRequest = { expandedConventionDropdown = false }
                        ) {
                            conventions.forEach { conv ->
                                DropdownMenuItem(
                                    text = { Text("Convention N° ${conv.ordNumber} (${conv.date}) - TVA ${conv.tvaRate}% - Plafond: ${formatDzd(conv.totalMontant)}") },
                                    onClick = {
                                        onConventionChange(conv.id)
                                        selectedArticle = null
                                        expandedConventionDropdown = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        color = AppTheme.colors.statusWarningContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AppTheme.colors.onStatusWarning
                            )
                            Text(
                                text = "Aucune convention-cadre active pour ce fournisseur. Vous pouvez émettre un bon hors convention en mode 'Dette'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.onStatusWarning
                            )
                        }
                    }
                }

                // Debt Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(checked = isDebt, onCheckedChange = { isDebt = it })
                    Column {
                        Text("Commande en Dette Urgente (Dépassement de Quota autorisé)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text("Permet de commander au-delà du quota conventionné en cas d'urgence médicale.", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                    }
                }

                HorizontalDivider()

                // Article Selector Section with Sous-Famille Filtering
                var selectedSousFamily by remember { mutableStateOf<String?>(null) }
                val availableSousFamilies = remember(conventionArticles) {
                    conventionArticles.map { it.sousFamily }.filter { it.isNotBlank() }.distinct()
                }
                val filteredArticles = if (selectedSousFamily == null) {
                    conventionArticles
                } else {
                    conventionArticles.filter { it.sousFamily.equals(selectedSousFamily, ignoreCase = true) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "3. Sélectionner un Article sous Convention :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (availableSousFamilies.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sous-famille :", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (selectedSousFamily == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedSousFamily = null }
                        ) {
                            Text(
                                "Toutes",
                                color = if (selectedSousFamily == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        availableSousFamilies.forEach { sf ->
                            val isSel = selectedSousFamily.equals(sf, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable { selectedSousFamily = sf }
                            ) {
                                Text(
                                    sf,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (conventionArticles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedArticleDropdown,
                        onExpandedChange = { expandedArticleDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedArticle?.let { "${it.designation} (Solde dispo: ${it.remainingQuantity.toLong()} ${it.unite})" } ?: "Choisir un article...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Article conventionné (${filteredArticles.size} dispo)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedArticleDropdown) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedArticleDropdown,
                            onDismissRequest = { expandedArticleDropdown = false }
                        ) {
                            filteredArticles.forEach { article ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(article.designation, fontWeight = FontWeight.SemiBold)
                                                if (article.sousFamily.isNotBlank()) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                                    ) {
                                                        Text(
                                                            article.sousFamily,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                "Reste: ${article.remainingQuantity.toLong()} / ${article.agreedQuantity.toLong()} ${article.unite} • Prix: ${formatDzd(article.unitPrice)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (article.remainingQuantity <= article.minQuantity) AppTheme.colors.statusUrgent else AppTheme.colors.textSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedArticle = article
                                        expandedArticleDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedArticle != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Solde restant: ${selectedArticle!!.remainingQuantity.toLong()} ${selectedArticle!!.unite}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (selectedArticle!!.remainingQuantity <= 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                                )
                                Text(
                                    text = "Prix: ${formatDzd(selectedArticle!!.unitPrice)} / ${selectedArticle!!.unite}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Quantity input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = itemQtyText,
                                onValueChange = { itemQtyText = it },
                                label = { Text("Quantité à commander") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                isError = itemQtyText.toDoubleOrNull() == null || (itemQtyText.toDoubleOrNull() ?: 0.0) <= 0,
                                supportingText = { if (itemQtyText.toDoubleOrNull() == null) Text("Veuillez saisir un nombre valide") },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val art = selectedArticle ?: return@Button
                                    if (enteredQty > 0 && (!isOverQuota || isDebt)) {
                                        val existingIndex = draftItems.indexOfFirst { it.conventionItemId == art.conventionItemId && it.designation == art.designation }
                                        if (existingIndex >= 0) {
                                            val existing = draftItems[existingIndex]
                                            draftItems[existingIndex] = existing.copy(quantity = existing.quantity + enteredQty)
                                        } else {
                                            draftItems.add(
                                                OrderDraftItem(
                                                    conventionItemId = art.conventionItemId,
                                                    designation = art.designation,
                                                    unite = art.unite,
                                                    quantity = enteredQty,
                                                    unitPrice = art.unitPrice
                                                )
                                            )
                                        }
                                        selectedArticle = null
                                        itemQtyText = "10"
                                    }
                                },
                                enabled = selectedArticle != null && enteredQty > 0 && (!isOverQuota || isDebt),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+ Ajouter Ligne")
                            }
                        }

                        if (isOverQuota) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = AppTheme.colors.statusUrgent
                                )
                                Text(
                                    text = "Quantité demandée (${enteredQty.toLong()}) supérieure au solde restant (${selectedArticle!!.remainingQuantity.toLong()}) ! Cochez 'Dette Urgente' pour autoriser.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTheme.colors.statusUrgent
                                )
                            }
                        }
                    }
                }

                // Table of added lines
                if (draftItems.isNotEmpty()) {
                    Text(
                        "Lignes de commande ajoutées (${draftItems.size}) :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(draftItems, key = { it.designation + it.conventionItemId }, contentType = { "draft_item" }) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(item.designation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text("${item.quantity.toLong()} ${item.unite} × ${formatDzd(item.unitPrice)}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                                    }
                                    Text(
                                        formatDzd(item.subtotal),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { draftItems.remove(item) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Retirer la ligne",
                                            modifier = Modifier.size(14.dp),
                                            tint = AppTheme.colors.statusUrgent
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // Financial Breakdown Card (HT, TVA, TTC)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Sous-total HT:",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                formatDzd(subtotalHt),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "TVA (${tvaRate}%):",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                formatDzd(tvaAmount),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total TTC:",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                formatDzd(totalTtc),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supplier = currentSupplier
                    if (ref.isNotBlank() && supplier != null && draftItems.isNotEmpty()) {
                        onConfirm(ref.trim(), supplier.id, date, isDebt, draftItems.toList())
                    }
                },
                enabled = ref.isNotBlank() && currentSupplier != null && draftItems.isNotEmpty()
            ) {
                Text("Émettre Bon de Commande (${draftItems.size} art. - ${formatDzd(totalTtc)} TTC)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
