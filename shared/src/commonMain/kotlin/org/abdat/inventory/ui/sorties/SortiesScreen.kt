package org.abdat.inventory.ui.sorties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.theme.AppTheme
import org.abdat.inventory.ui.components.AppBadge
import org.abdat.inventory.ui.components.AppSearchBar
import org.abdat.inventory.ui.components.MetricCard

@Composable
fun SortiesScreen(viewModel: SortiesViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var isAddArticleDialogOpen by remember { mutableStateOf(false) }
    var selectedSortieForInspection by remember { mutableStateOf<SortieDisplayItem?>(null) }
    var showCsvNotification by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundSlate)
            .verticalScroll(scrollState)
    ) {
        // 1. En-tête contextuel & Fil d'Ariane
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
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "OPÉRATIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Text(
                                text = "DÉSTOCKAGE & CONSOMMATION INTERNE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Sorties Matériel (Bons de Sortie)",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = AppTheme.colors.textPrimary
                        )

                        Text(
                            text = "Gestion des bordereaux d'imputation, distribution par service/local et décrémentation automatique des stocks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCsvNotification = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textPrimary),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exporter Registre CSV",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Button(
                        onClick = { isAddArticleDialogOpen = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nouveau Bon de Sortie",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }
        }

        // Notifications Banner
        state.actionSuccessMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(AppTheme.colors.statusSuccess.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.colors.statusSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppTheme.colors.statusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearNotifications() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        state.actionErrorMessage?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .background(AppTheme.colors.statusUrgent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .border(1.dp, AppTheme.colors.statusUrgent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppTheme.colors.statusUrgent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = AppTheme.colors.statusUrgent
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearNotifications() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = AppTheme.colors.statusUrgent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 2. Indicateurs Clés Supérieurs (KPI Cards)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MetricCard(
                    title = "Sorties du Jour",
                    value = "${state.todaySortiesCount} Bons",
                    subtitle = "Validés aujourd'hui",
                    icon = Icons.Default.CheckCircle,
                    accentColor = AppTheme.colors.statusSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Valeur Distribuée",
                    value = "${state.monthlyDistributedDzd.toInt()} DZD",
                    subtitle = "Imputation ce mois",
                    icon = Icons.Default.Payments,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Services Demandeurs",
                    value = "${state.activeServicesCount} Actifs",
                    subtitle = "Bloc, Bioméd., Chirurgie",
                    icon = Icons.Default.Business,
                    accentColor = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Alertes Post-Sortie",
                    value = "${state.postIssueAlertsCount} Articles",
                    subtitle = "Seuil min atteint",
                    icon = Icons.Default.Warning,
                    accentColor = AppTheme.colors.statusWarning,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. SECTION PRINCIPALE : Formulaire d'Enregistrement de Bon de Sortie (BS)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Header du formulaire
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
                                    .size(32.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "Bordereau de Sortie Interne & Imputation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Renseignez les métadonnées de décharge et les lignes d'articles à déstocker.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        }

                        AppBadge(
                            text = "DÉSTOCKAGE IMMÉDIAT",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Row 1: Métadonnées Système & Papier
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // N° Bon Système
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "N° BON SYSTÈME",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = state.systemReference,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Réf Souche Papier
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RÉF SOUCHE PAPIER *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CustomInputField(
                                value = state.paperStubReference,
                                onValueChange = { viewModel.updatePaperStubReference(it) },
                                placeholder = "Ex: BSP-4402"
                            )
                        }

                        // Date & Heure
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DATE & HEURE DE SORTIE *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CustomInputField(
                                value = state.issueDate,
                                onValueChange = { viewModel.updateIssueDate(it) },
                                placeholder = "YYYY-MM-DD HH:mm",
                                leadingIcon = Icons.Default.CalendarToday
                            )
                        }

                        // Demandeur / Récepteur
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "DEMANDEUR / RÉCEPTEUR INTERNE *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CustomInputField(
                                value = state.recipientName,
                                onValueChange = { viewModel.updateRecipientName(it) },
                                placeholder = "Nom & Prénom / Fonction",
                                leadingIcon = Icons.Default.Person
                            )
                        }
                    }

                    // Row 2: Imputation Analytique (Service, Local, Motif)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Service Bénéficiaire Dropdown
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "SERVICE BÉNÉFICIAIRE *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ServiceDropdownSelector(
                                services = state.services,
                                selectedService = state.selectedService,
                                onServiceSelected = { viewModel.selectService(it.id) }
                            )
                        }

                        // Local / Salle Dropdown
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = "LOCAL / SALLE DESTINATION",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LocalDropdownSelector(
                                locals = state.localsForSelectedService,
                                selectedLocal = state.selectedLocal,
                                onLocalSelected = { viewModel.selectLocal(it?.id) }
                            )
                        }

                        // Motif d'Imputation Dropdown
                        Column(modifier = Modifier.weight(1.4f)) {
                            Text(
                                text = "MOTIF / TYPE D'IMPUTATION *",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            MotiveDropdownSelector(
                                selectedMotive = state.selectedMotive,
                                onMotiveSelected = { viewModel.updateMotive(it) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 4. Tableau Dynamique de Déstockage des Articles
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Lignes d'Articles à Déstocker",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Vérifiez les disponibilités physiques en temps réel avant validation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }

                            Button(
                                onClick = { isAddArticleDialogOpen = true },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Ajouter un Article",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }

                        if (state.draftLines.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Aucun article dans ce bon. Cliquez sur '+ Ajouter un Article' pour sélectionner des produits.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        } else {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ARTICLE & CODE",
                                    modifier = Modifier.weight(2.5f),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "STOCK DISPO",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "QUANTITÉ SORTIE",
                                    modifier = Modifier.weight(1.3f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "NOUVEAU STOCK",
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Text(
                                    text = "STATUT",
                                    modifier = Modifier.weight(1.8f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textSecondary
                                )
                                Box(modifier = Modifier.width(40.dp))
                            }

                            // Table Rows
                            state.draftLines.forEach { line ->
                                SortieLineRow(
                                    line = line,
                                    onQuantityChange = { viewModel.updateIssueQuantity(line.articleId, it) },
                                    onDelete = { viewModel.removeDraftLine(line.articleId) }
                                )
                            }
                        }

                        // Summary Box & Action Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "ARTICLES SÉLECTIONNÉS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.textSecondary
                                    )
                                    Text(
                                        text = "${state.draftLines.size} références",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.textPrimary
                                    )
                                }

                                Column {
                                    Text(
                                        text = "TOTAL UNITÉS DÉSTOCKÉES",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.textSecondary
                                    )
                                    Text(
                                        text = "${state.totalUnitsIssued.toInt()} unités",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column {
                                    Text(
                                        text = "VALORISATION ESTIMÉE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.textSecondary
                                    )
                                    Text(
                                        text = "${state.totalValuationDzd.toInt()} DZD",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = AppTheme.colors.statusSuccess
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.loadSortiesSync() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.textSecondary)
                                ) {
                                    Text("Réinitialiser")
                                }

                                Button(
                                    onClick = {
                                        state.selectedService?.let { srv ->
                                            viewModel.validateSortie(
                                                referenceBon = state.systemReference,
                                                serviceId = srv.id,
                                                localId = state.selectedLocal?.id,
                                                recipient = state.recipientName,
                                                date = state.issueDate
                                            )
                                        }
                                    },
                                    enabled = state.canSubmit,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Valider la Sortie & Décrémenter le Stock",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. SECTION INFÉRIEURE : Registre des Bons de Sortie Récents & Audit Trail
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surfaceWhite),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Registre des Bons de Sortie & Historique",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = AppTheme.colors.textPrimary
                            )
                            Text(
                                text = "Traçabilité intégrale des mouvements de consommation interne par service.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                        }

                        AppSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = "Rechercher un bon, service, souche papier...",
                            modifier = Modifier.width(320.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // History Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "N° BON & SOUCHE",
                            modifier = Modifier.weight(1.8f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "DATE & HEURE",
                            modifier = Modifier.weight(1.3f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "SERVICE & LOCAL",
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "DEMANDEUR / RÉCEPTEUR",
                            modifier = Modifier.weight(1.8f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "LIGNES",
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "VALORISATION",
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "STATUT",
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textSecondary
                        )
                        Box(modifier = Modifier.width(44.dp))
                    }

                    if (state.filteredSorties.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun bon de sortie enregistré.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary
                            )
                        }
                    } else {
                        state.filteredSorties.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(
                                        text = item.referenceBon,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = item.paperStubReference,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = item.date,
                                    modifier = Modifier.weight(1.3f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary
                                )

                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = item.serviceName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = item.localName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }

                                Text(
                                    text = item.recipient,
                                    modifier = Modifier.weight(1.8f),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AppTheme.colors.textPrimary
                                )

                                Text(
                                    text = "${item.itemsCount}",
                                    modifier = Modifier.weight(0.8f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )

                                Text(
                                    text = "${item.totalAmountDzd.toInt()} DZD",
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AppTheme.colors.textPrimary
                                )

                                Box(
                                    modifier = Modifier.weight(1.5f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppBadge(
                                        text = item.statusLabel,
                                        containerColor = AppTheme.colors.statusSuccess.copy(alpha = 0.15f),
                                        contentColor = AppTheme.colors.statusSuccess
                                    )
                                }

                                IconButton(
                                    onClick = { selectedSortieForInspection = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = "Visualiser",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }
                }
            }
        }
    }

    // Modal : Ajouter un Article
    if (isAddArticleDialogOpen) {
        AddArticleDialog(
            articles = state.availableArticles,
            onDismiss = { isAddArticleDialogOpen = false },
            onArticleSelected = { article ->
                viewModel.addDraftLine(article, 1.0)
                isAddArticleDialogOpen = false
            }
        )
    }

    // Modal : Détail / Inspection Bon de Sortie
    selectedSortieForInspection?.let { item ->
        SortieDetailDialog(
            item = item,
            onDismiss = { selectedSortieForInspection = null }
        )
    }

    // Notification CSV Dialog
    if (showCsvNotification) {
        Dialog(onDismissRequest = { showCsvNotification = false }) {
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
                            text = "Export Registre CSV",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = "Le fichier 'registre_sorties_materiel.csv' a été préparé avec succès (${state.sorties.size} enregistrements).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary
                    )
                    Button(
                        onClick = { showCsvNotification = false },
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
private fun SortieLineRow(
    line: SortieDraftLine,
    onQuantityChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(6.dp))
            .border(
                1.dp,
                if (line.isExceeded) AppTheme.colors.statusUrgent.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Article Info
        Column(modifier = Modifier.weight(2.5f)) {
            Text(
                text = line.designation,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppTheme.colors.textPrimary
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = line.codeArticle,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text("•", style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textSecondary)
                Text(
                    text = line.family,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }

        // Current stock
        Text(
            text = "${line.currentStock.toInt()}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = AppTheme.colors.textPrimary
        )

        // Quantity input
        Row(
            modifier = Modifier.weight(1.3f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onQuantityChange((line.issueQuantity - 1.0).coerceAtLeast(0.0)) },
                contentAlignment = Alignment.Center
            ) {
                Text("-", fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "${line.issueQuantity.toInt()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (line.isExceeded) AppTheme.colors.statusUrgent else AppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onQuantityChange(line.issueQuantity + 1.0) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontWeight = FontWeight.Bold, color = AppTheme.colors.textPrimary)
            }
        }

        // Forecasted remaining stock
        Text(
            text = "${line.remainingStock.toInt()}",
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = when {
                line.isExceeded -> AppTheme.colors.statusUrgent
                line.isBelowMin -> AppTheme.colors.statusWarning
                else -> AppTheme.colors.statusSuccess
            }
        )

        // Status Badge
        Box(
            modifier = Modifier.weight(1.8f),
            contentAlignment = Alignment.Center
        ) {
            when {
                line.isExceeded -> {
                    AppBadge(
                        text = "RUPTURE / EXCÈS",
                        containerColor = AppTheme.colors.statusUrgent.copy(alpha = 0.15f),
                        contentColor = AppTheme.colors.statusUrgent
                    )
                }
                line.isBelowMin -> {
                    AppBadge(
                        text = "SEUIL MIN ATTEINT",
                        containerColor = AppTheme.colors.statusWarning.copy(alpha = 0.15f),
                        contentColor = AppTheme.colors.statusWarning
                    )
                }
                else -> {
                    AppBadge(
                        text = "STOCK SUFFISANT",
                        containerColor = AppTheme.colors.statusSuccess.copy(alpha = 0.15f),
                        contentColor = AppTheme.colors.statusSuccess
                    )
                }
            }
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = AppTheme.colors.statusUrgent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
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
                    color = AppTheme.colors.textPrimary
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ServiceDropdownSelector(
    services: List<org.abdat.inventory.domain.model.Service>,
    selectedService: org.abdat.inventory.domain.model.Service?,
    onServiceSelected: (org.abdat.inventory.domain.model.Service) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedService?.name ?: "Sélectionner un service...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (selectedService != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            services.forEach { srv ->
                DropdownMenuItem(
                    text = { Text(srv.name) },
                    onClick = {
                        onServiceSelected(srv)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LocalDropdownSelector(
    locals: List<org.abdat.inventory.domain.model.Local>,
    selectedLocal: org.abdat.inventory.domain.model.Local?,
    onLocalSelected: (org.abdat.inventory.domain.model.Local?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedLocal?.name ?: (if (locals.isEmpty()) "Aucun local défini" else "Sélectionner un local..."),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (selectedLocal != null) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            locals.forEach { loc ->
                DropdownMenuItem(
                    text = { Text(loc.name) },
                    onClick = {
                        onLocalSelected(loc)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MotiveDropdownSelector(
    selectedMotive: SortieMotive,
    onMotiveSelected: (SortieMotive) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(AppTheme.colors.surfaceWhite, RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMotive.label,
                style = MaterialTheme.typography.bodyMedium.copy(color = AppTheme.colors.textPrimary)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortieMotive.entries.forEach { motive ->
                DropdownMenuItem(
                    text = { Text(motive.label) },
                    onClick = {
                        onMotiveSelected(motive)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddArticleDialog(
    articles: List<StockArticle>,
    onDismiss: () -> Unit,
    onArticleSelected: (StockArticle) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = articles.filter {
        search.isEmpty() ||
                it.designation.contains(search, ignoreCase = true) ||
                it.codeArticle.contains(search, ignoreCase = true) ||
                it.family.contains(search, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sélectionner un Article du Stock",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colors.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                AppSearchBar(
                    query = search,
                    onQueryChange = { search = it },
                    placeholder = "Rechercher par désignation ou code article..."
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { art ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(8.dp))
                                .clickable { onArticleSelected(art) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = art.designation,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = AppTheme.colors.textPrimary
                                )
                                Text(
                                    text = "${art.codeArticle} • ${art.family} • ${art.unitPrice.toInt()} DZD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppTheme.colors.textSecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AppBadge(
                                    text = "Dispo : ${art.quantity.toInt()} ${art.uniteQuantite}",
                                    containerColor = if (art.quantity > 0) AppTheme.colors.statusSuccess.copy(alpha = 0.15f) else AppTheme.colors.statusUrgent.copy(alpha = 0.15f),
                                    contentColor = if (art.quantity > 0) AppTheme.colors.statusSuccess else AppTheme.colors.statusUrgent
                                )

                                Button(
                                    onClick = { onArticleSelected(art) },
                                    enabled = art.quantity > 0,
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Ajouter", style = MaterialTheme.typography.labelSmall)
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
private fun SortieDetailDialog(
    item: SortieDisplayItem,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(550.dp),
            shape = RoundedCornerShape(12.dp),
            color = AppTheme.colors.surfaceWhite,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Détails du Bon de Sortie",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "${item.referenceBon} (${item.paperStubReference})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Date & Heure :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(item.date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Service Bénéficiaire :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(item.serviceName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Local / Destination :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(item.localName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Demandeur / Récepteur :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text(item.recipient, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Valorisation Globale :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    Text("${item.totalAmountDzd.toInt()} DZD", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AppTheme.colors.statusSuccess)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Statut Imputation :", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textSecondary)
                    AppBadge(
                        text = item.statusLabel,
                        containerColor = AppTheme.colors.statusSuccess.copy(alpha = 0.15f),
                        contentColor = AppTheme.colors.statusSuccess
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Fermer")
                }
            }
        }
    }
}
