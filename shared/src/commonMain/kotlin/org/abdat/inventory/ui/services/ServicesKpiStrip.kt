package org.abdat.inventory.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun ServicesKpiStrip(
    totalServicesCount: Int,
    totalLocalsCount: Int,
    totalSortiesCount: Int,
    assignmentRatePercent: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "Services Opérationnels",
            value = "$totalServicesCount",
            subtitle = "Centres de coût actifs",
            icon = Icons.Default.Domain,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Locaux Rattachés",
            value = "$totalLocalsCount",
            subtitle = "Salles & magasins déclarés",
            icon = Icons.Default.MeetingRoom,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Mouvements Récents",
            value = "$totalSortiesCount",
            subtitle = "Imputations sur bons de sortie",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            accentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Taux d'Affectation",
            value = "$assignmentRatePercent%",
            subtitle = "0 local orphelin • Cascade active",
            icon = Icons.Default.CheckCircle,
            accentColor = AppTheme.colors.statusSuccess,
            modifier = Modifier.weight(1f)
        )
    }
}
