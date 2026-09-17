package org.abdat.inventory.ui.suppliers

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
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
    val unitPrice: Double = 0.0,
    val sousFamily: String = "",
    val family: String = ""
)

data class SupplierHistoryItem(
    val id: String = "",
    val reference: String,
    val date: String,
    val type: String,
    val amountDzd: Double,
    val status: String
)

@Stable
data class SuppliersUiState(
    val suppliers: ImmutableList<Fournisseur> = persistentListOf(),
    val filteredSuppliers: ImmutableList<Fournisseur> = persistentListOf(),
    val selectedSupplier: Fournisseur? = null,
    val searchQuery: String = "",
    val activeTab: SupplierDetailTab = SupplierDetailTab.CONVENTIONS,
    val conventions: ImmutableList<Convention> = persistentListOf(),
    val conventionBalances: ImmutableMap<Long, ImmutableList<ConventionQuotaBalance>> = persistentMapOf(),
    val historyItems: ImmutableList<SupplierHistoryItem> = persistentListOf(),
    val selectedConventionForDetail: Convention? = null,
    val isProfileViewOpen: Boolean = false,
    val isEditProfileDialogOpen: Boolean = false,
    val isAddSupplierDialogOpen: Boolean = false,
    val isAddConventionDialogOpen: Boolean = false,
    val isAddArticleDialogOpen: Boolean = false,
    val isSensitiveWarningDialogOpen: Boolean = false,
    val isEditArticleDialogOpen: Boolean = false,
    val articlePendingEdit: ConventionQuotaBalance? = null,
    val isLoading: Boolean = false
)

class SuppliersViewModel(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    init {
        loadSuppliersSync()

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllFournisseurs().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllConventions().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllLivraisons().asFlow().mapToList(dispatcher)
            ) { _, _, _ -> Unit }
                .collect {
                    loadSuppliersSync()
                }
        }
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
                suppliers = domainSuppliers.toImmutableList(),
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
        _uiState.update { it.copy(filteredSuppliers = filtered.toImmutableList()) }
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
        val balancesMap = mutableMapOf<Long, ImmutableList<ConventionQuotaBalance>>()
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
            balancesMap[conv.id] = balances.toImmutableList()
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
                    id = "bc_${bc.id}",
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
                    id = "bl_${bl.id}",
                    reference = bl.reference_bon,
                    date = bl.date_livraison,
                    type = "Bon de Livraison",
                    amountDzd = totalAmount,
                    status = "Réceptionné"
                )
            )
        }
        history.sortByDescending { it.date }

        val currentDetailConvId = _uiState.value.selectedConventionForDetail?.id
        val updatedDetailConv = if (currentDetailConvId != null) {
            domainConventions.firstOrNull { it.id == currentDetailConvId }
        } else null

        _uiState.update {
            it.copy(
                conventions = domainConventions.toImmutableList(),
                conventionBalances = balancesMap.toImmutableMap(),
                historyItems = history.toImmutableList(),
                selectedConventionForDetail = updatedDetailConv ?: it.selectedConventionForDetail
            )
        }
    }

    fun selectConventionForDetail(convention: Convention) {
        _uiState.update { it.copy(selectedConventionForDetail = convention) }
    }

    fun closeConventionDetail() {
        _uiState.update {
            it.copy(
                selectedConventionForDetail = null,
                isAddArticleDialogOpen = false,
                isSensitiveWarningDialogOpen = false,
                isEditArticleDialogOpen = false,
                articlePendingEdit = null
            )
        }
    }

    fun openAddSupplierDialog() {
        _uiState.update { it.copy(isAddSupplierDialogOpen = true) }
    }

    fun closeAddSupplierDialog() {
        _uiState.update { it.copy(isAddSupplierDialogOpen = false) }
    }

    fun openSupplierProfile() {
        _uiState.update { it.copy(isProfileViewOpen = true) }
    }

    fun closeSupplierProfile() {
        _uiState.update { it.copy(isProfileViewOpen = false, isEditProfileDialogOpen = false) }
    }

    fun openEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = true) }
    }

    fun closeEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = false) }
    }

    fun updateSupplierProfile(
        id: Long,
        fullName: String,
        societe: String = "",
        phone: String = "",
        responsablePhone: String = "",
        ville: String = "",
        email: String = "",
        otherLinks: String = ""
    ) {
        viewModelScope.launch {
            updateSupplierProfileSync(
                id = id,
                fullName = fullName,
                societe = societe,
                phone = phone,
                responsablePhone = responsablePhone,
                ville = ville,
                email = email,
                otherLinks = otherLinks
            )
        }
    }

    fun updateSupplierProfileSync(
        id: Long,
        fullName: String,
        societe: String = "",
        phone: String = "",
        responsablePhone: String = "",
        ville: String = "",
        email: String = "",
        otherLinks: String = ""
    ) {
        database.appDatabaseQueries.updateFournisseurProfile(
            full_name = fullName.trim(),
            societe = societe.trim(),
            phone = phone.trim(),
            responsable_phone = responsablePhone.trim(),
            ville = ville.trim(),
            email = email.trim(),
            other_links = otherLinks.trim(),
            id = id
        )
        closeEditProfileDialog()
        loadSuppliersSync()
    }

    fun addSupplier(name: String) {
        addSupplierSync(name)
    }

    fun addSupplierSync(name: String) {
        database.appDatabaseQueries.insertFournisseur(name.trim())
        closeAddSupplierDialog()
        loadSuppliersSync()
    }

    fun addSupplierFull(
        name: String,
        societe: String = "",
        phone: String = "",
        responsablePhone: String = "",
        ville: String = "",
        email: String = "",
        otherLinks: String = ""
    ) {
        viewModelScope.launch {
            addSupplierFullSync(name, societe, phone, responsablePhone, ville, email, otherLinks)
        }
    }

    fun addSupplierFullSync(
        name: String,
        societe: String = "",
        phone: String = "",
        responsablePhone: String = "",
        ville: String = "",
        email: String = "",
        otherLinks: String = ""
    ) {
        database.appDatabaseQueries.insertFournisseurFull(
            full_name = name.trim(),
            societe = societe.trim(),
            phone = phone.trim(),
            responsable_phone = responsablePhone.trim(),
            ville = ville.trim(),
            email = email.trim(),
            other_links = otherLinks.trim()
        )
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
        items: List<ConventionDraftArticle>,
        tvaRate: Double = 0.0
    ) {
        addConventionSync(ordNumber, date, totalMontant, items, tvaRate)
    }

    fun addConventionSync(
        ordNumber: Long,
        date: String,
        totalMontant: Double,
        items: List<ConventionDraftArticle>,
        tvaRate: Double = 0.0
    ) {
        val currentSupplier = _uiState.value.selectedSupplier ?: return
        database.transaction {
            database.appDatabaseQueries.insertConvention(
                fournisseur_id = currentSupplier.id,
                ord_number = ordNumber,
                date = date,
                global_unit_price = 0.0,
                total_montant = totalMontant,
                tva_rate = tvaRate
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

    fun openAddArticleDialog() {
        _uiState.update { it.copy(isAddArticleDialogOpen = true) }
    }

    fun closeAddArticleDialog() {
        _uiState.update { it.copy(isAddArticleDialogOpen = false) }
    }

    fun addArticleToConvention(
        conventionId: Long,
        designation: String,
        unite: String,
        quantity: Double,
        minQuantity: Double = 5.0
    ) {
        viewModelScope.launch {
            addArticleToConventionSync(conventionId, designation, unite, quantity, minQuantity)
        }
    }

    fun addArticleToConventionSync(
        conventionId: Long,
        designation: String,
        unite: String,
        quantity: Double,
        minQuantity: Double = 5.0
    ) {
        database.appDatabaseQueries.insertConventionItem(
            convention_id = conventionId,
            designation = designation.trim(),
            unite = unite.trim(),
            quantity = quantity,
            min_quantity = minQuantity
        )
        closeAddArticleDialog()
        val supplierId = _uiState.value.selectedSupplier?.id
        if (supplierId != null) {
            loadSupplierDetailsSync(supplierId)
        }
    }

    fun requestEditArticle(article: ConventionQuotaBalance) {
        _uiState.update {
            it.copy(
                articlePendingEdit = article,
                isSensitiveWarningDialogOpen = true
            )
        }
    }

    fun dismissSensitiveWarning() {
        _uiState.update {
            it.copy(
                isSensitiveWarningDialogOpen = false,
                articlePendingEdit = null
            )
        }
    }

    fun confirmSensitiveWarning() {
        _uiState.update {
            it.copy(
                isSensitiveWarningDialogOpen = false,
                isEditArticleDialogOpen = true
            )
        }
    }

    fun closeEditArticleDialog() {
        _uiState.update {
            it.copy(
                isEditArticleDialogOpen = false,
                articlePendingEdit = null
            )
        }
    }

    fun updateConventionArticle(
        itemId: Long,
        conventionId: Long,
        designation: String,
        unite: String,
        quantity: Double,
        minQuantity: Double = 5.0
    ) {
        viewModelScope.launch {
            updateConventionArticleSync(itemId, conventionId, designation, unite, quantity, minQuantity)
        }
    }

    fun updateConventionArticleSync(
        itemId: Long,
        conventionId: Long,
        designation: String,
        unite: String,
        quantity: Double,
        minQuantity: Double = 5.0
    ) {
        database.appDatabaseQueries.updateConventionItem(
            id = itemId,
            designation = designation.trim(),
            unite = unite.trim(),
            quantity = quantity,
            min_quantity = minQuantity
        )
        closeEditArticleDialog()
        val supplierId = _uiState.value.selectedSupplier?.id
        if (supplierId != null) {
            loadSupplierDetailsSync(supplierId)
        }
    }
}
