package org.abdat.inventory.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.ConventionItem
import org.abdat.inventory.domain.model.Fournisseur

enum class SupplierDetailTab(val label: String) {
    CONVENTIONS("Conventions"),
    ARTICLES("Articles Référencés"),
    HISTORIQUE("Historique BL & BC"),
    EVALUATIONS("Évaluations & SLA")
}

data class ConventionDraftArticle(
    val designation: String,
    val unite: String = "UNITE",
    val quantity: Double,
    val minQuantity: Double = 5.0
)

data class ConventionQuotaBalance(
    val conventionItemId: Long,
    val conventionId: Long,
    val designation: String,
    val unite: String,
    val agreedQuantity: Double,
    val minQuantity: Double,
    val orderedQuantity: Double,
    val remainingQuantity: Double,
    val unitPrice: Double = 0.0
)

data class SupplierHistoryItem(
    val reference: String,
    val date: String,
    val type: String,
    val amountDzd: Double,
    val status: String
)

data class SuppliersUiState(
    val suppliers: List<Fournisseur> = emptyList(),
    val filteredSuppliers: List<Fournisseur> = emptyList(),
    val selectedSupplier: Fournisseur? = null,
    val searchQuery: String = "",
    val activeTab: SupplierDetailTab = SupplierDetailTab.CONVENTIONS,
    val conventions: List<Convention> = emptyList(),
    val conventionBalances: Map<Long, List<ConventionQuotaBalance>> = emptyMap(),
    val historyItems: List<SupplierHistoryItem> = emptyList(),
    val isAddSupplierDialogOpen: Boolean = false,
    val isAddConventionDialogOpen: Boolean = false,
    val isLoading: Boolean = false
)

class SuppliersViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    init {
        loadSuppliersSync()
    }

    fun loadSuppliers() {
        viewModelScope.launch {
            loadSuppliersSync()
        }
    }

    fun loadSuppliersSync() {
        val dbSuppliers = database.appDatabaseQueries.selectAllFournisseurs().executeAsList()
        val domainSuppliers = dbSuppliers.map { it.toDomain() }

        val currentSelected = _uiState.value.selectedSupplier
        val selected = domainSuppliers.firstOrNull { it.id == currentSelected?.id }
            ?: domainSuppliers.firstOrNull()

        _uiState.update {
            it.copy(
                suppliers = domainSuppliers,
                selectedSupplier = selected,
                isLoading = false
            )
        }
        applyFilter()

        if (selected != null) {
            loadSupplierDetailsSync(selected.id)
        }
    }

    fun selectSupplier(supplier: Fournisseur) {
        _uiState.update { it.copy(selectedSupplier = supplier) }
        loadSupplierDetailsSync(supplier.id)
    }

    fun setTab(tab: SupplierDetailTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter()
    }

    private fun applyFilter() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val filtered = if (query.isEmpty()) {
            _uiState.value.suppliers
        } else {
            _uiState.value.suppliers.filter { it.fullName.lowercase().contains(query) }
        }
        _uiState.update { it.copy(filteredSuppliers = filtered) }
    }

    fun loadSupplierDetails(supplierId: Long) {
        viewModelScope.launch {
            loadSupplierDetailsSync(supplierId)
        }
    }

    fun loadSupplierDetailsSync(supplierId: Long) {
        val dbConventions = database.appDatabaseQueries.selectConventionsByFournisseur(supplierId).executeAsList()
        val domainConventions = dbConventions.map { conv ->
            val items = database.appDatabaseQueries.selectConventionItemsByConventionId(conv.id)
                .executeAsList()
                .map { it.toDomain() }
            conv.toDomain(items)
        }

        val stockPriceMap = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .associate { it.designation.lowercase().trim() to it.unit_price }

        // Quota balances for each convention
        val balancesMap = mutableMapOf<Long, List<ConventionQuotaBalance>>()
        for (conv in domainConventions) {
            val balances = database.appDatabaseQueries.selectConventionBalancesByConventionId(conv.id)
                .executeAsList()
                .map {
                    val price = stockPriceMap[it.designation.lowercase().trim()] ?: 150.0
                    ConventionQuotaBalance(
                        conventionItemId = it.conventionItemId,
                        conventionId = it.conventionId,
                        designation = it.designation,
                        unite = it.unite,
                        agreedQuantity = it.agreedQuantity,
                        minQuantity = it.minQuantity,
                        orderedQuantity = it.orderedQuantity,
                        remainingQuantity = it.remainingQuantity,
                        unitPrice = price
                    )
                }
            balancesMap[conv.id] = balances
        }

        // History from BCs and BLs
        val bcs = database.appDatabaseQueries.selectCommandesByFournisseur(supplierId).executeAsList()
        val bls = database.appDatabaseQueries.selectLivraisonsByFournisseur(supplierId).executeAsList()

        val history = mutableListOf<SupplierHistoryItem>()
        bcs.forEach { bc ->
            val items = database.appDatabaseQueries.selectCommandeItemsByCommandeId(bc.id).executeAsList()
            val totalAmount = items.sumOf { it.quantity * it.unit_price }
            history.add(
                SupplierHistoryItem(
                    reference = bc.reference_bon,
                    date = bc.date,
                    type = "Bon de Commande",
                    amountDzd = totalAmount,
                    status = if (bc.is_debt == 1L) "En Dette" else "Validé"
                )
            )
        }
        bls.forEach { bl ->
            val items = database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(bl.id).executeAsList()
            val totalAmount = items.sumOf { it.quantity * it.unit_price }
            history.add(
                SupplierHistoryItem(
                    reference = bl.reference_bon,
                    date = bl.date_livraison,
                    type = "Bon de Livraison",
                    amountDzd = totalAmount,
                    status = "Réceptionné"
                )
            )
        }
        history.sortByDescending { it.date }

        _uiState.update {
            it.copy(
                conventions = domainConventions,
                conventionBalances = balancesMap,
                historyItems = history
            )
        }
    }

    fun openAddSupplierDialog() {
        _uiState.update { it.copy(isAddSupplierDialogOpen = true) }
    }

    fun closeAddSupplierDialog() {
        _uiState.update { it.copy(isAddSupplierDialogOpen = false) }
    }

    fun addSupplier(name: String) {
        addSupplierSync(name)
    }

    fun addSupplierSync(name: String) {
        database.appDatabaseQueries.insertFournisseur(name.trim())
        closeAddSupplierDialog()
        loadSuppliersSync()
    }

    fun openAddConventionDialog() {
        _uiState.update { it.copy(isAddConventionDialogOpen = true) }
    }

    fun closeAddConventionDialog() {
        _uiState.update { it.copy(isAddConventionDialogOpen = false) }
    }

    fun addConvention(
        ordNumber: Long,
        date: String,
        totalMontant: Double,
        items: List<ConventionDraftArticle>
    ) {
        addConventionSync(ordNumber, date, totalMontant, items)
    }

    fun addConventionSync(
        ordNumber: Long,
        date: String,
        totalMontant: Double,
        items: List<ConventionDraftArticle>
    ) {
        val currentSupplier = _uiState.value.selectedSupplier ?: return
        database.transaction {
            database.appDatabaseQueries.insertConvention(
                fournisseur_id = currentSupplier.id,
                ord_number = ordNumber,
                date = date,
                global_unit_price = 0.0,
                total_montant = totalMontant
            )
            val convId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()
            items.forEach { item ->
                database.appDatabaseQueries.insertConventionItem(
                    convention_id = convId,
                    designation = item.designation,
                    unite = item.unite,
                    quantity = item.quantity,
                    min_quantity = item.minQuantity
                )
            }
        }
        closeAddConventionDialog()
        loadSupplierDetailsSync(currentSupplier.id)
    }
}
