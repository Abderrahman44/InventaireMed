package org.abdat.inventory.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.BonDeCommande
import org.abdat.inventory.domain.model.CommandeItem
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

enum class OrderStatusFilter(val label: String) {
    ALL("Tous les BC"),
    VALIDATED("Validés / Émis"),
    DRAFT("En Brouillon"),
    RECEIVING("En Réception"),
    CLOSED("Clôturés / Soldés")
}

data class OrderDisplayItem(
    val commande: BonDeCommande,
    val supplierName: String,
    val itemsCount: Int,
    val totalAmountDzd: Double,
    val statusLabel: String,
    val progressPercent: Int
)

data class OrderDraftItem(
    val conventionItemId: Long? = null,
    val codeArticle: String = "",
    val designation: String,
    val unite: String = "UNITE",
    val quantity: Double,
    val unitPrice: Double
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class OrdersUiState(
    val orders: List<OrderDisplayItem> = emptyList(),
    val filteredOrders: List<OrderDisplayItem> = emptyList(),
    val selectedFilter: OrderStatusFilter = OrderStatusFilter.ALL,
    val searchQuery: String = "",
    val activeOrdersCount: Int = 0,
    val totalCommittedDzd: Double = 0.0,
    val pendingDeliveriesCount: Int = 0,
    val strainedConventionsCount: Int = 0,
    val suppliers: List<Fournisseur> = emptyList(),
    val isNewOrderDialogOpen: Boolean = false,
    val selectedSupplierId: Long? = null,
    val conventionsForSupplier: List<Convention> = emptyList(),
    val selectedConventionId: Long? = null,
    val articlesForConvention: List<ConventionQuotaBalance> = emptyList(),
    val isLoading: Boolean = false
)

class OrdersViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrdersSync()
    }

    fun loadOrders() {
        viewModelScope.launch {
            loadOrdersSync()
        }
    }

    fun loadOrdersSync() {
        val dbCommandes = database.appDatabaseQueries.selectAllCommandes().executeAsList()
        val dbSuppliers = database.appDatabaseQueries.selectAllFournisseurs().executeAsList().map { it.toDomain() }
        val supplierMap = dbSuppliers.associateBy { it.id }

        val orderItems = dbCommandes.map { dbCmd ->
            val items = database.appDatabaseQueries.selectCommandeItemsByCommandeId(dbCmd.id)
                .executeAsList()
                .map { it.toDomain() }
            val domainCmd = dbCmd.toDomain(items)
            val totalAmount = items.sumOf { it.quantity * it.unitPrice }
            val supplierName = supplierMap[domainCmd.fournisseurId]?.fullName ?: "Fournisseur Inconnu"

            val (status, progress) = when {
                domainCmd.isDebt -> "Dette Urgente" to 25
                items.isEmpty() -> "En Brouillon" to 10
                domainCmd.id % 2L == 0L -> "Partiellement Reçu" to 65
                else -> "Validé / Émis" to 40
            }

            OrderDisplayItem(
                commande = domainCmd,
                supplierName = supplierName,
                itemsCount = items.size,
                totalAmountDzd = totalAmount,
                statusLabel = status,
                progressPercent = progress
            )
        }

        val totalCommitted = orderItems.sumOf { it.totalAmountDzd }
        val pendingLivraisons = database.appDatabaseQueries.selectAllLivraisons().executeAsList().size

        _uiState.update {
            it.copy(
                orders = orderItems,
                suppliers = dbSuppliers,
                activeOrdersCount = orderItems.size,
                totalCommittedDzd = totalCommitted,
                pendingDeliveriesCount = pendingLivraisons,
                strainedConventionsCount = 2,
                isLoading = false
            )
        }
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onFilterChange(filter: OrderStatusFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val filter = state.selectedFilter

        val filtered = state.orders.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.commande.referenceBon.lowercase().contains(query) ||
                    item.supplierName.lowercase().contains(query)

            val matchesFilter = when (filter) {
                OrderStatusFilter.ALL -> true
                OrderStatusFilter.VALIDATED -> item.statusLabel.contains("Validé")
                OrderStatusFilter.DRAFT -> item.statusLabel.contains("Brouillon")
                OrderStatusFilter.RECEIVING -> item.statusLabel.contains("Reçu") || item.statusLabel.contains("Réception")
                OrderStatusFilter.CLOSED -> item.progressPercent >= 100 || item.statusLabel.contains("Soldé")
            }

            matchesQuery && matchesFilter
        }

        _uiState.update { it.copy(filteredOrders = filtered) }
    }

    fun openNewOrderDialog() {
        val firstSupplier = _uiState.value.suppliers.firstOrNull()
        _uiState.update { it.copy(isNewOrderDialogOpen = true) }
        if (firstSupplier != null) {
            selectSupplierForNewOrder(firstSupplier.id)
        }
    }

    fun closeNewOrderDialog() {
        _uiState.update { it.copy(isNewOrderDialogOpen = false) }
    }

    fun selectSupplierForNewOrder(supplierId: Long) {
        val dbConventions = database.appDatabaseQueries.selectConventionsByFournisseur(supplierId).executeAsList()
        val conventions = dbConventions.map { it.toDomain() }
        val firstConvention = conventions.firstOrNull()

        _uiState.update {
            it.copy(
                selectedSupplierId = supplierId,
                conventionsForSupplier = conventions,
                selectedConventionId = firstConvention?.id
            )
        }

        if (firstConvention != null) {
            selectConventionForNewOrder(firstConvention.id)
        } else {
            _uiState.update { it.copy(articlesForConvention = emptyList()) }
        }
    }

    fun selectConventionForNewOrder(conventionId: Long) {
        val stockPriceMap = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .associate { it.designation.lowercase().trim() to (it.unit_price to it.code_article) }

        val balances = database.appDatabaseQueries.selectConventionBalancesByConventionId(conventionId)
            .executeAsList()
            .map {
                val stockInfo = stockPriceMap[it.designation.lowercase().trim()]
                val price = stockInfo?.first ?: 150.0
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

        _uiState.update {
            it.copy(
                selectedConventionId = conventionId,
                articlesForConvention = balances
            )
        }
    }

    fun createOrder(
        reference: String,
        supplierId: Long,
        date: String,
        isDebt: Boolean,
        items: List<OrderDraftItem>
    ) {
        createOrderSync(reference, supplierId, date, isDebt, items)
    }

    fun createOrderSync(
        reference: String,
        supplierId: Long,
        date: String,
        isDebt: Boolean,
        items: List<OrderDraftItem>
    ) {
        database.transaction {
            database.appDatabaseQueries.insertCommande(
                reference_bon = reference,
                fournisseur_id = supplierId,
                date = date,
                is_debt = if (isDebt) 1L else 0L
            )
            val newOrderId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

            items.forEach { draft ->
                database.appDatabaseQueries.insertCommandeItem(
                    commande_id = newOrderId,
                    convention_item_id = draft.conventionItemId,
                    code_article = draft.codeArticle,
                    designation = draft.designation,
                    type = "STANDARD",
                    family = "Médical",
                    quantity = draft.quantity,
                    unit_price = draft.unitPrice
                )
            }
        }
        closeNewOrderDialog()
        loadOrdersSync()
    }

    fun deleteOrder(id: Long) {
        deleteOrderSync(id)
    }

    fun deleteOrderSync(id: Long) {
        database.appDatabaseQueries.deleteCommande(id)
        loadOrdersSync()
    }
}
