package org.abdat.inventory.ui.sorties

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.data.repository.StockSnapshot
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.Local
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.StockArticle

data class SortieDraftLine(
    val articleId: Long,
    val codeArticle: String,
    val designation: String,
    val family: String,
    val currentStock: Double,
    val minQuantity: Double = 10.0,
    val issueQuantity: Double = 1.0,
    val unitPrice: Double = 0.0
) {
    val remainingStock: Double get() = currentStock - issueQuantity
    val isExceeded: Boolean get() = issueQuantity > currentStock
    val isBelowMin: Boolean get() = !isExceeded && remainingStock <= minQuantity
    val isSufficient: Boolean get() = !isExceeded && remainingStock > minQuantity
    val totalCost: Double get() = issueQuantity * unitPrice
}

data class SortieDisplayItem(
    val id: Long,
    val referenceBon: String,
    val paperStubReference: String,
    val serviceName: String,
    val localName: String,
    val recipient: String,
    val demandeur: String = "",
    val recepteurInterne: String = "",
    val chefDeServiceName: String = "",
    val beneficiaireNames: String = "",
    val motif: String = "",
    val date: String,
    val itemsCount: Int,
    val totalAmountDzd: Double,
    val statusLabel: String = "VALIDÉ & DÉSTOCKÉ"
)

enum class SortieMotive(val label: String) {
    CONSOMMATION_COURANTE("Consommation Interne Courante"),
    URGENCE_INTERVENTION("Urgence / Intervention"),
    MAINTENANCE_PREVENTIVE("Maintenance Préventive"),
    DOTATION_INITIALE("Dotation Initiale"),
    REFORME_REBUT("Réforme / Rebut")
}

@Stable
data class SortiesUiState(
    val sorties: ImmutableList<SortieDisplayItem> = persistentListOf(),
    val filteredSorties: ImmutableList<SortieDisplayItem> = persistentListOf(),
    val searchQuery: String = "",

    // KPI Metrics
    val todaySortiesCount: Int = 0,
    val monthlyDistributedDzd: Double = 0.0,
    val activeServicesCount: Int = 0,
    val postIssueAlertsCount: Int = 0,

    // Form fields
    val systemReference: String = "",
    val paperStubReference: String = "",
    val issueDate: String = "",
    val demandeur: String = "",
    val recepteurInterne: String = "",
    val recipientName: String = "",
    val motive: String = "",
    val selectedMotive: SortieMotive = SortieMotive.CONSOMMATION_COURANTE,

    // Organization Selectors
    val services: ImmutableList<Service> = persistentListOf(),
    val selectedService: Service? = null,
    val localsForSelectedService: ImmutableList<Local> = persistentListOf(),
    val selectedLocal: Local? = null,

    // Beneficiary Staff & Head of Service (Chef de Service)
    val chefDeServiceName: String = "",
    val availableServiceStaff: ImmutableList<Person> = persistentListOf(),
    val selectedBeneficiaryIds: ImmutableList<Long> = persistentListOf(),

    // Available Inventory articles for adding to draft
    val availableArticles: ImmutableList<StockArticle> = persistentListOf(),

    // Draft Lines for the current voucher (clean empty list by default)
    val draftLines: ImmutableList<SortieDraftLine> = persistentListOf(),

    // Financial & Total computations
    val totalUnitsIssued: Double = 0.0,
    val totalValuationDzd: Double = 0.0,

    val isLoading: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null
) {
    val motif: String get() = motive
    val hasExceededLines: Boolean get() = draftLines.any { it.isExceeded || it.issueQuantity <= 0.0 }
    val canSubmit: Boolean get() = draftLines.isNotEmpty() && !hasExceededLines && selectedService != null
}

class SortiesViewModel(
    private val database: AppDatabase,
    private val stockRepository: StockRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SortiesUiState())
    val uiState: StateFlow<SortiesUiState> = _uiState.asStateFlow()

    init {
        onStockSnapshot(stockRepository.currentSnapshot())
        loadSortiesSync()

        viewModelScope.launch {
            stockRepository.snapshots.collect { onStockSnapshot(it) }
        }

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllSorties().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllServices().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllLocals().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllPersons().asFlow().mapToList(dispatcher)
            ) { _, _, _, _ -> Unit }
                .collect {
                    loadSortiesSync()
                }
        }
    }

    /**
     * Folds stock into the fields that are purely derived from it, leaving the in-progress voucher
     * draft, the selected service/local, and the typed form fields untouched.
     */
    private fun onStockSnapshot(snapshot: StockSnapshot) {
        val alerts = snapshot.countAtOrBelow(POST_ISSUE_ALERT_THRESHOLD)
        _uiState.update { current ->
            current.copy(
                availableArticles = snapshot.articles,
                postIssueAlertsCount = alerts
            )
        }
    }

    private fun currentFormattedDateTime(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val hh = now.hour.toString().padStart(2, '0')
        val min = now.minute.toString().padStart(2, '0')
        return "${now.date} $hh:$min"
    }

    fun loadSorties() {
        viewModelScope.launch {
            loadSortiesSync()
        }
    }

    fun loadSortiesSync() {
        val dbSorties = database.appDatabaseQueries.selectAllSorties().executeAsList()
        val dbServices = database.appDatabaseQueries.selectAllServices().executeAsList().map { it.toDomain() }
        val dbLocals = database.appDatabaseQueries.selectAllLocals().executeAsList().map { it.toDomain() }
        val dbArticles = stockRepository.currentSnapshot().articles
        val dbPersons = database.appDatabaseQueries.selectAllPersons().executeAsList().map { it.toDomain() }

        val serviceMap = dbServices.associateBy { it.id }
        val localMap = dbLocals.associateBy { it.id }
        val articleMap = dbArticles.associateBy { it.codeArticle }
        val personMap = dbPersons.associateBy { it.id }

        val displayItems = dbSorties.map { sortie ->
            val items = database.appDatabaseQueries.selectSortieItemsBySortieId(sortie.id).executeAsList()
            val totalAmount = items.sumOf { item ->
                val art = articleMap[item.code_article]
                item.quantity * (art?.unitPrice ?: 0.0)
            }
            val service = serviceMap[sortie.service_id]
            val serviceName = service?.name ?: "Service Inconnu"
            val local = sortie.local_id?.let { localMap[it] }
            val localName = local?.name ?: "Local non spécifié"

            val resolvedChef = if (!sortie.chef_service.isNullOrBlank()) {
                sortie.chef_service
            } else {
                service?.chefName?.takeIf { it.isNotBlank() }
                    ?: dbPersons.firstOrNull { it.serviceId == sortie.service_id && it.isChef }?.fullName
                    ?: dbPersons.firstOrNull { it.serviceId == sortie.service_id && it.notes.contains("Chef", ignoreCase = true) }?.fullName
                    ?: "Responsable de service"
            }

            val benefIds = sortie.beneficiaire_ids.split(",").mapNotNull { it.trim().toLongOrNull() }
            val benefNames = if (benefIds.isNotEmpty()) {
                benefIds.mapNotNull { personMap[it]?.fullName }.joinToString(", ")
            } else {
                if (!sortie.recepteur_interne.isNullOrBlank() && !sortie.recepteur_interne.startsWith("Ensemble")) {
                    sortie.recepteur_interne
                } else {
                    "Ensemble du service"
                }
            }

            val demandeur = sortie.demandeur
            val recepteur = sortie.recepteur_interne
            val recipientDisplay = when {
                demandeur.isNotBlank() && recepteur.isNotBlank() -> "$demandeur / $recepteur"
                demandeur.isNotBlank() -> demandeur
                recepteur.isNotBlank() -> recepteur
                else -> benefNames
            }

            SortieDisplayItem(
                id = sortie.id,
                referenceBon = sortie.reference_bon,
                paperStubReference = "BSP-${sortie.id.toString().padStart(3, '0')}",
                serviceName = serviceName,
                localName = localName,
                recipient = recipientDisplay,
                demandeur = demandeur,
                recepteurInterne = recepteur,
                chefDeServiceName = resolvedChef,
                beneficiaireNames = benefNames,
                motif = sortie.motif,
                date = sortie.date,
                itemsCount = items.size,
                totalAmountDzd = totalAmount,
                statusLabel = "VALIDÉ & DÉSTOCKÉ"
            )
        }

        val totalDistributed = displayItems.sumOf { it.totalAmountDzd }
        val activeServices = displayItems.map { it.serviceName }.distinct().size

        val currentSelectedService = _uiState.value.selectedService ?: dbServices.firstOrNull()
        val localsForService = if (currentSelectedService != null) {
            dbLocals.filter { it.serviceId == currentSelectedService.id }
        } else emptyList()

        val staffForService = if (currentSelectedService != null) {
            dbPersons.filter { it.serviceId == currentSelectedService.id }
        } else emptyList()

        val chefNameForSelected = currentSelectedService?.let { srv ->
            srv.chefName?.takeIf { it.isNotBlank() }
                ?: staffForService.firstOrNull { it.isChef }?.fullName
                ?: staffForService.firstOrNull { it.notes.contains("Chef", ignoreCase = true) }?.fullName
                ?: ""
        } ?: ""

        val prefix = database.appDatabaseQueries.getMetadata("prefix_sortie").executeAsOneOrNull() ?: "BS-2026-"
        val nextId = (dbSorties.maxOfOrNull { it.id } ?: 0L) + 1L
        val generatedSystemRef = "$prefix${nextId.toString().padStart(3, '0')}"

        val initialDraftLines = _uiState.value.draftLines
        val totalUnits = initialDraftLines.sumOf { it.issueQuantity }
        val totalVal = initialDraftLines.sumOf { it.totalCost }

        val nowDateTime = currentFormattedDateTime()

        _uiState.update { current ->
            current.copy(
                sorties = displayItems.toImmutableList(),
                services = dbServices.toImmutableList(),
                selectedService = currentSelectedService,
                localsForSelectedService = localsForService.toImmutableList(),
                selectedLocal = current.selectedLocal?.takeIf { it.serviceId == currentSelectedService?.id } ?: localsForService.firstOrNull(),
                availableServiceStaff = staffForService.toImmutableList(),
                chefDeServiceName = current.chefDeServiceName.ifBlank { chefNameForSelected },
                demandeur = current.demandeur.ifBlank { chefNameForSelected },
                draftLines = initialDraftLines.toImmutableList(),
                totalUnitsIssued = totalUnits,
                totalValuationDzd = totalVal,
                issueDate = current.issueDate.ifBlank { nowDateTime },
                systemReference = current.systemReference.ifBlank { generatedSystemRef },
                todaySortiesCount = displayItems.size,
                monthlyDistributedDzd = totalDistributed,
                activeServicesCount = activeServices,
                isLoading = false
            )
        }

        applyFilters()
    }

    companion object {
        /** Sorties flags post-issue alerts at a wider threshold than the inventory low-stock rule. */
        private const val POST_ISSUE_ALERT_THRESHOLD = 15.0
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.sorties.filter { item ->
            query.isEmpty() ||
                    item.referenceBon.lowercase().contains(query) ||
                    item.paperStubReference.lowercase().contains(query) ||
                    item.serviceName.lowercase().contains(query) ||
                    item.localName.lowercase().contains(query) ||
                    item.recipient.lowercase().contains(query) ||
                    item.demandeur.lowercase().contains(query) ||
                    item.recepteurInterne.lowercase().contains(query) ||
                    item.chefDeServiceName.lowercase().contains(query) ||
                    item.beneficiaireNames.lowercase().contains(query) ||
                    item.motif.lowercase().contains(query)
        }

        _uiState.update { it.copy(filteredSorties = filtered.toImmutableList()) }
    }

    fun selectService(serviceId: Long) {
        val selected = _uiState.value.services.firstOrNull { it.id == serviceId } ?: return
        val dbLocals = database.appDatabaseQueries.selectLocalsByServiceId(serviceId).executeAsList().map { it.toDomain() }
        val dbStaff = database.appDatabaseQueries.selectPersonsByServiceId(serviceId).executeAsList().map { it.toDomain() }
        val chefName = selected.chefName?.takeIf { it.isNotBlank() }
            ?: dbStaff.firstOrNull { it.isChef }?.fullName
            ?: dbStaff.firstOrNull { it.notes.contains("Chef", ignoreCase = true) }?.fullName
            ?: ""

        _uiState.update { current ->
            current.copy(
                selectedService = selected,
                localsForSelectedService = dbLocals.toImmutableList(),
                selectedLocal = dbLocals.firstOrNull(),
                availableServiceStaff = dbStaff.toImmutableList(),
                selectedBeneficiaryIds = persistentListOf(), // Reset on service change
                chefDeServiceName = chefName,
                demandeur = if (current.demandeur.isBlank() || current.demandeur == current.chefDeServiceName) chefName else current.demandeur,
                recepteurInterne = "",
                recipientName = if (chefName.isNotBlank()) "Chef: $chefName" else ""
            )
        }
    }

    fun toggleBeneficiary(personId: Long) {
        _uiState.update { current ->
            val updated = if (current.selectedBeneficiaryIds.contains(personId)) {
                current.selectedBeneficiaryIds.filterNot { it == personId }
            } else {
                current.selectedBeneficiaryIds + personId
            }
            val staffMap = current.availableServiceStaff.associateBy { it.id }
            val summaryText = if (updated.isEmpty()) {
                ""
            } else {
                updated.mapNotNull { staffMap[it]?.fullName }.joinToString(", ")
            }
            current.copy(
                selectedBeneficiaryIds = updated.toImmutableList(),
                recepteurInterne = summaryText
            )
        }
    }

    fun clearBeneficiaries() {
        _uiState.update { current ->
            current.copy(
                selectedBeneficiaryIds = persistentListOf(),
                recepteurInterne = ""
            )
        }
    }

    fun selectLocal(localId: Long?) {
        val local = _uiState.value.localsForSelectedService.firstOrNull { it.id == localId }
        _uiState.update { it.copy(selectedLocal = local) }
    }

    fun updateMotive(motive: String) {
        _uiState.update { it.copy(motive = motive) }
    }

    fun updateMotive(motive: SortieMotive) {
        _uiState.update { it.copy(selectedMotive = motive, motive = motive.label) }
    }

    fun updateDemandeur(demandeur: String) {
        _uiState.update { it.copy(demandeur = demandeur) }
    }

    fun updateRecepteurInterne(recepteur: String) {
        _uiState.update { it.copy(recepteurInterne = recepteur) }
    }

    fun updatePaperStubReference(ref: String) {
        _uiState.update { it.copy(paperStubReference = ref) }
    }

    fun updateRecipientName(name: String) {
        _uiState.update { it.copy(recipientName = name) }
    }

    fun updateIssueDate(date: String) {
        _uiState.update { it.copy(issueDate = date) }
    }

    fun addDraftLine(article: StockArticle, initialQuantity: Double = 1.0) {
        _uiState.update { current ->
            val existing = current.draftLines.firstOrNull { it.articleId == article.id }
            val updatedLines = if (existing != null) {
                current.draftLines.map { line ->
                    if (line.articleId == article.id) {
                        val newQty = (line.issueQuantity + initialQuantity).coerceAtMost(line.currentStock)
                        line.copy(issueQuantity = newQty)
                    } else line
                }
            } else {
                val newLine = SortieDraftLine(
                    articleId = article.id,
                    codeArticle = article.codeArticle,
                    designation = article.designation,
                    family = article.family,
                    currentStock = article.quantity,
                    minQuantity = 20.0,
                    issueQuantity = initialQuantity.coerceAtMost(article.quantity),
                    unitPrice = article.unitPrice
                )
                current.draftLines + newLine
            }
            val totalUnits = updatedLines.sumOf { it.issueQuantity }
            val totalVal = updatedLines.sumOf { it.totalCost }

            current.copy(
                draftLines = updatedLines.toImmutableList(),
                totalUnitsIssued = totalUnits,
                totalValuationDzd = totalVal
            )
        }
    }

    fun removeDraftLine(articleId: Long) {
        _uiState.update { current ->
            val updatedLines = current.draftLines.filterNot { it.articleId == articleId }
            val totalUnits = updatedLines.sumOf { it.issueQuantity }
            val totalVal = updatedLines.sumOf { it.totalCost }

            current.copy(
                draftLines = updatedLines.toImmutableList(),
                totalUnitsIssued = totalUnits,
                totalValuationDzd = totalVal
            )
        }
    }

    fun updateIssueQuantity(articleId: Long, quantity: Double) {
        val safeQty = quantity.coerceAtLeast(0.0)
        _uiState.update { current ->
            val updatedLines = current.draftLines.map { line ->
                if (line.articleId == articleId) {
                    line.copy(issueQuantity = safeQty)
                } else line
            }
            val totalUnits = updatedLines.sumOf { it.issueQuantity }
            val totalVal = updatedLines.sumOf { it.totalCost }

            current.copy(
                draftLines = updatedLines.toImmutableList(),
                totalUnitsIssued = totalUnits,
                totalValuationDzd = totalVal
            )
        }
    }

    fun clearNotifications() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }

    fun validateSortie(
        referenceBon: String,
        serviceId: Long,
        localId: Long?,
        demandeur: String,
        recepteurInterne: String,
        motif: String,
        date: String
    ) {
        viewModelScope.launch {
            validateSortieSync(referenceBon, serviceId, localId, demandeur, recepteurInterne, motif, date)
        }
    }

    fun validateSortie(
        referenceBon: String,
        serviceId: Long,
        localId: Long?,
        recipient: String,
        date: String
    ) {
        val state = _uiState.value
        val dem = state.demandeur.ifBlank { recipient }
        val rec = state.recepteurInterne.ifBlank { recipient }
        validateSortie(referenceBon, serviceId, localId, dem, rec, state.motive, date)
    }

    fun validateSortieSync(
        referenceBon: String,
        serviceId: Long,
        localId: Long?,
        recipient: String,
        date: String
    ): Boolean {
        val state = _uiState.value
        val dem = state.demandeur.ifBlank { recipient }
        val rec = state.recepteurInterne.ifBlank { recipient }
        return validateSortieSync(referenceBon, serviceId, localId, dem, rec, state.motive, date)
    }

    fun validateSortieSync(
        referenceBon: String,
        serviceId: Long,
        localId: Long?,
        demandeur: String,
        recepteurInterne: String,
        motif: String,
        date: String
    ): Boolean {
        val state = _uiState.value

        if (referenceBon.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le numéro de bon de sortie est obligatoire.") }
            return false
        }

        if (state.draftLines.isEmpty()) {
            _uiState.update { it.copy(actionErrorMessage = "Aucun article sélectionné pour la sortie.") }
            return false
        }

        if (state.hasExceededLines) {
            _uiState.update { it.copy(actionErrorMessage = "Certains articles dépassent le stock physique disponible ou ont une quantité nulle.") }
            return false
        }

        val issueDateStr = date.ifBlank { currentFormattedDateTime() }
        val benefIdsStr = state.selectedBeneficiaryIds.joinToString(",")
        val staffMap = state.availableServiceStaff.associateBy { it.id }
        val computedRecepteur = if (state.selectedBeneficiaryIds.isNotEmpty()) {
            state.selectedBeneficiaryIds.mapNotNull { staffMap[it]?.fullName }.joinToString(", ")
        } else {
            "Ensemble du service"
        }
        val finalRecepteur = recepteurInterne.ifBlank { computedRecepteur }
        val finalChef = state.chefDeServiceName.ifBlank {
            state.selectedService?.chefName ?: "Chef de Service"
        }

        try {
            database.transaction {
                // 1. Insert bon_de_sortie
                database.appDatabaseQueries.insertSortie(
                    reference_bon = referenceBon.trim(),
                    service_id = serviceId,
                    local_id = localId,
                    date = issueDateStr,
                    demandeur = demandeur.trim().ifBlank { finalChef },
                    recepteur_interne = finalRecepteur.trim(),
                    motif = motif.trim(),
                    chef_service = finalChef.trim(),
                    beneficiaire_ids = benefIdsStr
                )
                val sortieId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

                // 2. Insert sortie_items & decrement stock
                state.draftLines.forEach { line ->
                    database.appDatabaseQueries.insertSortieItem(
                        sortie_id = sortieId,
                        code_article = line.codeArticle,
                        designation = line.designation,
                        quantity = line.issueQuantity
                    )

                    // Decrement stock_article.quantity
                    database.appDatabaseQueries.decreaseStockQuantity(
                        quantity = line.issueQuantity,
                        date = issueDateStr,
                        code_article = line.codeArticle
                    )
                }
            }

            val prefix = database.appDatabaseQueries.getMetadata("prefix_sortie").executeAsOneOrNull() ?: "BS-2026-"
            val nextId = (database.appDatabaseQueries.selectAllSorties().executeAsList().maxOfOrNull { it.id } ?: 0L) + 1L
            val nextRef = "$prefix${nextId.toString().padStart(3, '0')}"

            _uiState.update {
                it.copy(
                    actionSuccessMessage = "Bon de Sortie $referenceBon validé avec succès. Stock décrémenté.",
                    actionErrorMessage = null,
                    systemReference = nextRef,
                    paperStubReference = "",
                    issueDate = currentFormattedDateTime(),
                    demandeur = finalChef,
                    recepteurInterne = "",
                    recipientName = "",
                    motive = "",
                    selectedBeneficiaryIds = persistentListOf(),
                    draftLines = persistentListOf(),
                    totalUnitsIssued = 0.0,
                    totalValuationDzd = 0.0
                )
            }

            loadSortiesSync()
            return true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(actionErrorMessage = "Erreur lors de la validation du bon de sortie: ${e.message}")
            }
            return false
        }
    }
}
