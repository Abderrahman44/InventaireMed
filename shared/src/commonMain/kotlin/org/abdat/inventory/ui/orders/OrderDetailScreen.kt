package org.abdat.inventory.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun OrderDetailKpiStrip(
    data: OrderDetailDisplayData,
    modifier: Modifier = Modifier
) {
    val fulfilledCount = data.lines.count { it.isFulfilled }
    val totalLines = data.lines.size

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Montant Engagé (TTC)",
            value = "${data.displayItem.totalAmountDzd.toLong()} DZD",
            subtitle = if (data.displayItem.tvaRate > 0) "HT: ${data.displayItem.subtotalHtDzd.toLong()} DZD (TVA ${data.displayItem.tvaRate}%)" else "Valeur totale BC (Exonéré)",
            icon = Icons.Default.Payments,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Lignes Commandées",
            value = "$totalLines articles",
            subtitle = "Volume contractuel",
            icon = Icons.Default.Inventory,
            accentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Articles Livrés",
            value = "$fulfilledCount / $totalLines",
            subtitle = if (fulfilledCount == totalLines) "Commande soldée" else "Livraison partielle",
            icon = Icons.Default.CheckCircle,
            accentColor = if (fulfilledCount == totalLines) AppTheme.colors.statusSuccess else AppTheme.colors.statusWarning,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Bons de Livraison",
            value = "${data.linkedDeliveries.size} BL",
            subtitle = "Réceptions physiques",
            icon = Icons.Default.LocalShipping,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun OrderDetailsTable(
    lines: ImmutableList<OrderLineFulfillmentItem>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredLines = remember(lines, searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) lines else {
            lines.filter {
                it.item.codeArticle.lowercase().contains(query) ||
                it.item.designation.lowercase().contains(query)
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val tableScrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar with search and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Articles & Suivi de Livraison",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    AppBadge(
                        text = "${filteredLines.size} articles",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = AppTheme.colors.textSecondary
                    )
                }

                AppSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Filtrer les articles...",
                    modifier = Modifier.width(320.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Table Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(tableScrollState)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CODE ARTICLE", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("DÉSIGNATION", modifier = Modifier.width(260.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("UNITÉ", modifier = Modifier.width(90.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("PRIX UNIT. (DZD)", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("QTÉ COMMANDÉE", modifier = Modifier.width(140.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("QTÉ LIVRÉE", modifier = Modifier.width(130.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("RELIQUAT (EN ATTENTE)", modifier = Modifier.width(170.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
                Text("STATUT LIVRAISON", modifier = Modifier.width(180.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.textSecondary)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredLines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = AppTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aucun article correspondant trouvé",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredLines, key = { it.item.id }, contentType = { "order_detail_row" }) { line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableScrollState)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = line.item.codeArticle.ifBlank { "—" },
                                modifier = Modifier.width(130.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = line.item.designation,
                                modifier = Modifier.width(260.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "UNITÉ",
                                modifier = Modifier.width(90.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "${line.item.unitPrice.toLong()} DZD",
                                modifier = Modifier.width(140.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "${line.item.quantity.toLong()}",
                                modifier = Modifier.width(140.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "${line.deliveredQuantity.toLong()}",
                                modifier = Modifier.width(130.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (line.isFulfilled) AppTheme.colors.statusSuccess else AppTheme.colors.textPrimary
                                )
                            )
                            Text(
                                text = "${line.remainingQuantity.toLong()}",
                                modifier = Modifier.width(170.dp),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (line.remainingQuantity > 0.001) AppTheme.colors.statusWarning else AppTheme.colors.textSecondary
                                )
                            )
                            Box(modifier = Modifier.width(180.dp)) {
                                if (line.isFulfilled) {
                                    AppBadge(
                                        text = "Livré",
                                        containerColor = AppTheme.colors.statusSuccessContainer,
                                        contentColor = AppTheme.colors.onStatusSuccess
                                    )
                                } else {
                                    AppBadge(
                                        text = "Pour livraison partielle",
                                        containerColor = AppTheme.colors.statusWarningContainer,
                                        contentColor = AppTheme.colors.onStatusWarning
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

@Composable
fun OrderLinkedDeliveriesCard(
    linkedDeliveries: ImmutableList<LinkedLivraisonSummary>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Bons de Livraison Associés (Réceptions physiques)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                }
                AppBadge(
                    text = "${linkedDeliveries.size} réceptions",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            if (linkedDeliveries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune livraison enregistrée pour cette commande à ce jour.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    linkedDeliveries.forEach { bl ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = bl.referenceBon,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Date: ${bl.date} • ${bl.itemsCount} articles",
                                        style = MaterialTheme.typography.bodySmall,
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

@Composable
fun OrderDetailScreen(
    data: OrderDetailDisplayData,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header Surface
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
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retour aux commandes", style = MaterialTheme.typography.labelMedium)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Bon de Commande : ${data.displayItem.commande.referenceBon}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "Fournisseur: ${data.displayItem.supplierName}",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = AppTheme.colors.textSecondary
                            )
                            AppBadge(
                                text = "TVA ${data.displayItem.tvaRate}%",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                            if (data.displayItem.commande.isDebt) {
                                AppBadge(
                                    text = "Dette Urgente",
                                    containerColor = AppTheme.colors.statusUrgentContainer,
                                    contentColor = AppTheme.colors.onStatusUrgent
                                )
                            }
                        }
                        Text(
                            text = "Date d'émission: ${data.displayItem.commande.date} • Suivi d'exécution budgétaire et réceptions physiques.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                // Status Badge & Progress on right
                Column(horizontalAlignment = Alignment.End) {
                    val (badgeBg, badgeFg) = when {
                        data.displayItem.statusLabel.contains("Soldé") || data.displayItem.statusLabel.contains("Clôturé") ->
                            AppTheme.colors.statusSuccessContainer to AppTheme.colors.onStatusSuccess
                        data.displayItem.statusLabel.contains("Partiellement") ->
                            AppTheme.colors.statusWarningContainer to AppTheme.colors.onStatusWarning
                        data.displayItem.statusLabel.contains("Dette") ->
                            AppTheme.colors.statusUrgentContainer to AppTheme.colors.onStatusUrgent
                        data.displayItem.statusLabel.contains("Validé") ->
                            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.primary
                        else ->
                            MaterialTheme.colorScheme.surfaceContainerHighest to AppTheme.colors.textSecondary
                    }
                    AppBadge(
                        text = data.displayItem.statusLabel,
                        containerColor = badgeBg,
                        contentColor = badgeFg
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Progression: ${data.displayItem.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        LinearProgressIndicator(
                            progress = { data.displayItem.progressPercent / 100f },
                            modifier = Modifier
                                .width(120.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                data.displayItem.progressPercent >= 100 -> AppTheme.colors.statusSuccess
                                data.displayItem.progressPercent > 0 -> AppTheme.colors.statusWarning
                                else -> MaterialTheme.colorScheme.primary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary KPI Strip
        OrderDetailKpiStrip(data = data)

        Spacer(modifier = Modifier.height(16.dp))

        // Table with line items fulfillment
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            OrderDetailsTable(lines = data.lines)
        }

        if (data.linkedDeliveries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            OrderLinkedDeliveriesCard(linkedDeliveries = data.linkedDeliveries)
        }
    }
}
