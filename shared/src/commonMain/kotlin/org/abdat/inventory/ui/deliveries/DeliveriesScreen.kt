package org.abdat.inventory.ui.deliveries

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.collections.immutable.ImmutableList
import org.abdat.inventory.domain.model.BonDeCommande
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppDateTimePickerField
import org.abdat.inventory.ui.components.AppSearchBar

@Composable
fun DeliveriesScreen(viewModel: DeliveriesViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var selectedDeliveryForDetail by remember { mutableStateOf<DeliveryDisplayItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
            .verticalScroll(scrollState)
    ) {
        // 1. Sub-header Breadcrumb & Logistics Status
        DeliveryHeaderBreadcrumb(state = state)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Notifications Banner
            val notificationMessage = state.actionSuccessMessage ?: state.actionErrorMessage
            if (notificationMessage != null) {
                DeliveryNotificationBanner(
                    message = notificationMessage,
                    isSuccess = state.actionSuccessMessage != null,
                    onDismiss = { viewModel.clearNotifications() }
                )
            }

            // 2. KPI Section: Reception & Inbound Metrics
            DeliveryKpiStrip(state = state)

            // 3. Inbound Delivery Registration Card
            DeliveryRegistrationCard(
                state = state,
                viewModel = viewModel
            )

            // 4. Quantitative Reconciliation Table Card
            DeliveryReconciliationTable(
                state = state,
                viewModel = viewModel
            )

            // 5. Historical Delivery Register Card
            DeliveryHistoryCard(
                state = state,
                viewModel = viewModel,
                onViewDetails = { selectedDeliveryForDetail = it }
            )
        }
    }

    // Interactive Detail Modal
    selectedDeliveryForDetail?.let { delivery ->
        DeliveryDetailDialog(
            delivery = delivery,
            onDismiss = { selectedDeliveryForDetail = null }
        )
    }
}

@Composable
private fun DeliveryHeaderBreadcrumb(state: DeliveriesUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppTheme.colors.surfaceWhite,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "OPÉRATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(text = "/", color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "RÉCEPTION & ENTRÉES EN STOCK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Text(text = "/", color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = state.selectedServiceName.ifBlank { "PHARMACIE CENTRALE" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppTheme.colors.statusSuccess, CircleShape)
                )
                Text(
                    text = "Bénéficiaire : ${state.selectedBeneficiaryName.ifBlank { "Affectation Directe" }}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "| Rapprochement BL/BC",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun DeliveryNotificationBanner(
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isSuccess) AppTheme.colors.statusSuccess.copy(alpha = 0.12f) else AppTheme.colors.statusUrgent.copy(alpha = 0.12f)
    val textColor = if (isSuccess) AppTheme.colors.statusSuccess else AppTheme.colors.statusUrgent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ReportProblem,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor
            )
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fermer",
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DeliveryKpiStrip(state: DeliveriesUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI 1: Livraisons du Jour
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVRAISONS DU JOUR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoveToInbox,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${state.deliveries.size}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "/ ${state.plannedDeliveriesCount} prévues",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppTheme.colors.statusSuccess
                    )
                    Text(
                        text = "${state.conformingDeliveriesCount} conformes aux B.C.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppTheme.colors.statusSuccess
                    )
                }
            }
        }

        // KPI 2: Volume Réceptionné ce Mois
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VOLUME RÉCEPTIONNÉ CE MOIS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppTheme.colors.statusSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppTheme.colors.statusSuccess
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatDzd(state.monthlyVolumeDzd),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "DZD",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Valorisation entrées de stock",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // KPI 3: En Attente de Rapprochement
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EN ATTENTE RAPPROCHEMENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppTheme.colors.statusWarning.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = AppTheme.colors.statusWarning
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${state.pendingReconciliationCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (state.pendingReconciliationCount > 0) AppTheme.colors.statusWarning else AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "B.C. avec reliquat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppTheme.colors.statusWarning
                    )
                    Text(
                        text = "Commandes à solder",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
        }

        // KPI 4: Anomalies / Écarts
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ANOMALIES / ÉCARTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (state.activeAnomaliesCount > 0) AppTheme.colors.statusUrgent.copy(alpha = 0.15f)
                                else AppTheme.colors.statusSuccess.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.activeAnomaliesCount > 0) Icons.Default.PriorityHigh else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (state.activeAnomaliesCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${state.activeAnomaliesCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (state.activeAnomaliesCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                    )
                    Text(
                        text = "déficits actifs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (state.activeAnomaliesCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                    )
                    Text(
                        text = if (state.activeAnomaliesCount > 0) "Écart quantitatif constaté" else "Aucun litige actif",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (state.activeAnomaliesCount > 0) AppTheme.colors.statusUrgent else AppTheme.colors.statusSuccess
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryRegistrationCard(
    state: DeliveriesUiState,
    viewModel: DeliveriesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Title Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                    Column {
                        Text(
                            text = "Enregistrement & Réception de Bon de Livraison (BL)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Contrôle physique, identification du service et bénéficiaire, rapprochement des bons de commande et intégration automatique à l'inventaire.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(
                        text = "Service : ${state.selectedServiceName.ifBlank { "Pharmacie Centrale" }}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.4f))
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Row 1: Document & Supplier Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Field 1: BL Reference
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "N° Bon Livraison Fournisseur *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Réf. Papier Transporteur",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DeliveryTextField(
                            value = state.paperBlReference,
                            onValueChange = { viewModel.updatePaperBlReference(it) },
                            icon = Icons.Default.Tag,
                            placeholder = "Ex: BL-2026-0941"
                        )
                    }

                    // Field 2: Date Réception
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Date de Réception *",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppTheme.colors.textPrimary
                        )
                        AppDateTimePickerField(
                            value = state.receptionDate,
                            onValueChange = { viewModel.updateReceptionDate(it) },
                            placeholder = "AAAA-MM-JJ HH:mm",
                            timeDialogTitle = "Sélectionner l'heure de réception",
                            height = 38.dp,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }

                    // Field 3: Commande BC
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Rattachement Bon de Commande *",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Validation ERP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        PurchaseOrderSelector(
                            availableCommandes = state.availableCommandes,
                            selectedCommandeId = state.selectedCommandeId,
                            selectedReference = state.selectedCommandeReference,
                            supplierName = state.supplierName,
                            onSelectOrder = { viewModel.selectPurchaseOrder(it) }
                        )
                        Text(
                            text = state.supplierConvention.ifBlank { "Sélectionner un bon de commande actif" },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                // Row 2: Mode de réception (Complète vs Partielle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            text = "Mode de Réception :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppTheme.colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.updateReceptionType(ReceptionType.COMPLETE) }
                        ) {
                            RadioButton(
                                selected = state.receptionType == ReceptionType.COMPLETE,
                                onClick = { viewModel.updateReceptionType(ReceptionType.COMPLETE) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = ReceptionType.COMPLETE.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.updateReceptionType(ReceptionType.PARTIAL) }
                        ) {
                            RadioButton(
                                selected = state.receptionType == ReceptionType.PARTIAL,
                                onClick = { viewModel.updateReceptionType(ReceptionType.PARTIAL) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = ReceptionType.PARTIAL.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textPrimary
                            )
                        }

                        if (state.verificationLines.any { it.isDeficit }) {
                            Row(
                                modifier = Modifier
                                    .background(AppTheme.colors.statusWarning.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AppTheme.colors.statusWarning,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "1 ou plusieurs lignes présentent un reliquat",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.statusWarning
                                )
                            }
                        }
                    }

                    Text(
                        text = "Entrée en stock : Pharmacie Centrale / Magasin Général",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryReconciliationTable(
    state: DeliveriesUiState,
    viewModel: DeliveriesViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Articles du Bon de Commande & Contrôle Quantitatif",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    AppBadge(
                        text = "${state.verificationLines.size} Références Rattachées",
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppTheme.colors.statusSuccess, CircleShape)
                        )
                        Text(
                            text = "Conforme",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AppTheme.colors.statusUrgent, CircleShape)
                        )
                        Text(
                            text = "Reliquat / Écart",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Table Content
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CODE ARTICLE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "DÉSIGNATION & SPÉCIFICATIONS",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.weight(2.5f)
                        )
                        Text(
                            text = "RELIQUAT ATTENDU",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.4f)
                        )
                        Text(
                            text = "QUANTITÉ REÇUE (UNITÉS)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.8f)
                        )
                        Text(
                            text = "N° LOT / TRAÇABILITÉ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.weight(1.6f)
                        )
                        Text(
                            text = "P.U. (DZD)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "TOTAL LIGNE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.3f)
                        )
                        Text(
                            text = "CONFORMITÉ",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    // Verification Rows
                    state.verificationLines.forEachIndexed { index, line ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f) else AppTheme.colors.surfaceWhite)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Code
                            Text(
                                text = line.codeArticle.ifBlank { "N/A" },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1.2f)
                            )

                            // Designation
                            Column(modifier = Modifier.weight(2.5f)) {
                                Text(
                                    text = line.designation,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Affectation : ${line.targetLocation}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AppTheme.colors.textSecondary
                                )
                            }

                            // Remaining Reliquat
                            Column(
                                modifier = Modifier.weight(1.4f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${line.remainingQuantity.toInt()} unités",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )
                                if (line.previouslyDeliveredQuantity > 0) {
                                    Text(
                                        text = "(${line.previouslyDeliveredQuantity.toInt()} déjà livrées)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                            }

                            // Editable Received Qty
                            Row(
                                modifier = Modifier.weight(1.8f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                QuantityStepperButton(
                                    text = "-",
                                    onClick = { viewModel.updateReceivedQuantity(line.commandeItemId, line.receivedQuantity - 1) }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                BasicTextField(
                                    value = line.receivedQuantity.toInt().toString(),
                                    onValueChange = { str ->
                                        val qty = str.toDoubleOrNull() ?: line.receivedQuantity
                                        viewModel.updateReceivedQuantity(line.commandeItemId, qty)
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = AppTheme.colors.textPrimary
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .width(48.dp)
                                        .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(4.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(vertical = 4.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                QuantityStepperButton(
                                    text = "+",
                                    onClick = { viewModel.updateReceivedQuantity(line.commandeItemId, line.receivedQuantity + 1) }
                                )
                            }

                            // Lot Number
                            Box(modifier = Modifier.weight(1.6f).padding(horizontal = 4.dp)) {
                                BasicTextField(
                                    value = line.lotNumber,
                                    onValueChange = { viewModel.updateLotNumber(line.commandeItemId, it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = AppTheme.colors.textPrimary
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(4.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            // PU
                            Text(
                                text = formatDzd(line.unitPrice),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = AppTheme.colors.textSecondary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )

                            // Total
                            Text(
                                text = formatDzd(line.totalAmount),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppTheme.colors.textPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.3f)
                            )

                            // Conformity Badge
                            Box(
                                modifier = Modifier.weight(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                DiscrepancyBadge(line = line)
                            }
                        }

                        if (index < state.verificationLines.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                    }
                }
            }

            // Discrepancy Notice
            if (state.discrepancyNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.statusWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AppTheme.colors.statusWarning.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppTheme.colors.statusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Réserve Transporteur & Reliquat Automatique",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.statusWarning
                            )
                            Text(
                                text = state.discrepancyNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            // Valuation Total & Validation Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Valorisation Entrée :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatDzd(state.totalCalculatedDzd),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "DZD",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.validateDeliverySync(
                            referenceBon = state.paperBlReference,
                            date = state.receptionDate,
                            isPartial = state.receptionType == ReceptionType.PARTIAL
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.statusSuccess,
                        contentColor = Color.White
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "Valider la Livraison & Mettre à Jour le Stock",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryHistoryCard(
    state: DeliveriesUiState,
    viewModel: DeliveriesViewModel,
    onViewDetails: (DeliveryDisplayItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Journal des Entrées (Registre Réceptions)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "${state.filteredDeliveries.size} réceptions enregistrées et intégrées au stock valorisé",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = "Rechercher BL, service, bénéficiaire, BC...",
                        modifier = Modifier.width(320.dp)
                    )

                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Imprimer Journal", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Historical Table
            Column(modifier = Modifier.fillMaxWidth()) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "N° RÉCEPTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "N° BL PAPIER",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "B.C. RATTACHÉ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "FOURNISSEUR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(1.8f)
                    )
                    Text(
                        text = "DESTINATION STOCK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = "DATE RÉCEPTION",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "VALORISATION (DZD)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.4f)
                    )
                    Text(
                        text = "STATUT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                val deliveriesList = state.filteredDeliveries
                if (deliveriesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aucune entrée enregistrée dans le registre.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                } else {
                    deliveriesList.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.3f) else AppTheme.colors.surfaceWhite)
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.receptionNumber,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = item.paperBlReference,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = AppTheme.colors.textSecondary,
                                modifier = Modifier.weight(1.2f)
                            )
                            Text(
                                text = item.commandeReference,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.textPrimary,
                                modifier = Modifier.weight(1.1f)
                            )
                            Column(modifier = Modifier.weight(1.8f)) {
                                Text(
                                    text = item.supplierName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = item.supplierConvention,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                            Column(modifier = Modifier.weight(2f)) {
                                Text(
                                    text = "Entrée en Stock",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = item.destinationStock,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                            Text(
                                text = item.dateLivraison,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                modifier = Modifier.weight(1.1f)
                            )
                            Text(
                                text = "${formatDzd(item.totalAmountDzd)} DZD",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AppTheme.colors.textPrimary,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.4f)
                            )
                            Box(
                                modifier = Modifier.weight(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    modifier = Modifier
                                        .background(AppTheme.colors.statusSuccess.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = AppTheme.colors.statusSuccess,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = item.statusLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = AppTheme.colors.statusSuccess
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.weight(0.8f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onViewDetails(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Détails",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (index < deliveriesList.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryDetailDialog(
    delivery: DeliveryDisplayItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Détails Bon de Livraison",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "${delivery.receptionNumber} • Réf : ${delivery.paperBlReference}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = AppTheme.colors.textSecondary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Detail Fields Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRow(label = "Fournisseur Partenaire :", value = delivery.supplierName)
                    DetailRow(label = "Convention Cadre :", value = delivery.supplierConvention)
                    DetailRow(label = "Bon de Commande Associé :", value = delivery.commandeReference)
                    DetailRow(label = "Date de Réception :", value = delivery.dateLivraison)
                    DetailRow(label = "Destination Stock :", value = delivery.destinationStock)
                    DetailRow(label = "Nombre d'Articles :", value = "${delivery.itemsCount} références")
                    DetailRow(label = "Montant Total Valorisé :", value = "${formatDzd(delivery.totalAmountDzd)} DZD")
                    DetailRow(label = "Statut Validation :", value = delivery.statusLabel)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Fermer")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = AppTheme.colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun DeliveryTextField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppTheme.colors.surfaceWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = AppTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.textPrimary
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PurchaseOrderSelector(
    availableCommandes: List<BonDeCommande>,
    selectedCommandeId: Long?,
    selectedReference: String,
    supplierName: String,
    onSelectOrder: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppTheme.colors.surfaceWhite)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (selectedReference.isNotBlank()) "$selectedReference ($supplierName)" else "Sélectionner un BC",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = AppTheme.colors.textPrimary
                )
            }
            Text(
                text = "▾",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableCommandes.forEach { cmd ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${cmd.referenceBon} - ${cmd.date}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${cmd.items.size} articles commandés",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectOrder(cmd.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun QuantityStepperButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun DiscrepancyBadge(line: DeliveryVerificationLine) {
    when {
        line.isConforming -> {
            AppBadge(
                text = "CONFORME",
                containerColor = AppTheme.colors.statusSuccess.copy(alpha = 0.15f),
                contentColor = AppTheme.colors.statusSuccess
            )
        }
        line.isDeficit -> {
            AppBadge(
                text = "MANQUE (${line.gap.toInt()})",
                containerColor = AppTheme.colors.statusUrgent.copy(alpha = 0.15f),
                contentColor = AppTheme.colors.statusUrgent
            )
        }
        line.isSurplus -> {
            AppBadge(
                text = "SURPLUS (+${line.gap.toInt()})",
                containerColor = AppTheme.colors.statusWarning.copy(alpha = 0.15f),
                contentColor = AppTheme.colors.statusWarning
            )
        }
    }
}

private fun formatDzd(amount: Double): String {
    val integerPart = amount.toLong()
    val formatted = integerPart.toString()
        .reversed()
        .chunked(3)
        .joinToString(" ")
        .reversed()
    return "$formatted,00"
}
