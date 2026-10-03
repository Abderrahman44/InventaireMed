package org.abdat.inventory.ui.orders

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
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
    val progressPercent: Int,
    val linkedDeliveriesCount: Int = 0,
    val linkedDeliveryRefs: ImmutableList<String> = persistentListOf(),
    val subtotalHtDzd: Double = totalAmountDzd,
    val tvaRate: Double = 0.0,
    val tvaAmountDzd: Double = 0.0
)

data class LinkedLivraisonSummary(
    val id: Long,
    val referenceBon: String,
    val date: String,
    val itemsCount: Int
)

data class OrderLineFulfillmentItem(
    val item: CommandeItem,
    val deliveredQuantity: Double,
    val remainingQuantity: Double,
    val isFulfilled: Boolean,
    val statusLabel: String
) {
    val totalAmount: Double get() = item.quantity * item.unitPrice
}

data class OrderDetailDisplayData(
    val displayItem: OrderDisplayItem,
    val lines: ImmutableList<OrderLineFulfillmentItem>,
    val linkedDeliveries: ImmutableList<LinkedLivraisonSummary>
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

@Stable
data class OrdersUiState(
    val orders: ImmutableList<OrderDisplayItem> = persistentListOf(),
    val filteredOrders: ImmutableList<OrderDisplayItem> = persistentListOf(),
    val selectedFilter: OrderStatusFilter = OrderStatusFilter.ALL,
    val searchQuery: String = "",
    val activeOrdersCount: Int = 0,
    val totalCommittedDzd: Double = 0.0,
    val pendingDeliveriesCount: Int = 0,
    val strainedConventionsCount: Int = 0,
    val suppliers: ImmutableList<Fournisseur> = persistentListOf(),
    val isNewOrderDialogOpen: Boolean = false,
    val selectedSupplierId: Long? = null,
    val conventionsForSupplier: ImmutableList<Convention> = persistentListOf(),
    val selectedConventionId: Long? = null,
    val selectedConventionTvaRate: Double = 0.0,
    val articlesForConvention: ImmutableList<ConventionQuotaBalance> = persistentListOf(),
    val isLoading: Boolean = false,
    val selectedOrderForDetail: OrderDetailDisplayData? = null
)

class OrdersViewModel(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrdersSync()

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllCommandes().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllFournisseurs().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllLivraisons().asFlow().mapToList(dispatcher)
            ) { _, _, _ -> Unit }
                .collect {
                    loadOrdersSync()
                }
        }
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

        val allLivraisons = database.appDatabaseQueries.selectAllLivraisons().executeAsList()
        val livraisonsByCmdId = allLivraisons.filter { it.commande_id != null }.groupBy { it.commande_id }

        val allConvItems = database.appDatabaseQueries.selectAllConventionItems().executeAsList()
        val convItemToConvId = allConvItems.associate { it.id to it.convention_id }
        val allConventions = database.appDatabaseQueries.selectAllConventions().executeAsList()
        val conventionMap = allConventions.associateBy { it.id }

        val orderItems = dbCommandes.map { dbCmd ->
            val items = database.appDatabaseQueries.selectCommandeItemsByCommandeId(dbCmd.id)
                .executeAsList()
                .map { it.toDomain() }
            val domainCmd = dbCmd.toDomain(items)
            val subtotalHt = items.sumOf { it.quantity * it.unitPrice }
            val convId = items.firstNotNullOfOrNull { convItemToConvId[it.conventionItemId] }
            val tvaRate = convId?.let { conventionMap[it]?.tva_rate } ?: 0.0
            val tvaAmount = subtotalHt * (tvaRate / 100.0)
            val totalTtc = subtotalHt + tvaAmount
            val supplierName = supplierMap[domainCmd.fournisseurId]?.fullName ?: "Fournisseur Inconnu"

            val linkedBls = livraisonsByCmdId[dbCmd.id] ?: emptyList()
            val linkedRefs = linkedBls.map { it.reference_bon }
            val deliveredItems = linkedBls.flatMap { bl ->
                database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(bl.id).executeAsList()
            }
            val deliveredQtyMap = mutableMapOf<String, Double>()
            deliveredItems.forEach { d ->
                val key = if (d.code_article.isNotBlank()) d.code_article.lowercase().trim() else d.designation.lowercase().trim()
                deliveredQtyMap[key] = (deliveredQtyMap[key] ?: 0.0) + d.quantity
            }

            val totalOrderedQty = items.sumOf { it.quantity }
            val totalDeliveredQty = items.sumOf { item ->
                val key = if (item.codeArticle.isNotBlank()) item.codeArticle.lowercase().trim() else item.designation.lowercase().trim()
                (deliveredQtyMap[key] ?: 0.0).coerceAtMost(item.quantity)
            }

            val allItemsFulfilled = items.isNotEmpty() && items.all { item ->
                val key = if (item.codeArticle.isNotBlank()) item.codeArticle.lowercase().trim() else item.designation.lowercase().trim()
                (deliveredQtyMap[key] ?: 0.0) >= (item.quantity - 0.001)
            }
            val hasAnyDelivered = totalDeliveredQty > 0.001

            val progress = if (totalOrderedQty > 0.0) {
                ((totalDeliveredQty / totalOrderedQty) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val status = when {
                items.isEmpty() -> "En Brouillon"
                allItemsFulfilled || progress >= 100 -> "Soldé / Clôturé"
                hasAnyDelivered -> "Partiellement Reçu"
                domainCmd.isDebt -> "Dette Urgente"
                else -> "Validé / Émis"
            }

            OrderDisplayItem(
                commande = domainCmd,
                supplierName = supplierName,
                itemsCount = items.size,
                totalAmountDzd = totalTtc,
                statusLabel = status,
                progressPercent = progress,
                linkedDeliveriesCount = linkedBls.size,
                linkedDeliveryRefs = linkedRefs.toImmutableList(),
                subtotalHtDzd = subtotalHt,
                tvaRate = tvaRate,
                tvaAmountDzd = tvaAmount
            )
        }

        val totalCommitted = orderItems.sumOf { it.totalAmountDzd }
        val activeOrders = orderItems.count { it.statusLabel != "Soldé / Clôturé" }
        val pendingLivraisons = allLivraisons.size

        _uiState.update {
            it.copy(
                orders = orderItems.toImmutableList(),
                suppliers = dbSuppliers.toImmutableList(),
                activeOrdersCount = activeOrders,
                totalCommittedDzd = totalCommitted,
                pendingDeliveriesCount = pendingLivraisons,
                strainedConventionsCount = allConventions.size,
                isLoading = false
            )
        }
        applyFilters()

        val currentDetailId = _uiState.value.selectedOrderForDetail?.displayItem?.commande?.id
        if (currentDetailId != null) {
            loadOrderDetailSync(currentDetailId)
        }
    }

    fun selectOrderForDetail(orderId: Long?) {
        if (orderId == null) {
            _uiState.update { it.copy(selectedOrderForDetail = null) }
            return
        }
        loadOrderDetailSync(orderId)
    }

    fun loadOrderDetailSync(orderId: Long) {
        val displayItem = _uiState.value.orders.firstOrNull { it.commande.id == orderId } ?: return
        val items = database.appDatabaseQueries.selectCommandeItemsByCommandeId(orderId)
            .executeAsList()
            .map { it.toDomain() }

        val allLivraisons = database.appDatabaseQueries.selectAllLivraisons().executeAsList()
        val linkedBls = allLivraisons.filter { it.commande_id == orderId }

        val deliveredQtyMap = mutableMapOf<String, Double>()
        val linkedSummaries = linkedBls.map { bl ->
            val blItems = database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(bl.id).executeAsList()
            blItems.forEach { d ->
                val key = if (d.code_article.isNotBlank()) d.code_article.lowercase().trim() else d.designation.lowercase().trim()
                deliveredQtyMap[key] = (deliveredQtyMap[key] ?: 0.0) + d.quantity
            }
            LinkedLivraisonSummary(
                id = bl.id,
                referenceBon = bl.reference_bon,
                date = bl.date_livraison,
                itemsCount = blItems.size
            )
        }

        val lineFulfillments = items.map { item ->
            val key = if (item.codeArticle.isNotBlank()) item.codeArticle.lowercase().trim() else item.designation.lowercase().trim()
            val delivered = (deliveredQtyMap[key] ?: 0.0).coerceAtLeast(0.0)
            val remaining = (item.quantity - delivered).coerceAtLeast(0.0)
            val isFulfilled = delivered >= (item.quantity - 0.001)
            val status = if (isFulfilled) "Livré" else "Pour livraison partielle"

            OrderLineFulfillmentItem(
                item = item,
                deliveredQuantity = delivered,
                remainingQuantity = remaining,
                isFulfilled = isFulfilled,
                statusLabel = status
            )
        }

        _uiState.update {
            it.copy(
                selectedOrderForDetail = OrderDetailDisplayData(
                    displayItem = displayItem,
                    lines = lineFulfillments.toImmutableList(),
                    linkedDeliveries = linkedSummaries.toImmutableList()
                )
            )
        }
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
                OrderStatusFilter.VALIDATED -> item.statusLabel.contains("Validé") || item.statusLabel.contains("Dette")
                OrderStatusFilter.DRAFT -> item.statusLabel.contains("Brouillon")
                OrderStatusFilter.RECEIVING -> item.statusLabel.contains("Partiellement") || item.statusLabel.contains("Reçu") || item.statusLabel.contains("Réception")
                OrderStatusFilter.CLOSED -> item.progressPercent >= 100 || item.statusLabel.contains("Soldé") || item.statusLabel.contains("Clôturé")
            }

            matchesQuery && matchesFilter
        }

        _uiState.update { it.copy(filteredOrders = filtered.toImmutableList()) }
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
                conventionsForSupplier = conventions.toImmutableList(),
                selectedConventionId = firstConvention?.id,
                selectedConventionTvaRate = firstConvention?.tvaRate ?: 0.0
            )
        }

        if (firstConvention != null) {
            selectConventionForNewOrder(firstConvention.id)
        } else {
            _uiState.update { it.copy(articlesForConvention = persistentListOf(), selectedConventionTvaRate = 0.0) }
        }
    }

    fun selectConventionForNewOrder(conventionId: Long) {
        val stockPriceMap = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .associateBy { it.designation.lowercase().trim() }

        val conv = _uiState.value.conventionsForSupplier.find { it.id == conventionId }
            ?: database.appDatabaseQueries.selectConventionById(conventionId).executeAsOneOrNull()?.toDomain()
        val tvaRate = conv?.tvaRate ?: 0.0

        val balances = database.appDatabaseQueries.selectConventionBalancesByConventionId(conventionId)
            .executeAsList()
            .map {
                val stockItem = stockPriceMap[it.designation.lowercase().trim()]
                val price = stockItem?.unit_price ?: 150.0
                val sousFam = stockItem?.sous_family ?: ""
                val fam = stockItem?.family ?: ""
                ConventionQuotaBalance(
                    conventionItemId = it.conventionItemId,
                    conventionId = it.conventionId,
                    designation = it.designation,
                    unite = it.unite,
                    agreedQuantity = it.agreedQuantity,
                    minQuantity = it.minQuantity,
                    orderedQuantity = it.orderedQuantity,
                    remainingQuantity = it.remainingQuantity,
                    unitPrice = price,
                    sousFamily = sousFam,
                    family = fam
                )
            }

        _uiState.update {
            it.copy(
                selectedConventionId = conventionId,
                selectedConventionTvaRate = tvaRate,
                articlesForConvention = balances.toImmutableList()
            )
        }
    }

    fun calculateOrderTotals(items: List<OrderDraftItem>, tvaRate: Double): Triple<Double, Double, Double> {
        val subtotalHt = items.sumOf { it.subtotal }
        val tvaAmount = subtotalHt * (tvaRate / 100.0)
        val totalTtc = subtotalHt + tvaAmount
        return Triple(subtotalHt, tvaAmount, totalTtc)
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
