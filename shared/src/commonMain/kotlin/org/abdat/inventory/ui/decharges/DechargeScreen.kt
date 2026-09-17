package org.abdat.inventory.ui.decharges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.components.StatusChip
import org.abdat.inventory.components.StatusType
import org.abdat.inventory.domain.model.DechargeItem
import org.abdat.inventory.domain.model.PapierDecharge
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun DechargeScreen(
    viewModel: DechargeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val strings = AppTheme.strings

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
                modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.dechargesTitle,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Réception de subventions étatiques, dons et legs sans incidence budgétaire (SRS-V2)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openCreateDialog() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.newDecharge)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Strict Financial Valuation Guard Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.statusWarningContainer.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                .border(1.dp, AppTheme.colors.statusWarning.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AppTheme.colors.onStatusWarning)
                Text(
                    text = strings.zeroValuationNotice,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.onStatusWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // KPI Summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Total Papiers de Décharge",
                value = "${state.decharges.size}",
                subtitle = "Bons enregistrés",
                icon = Icons.Default.CardGiftcard,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Impact Budgétaire DZD",
                value = "0.00 DZD",
                subtitle = "Strictement neutre",
                icon = Icons.Default.Info,
                accentColor = AppTheme.colors.statusSuccess,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Articles Disponibles",
                value = "${state.availableArticles.size}",
                subtitle = "Au catalogue actif",
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Master-Detail Workspace
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: List of Décharges
            Card(
                modifier = Modifier.weight(0.55f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (state.decharges.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aucun papier de décharge enregistré", color = AppTheme.colors.textSecondary)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.decharges, key = { it.id }) { decharge ->
                            val isSelected = state.selectedDecharge?.id == decharge.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectDecharge(decharge) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = decharge.referenceBon,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        StatusChip(type = StatusType.SUCCESS, text = "0.00 DZD")
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = decharge.organizationName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    if (decharge.donorName.isNotBlank()) {
                                        Text(
                                            text = "Donateur : ${decharge.donorName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Reçu le : ${decharge.date}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right: Selected Décharge Details & Items
            Card(
                modifier = Modifier.weight(0.45f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                val selected = state.selectedDecharge
                if (selected == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sélectionnez un papier de décharge pour afficher les lignes", color = AppTheme.colors.textSecondary)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "Détails du Bordereau : ${selected.referenceBon}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = selected.organizationName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (selected.notes.isNotBlank()) {
                            Text(
                                text = "Note : ${selected.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Articles Réceptionnés en Stock (Entrée Gratuite)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.selectedDechargeItems, key = { it.id }) { item ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.codeArticle,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = item.designation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = AppTheme.colors.textPrimary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "+${item.quantity}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AppTheme.colors.statusSuccess
                                            )
                                            Text(
                                                text = "0.00 DZD",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Intake Dialog
    if (state.isCreateDialogOpen) {
        CreateDechargeDialog(
            nextReference = state.nextGeneratedReference,
            availableArticles = state.availableArticles,
            onDismiss = { viewModel.closeCreateDialog() },
            onConfirm = { ref, org, donor, dt, notes, lines ->
                viewModel.createDechargeVoucher(ref, org, donor, dt, notes, lines)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateDechargeDialog(
    nextReference: String,
    availableArticles: List<StockArticle>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, List<DechargeDraftLine>) -> Unit
) {
    var reference by remember { mutableStateOf(nextReference) }
    var organizationName by remember { mutableStateOf("") }
    var donorName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-09-09 20:30") }
    var notes by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<DechargeDraftLine>() }
    var orgError by remember { mutableStateOf(false) }

    // Toggle: false for Existing Article, true for Brand New Donation Article
    var isNewArticleMode by remember { mutableStateOf(false) }

    // Existing Article fields
    var selectedArticle by remember { mutableStateOf<StockArticle?>(availableArticles.firstOrNull()) }
    var expandedArticleDropdown by remember { mutableStateOf(false) }
    var existingQuantityText by remember { mutableStateOf("10.0") }

    // New Article fields
    var newCodeArticle by remember { mutableStateOf("") }
    var newDesignation by remember { mutableStateOf("") }
    var newUnite by remember { mutableStateOf("U") }
    var newFamily by remember { mutableStateOf("Matériel Médical") }
    var newSousFamily by remember { mutableStateOf("Divers") }
    var newQuantityText by remember { mutableStateOf("10.0") }
    var newExpirationDate by remember { mutableStateOf("") }
    var newArticleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enregistrer un Nouveau Papier de Décharge (Don / Subvention)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Référence Document (ex: DCH-2026-001)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = organizationName,
                    onValueChange = { organizationName = it; orgError = false },
                    label = { Text("Nom de l'Organisme / Donateur (Obligatoire)") },
                    isError = orgError,
                    supportingText = { if (orgError) Text("L'organisme est obligatoire") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text("Nom du Représentant") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date de Réception") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Nature de la convention de don") },
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Strict Zero Financial Valuation indicator
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Valorisation financière strictement nulle : 0.00 DZD (Don / Dotation non facturée)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { isNewArticleMode = false },
                        colors = if (!isNewArticleMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Article Existant")
                    }
                    OutlinedButton(
                        onClick = { isNewArticleMode = true },
                        colors = if (isNewArticleMode) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nouveau Produit")
                    }
                }

                if (!isNewArticleMode) {
                    // Existing Article Section
                    ExposedDropdownMenuBox(
                        expanded = expandedArticleDropdown,
                        onExpandedChange = { expandedArticleDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = selectedArticle?.let { "${it.codeArticle} - ${it.designation}" } ?: "Sélectionner un article du stock",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Article en Stock") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedArticleDropdown) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedArticleDropdown,
                            onDismissRequest = { expandedArticleDropdown = false }
                        ) {
                            availableArticles.forEach { article ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${article.codeArticle} - ${article.designation}", fontWeight = FontWeight.SemiBold)
                                            Text("En stock: ${article.quantity} ${article.uniteQuantite}", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = existingQuantityText,
                            onValueChange = { existingQuantityText = it },
                            label = { Text("Qté Donnée (${selectedArticle?.uniteQuantite ?: "U"})") },
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val art = selectedArticle
                                val qty = existingQuantityText.toDoubleOrNull() ?: 1.0
                                if (art != null && qty > 0) {
                                    lines.add(DechargeDraftLine(article = art, quantity = qty))
                                }
                            },
                            enabled = selectedArticle != null
                        ) {
                            Text("Ajouter")
                        }
                    }
                } else {
                    // Brand New Donation Article Section
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCodeArticle,
                            onValueChange = { newCodeArticle = it; newArticleError = false },
                            label = { Text("Code Article *") },
                            isError = newArticleError && newCodeArticle.isBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newUnite,
                            onValueChange = { newUnite = it },
                            label = { Text("Unité (ex: Boîte, U)") },
                            modifier = Modifier.width(130.dp)
                        )
                    }

                    OutlinedTextField(
                        value = newDesignation,
                        onValueChange = { newDesignation = it; newArticleError = false },
                        label = { Text("Désignation du Produit *") },
                        isError = newArticleError && newDesignation.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newFamily,
                            onValueChange = { newFamily = it },
                            label = { Text("Famille") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSousFamily,
                            onValueChange = { newSousFamily = it },
                            label = { Text("Sous-famille") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newExpirationDate,
                            onValueChange = { newExpirationDate = it },
                            label = { Text("Date Exp. (ex: 2028-12-31)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newQuantityText,
                            onValueChange = { newQuantityText = it },
                            label = { Text("Quantité Reçue *") },
                            modifier = Modifier.width(120.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val qty = newQuantityText.toDoubleOrNull() ?: 0.0
                            if (newCodeArticle.isBlank() || newDesignation.isBlank() || qty <= 0.0) {
                                newArticleError = true
                                return@Button
                            }
                            lines.add(
                                DechargeDraftLine(
                                    codeArticle = newCodeArticle.trim(),
                                    designation = newDesignation.trim(),
                                    unite = newUnite.trim().ifBlank { "U" },
                                    family = newFamily.trim(),
                                    sousFamily = newSousFamily.trim(),
                                    quantity = qty,
                                    expirationDate = newExpirationDate.trim().ifBlank { null },
                                    article = null
                                )
                            )
                            newCodeArticle = ""
                            newDesignation = ""
                            newExpirationDate = ""
                            newArticleError = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Ajouter au Don")
                    }
                }

                // Selected lines
                if (lines.isNotEmpty()) {
                    Text(
                        text = "Articles inclus dans le don (${lines.size}) :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lines.forEachIndexed { index, line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${line.codeArticle} • ${line.designation} (+${line.quantity} ${line.unite})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    val expInfo = if (!line.expirationDate.isNullOrBlank()) " • Exp: ${line.expirationDate}" else ""
                                    Text(
                                        text = "Prix: 0.00 DZD (Don)$expInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { lines.removeAt(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (organizationName.isBlank()) {
                        orgError = true
                        return@Button
                    }
                    if (lines.isNotEmpty()) {
                        onConfirm(reference, organizationName, donorName, date, notes, lines.toList())
                    }
                },
                enabled = lines.isNotEmpty()
            ) {
                Text("Valider la Réception (+ Stock)")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
