package org.abdat.inventory.ui.suppliers

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.MetricCard
import org.abdat.inventory.ui.components.formatDzd
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.PdfExporter
import org.abdat.inventory.util.showSaveFileDialog

@Composable
fun SupplierProfileScreen(
    supplier: Fournisseur,
    conventions: List<Convention>,
    historyItems: List<SupplierHistoryItem>,
    isEditDialogOpen: Boolean,
    onBack: () -> Unit,
    onOpenEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSaveProfile: (
        fullName: String,
        societe: String,
        phone: String,
        responsablePhone: String,
        ville: String,
        email: String,
        otherLinks: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val totalCeilingDzd = conventions.sumOf { it.totalMontant }
    val coroutineScope = rememberCoroutineScope()
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var exportErrorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(scrollState)
    ) {
        // Navigation & Top Action Bar
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text("← Retour", style = MaterialTheme.typography.labelMedium)
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Fiche Fournisseur & Profil",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = AppTheme.colors.textPrimary
                            )
                            AppBadge(
                                text = "FRN-${supplier.id.toString().padStart(4, '0')}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Gestion des coordonnées, contacts responsables et synthèses administratives.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                exportErrorMessage = null
                                val path = showSaveFileDialog(
                                    title = "Imprimer Dossier Fournisseur (PDF)",
                                    defaultFileName = "dossier_fournisseur_${supplier.id}.pdf",
                                    filters = listOf(FileTypeFilter("PDF Files", "pdf"))
                                )
                                if (path != null) {
                                    try {
                                        PdfExporter.generateSupplierDossierPdf(
                                            filePath = path,
                                            supplier = supplier,
                                            conventions = conventions,
                                            balancesMap = emptyMap()
                                        )
                                        exportSuccessMessage = "Le dossier fournisseur a été exporté en PDF avec succès.\n\nChemin : $path"
                                    } catch (e: Exception) {
                                        exportErrorMessage = "Erreur lors de l'export PDF : ${e.message ?: "Erreur inconnue"}"
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Imprimer Dossier (PDF)", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onOpenEdit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifier Profil", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Supplier Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Profile Avatar / Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (supplier.societe.isNotBlank()) {
                            supplier.societe.take(2).uppercase()
                        } else {
                            supplier.fullName.take(2).uppercase()
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = supplier.fullName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppTheme.colors.textPrimary
                        )

                        AppBadge(
                            text = "FOURNISSEUR AGRÉÉ",
                            containerColor = AppTheme.colors.statusSuccessContainer,
                            contentColor = AppTheme.colors.onStatusSuccess
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (supplier.societe.isNotBlank()) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                        )
                        Text(
                            text = if (supplier.societe.isNotBlank()) {
                                "Société: ${supplier.societe}"
                            } else {
                                "Société non renseignée"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (supplier.societe.isNotBlank()) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "Localisation: ${supplier.ville.ifBlank { "Non renseignée" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "Réf: FRN-${supplier.id.toString().padStart(4, '0')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Coordonnées & Contacts Section Header
        Text(
            text = "COORDONNÉES & CONTACTS DIRECTS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = AppTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Contact Cards Grid (2x2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContactInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Phone,
                title = "Téléphone Société / Fixe",
                value = supplier.phone.ifBlank { "Non renseigné" },
                subtitle = "Ligne principale ou standard commercial",
                isConfigured = supplier.phone.isNotBlank()
            )

            ContactInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Person,
                title = "Responsable Commercial",
                value = supplier.responsablePhone.ifBlank { "Non renseigné" },
                subtitle = "Ligne directe mobile du contact attitré",
                isConfigured = supplier.responsablePhone.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ContactInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Email,
                title = "Courrier Électronique (Email)",
                value = supplier.email.ifBlank { "Non renseigné" },
                subtitle = "Pour envois des bons de commande & factures",
                isConfigured = supplier.email.isNotBlank()
            )

            ContactInfoCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Chat,
                title = "Canaux Rapides (WhatsApp / Telegram)",
                value = supplier.otherLinks.ifBlank { "Aucun lien enregistré" },
                subtitle = "Messagerie instantanée & liens utiles",
                isConfigured = supplier.otherLinks.isNotBlank()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Procurement & Engagement Summary Section
        Text(
            text = "SYNTHÈSE DES ENGAGEMENTS & ACTIVITÉ",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = AppTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MetricCard(
                title = "Conventions Cadres",
                value = "${conventions.size}",
                subtitle = "Actives & Archivées",
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Plafond Contractuel",
                value = formatDzd(totalCeilingDzd),
                subtitle = "Cumul global",
                icon = Icons.Default.Payments,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Bons & Livraisons",
                value = "${historyItems.size}",
                subtitle = "Opérations BC / BL",
                icon = Icons.Default.Inventory2,
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Ville / Antenne",
                value = supplier.ville.ifBlank { "N/A" },
                subtitle = "Bassin logistique",
                icon = Icons.Default.Place,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Additional information note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Synchronisation des coordonnées fournisseur",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "Les modifications apportées au profil (téléphone, responsable, email, liens) sont instantanément reflétées sur l'ensemble des modules d'inventaire, de passation des commandes et de réception des livraisons.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }
        }
    }

    // Edit Profile Dialog
    if (isEditDialogOpen) {
        EditSupplierProfileDialog(
            supplier = supplier,
            onDismiss = onCloseEdit,
            onConfirm = onSaveProfile
        )
    }

    // Export Success Notification Dialog
    exportSuccessMessage?.let { msg ->
        Dialog(onDismissRequest = { exportSuccessMessage = null }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AppTheme.colors.surfaceWhite,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.statusSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Export Réussi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Button(
                        onClick = { exportSuccessMessage = null },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }

    // Export Error Notification Dialog
    exportErrorMessage?.let { err ->
        AlertDialog(
            onDismissRequest = { exportErrorMessage = null },
            title = { Text("Erreur d'export", fontWeight = FontWeight.Bold) },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = { exportErrorMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun ContactInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    isConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConfigured) AppTheme.colors.surfaceWhite else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            1.dp,
            if (isConfigured) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isConfigured) 1.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
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
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textSecondary
                    )
                }

                if (!isConfigured) {
                    AppBadge(
                        text = "NON RENSEIGNÉ",
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isConfigured) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isConfigured) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun EditSupplierProfileDialog(
    supplier: Fournisseur,
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        societe: String,
        phone: String,
        responsablePhone: String,
        ville: String,
        email: String,
        otherLinks: String
    ) -> Unit
) {
    var fullName by remember { mutableStateOf(supplier.fullName) }
    var societe by remember { mutableStateOf(supplier.societe) }
    var phone by remember { mutableStateOf(supplier.phone) }
    var responsablePhone by remember { mutableStateOf(supplier.responsablePhone) }
    var ville by remember { mutableStateOf(supplier.ville) }
    var email by remember { mutableStateOf(supplier.email) }
    var otherLinks by remember { mutableStateOf(supplier.otherLinks) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Modifier le Profil Fournisseur",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "FRN-${supplier.id.toString().padStart(4, '0')} • ${supplier.fullName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = AppTheme.colors.statusUrgentContainer
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = AppTheme.colors.onStatusUrgent,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Nom ou Raison Sociale (Principal) *") },
                    placeholder = { Text("ex: SARL BIOPHARM ALGERIE") },
                    singleLine = true,
                    isError = errorMessage != null && fullName.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = societe,
                    onValueChange = {
                        societe = it
                        if (it.isNotBlank()) errorMessage = null
                    },
                    label = { Text("Société / Raison Sociale Légale *") },
                    placeholder = { Text("ex: BIOPHARM SPA") },
                    singleLine = true,
                    isError = errorMessage != null && societe.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Téléphone Société") },
                        placeholder = { Text("021 98 00 00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = responsablePhone,
                        onValueChange = { responsablePhone = it },
                        label = { Text("Tél. Responsable Commercial") },
                        placeholder = { Text("0550 12 34 56") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ville,
                        onValueChange = { ville = it },
                        label = { Text("Ville / Lieu") },
                        placeholder = { Text("Alger, Constantine...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Professionnel") },
                        placeholder = { Text("contact@societe.dz") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = otherLinks,
                    onValueChange = { otherLinks = it },
                    label = { Text("Autres Liens (WhatsApp, Telegram, etc.)") },
                    placeholder = { Text("https://wa.me/213550123456 ou @contact_telegram") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "Le nom principal du fournisseur est obligatoire."
                    } else if (societe.isBlank()) {
                        errorMessage = "La raison sociale / société est obligatoire."
                    } else {
                        onConfirm(
                            fullName.trim(),
                            societe.trim(),
                            phone.trim(),
                            responsablePhone.trim(),
                            ville.trim(),
                            email.trim(),
                            otherLinks.trim()
                        )
                    }
                }
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
