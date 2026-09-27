package org.abdat.inventory.ui.suppliers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import org.abdat.inventory.ui.components.AppDatePickerField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.EmptyState
import org.abdat.inventory.ui.components.formatDzd
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.PdfExporter
import org.abdat.inventory.util.showSaveFileDialog

@Composable
fun SuppliersScreen(
    viewModel: SuppliersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    val selectedConv = state.selectedConventionForDetail
    val selectedSupplier = state.selectedSupplier

    if (state.isProfileViewOpen && selectedSupplier != null) {
        SupplierProfileScreen(
            supplier = selectedSupplier,
            conventions = state.conventions,
            historyItems = state.historyItems,
            isEditDialogOpen = state.isEditProfileDialogOpen,
            onBack = { viewModel.closeSupplierProfile() },
            onOpenEdit = { viewModel.openEditProfileDialog() },
            onCloseEdit = { viewModel.closeEditProfileDialog() },
            onSaveProfile = { fullName, societe, phone, responsablePhone, ville, email, otherLinks ->
                viewModel.updateSupplierProfile(
                    id = selectedSupplier.id,
                    fullName = fullName,
                    societe = societe,
                    phone = phone,
                    responsablePhone = responsablePhone,
                    ville = ville,
                    email = email,
                    otherLinks = otherLinks
                )
            },
            modifier = modifier
        )
    } else if (selectedConv != null && selectedSupplier != null) {
        val balances = state.conventionBalances[selectedConv.id] ?: emptyList()
        ConventionDetailScreen(
            convention = selectedConv,
            supplier = selectedSupplier,
            balances = balances,
            onBack = { viewModel.closeConventionDetail() },
            onOpenAddArticle = { viewModel.openAddArticleDialog() },
            onEditArticle = { viewModel.requestEditArticle(it) },
            modifier = modifier
        )

        if (state.isAddArticleDialogOpen) {
            AddArticleToConventionDialog(
                convention = selectedConv,
                supplier = selectedSupplier,
                onDismiss = { viewModel.closeAddArticleDialog() },
                onConfirm = { des, unit, qty, minQty ->
                    viewModel.addArticleToConvention(selectedConv.id, des, unit, qty, minQty)
                }
            )
        }

        if (state.isSensitiveWarningDialogOpen && state.articlePendingEdit != null) {
            SensitiveEditWarningDialog(
                article = state.articlePendingEdit!!,
                convention = selectedConv,
                onDismiss = { viewModel.dismissSensitiveWarning() },
                onConfirm = { viewModel.confirmSensitiveWarning() }
            )
        }

        if (state.isEditArticleDialogOpen && state.articlePendingEdit != null) {
            EditConventionArticleDialog(
                article = state.articlePendingEdit!!,
                convention = selectedConv,
                onDismiss = { viewModel.closeEditArticleDialog() },
                onConfirm = { des, unit, qty, minQty ->
                    viewModel.updateConventionArticle(
                        itemId = state.articlePendingEdit!!.conventionItemId,
                        conventionId = selectedConv.id,
                        designation = des,
                        unite = unit,
                        quantity = qty,
                        minQuantity = minQty
                    )
                }
            )
        }
    } else {
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
                            imageVector = Icons.Default.Business,
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
                                text = "Fournisseurs & Conventions",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "Dépôt A / Quai 03",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Suivi des engagements financiers, conventions-cadres et quotas contractuels (Algérie - DZD).",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.openAddSupplierDialog() },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("+ Nouveau Fournisseur", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.openAddConventionDialog() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("+ Nouvelle Convention", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Split Layout: Left Master List | Right Detail Pane
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Suppliers List (340.dp)
            Card(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    AppSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = "Filtrer fournisseur...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RÉPERTOIRE FOURNISSEURS (${state.filteredSuppliers.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp
                        ),
                        color = AppTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (state.filteredSuppliers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                message = "Aucun fournisseur trouvé",
                                action = {
                                    TextButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                        Text("Réinitialiser les filtres")
                                    }
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredSuppliers, key = { it.id }, contentType = { "supplier_card" }) { supplier ->
                                val isSelected = supplier.id == state.selectedSupplier?.id
                                Box(modifier = Modifier.animateItem()) {
                                    SupplierCardItem(
                                        supplier = supplier,
                                        isSelected = isSelected,
                                        onClick = { viewModel.selectSupplier(supplier) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Pane: Supplier Detail Workspace (Weight 1f)
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                val selectedSupplier = state.selectedSupplier
                if (selectedSupplier == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sélectionnez un fournisseur pour voir ses conventions.", color = AppTheme.colors.textSecondary)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Supplier Info Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedSupplier.fullName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    AppBadge(
                                        text = "FOURNISSEUR AGRÉÉ",
                                        containerColor = AppTheme.colors.statusSuccessContainer,
                                        contentColor = AppTheme.colors.onStatusSuccess
                                    )
                                }
                                Text(
                                    text = "Réf: FRN-${selectedSupplier.id.toString().padStart(4, '0')} • Conventions en cours: ${state.conventions.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }

                            // Quick metrics badges and Export / Action buttons
                            val totalCeilingDzd = state.conventions.sumOf { it.totalMontant }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "Plafond: ${formatDzd(totalCeilingDzd)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            exportErrorMessage = null
                                            val path = showSaveFileDialog(
                                                title = "Exporter Conventions CSV",
                                                defaultFileName = "fournisseur_${selectedSupplier.id}_conventions.csv",
                                                filters = listOf(FileTypeFilter("CSV Files", "csv"))
                                            )
                                            if (path != null) {
                                                try {
                                                    val (headers, rows) = SupplierExportDataMapper.toSupplierConventionsCsv(
                                                        supplier = selectedSupplier,
                                                        conventions = state.conventions,
                                                        balancesMap = state.conventionBalances
                                                    )
                                                    CsvExporter.writeCsv(path, headers, rows)
                                                    exportSuccessMessage = "Le fichier CSV a été exporté avec succès (${state.conventions.size} conventions).\n\nChemin : $path"
                                                } catch (e: Exception) {
                                                    exportErrorMessage = "Erreur lors de l'export CSV : ${e.message ?: "Erreur inconnue"}"
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exporter CSV", style = MaterialTheme.typography.labelMedium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            exportErrorMessage = null
                                            val path = showSaveFileDialog(
                                                title = "Imprimer / Exporter Dossier PDF",
                                                defaultFileName = "fournisseur_${selectedSupplier.id}_conventions.pdf",
                                                filters = listOf(FileTypeFilter("PDF Files", "pdf"))
                                            )
                                            if (path != null) {
                                                try {
                                                    PdfExporter.generateSupplierDossierPdf(
                                                        filePath = path,
                                                        supplier = selectedSupplier,
                                                        conventions = state.conventions,
                                                        balancesMap = state.conventionBalances
                                                    )
                                                    exportSuccessMessage = "Le dossier fournisseur a été exporté en PDF avec succès.\n\nChemin : $path"
                                                } catch (e: Exception) {
                                                    exportErrorMessage = "Erreur lors de l'export PDF : ${e.message ?: "Erreur inconnue"}"
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Print,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Imprimer / PDF", style = MaterialTheme.typography.labelMedium)
                                }

                                Button(
                                    onClick = { viewModel.openSupplierProfile() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Fiche / Profil", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Detail Tabs
                        AppFilterTabs(
                            items = SupplierDetailTab.entries,
                            selectedItem = state.activeTab,
                            onItemSelected = { viewModel.setTab(it) },
                            labelProvider = { it.label }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Tab Contents
                        when (state.activeTab) {
                            SupplierDetailTab.CONVENTIONS -> ConventionsTabView(
                                conventions = state.conventions,
                                balances = state.conventionBalances,
                                onSelectConvention = { viewModel.selectConventionForDetail(it) }
                            )
                            SupplierDetailTab.ARTICLES -> ReferencedArticlesTabView(
                                balances = state.conventionBalances
                            )
                            SupplierDetailTab.HISTORIQUE -> HistoryTabView(
                                historyItems = state.historyItems
                            )
                            SupplierDetailTab.EVALUATIONS -> EvaluationsTabView()
                        }
                    }
                }
            }
        }
    }
    }

    // Dialogs
    if (state.isAddSupplierDialogOpen) {
        AddSupplierDialog(
            onDismiss = { viewModel.closeAddSupplierDialog() },
            onConfirm = { name, societe, phone, ville ->
                viewModel.addSupplierFull(name = name, societe = societe, phone = phone, ville = ville)
            }
        )
    }

    if (state.isAddConventionDialogOpen) {
        AddConventionDialog(
            supplierName = state.selectedSupplier?.fullName ?: "",
            onDismiss = { viewModel.closeAddConventionDialog() },
            onConfirm = { ordNum, date, montant, items, tvaRate ->
                viewModel.addConvention(ordNum, date, montant, items, tvaRate)
            }
        )
    }

    // Export Success Notification Dialog
    exportSuccessMessage?.let { msg ->
        Dialog(onDismissRequest = { exportSuccessMessage = null }) {
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
                            text = "Export Réussi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Button(
                        onClick = { exportSuccessMessage = null },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    // Export Error Notification Dialog
    exportErrorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { exportErrorMessage = null },
            title = { Text("Erreur d'export", fontWeight = FontWeight.Bold) },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { exportErrorMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SupplierCardItem(
    supplier: Fournisseur,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
    } else {
        AppTheme.colors.surfaceWhite
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = supplier.fullName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else AppTheme.colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppTheme.colors.statusSuccess, CircleShape)
                )
            }
            if (supplier.societe.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = supplier.societe,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (supplier.ville.isNotBlank()) "${supplier.ville} • Réf: FRN-${supplier.id.toString().padStart(4, '0')}"
                else "Fournisseur Officiel • Réf: FRN-${supplier.id.toString().padStart(4, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun ConventionsTabView(
    conventions: List<org.abdat.inventory.domain.model.Convention>,
    balances: Map<Long, List<ConventionQuotaBalance>>,
    onSelectConvention: (org.abdat.inventory.domain.model.Convention) -> Unit
) {
    if (conventions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = "Aucune convention-cadre enregistrée pour ce fournisseur.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(conventions, key = { it.id }, contentType = { "convention_card" }) { conv ->
                val convBalances = balances[conv.id] ?: emptyList()
                Card(
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectConvention(conv) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Convention N° ${conv.ordNumber} (${conv.date})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Plafond global: ${formatDzd(conv.totalMontant)} • ${convBalances.size} article(s) • TVA: ${if (conv.tvaRate > 0) "${conv.tvaRate}%" else "0% (Exonéré)"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (conv.tvaRate > 0) {
                                    AppBadge(
                                        text = "TVA ${conv.tvaRate}%",
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                AppBadge(
                                    text = "ACTIVE",
                                    containerColor = AppTheme.colors.statusSuccessContainer,
                                    contentColor = AppTheme.colors.onStatusSuccess
                                )
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Gérer les articles →",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Suivi des quotas par article (${convBalances.size}):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        convBalances.forEach { item ->
                            val progress = if (item.agreedQuantity > 0) {
                                (item.orderedQuantity / item.agreedQuantity).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        item.designation,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Text(
                                        "${item.orderedQuantity.toLong()} / ${item.agreedQuantity.toLong()} ${item.unite} (Reste: ${item.remainingQuantity.toLong()})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.remainingQuantity <= item.minQuantity) AppTheme.colors.statusUrgent else AppTheme.colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (progress >= 0.9f) AppTheme.colors.statusUrgent else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferencedArticlesTabView(
    balances: Map<Long, List<ConventionQuotaBalance>>
) {
    val allItems = balances.values.flatten()

    if (allItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = "Aucun article conventionné enregistré.")
        }
    } else {
        val tableScrollState = rememberScrollState()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DÉSIGNATION", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("UNITÉ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("PRIX NÉGOCIÉ", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("QUOTA CONVENU", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("COMMANDÉ", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("SOLDE RESTANT", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }
            }
            items(allItems, key = { it.conventionItemId }, contentType = { "convention_article_row" }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.designation, modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = AppTheme.colors.textPrimary)
                    Text(item.unite, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(formatDzd(item.unitPrice), modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("${item.agreedQuantity.toLong()}", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.textPrimary)
                    Text("${item.orderedQuantity.toLong()}", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(
                        "${item.remainingQuantity.toLong()}",
                        modifier = Modifier.width(110.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.remainingQuantity <= item.minQuantity) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
private fun HistoryTabView(historyItems: List<SupplierHistoryItem>) {
    if (historyItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(message = "Aucun mouvement (BC ou BL) répertorié pour ce fournisseur.")
        }
    } else {
        val tableScrollState = rememberScrollState()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RÉFÉRENCE", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("TYPE", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("DATE", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("MONTANT (DZD)", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("STATUT", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }
            }
            itemsIndexed(
                items = historyItems,
                key = { index, item -> item.id.ifEmpty { "${item.type}_${item.reference}_${item.date}_$index" } },
                contentType = { _, _ -> "history_row" }
            ) { _, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tableScrollState)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.reference, modifier = Modifier.width(180.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                    Text(item.type, modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(item.date, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(formatDzd(item.amountDzd), modifier = Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textPrimary)
                    Box(modifier = Modifier.width(110.dp)) {
                        AppBadge(
                            text = item.status,
                            containerColor = if (item.status == "Réceptionné" || item.status == "Validé") AppTheme.colors.statusSuccessContainer else AppTheme.colors.statusWarningContainer,
                            contentColor = if (item.status == "Réceptionné" || item.status == "Validé") AppTheme.colors.onStatusSuccess else AppTheme.colors.onStatusWarning
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
private fun EvaluationsTabView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EvaluationScoreCard("Note Globale SLA", "4.8 / 5", "Performance Excellente", AppTheme.colors.statusSuccess, Modifier.weight(1f))
            EvaluationScoreCard("Ponctualité Livraison", "96%", "Respect des délais contractuels", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            EvaluationScoreCard("Conformité des Lots", "99.4%", "Lots conformes réceptionnés", AppTheme.colors.statusSuccess, Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Observations d'Audit & Conformité Réglementaire",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Fournisseur conforme aux normes nationales de distribution pharmaceutique et de matériel médical (MSP / Algérie).\n• Respect scrupuleux des prix plafonds stipulés dans la convention cadre.\n• Facturation transparente avec traçabilité intégrale des bordereaux de livraison.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun EvaluationScoreCard(
    title: String,
    score: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(score, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onConfirm: (fullName: String, societe: String, phone: String, ville: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var societe by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var ville by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un Nouveau Fournisseur", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom ou Contact Principal *") },
                    placeholder = { Text("ex: SARL BIOPHARM ALGERIE") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = societe,
                    onValueChange = { societe = it },
                    label = { Text("Société / Raison Sociale") },
                    placeholder = { Text("ex: BIOPHARM SPA") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone") },
                        placeholder = { Text("021 98 00 00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ville,
                        onValueChange = { ville = it },
                        label = { Text("Ville / Lieu") },
                        placeholder = { Text("Alger") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, societe, phone, ville) }
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun AddConventionDialog(
    supplierName: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, Double, List<ConventionDraftArticle>, Double) -> Unit
) {
    var ordNumberText by remember { mutableStateOf("1") }
    var date by remember { mutableStateOf("2026-09-08") }
    var montantText by remember { mutableStateOf("5000000") }
    var tvaRateText by remember { mutableStateOf("0") }

    val draftArticles = remember { mutableStateListOf<ConventionDraftArticle>() }

    var itemDesignation by remember { mutableStateOf("") }
    var itemUnite by remember { mutableStateOf("BOITE") }
    var itemQtyText by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Convention (Multi-Articles) - $supplierName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ordNumberText,
                    onValueChange = { ordNumberText = it },
                    label = { Text("N° d'Ordre / Convention") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppDatePickerField(
                        value = date,
                        onValueChange = { date = it },
                        label = "Date Convention",
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = montantText,
                        onValueChange = { montantText = it },
                        label = { Text("Plafond Global (DZD)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Taux TVA :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                    listOf(0.0, 9.0, 19.0).forEach { rate ->
                        val isSelected = (tvaRateText.toDoubleOrNull() ?: -1.0) == rate
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                tvaRateText = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()
                            },
                            label = { Text("${rate.toInt()}%") }
                        )
                    }
                    OutlinedTextField(
                        value = tvaRateText,
                        onValueChange = { tvaRateText = it },
                        label = { Text("TVA %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Articles sous convention (${draftArticles.size}):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))

                // Table of currently added articles
                if (draftArticles.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            items(draftArticles, key = { it.designation }, contentType = { "draft_article" }) { article ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${article.designation} (${article.quantity.toLong()} ${article.unite})",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    IconButton(
                                        onClick = { draftArticles.remove(article) },
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
                            }
                        }
                    }
                }

                // Add article row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = itemDesignation,
                        onValueChange = { itemDesignation = it },
                        label = { Text("Désignation") },
                        singleLine = true,
                        modifier = Modifier.weight(1.8f)
                    )
                    OutlinedTextField(
                        value = itemUnite,
                        onValueChange = { itemUnite = it },
                        label = { Text("Unité") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = itemQtyText,
                        onValueChange = { itemQtyText = it },
                        label = { Text("Quota") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedButton(
                    onClick = {
                        if (itemDesignation.isNotBlank()) {
                            val qty = itemQtyText.toDoubleOrNull() ?: 50.0
                            draftArticles.add(
                                ConventionDraftArticle(
                                    designation = itemDesignation.trim(),
                                    unite = itemUnite.trim(),
                                    quantity = qty
                                )
                            )
                            itemDesignation = ""
                            itemQtyText = "100"
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("+ Ajouter cet Article", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ord = ordNumberText.toLongOrNull() ?: 1L
                    val montant = montantText.toDoubleOrNull() ?: 0.0
                    val tvaRate = tvaRateText.toDoubleOrNull() ?: 0.0
                    onConfirm(ord, date, montant, draftArticles.toList(), tvaRate)
                }
            ) {
                Text("Créer Convention")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
