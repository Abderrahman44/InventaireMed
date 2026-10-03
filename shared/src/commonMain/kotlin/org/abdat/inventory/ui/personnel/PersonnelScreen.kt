package org.abdat.inventory.ui.personnel

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.components.StatusChip
import org.abdat.inventory.components.StatusType
import org.abdat.inventory.domain.model.EquipmentAssignment
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.EmptyState
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun PersonnelScreen(
    viewModel: PersonnelViewModel,
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
                            imageVector = Icons.Default.Badge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = strings.personnelTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Répertoire des collaborateurs & chefs de service",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openAddPersonDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.addPerson)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Metric KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Total Personnel",
                value = "${state.totalStaffCount}",
                icon = Icons.Default.Person,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Chefs de Service",
                value = "${state.activeChefsCount}",
                icon = Icons.Default.SupervisorAccount,
                accentColor = AppTheme.colors.statusWarning,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Services Rattachés",
                value = "${state.services.size}",
                icon = Icons.Default.Domain,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Dotations Actives",
                value = "${state.activeAssignmentsCount}",
                icon = Icons.Default.Assignment,
                accentColor = AppTheme.colors.statusUrgent,
                modifier = Modifier.weight(1f)
            )
        }

        // Notification Alerts
        if (state.actionSuccessMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = AppTheme.colors.statusSuccess.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AppTheme.colors.statusSuccess.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AppTheme.colors.statusSuccess, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = state.actionSuccessMessage!!, color = AppTheme.colors.statusSuccess, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (state.actionErrorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = state.actionErrorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.dismissMessages() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Filter & Search Controls
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = AppTheme.colors.surfaceWhite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Rechercher un collaborateur, service, note...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                // Service Filter Selector
                ServiceDropdownFilter(
                    services = state.services,
                    selectedServiceId = state.selectedServiceId,
                    onServiceSelected = { viewModel.onServiceFilterChange(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Two-Pane Split Layout
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Staff Roster Directory (45% width)
            Surface(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.surfaceWhite,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Effectifs (${state.filteredPersons.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.filteredPersons.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(message = "Aucun collaborateur trouvé")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.filteredPersons, key = { it.id }) { person ->
                                val isSelected = person.id == state.selectedPerson?.id
                                StaffCard(
                                    person = person,
                                    isSelected = isSelected,
                                    onSelect = { viewModel.selectPerson(person) },
                                    onEdit = { viewModel.openEditPersonDialog(person) },
                                    onDelete = { viewModel.openDeletePersonDialog(person) },
                                    onToggleChef = { viewModel.toggleChefStatus(person) },
                                    onAssign = { viewModel.openAssignEquipmentDialog(person) }
                                )
                            }
                        }
                    }
                }
            }

            // Right Pane: Dotations and Equipment History (55% width)
            Surface(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.surfaceWhite,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                val person = state.selectedPerson
                if (person == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sélectionnez un collaborateur pour voir ses dotations de matériel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        // Person Detail Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(
                                            if (person.isChef) AppTheme.colors.statusWarningContainer
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (person.isChef) {
                                        Icon(
                                            Icons.Default.SupervisorAccount,
                                            contentDescription = "Chef de Service",
                                            tint = AppTheme.colors.onStatusWarning,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Text(
                                            text = person.fullName.take(2).uppercase(),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = person.fullName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = AppTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "(ID: #${person.id})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textSecondary
                                        )
                                        if (person.isChef) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AppTheme.colors.statusWarningContainer,
                                                border = BorderStroke(1.dp, AppTheme.colors.statusWarning.copy(alpha = 0.6f))
                                            ) {
                                                Text(
                                                    text = "Chef de Service",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AppTheme.colors.onStatusWarning,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Domain,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = person.serviceName.ifBlank { "Service non défini" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (person.birthDate.isNotBlank()) {
                                            Text(
                                                text = "• Date de naissance: ${person.birthDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AppTheme.colors.textSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleChefStatus(person) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = if (person.isChef) {
                                        ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.statusWarning)
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (person.isChef) Icons.Default.Star else Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (person.isChef) "Démis Chef" else "Désigner Chef",
                                        fontSize = 12.sp
                                    )
                                }

                                Button(
                                    onClick = { viewModel.openAssignEquipmentDialog(person) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Attribuer Matériel", fontSize = 12.sp)
                                }
                            }
                        }

                        if (person.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Note: ${person.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Matériels attribués (${state.personAssignments.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AppTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (state.personAssignments.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyState(
                                    message = "Aucun matériel actuellement attribué à ce collaborateur.",
                                    icon = Icons.Default.Assignment
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(state.personAssignments, key = { it.id }) { assignment ->
                                    EquipmentAssignmentRow(
                                        assignment = assignment,
                                        onReturnClick = { viewModel.openReturnDialog(assignment) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Dialogs
    // ------------------------------------------------------------------------

    if (state.isAddPersonDialogOpen) {
        PersonFormDialog(
            title = "Ajouter un Collaborateur",
            services = state.services,
            initialServiceId = state.selectedServiceId ?: state.services.firstOrNull()?.id ?: 1L,
            initialFullName = "",
            initialBirthDate = "",
            initialNotes = "",
            onDismiss = { viewModel.closeAddPersonDialog() },
            onConfirm = { serviceId, fullName, birthDate, notes ->
                viewModel.addPerson(serviceId, fullName, birthDate, notes)
            }
        )
    }

    if (state.isEditPersonDialogOpen && state.personPendingEdit != null) {
        val target = state.personPendingEdit!!
        PersonFormDialog(
            title = "Modifier la Fiche Collaborateur",
            services = state.services,
            initialServiceId = target.serviceId,
            initialFullName = target.fullName,
            initialBirthDate = target.birthDate,
            initialNotes = target.notes,
            onDismiss = { viewModel.closeEditPersonDialog() },
            onConfirm = { serviceId, fullName, birthDate, notes ->
                viewModel.updatePerson(target.id, serviceId, fullName, birthDate, notes)
            }
        )
    }

    if (state.isDeletePersonDialogOpen && state.personPendingDelete != null) {
        val target = state.personPendingDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.closeDeletePersonDialog() },
            title = { Text("Confirmer le retrait") },
            text = { Text("Êtes-vous sûr de vouloir retirer le collaborateur « ${target.fullName} » du personnel ?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePerson(target.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeDeletePersonDialog() }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (state.isAssignEquipmentDialogOpen && state.assignPersonTarget != null) {
        AssignEquipmentDialog(
            person = state.assignPersonTarget!!,
            articles = state.availableArticles,
            onDismiss = { viewModel.closeAssignEquipmentDialog() },
            onConfirm = { articleId, quantity, date, notes ->
                viewModel.assignEquipment(state.assignPersonTarget!!.id, articleId, quantity, date, notes)
            }
        )
    }

    if (state.isReturnDialogOpen && state.assignmentPendingReturn != null) {
        ReturnEquipmentDialog(
            assignment = state.assignmentPendingReturn!!,
            onDismiss = { viewModel.closeReturnDialog() },
            onConfirm = { returnDate ->
                viewModel.returnEquipment(state.assignmentPendingReturn!!.id, returnDate)
            }
        )
    }
}

// ----------------------------------------------------------------------------
// Subcomponents
// ----------------------------------------------------------------------------

@Composable
private fun StaffCard(
    person: Person,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleChef: () -> Unit,
    onAssign: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (person.isChef) AppTheme.colors.statusWarningContainer
                            else if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (person.isChef) {
                        Icon(
                            imageVector = Icons.Default.SupervisorAccount,
                            contentDescription = "Chef de Service",
                            tint = AppTheme.colors.onStatusWarning,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = person.fullName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = person.fullName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AppTheme.colors.textPrimary,
                            maxLines = 1
                        )
                        if (person.isChef) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AppTheme.colors.statusWarningContainer,
                                border = BorderStroke(1.dp, AppTheme.colors.statusWarning.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "Chef de Service",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.onStatusWarning,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#${person.id} • ${person.serviceName.ifBlank { "Sans service" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        if (person.birthDate.isNotBlank()) {
                            Text(
                                text = "• Né(e): ${person.birthDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = onToggleChef, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (person.isChef) Icons.Default.Star else Icons.Default.SupervisorAccount,
                        contentDescription = if (person.isChef) "Démis Chef de Service" else "Désigner Chef de Service",
                        modifier = Modifier.size(16.dp),
                        tint = if (person.isChef) AppTheme.colors.statusWarning else AppTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = onAssign, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Attribuer Matériel", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(15.dp), tint = AppTheme.colors.textSecondary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EquipmentAssignmentRow(
    assignment: EquipmentAssignment,
    onReturnClick: () -> Unit
) {
    val isActive = assignment.status == "ACTIVE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = assignment.articleDesignation,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "(${assignment.articleCode})",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quantité: ${assignment.quantity} ${assignment.unite}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Attribué le: ${assignment.assignedDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                    if (!isActive && assignment.returnDate != null) {
                        Text(
                            text = "Restitué le: ${assignment.returnDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.statusSuccess
                        )
                    }
                }

                if (assignment.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Notes: ${assignment.notes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusChip(
                    type = if (isActive) StatusType.WARNING else StatusType.SUCCESS,
                    text = if (isActive) "En dotation" else "Restitué"
                )

                if (isActive) {
                    Button(
                        onClick = onReturnClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restituer", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDropdownFilter(
    services: List<Service>,
    selectedServiceId: Long?,
    onServiceSelected: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = services.firstOrNull { it.id == selectedServiceId }?.name ?: "Tous les services"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Filtrer par service", fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.width(220.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Tous les services") },
                onClick = {
                    onServiceSelected(null)
                    expanded = false
                }
            )
            services.forEach { srv ->
                DropdownMenuItem(
                    text = { Text(srv.name) },
                    onClick = {
                        onServiceSelected(srv.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonFormDialog(
    title: String,
    services: List<Service>,
    initialServiceId: Long,
    initialFullName: String,
    initialBirthDate: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(initialFullName) }
    var selectedServiceId by remember { mutableStateOf(initialServiceId) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var notes by remember { mutableStateOf(initialNotes) }
    var serviceExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nom et Prénom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = serviceExpanded,
                    onExpandedChange = { serviceExpanded = it }
                ) {
                    val currentServiceName = services.firstOrNull { it.id == selectedServiceId }?.name ?: "Sélectionner un service"
                    OutlinedTextField(
                        value = currentServiceName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Service d'affectation *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = serviceExpanded,
                        onDismissRequest = { serviceExpanded = false }
                    ) {
                        services.forEach { srv ->
                            DropdownMenuItem(
                                text = { Text(srv.name) },
                                onClick = {
                                    selectedServiceId = srv.id
                                    serviceExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Date de Naissance (JJ/MM/AAAA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Fonction") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedServiceId, fullName, birthDate, notes) },
                enabled = fullName.isNotBlank()
            ) {
                Text("Enregistrer")
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
private fun AssignEquipmentDialog(
    person: Person,
    articles: List<StockArticle>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String, String) -> Unit
) {
    var selectedArticleId by remember { mutableStateOf(articles.firstOrNull()?.id ?: 0L) }
    var quantityText by remember { mutableStateOf("1") }
    var assignedDate by remember { mutableStateOf("2026-09-09") }
    var notes by remember { mutableStateOf("") }
    var articleExpanded by remember { mutableStateOf(false) }

    val selectedArticle = articles.firstOrNull { it.id == selectedArticleId }
    val parsedQty = quantityText.toDoubleOrNull() ?: 0.0
    val isStockSufficient = selectedArticle != null && selectedArticle.quantity >= parsedQty && parsedQty > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dotation de Matériel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Bénéficiaire : ${person.fullName} (${person.serviceName})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary
                )

                // Article Dropdown
                ExposedDropdownMenuBox(
                    expanded = articleExpanded,
                    onExpandedChange = { articleExpanded = it }
                ) {
                    val label = selectedArticle?.let { "${it.designation} (${it.quantity} ${it.uniteQuantite} dispo)" } ?: "Sélectionner un article"
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Article en stock *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = articleExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = articleExpanded,
                        onDismissRequest = { articleExpanded = false }
                    ) {
                        articles.forEach { art ->
                            DropdownMenuItem(
                                text = { Text("${art.designation} [Stock: ${art.quantity} ${art.uniteQuantite}]") },
                                onClick = {
                                    selectedArticleId = art.id
                                    articleExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantité attribuée *") },
                    isError = !isStockSufficient && parsedQty > 0.0,
                    supportingText = {
                        if (selectedArticle != null && selectedArticle.quantity < parsedQty) {
                            Text("Quantité supérieure au stock disponible (${selectedArticle.quantity})", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = assignedDate,
                    onValueChange = { assignedDate = it },
                    label = { Text("Date d'attribution (AAAA-MM-JJ) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / N° de Série / Remarques") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedArticleId, parsedQty, assignedDate, notes) },
                enabled = isStockSufficient
            ) {
                Text("Valider la dotation")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun ReturnEquipmentDialog(
    assignment: EquipmentAssignment,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var returnDate by remember { mutableStateOf("2026-09-09") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restitution d'Équipement") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Confirmez la réintégration en stock de :",
                    style = MaterialTheme.typography.bodyMedium
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Article: ${assignment.articleDesignation}", fontWeight = FontWeight.Bold)
                        Text(text = "Quantité: ${assignment.quantity} ${assignment.unite}")
                        Text(text = "Collaborateur: ${assignment.personName}")
                    }
                }
                OutlinedTextField(
                    value = returnDate,
                    onValueChange = { returnDate = it },
                    label = { Text("Date de restitution (AAAA-MM-JJ) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(returnDate) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirmer la restitution")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
