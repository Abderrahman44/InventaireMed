package org.abdat.inventory.ui.config

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.abdat.inventory.domain.model.DynamicConfigItem
import org.abdat.inventory.theme.AppTheme

@Composable
fun DynamicConfigScreen(
    viewModel: DynamicConfigViewModel,
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
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "Gestion des listes déroulantes, seuils d'alerte, formats de numérotation et profil administrateur",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
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

        // 2. Navigation Tabs
        TabRow(
            selectedTabIndex = state.selectedTab.ordinal,
            containerColor = AppTheme.colors.surfaceWhite,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            ConfigTab.values().forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Tab Contents
        when (state.selectedTab) {
            ConfigTab.TYPES -> ConfigDropdownsTab(viewModel = viewModel, state = state)
            ConfigTab.THRESHOLDS -> ConfigThresholdsTab(viewModel = viewModel, state = state)
            ConfigTab.PREFIXES -> ConfigPrefixesTab(viewModel = viewModel, state = state)
            ConfigTab.PROFILE -> ConfigProfileTab(viewModel = viewModel, state = state)
            ConfigTab.BACKUP -> ConfigBackupTab(viewModel = viewModel, state = state)
        }
    }

    // Dialogs
    if (state.isRestoreConfirmationDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = strings.confirmRestoreTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.confirmRestoreMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    state.pendingRestoreFilePath?.let { path ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Fichier sélectionné :",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary
                                )
                                state.pendingRestoreValidationDetails?.let { details ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = details,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.statusSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restaurer maintenant")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelRestore() }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (state.isAddOptionDialogOpen) {
        AddConfigOptionDialog(
            category = state.targetCategoryForAdd,
            families = state.families.filter { it.isActive },
            errorMessage = state.actionErrorMessage,
            onDismiss = { viewModel.closeAddOptionDialog() },
            onConfirm = { label, code, parentCode ->
                viewModel.addConfigItem(
                    category = state.targetCategoryForAdd,
                    code = code,
                    label = label,
                    parentCategory = if (state.targetCategoryForAdd == "SOUS_FAMILY") "FAMILY" else "",
                    parentCode = parentCode
                )
            }
        )
    }

    if (state.isDeleteOptionDialogOpen && state.optionPendingDelete != null) {
        val item = state.optionPendingDelete!!
        AlertDialog(
            onDismissRequest = { viewModel.closeDeleteOptionDialog() },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer l'option « ${item.label} » (${item.code}) ?") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteConfigItem(item.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.closeDeleteOptionDialog() }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (state.isEditProfileDialogOpen) {
        EditProfileDialog(
            currentProfile = state.userProfile,
            onDismiss = { viewModel.closeEditProfileDialog() },
            onConfirm = { fullName, phone, email, role ->
                viewModel.saveUserProfile(fullName, phone, email, role)
            }
        )
    }

    if (state.isRestoreConfirmationDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestore() },
            title = { Text("Confirmer la restauration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Êtes-vous sûr de vouloir restaurer cette sauvegarde ?")
                    state.pendingRestoreFilePath?.let {
                        Text("Fichier : $it", style = MaterialTheme.typography.bodySmall)
                    }
                    state.pendingRestoreValidationDetails?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "Attention : Les données actuelles seront remplacées par celles de la sauvegarde.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.cancelRestore() }) {
                    Text("Annuler")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// Tab 1: Dropdown Options (Units, Families, Subfamilies)
// ----------------------------------------------------------------------------

@Composable
private fun ConfigDropdownsTab(
    viewModel: DynamicConfigViewModel,
    state: DynamicConfigUiState
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Units
        ConfigColumnCard(
            title = "Unités de Conditionnement",
            count = state.units.size,
            items = state.units,
            onAddClick = { viewModel.openAddOptionDialog("UNIT") },
            onDeleteClick = { viewModel.openDeleteOptionDialog(it) },
            modifier = Modifier.weight(1f)
        )

        // Families
        ConfigColumnCard(
            title = "Familles d'Articles",
            count = state.families.size,
            items = state.families,
            onAddClick = { viewModel.openAddOptionDialog("FAMILY") },
            onDeleteClick = { viewModel.openDeleteOptionDialog(it) },
            modifier = Modifier.weight(1f)
        )

        // Sub-families
        ConfigColumnCard(
            title = "Sous-Familles d'Articles",
            count = state.sousFamilies.size,
            items = state.sousFamilies,
            showParent = true,
            onAddClick = { viewModel.openAddOptionDialog("SOUS_FAMILY") },
            onDeleteClick = { viewModel.openDeleteOptionDialog(it) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ConfigColumnCard(
    title: String,
    count: Int,
    items: List<DynamicConfigItem>,
    showParent: Boolean = false,
    onAddClick: () -> Unit,
    onDeleteClick: (DynamicConfigItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
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
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "$count option(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.colors.textSecondary
                    )
                }

                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = AppTheme.colors.textPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Code: ${item.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                    if (showParent && item.parentCode.isNotBlank()) {
                                        Text(
                                            text = "• Parent: ${item.parentCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDeleteClick(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 2: Alert Thresholds
// ----------------------------------------------------------------------------

@Composable
private fun ConfigThresholdsTab(
    viewModel: DynamicConfigViewModel,
    state: DynamicConfigUiState
) {
    var thresholdInput by remember(state.defaultMinQuantity) { mutableStateOf(state.defaultMinQuantity.toString()) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceWhite,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AppTheme.colors.statusUrgent,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Seuils Globaux d'Alerte de Stock",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Définissez la quantité minimale globale à partir de laquelle un article sans seuil spécifique passe automatiquement en état d'alerte « Stock Faible » (orange) dans le catalogue et les rapports.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text("Seuil Minimal Global (Unités)") },
                    singleLine = true,
                    modifier = Modifier.width(280.dp),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        val parsed = thresholdInput.toDoubleOrNull() ?: 5.0
                        viewModel.saveGlobalMinQuantity(parsed)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enregistrer le seuil")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 3: Document Prefixes & Auto-Numbering
// ----------------------------------------------------------------------------

@Composable
private fun ConfigPrefixesTab(
    viewModel: DynamicConfigViewModel,
    state: DynamicConfigUiState
) {
    var cmd by remember(state.prefixCommande) { mutableStateOf(state.prefixCommande) }
    var bl by remember(state.prefixLivraison) { mutableStateOf(state.prefixLivraison) }
    var bs by remember(state.prefixSortie) { mutableStateOf(state.prefixSortie) }
    var dch by remember(state.prefixDecharge) { mutableStateOf(state.prefixDecharge) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceWhite,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Formats de Numérotation & Préfixes des Documents",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AppTheme.colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personnalisez les préfixes utilisés lors de la génération automatique des références de bons de commande, réceptions, sorties et papiers de décharge.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = cmd,
                        onValueChange = { cmd = it },
                        label = { Text("Préfixe Bons de Commande") },
                        supportingText = { Text("Exemple généré : ${cmd}001") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = bl,
                        onValueChange = { bl = it },
                        label = { Text("Préfixe Bons de Livraison") },
                        supportingText = { Text("Exemple généré : ${bl}001") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = bs,
                        onValueChange = { bs = it },
                        label = { Text("Préfixe Bons de Sortie Interne") },
                        supportingText = { Text("Exemple généré : ${bs}001") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = dch,
                        onValueChange = { dch = it },
                        label = { Text("Préfixe Papiers de Décharge (Dons)") },
                        supportingText = { Text("Exemple généré : ${dch}001") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.savePrefixes(cmd, bl, bs, dch) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enregistrer les préfixes de numérotation")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Tab 4: User Profile
// ----------------------------------------------------------------------------

@Composable
private fun ConfigProfileTab(
    viewModel: DynamicConfigViewModel,
    state: DynamicConfigUiState
) {
    val profile = state.userProfile

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AppTheme.colors.surfaceWhite,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column {
                        Text(
                            text = profile.fullName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = profile.role,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Button(
                    onClick = { viewModel.openEditProfileDialog() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modifier le profil")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileDetailRow(label = "Téléphone Professionnel", value = profile.phone.ifBlank { "Non renseigné" })
                ProfileDetailRow(label = "Courrier Électronique", value = profile.email.ifBlank { "Non renseigné" })
                ProfileDetailRow(label = "Rôle / Responsabilité", value = profile.role)
                ProfileDetailRow(label = "Mode d'affichage initial", value = if (profile.themeMode == "DARK") "Mode Sombre" else "Mode Clair")
                ProfileDetailRow(label = "Langue de démarrage", value = if (profile.locale == "EN") "English" else "Français")
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = AppTheme.colors.textPrimary)
    }
}

// ----------------------------------------------------------------------------
// Dialogs
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddConfigOptionDialog(
    category: String,
    families: List<DynamicConfigItem>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedParentCode by remember { mutableStateOf(families.firstOrNull()?.code ?: "") }

    val categoryLabel = when (category) {
        "UNIT" -> "Unité de Conditionnement"
        "FAMILY" -> "Famille d'Articles"
        "SOUS_FAMILY" -> "Sous-Famille d'Articles"
        else -> "Option"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter une $categoryLabel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        if (code.isBlank() || code == label.dropLast(1).uppercase().replace(" ", "_")) {
                            code = it.uppercase().replace(" ", "_")
                        }
                    },
                    label = { Text("Libellé / Nom *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text("Code Identifiant (Optionnel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (category == "SOUS_FAMILY") {
                    ParentFamilySelector(
                        families = families,
                        selectedParentCode = selectedParentCode,
                        onSelected = { selectedParentCode = it }
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(label, code, selectedParentCode) },
                // A sous-family cannot exist without a parent, so there is nothing to add until one exists.
                enabled = label.isNotBlank() && (category != "SOUS_FAMILY" || selectedParentCode.isNotBlank())
            ) {
                Text("Ajouter")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Offers every active family as a parent. A dropdown rather than chips, so the list is not capped by
 * the dialog width — a hospital-wide taxonomy easily exceeds the four that used to fit.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentFamilySelector(
    families: List<DynamicConfigItem>,
    selectedParentCode: String,
    onSelected: (String) -> Unit
) {
    if (families.isEmpty()) {
        Text(
            text = "Aucune famille configurée : créez d'abord une famille avant d'y ajouter une sous-famille.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = families.firstOrNull { it.code == selectedParentCode }?.label ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Famille parente associée *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            families.forEach { family ->
                DropdownMenuItem(
                    text = { Text(family.label) },
                    onClick = {
                        onSelected(family.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    currentProfile: org.abdat.inventory.domain.model.UserProfile,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var fullName by remember { mutableStateOf(currentProfile.fullName) }
    var phone by remember { mutableStateOf(currentProfile.phone) }
    var email by remember { mutableStateOf(currentProfile.email) }
    var role by remember { mutableStateOf(currentProfile.role) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier le Profil") },
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

                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Fonction / Titre *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone Professionnel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Adresse Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(fullName, phone, email, role) },
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

@Composable
private fun ConfigBackupTab(
    viewModel: DynamicConfigViewModel,
    state: DynamicConfigUiState
) {
    val strings = AppTheme.strings

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Banner / Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = strings.backupTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = strings.backupSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Last Backup
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = strings.lastBackup,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = state.lastBackupDate ?: strings.never,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (state.lastBackupDate != null) AppTheme.colors.statusSuccess else AppTheme.colors.textSecondary
                            )
                            state.lastBackupPath?.let { path ->
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Last Restore
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = strings.lastRestore,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = state.lastRestoreDate ?: strings.never,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Action Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Export Backup
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = strings.exportBackup,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }

                    Text(
                        text = strings.exportBackupDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )

                    Surface(
                        color = AppTheme.colors.backgroundSlate.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Contenu de la sauvegarde complète :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text("• Stock, articles, seuils d'alerte et valorisation", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                            Text("• Fournisseurs, conventions-cadres et quotas", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                            Text("• Bons de commande, réceptions et sorties", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                            Text("• Dons, décharges, départements et personnel", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                            Text("• Préfixes, listes déroulantes et profil utilisateur", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { viewModel.exportBackup() },
                        enabled = !state.isBackupOperationInProgress,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporter vers un fichier...")
                    }
                }
            }

            // Card 2: Restore Backup
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = strings.restoreBackup,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }

                    Text(
                        text = strings.restoreBackupDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "La restauration écrase les données actuelles avec celles de la sauvegarde. Une copie locale 'inventory.db.pre_restore_bak' est automatiquement préservée par sécurité.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { viewModel.requestImportBackup() },
                        enabled = !state.isBackupOperationInProgress,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choisir un fichier à restaurer...")
                    }
                }
            }
        }
    }
}

