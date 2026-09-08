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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

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

    // New Order Dialog with Convention & Article Selection
    if (state.isNewOrderDialogOpen) {
        NewOrderDialog(
            suppliers = state.suppliers,
            conventions = state.conventionsForSupplier,
            conventionArticles = state.articlesForConvention,
            selectedSupplierId = state.selectedSupplierId,
            selectedConventionId = state.selectedConventionId,
            onSupplierChange = { viewModel.selectSupplierForNewOrder(it) },
            onConventionChange = { viewModel.selectConventionForNewOrder(it) },
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
    conventions: List<Convention>,
    conventionArticles: List<ConventionQuotaBalance>,
    selectedSupplierId: Long?,
    selectedConventionId: Long?,
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

    val totalOrderAmount = draftItems.sumOf { it.subtotal }

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
                    .padding(vertical = 6.dp),
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
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (AAAA-MM-JJ)") },
                        singleLine = true,
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
                            value = if (currentConvention != null) "Convention N° ${currentConvention.ordNumber} (${currentConvention.date}) - Plafond ${currentConvention.totalMontant.toLong()} DZD" else "Sélectionner Convention",
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
                                    text = { Text("Convention N° ${conv.ordNumber} (${conv.date}) - Plafond: ${conv.totalMontant.toLong()} DZD") },
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
                        Text(
                            text = "ℹ️ Aucune convention-cadre active pour ce fournisseur. Vous pouvez émettre un bon hors convention en mode 'Dette'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.onStatusWarning,
                            modifier = Modifier.padding(8.dp)
                        )
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

                // Article Selector Section
                Text(
                    "3. Sélectionner un Article sous Convention :",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (conventionArticles.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedArticleDropdown,
                        onExpandedChange = { expandedArticleDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedArticle?.let { "${it.designation} (Solde dispo: ${it.remainingQuantity.toLong()} ${it.unite})" } ?: "Choisir un article...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Article conventionné") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedArticleDropdown) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedArticleDropdown,
                            onDismissRequest = { expandedArticleDropdown = false }
                        ) {
                            conventionArticles.forEach { article ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(article.designation, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Reste: ${article.remainingQuantity.toLong()} / ${article.agreedQuantity.toLong()} ${article.unite} • Prix: ${article.unitPrice.toLong()} DZD",
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
                                    text = "Prix: ${selectedArticle!!.unitPrice.toLong()} DZD / ${selectedArticle!!.unite}",
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
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val art = selectedArticle ?: return@Button
                                    if (enteredQty > 0 && (!isOverQuota || isDebt)) {
                                        draftItems.add(
                                            OrderDraftItem(
                                                conventionItemId = art.conventionItemId,
                                                designation = art.designation,
                                                unite = art.unite,
                                                quantity = enteredQty,
                                                unitPrice = art.unitPrice
                                            )
                                        )
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
                            Text(
                                text = "⚠️ Quantité demandée (${enteredQty.toLong()}) supérieure au solde restant (${selectedArticle!!.remainingQuantity.toLong()}) ! Cochez 'Dette Urgente' pour autoriser.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.statusUrgent
                            )
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
                            items(draftItems) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(2f)) {
                                        Text(item.designation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                        Text("${item.quantity.toLong()} ${item.unite} × ${item.unitPrice.toLong()} DZD", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                                    }
                                    Text(
                                        "${item.subtotal.toLong()} DZD",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        "✕",
                                        modifier = Modifier.clickable { draftItems.remove(item) }.padding(4.dp),
                                        fontSize = 12.sp,
                                        color = AppTheme.colors.statusUrgent
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }

                // Total Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Bon de Commande:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "${totalOrderAmount.toLong()} DZD",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                Text("Émettre Bon de Commande (${draftItems.size} art.)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
