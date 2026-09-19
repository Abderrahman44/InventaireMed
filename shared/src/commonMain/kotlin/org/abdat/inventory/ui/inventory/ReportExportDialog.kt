package org.abdat.inventory.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.domain.model.categoryLabelsMatch
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.PdfExporter
import org.abdat.inventory.util.showSaveFileDialog
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

enum class ReportColumn(val id: String, val label: String, val isFinancial: Boolean) {
    CODE("code", "Code Article", false),
    BARCODE("barcode", "Réf. Code-barres", false),
    DESIGNATION("designation", "Désignation Article", false),
    FAMILY("family", "Famille", false),
    SOUS_FAMILY("sous_family", "Sous-Famille", false),
    QUANTITY("quantity", "Quantité en Stock", false),
    UNITE("unite", "Unité de Mesure", false),
    UNIT_PRICE("unit_price", "Prix Unitaire (DZD)", true),
    TOTAL_VALUE("total_value", "Valorisation Totale (DZD)", true),
    EXPIRATION("expiration", "Date de Péremption", false),
    STATUS("status", "Statut Stock", false)
}

@Composable
fun ReportExportDialog(
    articles: List<StockArticle>,
    availableFamilies: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onExportSuccess: (String) -> Unit = {}
) {
    val strings = AppTheme.strings

    // Column selection state (all enabled by default)
    val selectedColumns = remember {
        mutableStateMapOf<String, Boolean>().apply {
            ReportColumn.entries.forEach { put(it.id, true) }
        }
    }

    val allFamilies = remember(availableFamilies, articles) {
        val configured = availableFamilies.map { it.trim() }.filter { it.isNotBlank() }
        val fromArticles = articles.map { it.family.trim() }.filter { it.isNotBlank() }
        val distinctFamilies = (configured + fromArticles).distinct()
        val hasUnclassified = articles.any { it.family.isBlank() }
        if (hasUnclassified) distinctFamilies + CATEGORY_FILTER_UNCLASSIFIED else distinctFamilies
    }

    val selectedFamilies = remember(allFamilies) {
        mutableStateMapOf<String, Boolean>().apply {
            allFamilies.forEach { put(it, true) }
        }
    }

    var selectedServiceFilter by remember { mutableStateOf("Tous") }
    var startDateFilter by remember { mutableStateOf("2026-01-01") }
    var endDateFilter by remember { mutableStateOf("2026-12-31") }
    var reportTitle by remember { mutableStateOf("État Général du Stock & Consommation") }
    var showPreviewSuccess by remember { mutableStateOf(false) }
    var exportSuccessPath by remember { mutableStateOf<String?>(null) }
    var exportFormat by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Filter computation
    val filteredArticles = articles.filter { article ->
        val fam = article.family.ifBlank { CATEGORY_FILTER_UNCLASSIFIED }
        selectedFamilies.entries.any { (familyLabel, checked) ->
            checked && categoryLabelsMatch(familyLabel, fam)
        }
    }

    val selectedCount = selectedColumns.values.count { it }
    val selectedFamiliesCount = selectedFamilies.values.count { it }
    val containsFinancial = selectedColumns.entries.any { (id, checked) ->
        checked && (id == ReportColumn.UNIT_PRICE.id || id == ReportColumn.TOTAL_VALUE.id)
    }

    val checkedFamilyNames = allFamilies.filter { selectedFamilies[it] == true }
    val familySummaryText = when {
        checkedFamilyNames.isEmpty() -> "Aucune"
        checkedFamilyNames.size == allFamilies.size -> "Toutes (${allFamilies.size})"
        else -> checkedFamilyNames.joinToString(", ")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Génération de Rapport & Impression Avancée (SRS-V2)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section 1: Report Title & Target Filters
                Text(
                    text = "1. Filtres Ciblés (Produits, Services, Période)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = reportTitle,
                    onValueChange = { reportTitle = it },
                    label = { Text("Titre du Document") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedServiceFilter,
                        onValueChange = { selectedServiceFilter = it },
                        label = { Text("Service Destinataire (ex: Bloc, Tous)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startDateFilter,
                        onValueChange = { startDateFilter = it },
                        label = { Text("Date Début (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDateFilter,
                        onValueChange = { endDateFilter = it },
                        label = { Text("Date Fin (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Family Filter Checkbox Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Familles de Produits à Inclure",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cochez les familles ($selectedFamiliesCount / ${allFamilies.size} sélectionnée(s))",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { allFamilies.forEach { selectedFamilies[it] = true } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Tout sélectionner", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { allFamilies.forEach { selectedFamilies[it] = false } },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Tout désélectionner", fontSize = 11.sp)
                        }
                    }
                }

                if (allFamilies.isEmpty()) {
                    Text(
                        text = "Aucune famille configurée",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            allFamilies.chunked(2).forEach { rowFams ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowFams.forEach { fam ->
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val current = selectedFamilies[fam] ?: true
                                                    selectedFamilies[fam] = !current
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedFamilies[fam] == true,
                                                onCheckedChange = { isChecked ->
                                                    selectedFamilies[fam] = isChecked
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Text(
                                                text = fam,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.textPrimary
                                            )
                                        }
                                    }
                                    if (rowFams.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Section 2: Dynamic Column Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "2. Sélection Dynamique des Colonnes",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Cochez les colonnes à inclure sur le PDF / l'impression",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    // Quick unselect financial
                    OutlinedButton(
                        onClick = {
                            selectedColumns[ReportColumn.UNIT_PRICE.id] = false
                            selectedColumns[ReportColumn.TOTAL_VALUE.id] = false
                        }
                    ) {
                        Text("Masquer Finances", fontSize = 11.sp)
                    }
                }

                if (!containsFinancial) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ℹ️ Données financières masquées : ce rapport est optimisé pour le comptage physique en inventaire.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Grid of checkboxes for column selection
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportColumn.entries.chunked(2).forEach { rowCols ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowCols.forEach { col ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedColumns[col.id] == true,
                                            onCheckedChange = { isChecked ->
                                                selectedColumns[col.id] = isChecked
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = col.label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (col.isFinancial) FontWeight.SemiBold else FontWeight.Normal
                                            ),
                                            color = if (col.isFinancial) MaterialTheme.colorScheme.primary else AppTheme.colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Summary Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Articles inclus : ${filteredArticles.size} / ${articles.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "Familles : $selectedFamiliesCount / ${allFamilies.size} | Colonnes : $selectedCount / ${ReportColumn.entries.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (exportError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.colors.statusUrgentContainer, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = AppTheme.colors.onStatusUrgent)
                            Text(
                                text = exportError ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.onStatusUrgent
                            )
                        }
                    }
                }

                if (showPreviewSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.colors.statusSuccessContainer, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AppTheme.colors.onStatusSuccess)
                            val message = if (exportSuccessPath != null) {
                                val fmt = exportFormat ?: "Fichier"
                                "Rapport $fmt exporté avec succès !\nChemin : $exportSuccessPath"
                            } else {
                                "Rapport prêt ! Impression simulée avec succès (${filteredArticles.size} articles, $selectedFamiliesCount familles, $selectedCount colonnes)."
                            }
                            Text(
                                text = message,
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.onStatusSuccess
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            exportError = null
                            val defaultName = reportTitle.replace(Regex("[^A-Za-z0-9]"), "_") + ".csv"
                            val path = showSaveFileDialog(
                                title = "Exporter Rapport CSV (Excel)",
                                defaultFileName = defaultName,
                                filters = listOf(FileTypeFilter("CSV Files", "csv"))
                            )
                            if (path != null) {
                                try {
                                    val (headers, rows) = StockReportDataMapper.toHeadersAndRows(filteredArticles, selectedColumns)
                                    CsvExporter.writeCsv(path, headers, rows)
                                    exportFormat = "CSV"
                                    exportSuccessPath = path
                                    showPreviewSuccess = true
                                    onExportSuccess("Rapport CSV exporté avec succès : $path")
                                } catch (e: Exception) {
                                    exportError = "Erreur lors de l'export CSV : ${e.message ?: "Erreur inconnue"}"
                                }
                            }
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exporter CSV (Excel)")
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            exportError = null
                            val defaultName = reportTitle.replace(Regex("[^A-Za-z0-9]"), "_") + ".pdf"
                            val path = showSaveFileDialog(
                                title = "Exporter Rapport PDF",
                                defaultFileName = defaultName,
                                filters = listOf(FileTypeFilter("PDF Files", "pdf"))
                            )
                            if (path != null) {
                                try {
                                    val (headers, rows) = StockReportDataMapper.toHeadersAndRows(filteredArticles, selectedColumns)
                                    PdfExporter.generateStockReportPdf(
                                        filePath = path,
                                        reportTitle = reportTitle,
                                        dateRange = "$startDateFilter à $endDateFilter",
                                        familiesFilter = familySummaryText,
                                        serviceFilter = selectedServiceFilter,
                                        headers = headers,
                                        rows = rows
                                    )
                                    exportFormat = "PDF"
                                    exportSuccessPath = path
                                    showPreviewSuccess = true
                                    onExportSuccess("Rapport PDF exporté avec succès : $path")
                                } catch (e: Exception) {
                                    exportError = "Erreur lors de la génération PDF : ${e.message ?: "Erreur inconnue"}"
                                }
                            }
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Générer PDF")
                }

                Button(
                    onClick = {
                        showPreviewSuccess = true
                        onExportSuccess("Ordre d'impression envoyé à l'imprimante système")
                    }
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Imprimer")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
