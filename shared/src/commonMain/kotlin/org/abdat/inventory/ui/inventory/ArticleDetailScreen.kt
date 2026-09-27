package org.abdat.inventory.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.abdat.inventory.components.StatusType
import org.abdat.inventory.domain.model.ArticleAuditLog
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.PackagingUnit
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.ArticleCategoryPicker
import org.abdat.inventory.ui.components.EmptyState
import org.abdat.inventory.ui.components.MetricCard
import org.abdat.inventory.ui.components.formatDzd
import org.abdat.inventory.ui.components.rememberArticleCategorySelection

@Composable
fun ArticleDetailScreen(
    articleId: Long,
    viewModel: ArticleDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val strings = AppTheme.strings

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val article = state.article
    if (article == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.errorMessage ?: "Article introuvable",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text(text = "Retour à l'inventaire")
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
            .padding(24.dp)
    ) {
        // Top Header Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = AppTheme.colors.textPrimary
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = article.codeArticle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
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

                    Text(
                        text = article.designation,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                }
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.toggleArchive() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (article.isArchived) Icons.Default.Restore else Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (article.isArchived) strings.unarchive else strings.archive)
                }

                if (!article.isObsolete) {
                    OutlinedButton(
                        onClick = { viewModel.markObsolete() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.statusUrgent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = strings.markObsolete)
                    }
                }

                Button(
                    onClick = { viewModel.openEditDialog() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.editArticle)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Key Metrics Summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = strings.quantity,
                value = "${article.quantity} ${article.uniteQuantite}",
                icon = Icons.Default.Category,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = strings.unitPrice,
                value = formatDzd(article.unitPrice),
                icon = Icons.Default.Payments,
                accentColor = AppTheme.colors.statusSuccess,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = strings.totalValue,
                value = formatDzd(article.quantity * article.unitPrice),
                icon = Icons.Default.Payments,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Historique Modifications",
                value = "${state.auditLogs.size} entrées",
                icon = Icons.Default.History,
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Content Area: Technical Card + Audit History Timeline
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left: Article Technical Specs Card
            Card(
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Fiche Technique & Traçabilité",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    SpecItem(label = strings.codeArticle, value = article.codeArticle)
                    SpecItem(
                        label = strings.barcodeReference,
                        value = article.additionalReferenceId.ifBlank { "Non renseigné" },
                        icon = Icons.Default.QrCode
                    )
                    SpecItem(label = strings.family, value = article.family.ifBlank { CATEGORY_FILTER_UNCLASSIFIED })
                    SpecItem(label = strings.sousFamily, value = article.sousFamily.ifBlank { "Standard" })
                    SpecItem(label = strings.packagingUnit, value = article.uniteQuantite)
                    SpecItem(
                        label = strings.expirationDate,
                        value = article.expirationDate ?: "Non périssable",
                        icon = Icons.Default.CalendarToday,
                        isWarning = article.expirationDate != null
                    )
                    SpecItem(label = "Date d'enregistrement", value = article.date)
                }
            }

            // Right: Immutable Audit Trail Log
            Card(
                modifier = Modifier.weight(0.58f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Journal d'Audit des Modifications",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Traçabilité stricte",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.auditLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(message = "Aucune modification enregistrée pour cet article.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.auditLogs, key = { it.id }) { log ->
                                AuditLogItemRow(log = log)
                            }
                        }
                    }
                }
            }
        }
    }

    // Mandatory Reason Edit Dialog
    if (state.isEditDialogOpen) {
        ArticleEditWithReasonDialog(
            article = article,
            taxonomy = state.taxonomy,
            saveError = state.errorMessage,
            onDismiss = { viewModel.closeEditDialog() },
            onConfirm = { desig, barcode, unit, fam, sousFam, price, exp, reason, dt ->
                viewModel.saveArticleModifications(
                    designation = desig,
                    additionalReferenceId = barcode,
                    unite = unit,
                    family = fam,
                    sousFamily = sousFam,
                    unitPrice = price,
                    expirationDate = exp,
                    reason = reason,
                    currentDate = dt
                )
            }
        )
    }
}

@Composable
private fun SpecItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isWarning) AppTheme.colors.statusWarning else MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isWarning) AppTheme.colors.statusWarning else AppTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun AuditLogItemRow(log: ArticleAuditLog) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Modifié par ${log.userName}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = log.editDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "Ancienne valeur : ${log.oldValue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "Nouvelle valeur : ${log.newValue}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Motif :",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = log.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ArticleEditWithReasonDialog(
    article: StockArticle,
    taxonomy: CategoryTaxonomy,
    saveError: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double, String?, String, String) -> Unit
) {
    var designation by remember { mutableStateOf(article.designation) }
    var barcode by remember { mutableStateOf(article.additionalReferenceId) }
    var unite by remember { mutableStateOf(article.uniteQuantite) }
    val categorySelection = rememberArticleCategorySelection(article.family, article.sousFamily, taxonomy)
    var showCategoryErrors by remember { mutableStateOf(false) }
    var priceText by remember { mutableStateOf(article.unitPrice.toString()) }
    var expirationDate by remember { mutableStateOf(article.expirationDate ?: "") }
    var reason by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Modifier la fiche article & enregistrer le motif",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Désignation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Réf. Code-barres Fabriquant") },
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
                        label = { Text("Unité") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Prix Unitaire (DZD)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
                    label = { Text("Date Péremption (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Mandatory Justification Field (SRS-V2 Requirement)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.statusWarningContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = "* Motif obligatoire pour l'audit trail :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.onStatusWarning
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = reason,
                            onValueChange = {
                                reason = it
                                hasError = false
                            },
                            placeholder = { Text("Ex: Correction faute frappe désignation, changement fournisseur...") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = hasError
                        )
                        if (hasError) {
                            Text(
                                text = "Le motif est obligatoire pour valider la modification.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.statusUrgent
                            )
                        }
                    }
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
                    val categoryLabels = categorySelection.resolvedLabels(taxonomy)
                    showCategoryErrors = categoryLabels == null
                    if (reason.trim().isEmpty()) {
                        hasError = true
                    }
                    if (!hasError && categoryLabels != null) {
                        val parsedPrice = priceText.toDoubleOrNull() ?: article.unitPrice
                        val exp = expirationDate.ifBlank { null }
                        onConfirm(
                            designation,
                            barcode,
                            unite,
                            categoryLabels.first,
                            categoryLabels.second,
                            parsedPrice,
                            exp,
                            reason.trim(),
                            "2026-09-09 20:30"
                        )
                    }
                }
            ) {
                Text("Valider & Enregistrer dans l'audit")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
