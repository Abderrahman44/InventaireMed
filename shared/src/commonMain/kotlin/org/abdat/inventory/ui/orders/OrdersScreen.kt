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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                        Text("📋", fontSize = 20.sp)
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
                        onClick = { /* Simulated export */ },
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
                iconLabel = "📑",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Montant Engagé (DZD)",
                value = "${state.totalCommittedDzd.toLong()} DZD",
                subtitle = "Total engagé validé",
                iconLabel = "💵",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Réceptions en Attente",
                value = "${state.pendingDeliveriesCount} BL",
                subtitle = "Livraisons programmées",
                iconLabel = "🚚",
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Conventions Suivies",
                value = "${state.strainedConventionsCount} Cadres",
                subtitle = "Quotas sous contrôle",
                iconLabel = "⚖️",
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("N° BON DE COMMANDE", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("FOURNISSEUR", modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("DATE ÉMISSION", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("LIGNES", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("MONTANT TOTAL (DZD)", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("PROGRESSION RÉCEPTION", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("STATUT", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("ACTIONS", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Table Rows
                if (state.filteredOrders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isEmpty()) "Aucun bon de commande enregistré." else "Aucun bon de commande correspondant à « ${state.searchQuery} ».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredOrders, key = { it.commande.id }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.commande.referenceBon,
                                    modifier = Modifier.width(180.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
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
                                Text(
                                    text = "${item.totalAmountDzd.toLong()} DZD",
                                    modifier = Modifier.width(160.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )

                                // Progress
                                Column(modifier = Modifier.width(160.dp).padding(end = 16.dp)) {
                                    Text("${item.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    LinearProgressIndicator(
                                        progress = { item.progressPercent / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (item.progressPercent >= 100) AppTheme.colors.statusSuccess else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }

                                // Status Badge
                                Box(modifier = Modifier.width(130.dp)) {
                                    val (badgeBg, badgeFg) = when {
                                        item.statusLabel.contains("Dette") -> AppTheme.colors.statusUrgentContainer to AppTheme.colors.onStatusUrgent
                                        item.statusLabel.contains("Validé") -> AppTheme.colors.statusSuccessContainer to AppTheme.colors.onStatusSuccess
                                        item.statusLabel.contains("Brouillon") -> MaterialTheme.colorScheme.surfaceContainerHighest to AppTheme.colors.textSecondary
                                        else -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.primary
                                    }
                                    AppBadge(
                                        text = item.statusLabel,
                                        containerColor = badgeBg,
                                        contentColor = badgeFg
                                    )
                                }

                                // Actions
                                Row(modifier = Modifier.width(70.dp)) {
                                    Text(
                                        text = "🗑️",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.deleteOrder(item.commande.id) }
                                            .padding(4.dp),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        }
    }

    // New Order Dialog
    if (state.isNewOrderDialogOpen) {
        NewOrderDialog(
            suppliers = state.suppliers,
            onDismiss = { viewModel.closeNewOrderDialog() },
            onConfirm = { ref, supplierId, date, isDebt, items ->
                viewModel.createOrder(ref, supplierId, date, isDebt, items)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewOrderDialog(
    suppliers: List<Fournisseur>,
    onDismiss: () -> Unit,
    onConfirm: (String, Long, String, Boolean, List<OrderDraftItem>) -> Unit
) {
    var ref by remember { mutableStateOf("BC-2026-${(1000..9999).random()}") }
    var selectedSupplier by remember { mutableStateOf(suppliers.firstOrNull()) }
    var expandedSupplierDropdown by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf("2026-09-08") }
    var isDebt by remember { mutableStateOf(false) }

    var itemDesignation by remember { mutableStateOf("") }
    var itemQtyText by remember { mutableStateOf("50") }
    var itemPriceText by remember { mutableStateOf("1200") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Créer un Nouveau Bon de Commande", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("N° Référence (ex: BC-2026-0842)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Supplier Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSupplierDropdown,
                    onExpandedChange = { expandedSupplierDropdown = it }
                ) {
                    OutlinedTextField(
                        value = selectedSupplier?.fullName ?: "Sélectionner Fournisseur",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fournisseur") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplierDropdown) },
                        modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSupplierDropdown,
                        onDismissRequest = { expandedSupplierDropdown = false }
                    ) {
                        suppliers.forEach { supplier ->
                            DropdownMenuItem(
                                text = { Text(supplier.fullName) },
                                onClick = {
                                    selectedSupplier = supplier
                                    expandedSupplierDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(checked = isDebt, onCheckedChange = { isDebt = it })
                    Text("Dette Urgente / Régularisation Hors Quota", style = MaterialTheme.typography.bodyMedium)
                }

                Text("Ligne d'article initiale:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                OutlinedTextField(
                    value = itemDesignation,
                    onValueChange = { itemDesignation = it },
                    label = { Text("Désignation de l'article") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = itemQtyText,
                        onValueChange = { itemQtyText = it },
                        label = { Text("Quantité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemPriceText,
                        onValueChange = { itemPriceText = it },
                        label = { Text("Prix Unit. DZD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val supplier = selectedSupplier
                    if (ref.isNotBlank() && supplier != null) {
                        val items = if (itemDesignation.isNotBlank()) {
                            listOf(
                                OrderDraftItem(
                                    designation = itemDesignation.trim(),
                                    quantity = itemQtyText.toDoubleOrNull() ?: 1.0,
                                    unitPrice = itemPriceText.toDoubleOrNull() ?: 0.0
                                )
                            )
                        } else emptyList()
                        onConfirm(ref.trim(), supplier.id, date, isDebt, items)
                    }
                }
            ) {
                Text("Émettre Bon de Commande")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
