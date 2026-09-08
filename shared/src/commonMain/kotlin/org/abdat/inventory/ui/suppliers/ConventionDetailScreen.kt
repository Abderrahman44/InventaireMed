package org.abdat.inventory.ui.suppliers

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun ConventionDetailScreen(
    convention: Convention,
    supplier: Fournisseur,
    balances: List<ConventionQuotaBalance>,
    onBack: () -> Unit,
    onOpenAddArticle: () -> Unit,
    onEditArticle: (ConventionQuotaBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredBalances = remember(balances, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            balances
        } else {
            balances.filter {
                it.designation.lowercase().contains(query) || it.unite.lowercase().contains(query)
            }
        }
    }

    val totalAgreed = balances.sumOf { it.agreedQuantity }
    val totalOrdered = balances.sumOf { it.orderedQuantity }
    val totalRemaining = balances.sumOf { it.remainingQuantity }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Navigation & Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text("← Retour aux conventions", style = MaterialTheme.typography.labelMedium)
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📜", fontSize = 18.sp)
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Convention N° ${convention.ordNumber}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.5).sp
                                    ),
                                    color = AppTheme.colors.textPrimary
                                )
                                AppBadge(
                                    text = "CONVENTION ACTIVE",
                                    containerColor = AppTheme.colors.statusSuccessContainer,
                                    contentColor = AppTheme.colors.onStatusSuccess
                                )
                            }
                            Text(
                                text = "Fournisseur : ${supplier.fullName} (FRN-${supplier.id.toString().padStart(4, '0')}) • Signée le : ${convention.date} • Plafond : ${convention.totalMontant.toLong()} DZD",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onOpenAddArticle,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("+ Ajouter un Article", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // KPI Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Articles Conventionnés",
                value = "${balances.size}",
                subtitle = "Lignes contractuelles actives",
                iconLabel = "📦",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Volume Global Convenu",
                value = "${totalAgreed.toLong()} U",
                subtitle = "Quota total négocié",
                iconLabel = "📊",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Volume Consommé",
                value = "${totalOrdered.toLong()} U",
                subtitle = "Engagé via bons de commande",
                iconLabel = "🛒",
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Solde Restant",
                value = "${totalRemaining.toLong()} U",
                subtitle = "Quota disponible pour commandes",
                iconLabel = "⚡",
                accentColor = if (totalRemaining <= 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Contractual Articles Table Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Table Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ARTICLES DE LA CONVENTION (${filteredBalances.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )

                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Rechercher un article...",
                        modifier = Modifier.width(320.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DÉSIGNATION", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("UNITÉ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("PRIX RÉF.", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("QUOTA CONVENU", modifier = Modifier.width(120.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("COMMANDÉ", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("CONSOMMATION", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("SOLDE RESTANT", modifier = Modifier.width(110.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                    Text("ACTION", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredBalances.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucun article trouvé dans cette convention.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredBalances, key = { it.conventionItemId }) { item ->
                            val progress = if (item.agreedQuantity > 0) {
                                (item.orderedQuantity / item.agreedQuantity).toFloat().coerceIn(0f, 1f)
                            } else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        item.designation,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    if (item.remainingQuantity <= item.minQuantity) {
                                        Text(
                                            "Seuil critique atteint (Min: ${item.minQuantity.toLong()})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppTheme.colors.statusUrgent
                                        )
                                    }
                                }

                                Text(
                                    item.unite,
                                    modifier = Modifier.width(90.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )

                                Text(
                                    "${item.unitPrice.toLong()} DZD",
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    "${item.agreedQuantity.toLong()}",
                                    modifier = Modifier.width(120.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )

                                Text(
                                    "${item.orderedQuantity.toLong()}",
                                    modifier = Modifier.width(100.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )

                                Column(modifier = Modifier.width(130.dp)) {
                                    Text(
                                        "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (progress >= 0.9f) AppTheme.colors.statusUrgent else AppTheme.colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
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

                                Text(
                                    "${item.remainingQuantity.toLong()}",
                                    modifier = Modifier.width(110.dp),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.remainingQuantity <= item.minQuantity) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                                )

                                Box(modifier = Modifier.width(100.dp)) {
                                    OutlinedButton(
                                        onClick = { onEditArticle(item) },
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("✏️ Éditer", style = MaterialTheme.typography.labelSmall)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddArticleToConventionDialog(
    convention: Convention,
    supplier: Fournisseur,
    onDismiss: () -> Unit,
    onConfirm: (designation: String, unite: String, quantity: Double, minQuantity: Double) -> Unit
) {
    var designation by remember { mutableStateOf("") }
    var unite by remember { mutableStateOf("UNITE") }
    var quantityText by remember { mutableStateOf("") }
    var minQuantityText by remember { mutableStateOf("5") }
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }

    val standardUnits = listOf("UNITE", "BOITE", "FLACON", "AMPOULE", "PACK", "CARTON", "KIT")

    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val minQuantity = minQuantityText.toDoubleOrNull() ?: 5.0
    val isValid = designation.isNotBlank() && quantity > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "+ Ajouter un Article à la Convention",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "Convention N° ${convention.ordNumber} • ${supplier.fullName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Désignation de l'article *") },
                    placeholder = { Text("Ex: Paracétamol 500mg, Sérum Salé...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isUnitDropdownExpanded,
                        onExpandedChange = { isUnitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unite,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unité *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = isUnitDropdownExpanded,
                            onDismissRequest = { isUnitDropdownExpanded = false }
                        ) {
                            standardUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unite = u
                                        isUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) quantityText = it },
                        label = { Text("Quota convenu *") },
                        placeholder = { Text("Ex: 500") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) minQuantityText = it },
                    label = { Text("Seuil d'alerte min") },
                    placeholder = { Text("Ex: 10") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(designation, unite, quantity, minQuantity) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Ajouter à la convention")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun SensitiveEditWarningDialog(
    article: ConventionQuotaBalance,
    convention: Convention,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("⚠️", fontSize = 22.sp)
                Text(
                    text = "Action Sensible : Modification d'Article",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.statusUrgent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.statusUrgentContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Article concerné : ${article.designation}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.onStatusUrgent
                        )
                        Text(
                            text = "Convention N° ${convention.ordNumber} • Quota actuel : ${article.agreedQuantity.toLong()} ${article.unite} (Commandé : ${article.orderedQuantity.toLong()})",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.onStatusUrgent.copy(alpha = 0.9f)
                        )
                    }
                }

                Text(
                    text = "Attention : Modifier un article sous convention active entraîne des conséquences directes :",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AppTheme.colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "• Les quotas restants et alertes de seuil seront immédiatement recalculés.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        "• Réduire le quota en dessous du volume déjà commandé (${article.orderedQuantity.toLong()} ${article.unite}) créera un dépassement contractuel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    Text(
                        "• Assurez-vous que cette modification correspond à un avenant ou accord formel avec le fournisseur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }

                Text(
                    text = "Voulez-vous vraiment continuer vers l'édition de cet article ?",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.statusUrgent)
            ) {
                Text("Continuer la modification", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConventionArticleDialog(
    article: ConventionQuotaBalance,
    convention: Convention,
    onDismiss: () -> Unit,
    onConfirm: (designation: String, unite: String, quantity: Double, minQuantity: Double) -> Unit
) {
    var designation by remember { mutableStateOf(article.designation) }
    var unite by remember { mutableStateOf(article.unite) }
    var quantityText by remember { mutableStateOf(if (article.agreedQuantity % 1.0 == 0.0) article.agreedQuantity.toLong().toString() else article.agreedQuantity.toString()) }
    var minQuantityText by remember { mutableStateOf(if (article.minQuantity % 1.0 == 0.0) article.minQuantity.toLong().toString() else article.minQuantity.toString()) }
    var isUnitDropdownExpanded by remember { mutableStateOf(false) }

    val standardUnits = listOf("UNITE", "BOITE", "FLACON", "AMPOULE", "PACK", "CARTON", "KIT")

    val newQuantity = quantityText.toDoubleOrNull() ?: 0.0
    val newMinQuantity = minQuantityText.toDoubleOrNull() ?: 0.0
    val isValid = designation.isNotBlank() && newQuantity > 0.0
    val isBelowOrdered = newQuantity < article.orderedQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Modifier l'Article Conventionné",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "Convention N° ${convention.ordNumber} • Déjà commandé : ${article.orderedQuantity.toLong()} ${article.unite}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Désignation de l'article *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isUnitDropdownExpanded,
                        onExpandedChange = { isUnitDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unite,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unité *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = isUnitDropdownExpanded,
                            onDismissRequest = { isUnitDropdownExpanded = false }
                        ) {
                            standardUnits.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unite = u
                                        isUnitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) quantityText = it },
                        label = { Text("Quota convenu *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = minQuantityText,
                    onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() || char == '.' }) minQuantityText = it },
                    label = { Text("Seuil d'alerte min") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isBelowOrdered) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.colors.statusUrgentContainer, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "⚠️ Attention : Le nouveau quota (${newQuantity.toLong()}) est inférieur à la quantité déjà commandée (${article.orderedQuantity.toLong()}) !",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.onStatusUrgent
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(designation, unite, newQuantity, newMinQuantity) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Enregistrer les modifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
