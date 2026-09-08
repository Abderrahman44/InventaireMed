package org.abdat.inventory.ui.inventory

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.components.StatusChip
import org.abdat.inventory.components.StockStatusType
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppFilterTabs
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

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
                        Text("📦", fontSize = 20.sp)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Gestion de Stock",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "MASTER INVENTORY",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = AppTheme.colors.textSecondary
                            )
                            AppBadge(
                                text = "Dépôt Quai 03",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Catalogue général des références matérielles, flux d'inventaire et valorisation en DZD.",
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
                        Text("Exporter CSV", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = { viewModel.openAddDialog() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("+ Nouvel Article", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
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
                iconLabel = "🔢",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Valeur Stock DZD",
                value = formatDzd(state.totalStockValueDzd),
                subtitle = "Valorisation PMP",
                iconLabel = "💰",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Alertes Rupture",
                value = "${state.outOfStockCount}",
                subtitle = if (state.outOfStockCount > 0) "Articles épuisés" else "Stock optimal",
                iconLabel = "⚠️",
                accentColor = if (state.outOfStockCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Stock Faible",
                value = "${state.lowStockCount}",
                subtitle = "Réappro imminent",
                iconLabel = "📉",
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Commandes en Cours",
                value = "${state.activeOrdersCount} BC",
                subtitle = "En attente réception",
                iconLabel = "🔄",
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
                placeholder = "Rechercher par code, désignation ou famille...",
                modifier = Modifier.width(360.dp)
            )

            AppFilterTabs(
                items = state.availableCategories,
                selectedItem = state.selectedCategory,
                onItemSelected = { viewModel.onCategoryChange(it) },
                labelProvider = { it }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Main Data Table
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
                    Text("CODE", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("DÉSIGNATION", modifier = Modifier.weight(2.2f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("FAMILLE", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("UNITÉ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("STOCK ACTUEL", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("PRIX UNIT. (DZD)", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("VALEUR TOTALE", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("STATUT", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("ACTIONS", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Table Rows
                if (state.filteredArticles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isEmpty()) "Aucun article enregistré." else "Aucun résultat trouvé pour « ${state.searchQuery} ».",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredArticles, key = { it.id }) { article ->
                            val status = when {
                                article.quantity <= 0 -> StockStatusType.OUT_OF_STOCK
                                article.quantity <= 10 -> StockStatusType.URGENT_DEBT
                                else -> StockStatusType.IN_STOCK
                            }
                            val totalItemValue = article.quantity * article.unitPrice

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = article.codeArticle,
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = article.designation,
                                    modifier = Modifier.weight(2.2f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = article.family.ifBlank { "Général" },
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = article.uniteQuantite,
                                    modifier = Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "${article.quantity}",
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = formatDzd(article.unitPrice),
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = formatDzd(totalItemValue),
                                    modifier = Modifier.width(130.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Box(modifier = Modifier.width(110.dp)) {
                                    StatusChip(status = status)
                                }
                                Row(
                                    modifier = Modifier.width(80.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "✏️",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.openEditDialog(article) }
                                            .padding(4.dp),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "🗑️",
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { viewModel.deleteArticle(article.id) }
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

    // Add / Edit Article Dialog
    if (state.isAddEditDialogOpen) {
        AddEditArticleDialog(
            article = state.articleToEdit,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { code, designation, unite, family, sousFamily, quantity, unitPrice, date ->
                viewModel.saveArticle(code, designation, unite, family, sousFamily, quantity, unitPrice, date)
            }
        )
    }
}

@Composable
private fun AddEditArticleDialog(
    article: StockArticle?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double, Double, String) -> Unit
) {
    var code by remember { mutableStateOf(article?.codeArticle ?: "") }
    var designation by remember { mutableStateOf(article?.designation ?: "") }
    var unite by remember { mutableStateOf(article?.uniteQuantite ?: "UNITE") }
    var family by remember { mutableStateOf(article?.family ?: "Médicaments") }
    var sousFamily by remember { mutableStateOf(article?.sousFamily ?: "") }
    var quantityText by remember { mutableStateOf(article?.quantity?.toString() ?: "0") }
    var priceText by remember { mutableStateOf(article?.unitPrice?.toString() ?: "0") }
    var date by remember { mutableStateOf(article?.date ?: "2026-09-08") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (article == null) "Ajouter un Nouvel Article" else "Modifier l'Article",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code Article (ex: MED-101)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Désignation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = family,
                        onValueChange = { family = it },
                        label = { Text("Famille / Catégorie") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unite,
                        onValueChange = { unite = it },
                        label = { Text("Unité (BOITE, FLACON...)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantité en Stock") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Unitaire (DZD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toDoubleOrNull() ?: 0.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    if (code.isNotBlank() && designation.isNotBlank()) {
                        onSave(code.trim(), designation.trim(), unite.trim(), family.trim(), sousFamily.trim(), qty, price, date)
                    }
                }
            ) {
                Text("Enregistrer")
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
