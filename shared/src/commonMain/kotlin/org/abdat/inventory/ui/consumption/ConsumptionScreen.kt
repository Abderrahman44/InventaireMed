package org.abdat.inventory.ui.consumption

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.OperationExportMenu
import org.abdat.inventory.ui.components.formatDzd
import org.abdat.inventory.util.ConsumptionExportDataMapper
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.DocxExporter
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.PdfExporter
import org.abdat.inventory.util.showSaveFileDialog

@Composable
fun ConsumptionScreen(viewModel: ConsumptionViewModel) {
    val state by viewModel.uiState.collectAsState()
    val strings = AppTheme.strings
    val coroutineScope = rememberCoroutineScope()

    var isServiceDropdownOpen by remember { mutableStateOf(false) }
    var isFamilyDropdownOpen by remember { mutableStateOf(false) }
    var isSousFamilyDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = strings.consumptionTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = strings.consumptionSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppSearchBar(
                    query = state.filterState.searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    placeholder = strings.searchPlaceholder,
                    modifier = Modifier.width(300.dp)
                )

                OperationExportMenu(
                    label = strings.printReport,
                    onExportCsv = {
                        if (state.filteredItems.isEmpty()) {
                            viewModel.setExportError(strings.noConsumptionFound)
                            return@OperationExportMenu
                        }
                        coroutineScope.launch {
                            val defaultName = "consommation_${state.filterState.dateFrom.ifBlank { "debut" }}_${state.filterState.dateTo.ifBlank { "fin" }}.csv"
                            val path = showSaveFileDialog(
                                title = "Exporter Consommation CSV",
                                defaultFileName = defaultName,
                                filters = listOf(FileTypeFilter.CSV)
                            )
                            if (path != null) {
                                try {
                                    val dateRangeText = if (state.filterState.dateFrom.isNotBlank() || state.filterState.dateTo.isNotBlank()) {
                                        "Du ${state.filterState.dateFrom.ifBlank { "---" }} Au ${state.filterState.dateTo.ifBlank { "---" }}"
                                    } else ""
                                    val serviceText = state.filterState.selectedServiceId?.let { sId ->
                                        state.services.firstOrNull { it.id == sId }?.name
                                    } ?: ""
                                    val filterDesc = listOfNotNull(
                                        dateRangeText.ifBlank { null },
                                        if (serviceText.isNotBlank()) "Service: $serviceText" else null,
                                        if (state.filterState.searchQuery.isNotBlank()) "Recherche: ${state.filterState.searchQuery}" else null
                                    ).joinToString(" | ")

                                    val dataSet = ConsumptionExportDataMapper.toExportDataSet(state.filteredItems, filterDesc)
                                    CsvExporter.writeCsv(path, dataSet.headers, dataSet.rows)
                                    viewModel.setExportSuccess("Rapport CSV généré avec succès : $path")
                                } catch (e: Exception) {
                                    viewModel.setExportError("Erreur lors de l'exportation CSV : ${e.message ?: "Erreur inconnue"}")
                                }
                            }
                        }
                    },
                    onExportWord = {
                        if (state.filteredItems.isEmpty()) {
                            viewModel.setExportError(strings.noConsumptionFound)
                            return@OperationExportMenu
                        }
                        coroutineScope.launch {
                            val defaultName = "consommation_${state.filterState.dateFrom.ifBlank { "debut" }}_${state.filterState.dateTo.ifBlank { "fin" }}.docx"
                            val path = showSaveFileDialog(
                                title = "Exporter Consommation Word",
                                defaultFileName = defaultName,
                                filters = listOf(FileTypeFilter.DOCX)
                            )
                            if (path != null) {
                                try {
                                    val dateRangeText = if (state.filterState.dateFrom.isNotBlank() || state.filterState.dateTo.isNotBlank()) {
                                        "Du ${state.filterState.dateFrom.ifBlank { "---" }} Au ${state.filterState.dateTo.ifBlank { "---" }}"
                                    } else ""
                                    val serviceText = state.filterState.selectedServiceId?.let { sId ->
                                        state.services.firstOrNull { it.id == sId }?.name
                                    } ?: ""
                                    val filterDesc = listOfNotNull(
                                        dateRangeText.ifBlank { null },
                                        if (serviceText.isNotBlank()) "Service: $serviceText" else null,
                                        if (state.filterState.searchQuery.isNotBlank()) "Recherche: ${state.filterState.searchQuery}" else null
                                    ).joinToString(" | ")

                                    val dataSet = ConsumptionExportDataMapper.toExportDataSet(state.filteredItems, filterDesc)
                                    DocxExporter.generateGenericTableDocx(
                                        filePath = path,
                                        documentTitle = dataSet.title,
                                        subtitle = dataSet.subtitle,
                                        metadata = dataSet.metadata,
                                        headers = dataSet.headers,
                                        rows = dataSet.rows
                                    )
                                    viewModel.setExportSuccess("Rapport Word généré avec succès : $path")
                                } catch (e: Exception) {
                                    viewModel.setExportError("Erreur lors de l'exportation Word : ${e.message ?: "Erreur inconnue"}")
                                }
                            }
                        }
                    },
                    onExportPdf = {
                        if (state.filteredItems.isEmpty()) {
                            viewModel.setExportError(strings.noConsumptionFound)
                            return@OperationExportMenu
                        }
                        coroutineScope.launch {
                            val defaultName = "consommation_${state.filterState.dateFrom.ifBlank { "debut" }}_${state.filterState.dateTo.ifBlank { "fin" }}.pdf"
                            val path = showSaveFileDialog(
                                title = strings.printReport,
                                defaultFileName = defaultName,
                                filters = listOf(FileTypeFilter.PDF)
                            )
                            if (path != null) {
                                try {
                                    val dateRangeText = if (state.filterState.dateFrom.isNotBlank() || state.filterState.dateTo.isNotBlank()) {
                                        "Du ${state.filterState.dateFrom.ifBlank { "---" }} Au ${state.filterState.dateTo.ifBlank { "---" }}"
                                    } else ""
                                    val serviceText = state.filterState.selectedServiceId?.let { sId ->
                                        state.services.firstOrNull { it.id == sId }?.name
                                    } ?: ""
                                    val filterDesc = listOfNotNull(
                                        dateRangeText.ifBlank { null },
                                        if (serviceText.isNotBlank()) "Service: $serviceText" else null,
                                        if (state.filterState.searchQuery.isNotBlank()) "Recherche: ${state.filterState.searchQuery}" else null
                                    ).joinToString(" | ")

                                    val dataSet = ConsumptionExportDataMapper.toExportDataSet(state.filteredItems, filterDesc)
                                    PdfExporter.generateGenericTablePdf(
                                        filePath = path,
                                        documentTitle = dataSet.title,
                                        subtitle = dataSet.subtitle,
                                        metadata = dataSet.metadata,
                                        headers = dataSet.headers,
                                        rows = dataSet.rows
                                    )
                                    viewModel.setExportSuccess("Rapport PDF généré avec succès : $path")
                                } catch (e: Exception) {
                                    viewModel.setExportError("Erreur lors de l'exportation : ${e.message ?: "Erreur inconnue"}")
                                }
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Banners (Success / Error)
        if (state.exportSuccessMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text(text = state.exportSuccessMessage!!, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textPrimary)
                }
                IconButton(onClick = { viewModel.clearExportMessages() }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (state.exportErrorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                    .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(text = state.exportErrorMessage!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { viewModel.clearExportMessages() }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Dynamic Filter Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                // Date From ("Du")
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = AppTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${strings.filterDateFrom}:", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = state.filterState.dateFrom,
                        onValueChange = { viewModel.updateDateFrom(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(85.dp)
                    )
                    if (state.filterState.dateFrom.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateDateFrom("") }, modifier = Modifier.size(18.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // Date To ("Au")
                Row(
                    modifier = Modifier
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = AppTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${strings.filterDateTo}:", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    BasicTextField(
                        value = state.filterState.dateTo,
                        onValueChange = { viewModel.updateDateTo(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = AppTheme.colors.textPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.width(85.dp)
                    )
                    if (state.filterState.dateTo.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateDateTo("") }, modifier = Modifier.size(18.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // Service Dropdown Selector
                Box {
                    val selectedServiceName = state.filterState.selectedServiceId?.let { sId ->
                        state.services.firstOrNull { it.id == sId }?.name
                    } ?: strings.allServices

                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { isServiceDropdownOpen = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedServiceName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (state.filterState.selectedServiceId != null) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (state.filterState.selectedServiceId != null) MaterialTheme.colorScheme.primary else AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(140.dp)
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }

                    DropdownMenu(
                        expanded = isServiceDropdownOpen,
                        onDismissRequest = { isServiceDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allServices) },
                            onClick = {
                                viewModel.selectService(null)
                                isServiceDropdownOpen = false
                            }
                        )
                        state.services.forEach { srv ->
                            DropdownMenuItem(
                                text = { Text(srv.name) },
                                onClick = {
                                    viewModel.selectService(srv.id)
                                    isServiceDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Family Selector
                Box {
                    val selectedFamilyName = state.filterState.selectedFamily ?: strings.allFamilies

                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { isFamilyDropdownOpen = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFamilyName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (state.filterState.selectedFamily != null) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (state.filterState.selectedFamily != null) MaterialTheme.colorScheme.primary else AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(130.dp)
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }

                    DropdownMenu(
                        expanded = isFamilyDropdownOpen,
                        onDismissRequest = { isFamilyDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allFamilies) },
                            onClick = {
                                viewModel.selectFamily(null)
                                isFamilyDropdownOpen = false
                            }
                        )
                        state.families.forEach { fam ->
                            DropdownMenuItem(
                                text = { Text(fam) },
                                onClick = {
                                    viewModel.selectFamily(fam)
                                    isFamilyDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Sous-Family Selector (Hierarchically Scoped)
                Box {
                    val selectedSousFamilyName = state.filterState.selectedSousFamily ?: strings.allSousFamilies

                    Row(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { isSousFamilyDropdownOpen = true }
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedSousFamilyName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (state.filterState.selectedSousFamily != null) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (state.filterState.selectedSousFamily != null) MaterialTheme.colorScheme.primary else AppTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(140.dp)
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }

                    DropdownMenu(
                        expanded = isSousFamilyDropdownOpen,
                        onDismissRequest = { isSousFamilyDropdownOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allSousFamilies) },
                            onClick = {
                                viewModel.selectSousFamily(null)
                                isSousFamilyDropdownOpen = false
                            }
                        )
                        state.availableSousFamilies.forEach { sf ->
                            DropdownMenuItem(
                                text = { Text(sf) },
                                onClick = {
                                    viewModel.selectSousFamily(sf)
                                    isSousFamilyDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Reset Button
                if (state.filterState.hasActiveFilters) {
                    OutlinedButton(
                        onClick = { viewModel.resetFilters() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.resetFilters, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Data Table Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Table Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date",
                        modifier = Modifier.width(95.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.voucherRef,
                        modifier = Modifier.width(110.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.filterService,
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "Code",
                        modifier = Modifier.width(90.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.designation,
                        modifier = Modifier.weight(1.8f),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = "${strings.family} / ${strings.sousFamily}",
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.quantity,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.unitPrice,
                        modifier = Modifier.width(110.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        text = strings.totalValue,
                        modifier = Modifier.width(130.dp),
                        textAlign = TextAlign.End,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Table Content
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (state.filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = strings.noConsumptionFound,
                                style = MaterialTheme.typography.bodyLarge,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.filteredItems, key = { "${it.sortieId}_${it.id}" }) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openDetail(item) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.date.take(10),
                                    modifier = Modifier.width(95.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = item.referenceBon,
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.serviceName,
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.codeArticle,
                                    modifier = Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.designation,
                                    modifier = Modifier.weight(1.8f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (item.sousFamily.isNotBlank()) "${item.family} / ${item.sousFamily}" else item.family.ifBlank { "-" },
                                    modifier = Modifier.weight(1.2f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${item.quantity.toInt()}",
                                    modifier = Modifier.width(80.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = formatDzd(item.unitPrice),
                                    modifier = Modifier.width(110.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = formatDzd(item.totalValue),
                                    modifier = Modifier.width(130.dp),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Persistent Dynamic Financial Total Bar at the Bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Text(
                            text = strings.totalLines,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "${state.totalLinesCount} lignes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column {
                        Text(
                            text = strings.totalUnitsConsumed,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "${state.totalQuantitySum.toInt()} unités",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.totalFinancialValue,
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = formatDzd(state.totalFinancialValueDzd),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Interactive Detail Dialog
    if (state.selectedDetailRecord != null) {
        ConsumptionDetailDialog(
            record = state.selectedDetailRecord!!,
            onDismiss = { viewModel.closeDetail() }
        )
    }
}

