package org.abdat.inventory.ui.services

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.EmptyState

@Composable
fun ServicesScreen(viewModel: ServicesViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
    ) {
        // --------------------------------------------------------------------
        // 1. En-tête contextuel & Fil d'Ariane
        // --------------------------------------------------------------------
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppTheme.colors.surfaceWhite,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
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
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Domain,
                            contentDescription = "Services & Locaux",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CONFIGURATION",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "STRUCTURE ORGANISATIONNELLE",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Services & Locaux",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )

                            AppBadge(
                                text = "${state.totalServicesCount} Services Actifs • ${state.totalLocalsCount} Locaux Rattachés",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Action Top Button
                Button(
                    onClick = { viewModel.openAddServiceDialog() },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nouveau Service",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nouveau Service", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ----------------------------------------------------------------
            // Feedback Notifications (Success & Error Banners)
            // ----------------------------------------------------------------
            state.actionSuccessMessage?.let { msg ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.statusSuccessContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AppTheme.colors.statusSuccess,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = AppTheme.colors.statusSuccess
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = AppTheme.colors.statusSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            state.actionErrorMessage?.let { err ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissMessages() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // 2. Section KPI Metrics Cards Strip
            // ----------------------------------------------------------------
            ServicesKpiStrip(
                totalServicesCount = state.totalServicesCount,
                totalLocalsCount = state.totalLocalsCount,
                totalSortiesCount = state.totalSortiesCount,
                assignmentRatePercent = state.assignmentRatePercent
            )

            // ----------------------------------------------------------------
            // 3. Section Master-Detail Split Screen Layout
            // ----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top
            ) {
                // LEFT PANEL: Master List of Services (Width 380dp, independent LazyColumn scrolling)
                Card(
                    modifier = Modifier
                        .width(380.dp)
                        .fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Services & Départements",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "${state.filteredServices.size} entités",
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = AppTheme.colors.textSecondary
                            )
                        }

                        // Search Field
                        AppSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = "Rechercher code, nom de service...",
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Independently Scrollable List of Services
                        if (state.filteredServices.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(message = "Aucun service ne correspond à la recherche.")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = state.filteredServices,
                                    key = { it.id }
                                ) { service ->
                                    val isSelected = state.selectedService?.id == service.id
                                    val localCount = state.localCountsByServiceId[service.id] ?: 0
                                    val sortieCount = state.sortieCountsByServiceId[service.id] ?: 0

                                    ServiceListItemCard(
                                        service = service,
                                        isSelected = isSelected,
                                        localCount = localCount,
                                        sortieCount = sortieCount,
                                        onClick = { viewModel.selectService(service) }
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT PANEL: Detail View of Selected Service & its Locaux (Bounded Column with Inner LazyColumn)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val currentService = state.selectedService
                    if (currentService != null) {
                        SelectedServiceHeaderCard(
                            service = currentService,
                            localsCount = state.localsForSelectedService.size,
                            sortieCount = state.sortieCountsByServiceId[currentService.id] ?: 0,
                            onEditService = { viewModel.openEditServiceDialog(currentService) },
                            onDeleteService = { viewModel.openDeleteServiceDialog(currentService) },
                            onAddLocal = { viewModel.openAddLocalDialog() }
                        )

                        ServiceLocauxTableCard(
                            service = currentService,
                            locals = state.localsForSelectedService,
                            onEditLocal = { viewModel.openEditLocalDialog(it) },
                            onDeleteLocal = { viewModel.openDeleteLocalDialog(it) },
                            onAddLocalInline = { name -> viewModel.addLocal(currentService.id, name) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    } else {
                        NoServiceSelectedCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Dialogs & Modals
    // ------------------------------------------------------------------------
    AddServiceDialog(
        isOpen = state.isAddServiceDialogOpen,
        onDismiss = { viewModel.closeAddServiceDialog() },
        onConfirm = { name, localNum -> viewModel.addService(name, localNum) }
    )

    EditServiceDialog(
        isOpen = state.isEditServiceDialogOpen,
        service = state.selectedService,
        onDismiss = { viewModel.closeEditServiceDialog() },
        onConfirm = { name, localNum ->
            state.selectedService?.let { srv ->
                viewModel.updateService(srv.id, name, localNum)
            }
        }
    )

    DeleteServiceDialog(
        isOpen = state.isDeleteServiceDialogOpen,
        service = state.servicePendingDelete,
        onDismiss = { viewModel.closeDeleteServiceDialog() },
        onConfirm = {
            state.servicePendingDelete?.let { srv ->
                viewModel.deleteService(srv.id)
            }
        }
    )

    AddLocalDialog(
        isOpen = state.isAddLocalDialogOpen,
        service = state.selectedService,
        onDismiss = { viewModel.closeAddLocalDialog() },
        onConfirm = { localName ->
            state.selectedService?.let { srv ->
                viewModel.addLocal(srv.id, localName)
            }
        }
    )

    EditLocalDialog(
        isOpen = state.isEditLocalDialogOpen,
        local = state.localPendingEdit,
        onDismiss = { viewModel.closeEditLocalDialog() },
        onConfirm = { localName ->
            state.localPendingEdit?.let { local ->
                viewModel.updateLocal(local.id, local.serviceId, localName)
            }
        }
    )

    DeleteLocalDialog(
        isOpen = state.isDeleteLocalDialogOpen,
        local = state.localPendingDelete,
        onDismiss = { viewModel.closeDeleteLocalDialog() },
        onConfirm = {
            state.localPendingDelete?.let { local ->
                viewModel.deleteLocal(local.id)
            }
        }
    )
}
