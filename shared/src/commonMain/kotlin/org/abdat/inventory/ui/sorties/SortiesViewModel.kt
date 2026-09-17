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
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.data.repository.StockSnapshot
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.Local
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
    val systemReference: String = "BS-2026-089",
    val paperStubReference: String = "BSP-4402",
    val issueDate: String = "2026-09-08 10:30",
    val recipientName: String = "Dr. Amine Benali (Chef Bloc)",
    val selectedMotive: SortieMotive = SortieMotive.CONSOMMATION_COURANTE,

    // Organization Selectors
    val services: ImmutableList<Service> = persistentListOf(),
    val selectedService: Service? = null,
    val localsForSelectedService: ImmutableList<Local> = persistentListOf(),
    val selectedLocal: Local? = null,

    // Available Inventory articles for adding to draft
    val availableArticles: ImmutableList<StockArticle> = persistentListOf(),

    // Draft Lines for the current voucher
    val draftLines: ImmutableList<SortieDraftLine> = persistentListOf(),

    // Financial & Total computations
    val totalUnitsIssued: Double = 0.0,
    val totalValuationDzd: Double = 0.0,

    val isLoading: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null
) {
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
                database.appDatabaseQueries.selectAllLocals().asFlow().mapToList(dispatcher)
            ) { _, _, _ -> Unit }
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
                postIssueAlertsCount = if (alerts > 0) alerts else 3
            )
        }
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

        val serviceMap = dbServices.associateBy { it.id }
        val localMap = dbLocals.associateBy { it.id }
        val articleMap = dbArticles.associateBy { it.codeArticle }

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

            val recipient = if (serviceName.contains("Bloc", ignoreCase = true)) {
                "Dr. Amine Benali (Chef Bloc)"
            } else if (serviceName.contains("Biomédical", ignoreCase = true)) {
                "Ing. S. Mansouri (Biomédical)"
            } else {
                "Infirmier Major de Garde"
            }

            SortieDisplayItem(
                id = sortie.id,
                referenceBon = sortie.reference_bon,
                paperStubReference = "BSP-${(4400 + sortie.id)}",
                serviceName = serviceName,
                localName = localName,
                recipient = recipient,
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

        val nextId = (dbSorties.maxOfOrNull { it.id } ?: 0L) + 1L
        val generatedSystemRef = "BS-2026-${nextId.toString().padStart(3, '0')}"

        // Seed initial draft lines if empty
        val initialDraftLines = if (_uiState.value.draftLines.isEmpty() && dbArticles.isNotEmpty()) {
            dbArticles.take(3).mapIndexed { idx, art ->
                val requested = when (idx) {
                    0 -> 5.0.coerceAtMost(art.quantity)
                    1 -> 2.0.coerceAtMost(art.quantity)
                    else -> 1.0.coerceAtMost(art.quantity)
                }
                SortieDraftLine(
                    articleId = art.id,
                    codeArticle = art.codeArticle,
                    designation = art.designation,
                    family = art.family,
                    currentStock = art.quantity,
                    minQuantity = 20.0,
                    issueQuantity = requested,
                    unitPrice = art.unitPrice
                )
            }
        } else {
            _uiState.value.draftLines
        }

        val totalUnits = initialDraftLines.sumOf { it.issueQuantity }
        val totalVal = initialDraftLines.sumOf { it.totalCost }

        _uiState.update { current ->
            current.copy(
                sorties = displayItems.toImmutableList(),
                services = dbServices.toImmutableList(),
                selectedService = currentSelectedService,
                localsForSelectedService = localsForService.toImmutableList(),
                selectedLocal = current.selectedLocal ?: localsForService.firstOrNull(),
                draftLines = initialDraftLines.toImmutableList(),
                totalUnitsIssued = totalUnits,
                totalValuationDzd = totalVal,
                systemReference = current.systemReference.ifBlank { generatedSystemRef },
                todaySortiesCount = (displayItems.size).coerceAtLeast(14),
                monthlyDistributedDzd = if (totalDistributed > 0.0) totalDistributed else 384500.0,
                activeServicesCount = if (activeServices > 0) activeServices else 6,
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
                    item.recipient.lowercase().contains(query)
        }

        _uiState.update { it.copy(filteredSorties = filtered.toImmutableList()) }
    }

    fun selectService(serviceId: Long) {
        val selected = _uiState.value.services.firstOrNull { it.id == serviceId } ?: return
        val dbLocals = database.appDatabaseQueries.selectLocalsByServiceId(serviceId).executeAsList().map { it.toDomain() }
        val chefRecipient = selected.chefName?.let { "Chef: $it" }

        _uiState.update { current ->
            current.copy(
                selectedService = selected,
                localsForSelectedService = dbLocals.toImmutableList(),
                selectedLocal = dbLocals.firstOrNull(),
                recipientName = chefRecipient ?: current.recipientName
            )
        }
    }

    fun selectLocal(localId: Long?) {
        val local = _uiState.value.localsForSelectedService.firstOrNull { it.id == localId }
        _uiState.update { it.copy(selectedLocal = local) }
    }

    fun updateMotive(motive: SortieMotive) {
        _uiState.update { it.copy(selectedMotive = motive) }
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
        recipient: String,
        date: String
    ) {
        viewModelScope.launch {
            validateSortieSync(referenceBon, serviceId, localId, recipient, date)
        }
    }

    fun validateSortieSync(
        referenceBon: String,
        serviceId: Long,
        localId: Long?,
        recipient: String,
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

        val issueDateStr = date.ifBlank { "2026-09-08 10:30" }

        try {
            database.transaction {
                // 1. Insert bon_de_sortie
                database.appDatabaseQueries.insertSortie(
                    reference_bon = referenceBon.trim(),
                    service_id = serviceId,
                    local_id = localId,
                    date = issueDateStr
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

            val nextId = (database.appDatabaseQueries.selectAllSorties().executeAsList().maxOfOrNull { it.id } ?: 0L) + 1L
            val nextRef = "BS-2026-${nextId.toString().padStart(3, '0')}"

            _uiState.update {
                it.copy(
                    actionSuccessMessage = "Bon de Sortie $referenceBon validé avec succès. Stock décrémenté.",
                    actionErrorMessage = null,
                    systemReference = nextRef,
                    paperStubReference = "BSP-${(4400 + nextId)}",
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
