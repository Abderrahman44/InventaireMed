package org.abdat.inventory.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.components.StatusChip
import org.abdat.inventory.components.StatusType
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.ArticleCategoryPicker
import org.abdat.inventory.ui.components.rememberArticleCategorySelection
import org.abdat.inventory.ui.components.MetricCard
import org.koin.compose.koinInject

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = AppTheme.strings
    var isReportExportDialogOpen by remember { mutableStateOf(false) }

    // If an article is selected for detailed inspection and audit view
    state.selectedArticleForDetail?.let { selectedArticle ->
        val detailViewModel: ArticleDetailViewModel = koinInject()
        ArticleDetailScreen(
            articleId = selectedArticle.id,
            viewModel = detailViewModel,
            onBack = {
                // No reload needed: the table observes stock reactively, so any edit made in the
                // detail view has already re-emitted by the time we return here.
                viewModel.selectArticleForDetail(null)
            }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // 1. Header Section
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
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.navInventory,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "${state.totalArticlesCount} articles",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Text(
                            text = "Nomenclature hospitalière, audit trail, et contrôle des stocks",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { isReportExportDialogOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rapports & Impression", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.openAddDialog() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(strings.addArticle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Operational KPI Strip (5 cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Articles",
                value = "${state.totalArticlesCount}",
                subtitle = "Références actives",
                icon = Icons.Default.Category,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Valeur Stock DZD",
                value = formatDzd(state.totalStockValueDzd),
                subtitle = "Valorisation PMP",
                icon = Icons.Default.Payments,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Alertes Rupture",
                value = "${state.outOfStockCount}",
                subtitle = if (state.outOfStockCount > 0) "Articles épuisés" else "Stock optimal",
                icon = Icons.Default.Warning,
                accentColor = if (state.outOfStockCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Stock Faible",
                value = "${state.lowStockCount}",
                subtitle = "Réappro imminent",
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Commandes en Cours",
                value = "${state.activeOrdersCount} BC",
                subtitle = "En attente réception",
                icon = Icons.Default.Sync,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Search and Category Filter Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                placeholder = strings.searchPlaceholder,
                modifier = Modifier.width(360.dp)
            )

            // Status Filter (Actifs, Tous, Archivés, Obsolètes)
            AppFilterTabs(
                items = listOf("Actifs", "Tous", "Archivés", "Obsolètes"),
                selectedItem = state.statusFilter,
                onItemSelected = { viewModel.onStatusFilterChange(it) },
                labelProvider = { it }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        FamilyFilterRow(
            families = state.availableCategories,
            selectedFamily = state.selectedCategory,
            onFamilySelected = { viewModel.onCategoryChange(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Virtualized Inventory Data Table
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "Aucun article ne correspond à votre recherche",
                            style = MaterialTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Modifiez vos filtres de statut ou de famille pour élargir les résultats",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                val tableScrollState = rememberScrollState()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Table Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Code", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Réf. Barcode", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Désignation", modifier = Modifier.width(260.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Famille / Sous-Fam.", modifier = Modifier.width(160.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Unité", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Quantité", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Prix Unitaire", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Valeur Totale", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Statut", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                            Text("Actions (SRS-V2)", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Table Rows
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.filteredArticles,
                            key = { it.id },
                            contentType = { "article_row" }
                        ) { article ->
                            val totalItemValue = article.quantity * article.unitPrice

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(tableScrollState)
                                    .clickable { viewModel.selectArticleForDetail(article) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.codeArticle,
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = article.additionalReferenceId.ifBlank { "-" },
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = article.designation,
                                    modifier = Modifier.width(260.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = if (article.sousFamily.isNotBlank()) "${article.family} • ${article.sousFamily}" else article.family.ifBlank { CATEGORY_FILTER_UNCLASSIFIED },
                                    modifier = Modifier.width(160.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = article.uniteQuantite,
                                    modifier = Modifier.width(80.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${article.quantity}",
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = formatDzd(article.unitPrice),
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = formatDzd(totalItemValue),
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Box(modifier = Modifier.width(110.dp)) {
                                    if (article.isArchived) {
                                        StatusChip(type = StatusType.NEUTRAL, text = strings.statusArchived)
                                    } else if (article.isObsolete) {
                                        StatusChip(type = StatusType.URGENT, text = strings.statusObsolete)
                                    } else if (article.quantity <= 0) {
                                        StatusChip(type = StatusType.URGENT, text = strings.statusOutOfStock)
                                    } else if (article.quantity <= 10.0) {
                                        StatusChip(type = StatusType.WARNING, text = strings.statusLowStock)
                                    } else {
                                        StatusChip(type = StatusType.SUCCESS, text = strings.statusInStock)
                                    }
                                }

                                // Actions: Detail View & Archive (Hard Delete strictly removed)
                                Row(
                                    modifier = Modifier.width(90.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.selectArticleForDetail(article) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = "Détails & Audit",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (article.isArchived) viewModel.unarchiveArticle(article.id)
                                            else viewModel.archiveArticle(article.id)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (article.isArchived) Icons.Default.Restore else Icons.Default.Archive,
                                            contentDescription = if (article.isArchived) strings.unarchive else strings.archive,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (article.isArchived) MaterialTheme.colorScheme.primary else AppTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Article Dialog with Barcode & Expiration
    if (state.isAddEditDialogOpen) {
        AddEditArticleDialog(
            article = state.articleToEdit,
            // Read from state on every recomposition, so categories added while the dialog is open appear.
            taxonomy = state.taxonomy,
            saveError = state.articleSaveError,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { code, designation, unite, family, sousFamily, quantity, unitPrice, date, barcode, expirationDate, reason ->
                viewModel.saveArticle(
                    code = code,
                    designation = designation,
                    unite = unite,
                    family = family,
                    sousFamily = sousFamily,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    date = date,
                    additionalReferenceId = barcode,
                    expirationDate = expirationDate,
                    auditReason = reason
                )
            }
        )
    }

    // Dynamic Column Report & Print Dialog
    if (isReportExportDialogOpen) {
        ReportExportDialog(
            articles = state.articles,
            availableFamilies = state.taxonomy.families.map { it.label },
            onDismiss = { isReportExportDialogOpen = false }
        )
    }
}

/**
 * Family chips on their own full-width row. Scrolls horizontally, since a hospital-wide taxonomy
 * outgrows the toolbar and would otherwise push the status filter off-screen.
 */
@Composable
private fun FamilyFilterRow(
    families: List<String>,
    selectedFamily: String,
    onFamilySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        AppFilterTabs(
            items = families,
            selectedItem = selectedFamily,
            onItemSelected = onFamilySelected,
            labelProvider = { it }
        )
    }
}

@Composable
private fun AddEditArticleDialog(
    article: StockArticle?,
    taxonomy: CategoryTaxonomy,
    saveError: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double, Double, String, String, String?, String) -> Unit
) {
    var code by remember { mutableStateOf(article?.codeArticle ?: "") }
    var barcode by remember { mutableStateOf(article?.additionalReferenceId ?: "") }
    var designation by remember { mutableStateOf(article?.designation ?: "") }
    var unite by remember { mutableStateOf(article?.uniteQuantite ?: "BOITE") }
    // No default category: creation starts empty; editing preselects only what still resolves.
    val categorySelection = rememberArticleCategorySelection(
        family = article?.family ?: "",
        sousFamily = article?.sousFamily ?: "",
        taxonomy = taxonomy
    )
    var quantityText by remember { mutableStateOf(article?.quantity?.toString() ?: "0") }
    var priceText by remember { mutableStateOf(article?.unitPrice?.toString() ?: "0") }
    var expirationDate by remember { mutableStateOf(article?.expirationDate ?: "") }
    var date by remember { mutableStateOf(article?.date ?: "2026-09-09") }
    var reason by remember { mutableStateOf("") }

    var codeError by remember { mutableStateOf(false) }
    var designationError by remember { mutableStateOf(false) }
    var reasonError by remember { mutableStateOf(false) }
    var showCategoryErrors by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (article == null) "+ Nouvel Article au Catalogue" else "Modifier la Fiche Article",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it; codeError = false },
                        label = { Text("Code Article (ex: ART-101)") },
                        isError = codeError,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Réf. Barcode / Fabriquant") },
                        modifier = Modifier.weight(1.2f)
                    )
                }

                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it; designationError = false },
                    label = { Text("Désignation de l'Article") },
                    isError = designationError,
                    modifier = Modifier.fillMaxWidth()
                )

                ArticleCategoryPicker(
                    taxonomy = taxonomy,
                    selection = categorySelection,
                    showErrors = showCategoryErrors,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = unite,
                        onValueChange = { unite = it },
                        label = { Text("Unité (Pièce, Boîte, Rame...)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = expirationDate,
                        onValueChange = { expirationDate = it },
                        label = { Text("Péremption (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantité Stock") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Unitaire (DZD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f)
                    )
                }

                // If modifying existing article, require justification reason for audit log
                if (article != null) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it; reasonError = false },
                        label = { Text("* Motif obligatoire de modification (Audit)") },
                        isError = reasonError,
                        placeholder = { Text("Raison de la modification...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (saveError != null) {
                    Text(
                        text = saveError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    codeError = code.isBlank()
                    designationError = designation.isBlank()
                    if (article != null && reason.isBlank()) {
                        reasonError = true
                    }
                    val categoryLabels = categorySelection.resolvedLabels(taxonomy)
                    showCategoryErrors = categoryLabels == null
                    if (codeError || designationError || reasonError || categoryLabels == null) return@Button

                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val exp = expirationDate.ifBlank { null }
                    onSave(
                        code.trim(),
                        designation.trim(),
                        unite.trim(),
                        categoryLabels.first,
                        categoryLabels.second,
                        qty,
                        price,
                        date,
                        barcode.trim(),
                        exp,
                        reason.trim()
                    )
                }
            ) {
                Text(if (article == null) "Ajouter" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun formatDzd(amount: Double): String {
    val rounded = amount.toLong()
    return "$rounded DZD"
}
