package org.abdat.inventory.ui.consumption

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
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.StockArticle

class ConsumptionViewModel(
    private val database: AppDatabase,
    private val stockRepository: StockRepository,
    private val categoryRepository: CategoryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsumptionUiState())
    val uiState: StateFlow<ConsumptionUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(ConsumptionFilterState())

    init {
        // Observe category taxonomy
        viewModelScope.launch {
            categoryRepository.taxonomy.collect { taxonomy ->
                onTaxonomyChanged(taxonomy)
            }
        }

        // Observe stock snapshot updates (for prices and taxonomy info)
        viewModelScope.launch {
            stockRepository.snapshots.collect {
                refreshConsumptionData()
            }
        }

        // Observe database mutations touching sorties, services, locals, persons
        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllSorties().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllServices().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllLocals().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllPersons().asFlow().mapToList(dispatcher)
            ) { _, _, _, _ -> Unit }
                .collect {
                    refreshConsumptionData()
                }
        }

        // Reactively filter items when filter state changes
        viewModelScope.launch {
            _filterState.collect { filter ->
                applyFilters(filter)
            }
        }

        // Initial synchronous load to prime first frame
        val initialTaxonomy = categoryRepository.currentTaxonomy()
        onTaxonomyChanged(initialTaxonomy)
        refreshConsumptionData()
    }

    private fun onTaxonomyChanged(taxonomy: CategoryTaxonomy) {
        val familyLabels = taxonomy.families.map { it.label }.distinct().sorted()
        _uiState.update { current ->
            val activeFamily = current.filterState.selectedFamily
            val availableSousFamilies = if (!activeFamily.isNullOrBlank()) {
                taxonomy.findFamilyByLabel(activeFamily)?.let { fam ->
                    taxonomy.sousFamiliesOf(fam.code).map { it.label }.distinct().sorted()
                } ?: emptyList()
            } else {
                taxonomy.allSousFamilies.filter { it.isActive }.map { it.label }.distinct().sorted()
            }

            current.copy(
                families = familyLabels.toImmutableList(),
                availableSousFamilies = availableSousFamilies.toImmutableList()
            )
        }
    }

    private fun refreshConsumptionData() {
        try {
            val dbSorties = database.appDatabaseQueries.selectAllSorties().executeAsList()
            val dbServices = database.appDatabaseQueries.selectAllServices().executeAsList().map { it.toDomain() }
            val dbLocals = database.appDatabaseQueries.selectAllLocals().executeAsList().map { it.toDomain() }
            val dbArticles = stockRepository.currentSnapshot().articles
            val serviceMap = dbServices.associateBy { it.id }
            val localMap = dbLocals.associateBy { it.id }
            val articleMap = dbArticles.associateBy { it.codeArticle.trim().uppercase() }

            val itemsList = mutableListOf<ConsumptionItem>()

            for (sortie in dbSorties) {
                val sortieItems = database.appDatabaseQueries.selectSortieItemsBySortieId(sortie.id).executeAsList()
                val service = serviceMap[sortie.service_id]
                val serviceName = service?.name ?: "Service Inconnu"
                val local = sortie.local_id?.let { localMap[it] }
                val localName = local?.name ?: "Non spécifié"

                for (item in sortieItems) {
                    val matchedArticle = articleMap[item.code_article.trim().uppercase()]
                    val unitPrice = matchedArticle?.unitPrice ?: 0.0
                    val family = matchedArticle?.family.orEmpty()
                    val sousFamily = matchedArticle?.sousFamily.orEmpty()

                    itemsList.add(
                        ConsumptionItem(
                            id = item.id,
                            sortieId = sortie.id,
                            referenceBon = sortie.reference_bon,
                            date = sortie.date,
                            serviceId = sortie.service_id,
                            serviceName = serviceName,
                            localId = sortie.local_id,
                            localName = localName,
                            codeArticle = item.code_article,
                            designation = item.designation.ifBlank { matchedArticle?.designation ?: item.code_article },
                            family = family,
                            sousFamily = sousFamily,
                            quantity = item.quantity,
                            unitPrice = unitPrice,
                            totalValue = item.quantity * unitPrice,
                            demandeur = sortie.demandeur,
                            recepteurInterne = sortie.recepteur_interne,
                            motif = sortie.motif,
                            chefService = sortie.chef_service
                        )
                    )
                }
            }

            // Default order: recent issue date descending, then id descending
            itemsList.sortWith(compareByDescending<ConsumptionItem> { it.date }.thenByDescending { it.id })

            val immutableItems = itemsList.toImmutableList()

            _uiState.update { current ->
                current.copy(
                    isLoading = false,
                    allItems = immutableItems,
                    services = dbServices.sortedBy { it.name }.toImmutableList()
                )
            }

            applyFilters(_filterState.value)
        } catch (_: Exception) {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateDateFrom(date: String) {
        _filterState.update { it.copy(dateFrom = date.trim()) }
    }

    fun updateDateTo(date: String) {
        _filterState.update { it.copy(dateTo = date.trim()) }
    }

    fun selectService(serviceId: Long?) {
        _filterState.update { it.copy(selectedServiceId = serviceId) }
    }

    fun selectFamily(familyLabel: String?) {
        val normalized = familyLabel?.takeIf { it.isNotBlank() }
        _filterState.update { current ->
            // Reset sous-family if family changes
            current.copy(
                selectedFamily = normalized,
                selectedSousFamily = null
            )
        }

        // Update available sous-families for this family
        val taxonomy = categoryRepository.currentTaxonomy()
        val availableSousFamilies = if (!normalized.isNullOrBlank()) {
            taxonomy.findFamilyByLabel(normalized)?.let { fam ->
                taxonomy.sousFamiliesOf(fam.code).map { it.label }.distinct().sorted()
            } ?: emptyList()
        } else {
            taxonomy.allSousFamilies.filter { it.isActive }.map { it.label }.distinct().sorted()
        }

        _uiState.update { current ->
            current.copy(availableSousFamilies = availableSousFamilies.toImmutableList())
        }
    }

    fun selectSousFamily(sousFamilyLabel: String?) {
        _filterState.update { it.copy(selectedSousFamily = sousFamilyLabel?.takeIf { it.isNotBlank() }) }
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun resetFilters() {
        _filterState.value = ConsumptionFilterState()
        val taxonomy = categoryRepository.currentTaxonomy()
        onTaxonomyChanged(taxonomy)
    }

    private fun applyFilters(filter: ConsumptionFilterState) {
        val allItems = _uiState.value.allItems
        if (allItems.isEmpty()) {
            _uiState.update {
                it.copy(
                    filterState = filter,
                    filteredItems = persistentListOf(),
                    totalLinesCount = 0,
                    totalQuantitySum = 0.0,
                    totalFinancialValueDzd = 0.0
                )
            }
            return
        }

        val filtered = allItems.filter { item ->
            // Date From filter: compare YYYY-MM-DD portion
            if (filter.dateFrom.isNotBlank()) {
                val itemDateNorm = normalizeDateForComparison(item.date)
                val filterFromNorm = normalizeDateForComparison(filter.dateFrom)
                if (itemDateNorm < filterFromNorm) return@filter false
            }

            // Date To filter: compare YYYY-MM-DD portion
            if (filter.dateTo.isNotBlank()) {
                val itemDateNorm = normalizeDateForComparison(item.date)
                val filterToNorm = normalizeDateForComparison(filter.dateTo)
                if (itemDateNorm > filterToNorm) return@filter false
            }

            // Service filter
            if (filter.selectedServiceId != null && item.serviceId != filter.selectedServiceId) {
                return@filter false
            }

            // Family filter
            if (!filter.selectedFamily.isNullOrBlank()) {
                if (!item.family.equals(filter.selectedFamily, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Sous-Family filter
            if (!filter.selectedSousFamily.isNullOrBlank()) {
                if (!item.sousFamily.equals(filter.selectedSousFamily, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Search query filter
            if (filter.searchQuery.isNotBlank()) {
                val q = filter.searchQuery.trim().lowercase()
                val matches = item.codeArticle.lowercase().contains(q) ||
                        item.designation.lowercase().contains(q) ||
                        item.referenceBon.lowercase().contains(q) ||
                        item.serviceName.lowercase().contains(q) ||
                        item.demandeur.lowercase().contains(q) ||
                        item.recepteurInterne.lowercase().contains(q)
                if (!matches) return@filter false
            }

            true
        }

        val totalLines = filtered.size
        val totalQuantity = filtered.sumOf { it.quantity }
        val totalValue = filtered.sumOf { it.totalValue }

        _uiState.update { current ->
            current.copy(
                filterState = filter,
                filteredItems = filtered.toImmutableList(),
                totalLinesCount = totalLines,
                totalQuantitySum = totalQuantity,
                totalFinancialValueDzd = totalValue
            )
        }
    }

    private fun normalizeDateForComparison(raw: String): String {
        val trimmed = raw.trim()
        // If DD/MM/YYYY or DD-MM-YYYY
        if (trimmed.length >= 10 && (trimmed[2] == '/' || trimmed[2] == '-')) {
            val parts = trimmed.take(10).split('/', '-')
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0')
                val month = parts[1].padStart(2, '0')
                val year = parts[2]
                return "$year-$month-$day"
            }
        }
        // If YYYY-MM-DD ...
        return trimmed.take(10)
    }

    fun openDetail(item: ConsumptionItem) {
        viewModelScope.launch(dispatcher) {
            try {
                val sortie = database.appDatabaseQueries.selectSortieById(item.sortieId).executeAsOneOrNull()
                val dbItems = database.appDatabaseQueries.selectSortieItemsBySortieId(item.sortieId).executeAsList()
                val dbArticles = stockRepository.currentSnapshot().articles.associateBy { it.codeArticle.trim().uppercase() }
                val dbPersons = database.appDatabaseQueries.selectAllPersons().executeAsList().map { it.toDomain() }.associateBy { it.id }

                val benefNames = if (sortie != null && sortie.beneficiaire_ids.isNotBlank()) {
                    val ids = sortie.beneficiaire_ids.split(",").mapNotNull { it.trim().toLongOrNull() }
                    ids.mapNotNull { dbPersons[it]?.fullName }.joinToString(", ")
                } else ""

                val voucherItems = dbItems.map { vItem ->
                    val article = dbArticles[vItem.code_article.trim().uppercase()]
                    val price = article?.unitPrice ?: 0.0
                    ConsumptionVoucherItem(
                        id = vItem.id,
                        codeArticle = vItem.code_article,
                        designation = vItem.designation.ifBlank { article?.designation ?: vItem.code_article },
                        family = article?.family.orEmpty(),
                        sousFamily = article?.sousFamily.orEmpty(),
                        quantity = vItem.quantity,
                        unitPrice = price,
                        totalCost = vItem.quantity * price
                    )
                }.toImmutableList()

                val detailRecord = ConsumptionDetailRecord(
                    sortieId = item.sortieId,
                    referenceBon = item.referenceBon,
                    date = item.date,
                    serviceId = item.serviceId,
                    serviceName = item.serviceName,
                    localName = item.localName,
                    demandeur = item.demandeur,
                    recepteurInterne = item.recepteurInterne,
                    motif = item.motif,
                    chefService = item.chefService,
                    beneficiaireNames = benefNames,
                    items = voucherItems,
                    totalVoucherCost = voucherItems.sumOf { it.totalCost },
                    totalVoucherQuantity = voucherItems.sumOf { it.quantity }
                )

                _uiState.update { it.copy(selectedDetailRecord = detailRecord) }
            } catch (_: Exception) {
                // Ignore detail load errors
            }
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedDetailRecord = null) }
    }

    fun clearExportMessages() {
        _uiState.update { it.copy(exportSuccessMessage = null, exportErrorMessage = null) }
    }

    fun setExportSuccess(path: String) {
        _uiState.update { it.copy(exportSuccessMessage = path, exportErrorMessage = null) }
    }

    fun setExportError(error: String) {
        _uiState.update { it.copy(exportErrorMessage = error, exportSuccessMessage = null) }
    }
}
