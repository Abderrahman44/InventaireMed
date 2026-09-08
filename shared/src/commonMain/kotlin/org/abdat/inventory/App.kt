package org.abdat.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.abdat.inventory.components.StatusChip
import org.abdat.inventory.components.StockStatusType
import org.abdat.inventory.theme.AppTheme

@Composable
@Preview
fun App() {
    AppTheme {
        var showDetails by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.backgroundSlate)
                .padding(AppTheme.spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.padding(AppTheme.spacing.md)
            ) {
                Column(
                    modifier = Modifier.padding(AppTheme.spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "InventaireMed - Gestion de Stock",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))

                    Text(
                        text = "Design System & Theme Initialized",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(AppTheme.spacing.md))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(status = StockStatusType.IN_STOCK)
                        StatusChip(status = StockStatusType.URGENT_DEBT)
                        StatusChip(status = StockStatusType.OUT_OF_STOCK)
                    }

                    Spacer(modifier = Modifier.height(AppTheme.spacing.lg))

                    Button(
                        onClick = { showDetails = !showDetails }
                    ) {
                        Text(if (showDetails) "Masquer Détails" else "Afficher Détails")
                    }

                    if (showDetails) {
                        Spacer(modifier = Modifier.height(AppTheme.spacing.sm))
                        Text(
                            text = "Thème M3 actif: Primary=${MaterialTheme.colorScheme.primary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}