package org.abdat.inventory.ui.decharges

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
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.DechargeItem
import org.abdat.inventory.domain.model.PapierDecharge
import org.abdat.inventory.domain.model.StockArticle

data class DechargeDraftLine(
    val codeArticle: String,
    val designation: String,
    val unite: String = "U",
    val family: String = "",
    val sousFamily: String = "",
    val quantity: Double = 1.0,
    val expirationDate: String? = null,
    val article: StockArticle? = null
) {
    constructor(article: StockArticle, quantity: Double = 1.0) : this(
        codeArticle = article.codeArticle,
        designation = article.designation,
        unite = article.uniteQuantite,
        family = article.family,
        sousFamily = article.sousFamily,
        quantity = quantity,
        expirationDate = article.expirationDate,
        article = article
    )
}

@Stable
data class DechargesUiState(
    val decharges: ImmutableList<PapierDecharge> = persistentListOf(),
    val availableArticles: ImmutableList<StockArticle> = persistentListOf(),
    val selectedDecharge: PapierDecharge? = null,
    val selectedDechargeItems: ImmutableList<DechargeItem> = persistentListOf(),
    val isCreateDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val nextGeneratedReference: String = "DCH-2026-001",
    val message: String? = null,
    val taxonomy: CategoryTaxonomy = CategoryTaxonomy()
)

class DechargeViewModel(
    private val database: AppDatabase,
    private val categoryRepository: CategoryRepository = CategoryRepository(database, Dispatchers.IO),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DechargesUiState())
    val uiState: StateFlow<DechargesUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(taxonomy = categoryRepository.currentTaxonomy()) }
        loadDataSync()

        viewModelScope.launch {
            categoryRepository.taxonomy.collect { taxonomy ->
                _uiState.update { it.copy(taxonomy = taxonomy) }
            }
        }

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllDecharges().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllActiveStockArticles().asFlow().mapToList(dispatcher)
            ) { _, _ -> Unit }
                .collect {
                    loadDataSync()
                }
        }
    }

    fun loadData() {
        loadDataSync()
    }

    fun loadDataSync() {
        val dbDecharges = database.appDatabaseQueries.selectAllDecharges().executeAsList().map { it.toDomain() }
        val dbArticles = database.appDatabaseQueries.selectAllActiveStockArticles().executeAsList().map { it.toDomain() }

        val prefix = database.appDatabaseQueries.getMetadata("prefix_decharge").executeAsOneOrNull() ?: "DCH-2026-"
        val seq = (dbDecharges.size + 1).toString().padStart(3, '0')
        val generatedRef = "$prefix$seq"

        _uiState.update {
            it.copy(
                decharges = dbDecharges.toImmutableList(),
                availableArticles = dbArticles.toImmutableList(),
                nextGeneratedReference = generatedRef,
                isLoading = false
            )
        }
    }

    fun openCreateDialog() {
        val prefix = database.appDatabaseQueries.getMetadata("prefix_decharge").executeAsOneOrNull() ?: "DCH-2026-"
        val seq = (_uiState.value.decharges.size + 1).toString().padStart(3, '0')
        _uiState.update { it.copy(isCreateDialogOpen = true, nextGeneratedReference = "$prefix$seq") }
    }

    fun closeCreateDialog() {
        _uiState.update { it.copy(isCreateDialogOpen = false) }
    }

    fun selectDecharge(decharge: PapierDecharge?) {
        if (decharge == null) {
            _uiState.update { it.copy(selectedDecharge = null, selectedDechargeItems = persistentListOf()) }
            return
        }
        viewModelScope.launch {
            val items = database.appDatabaseQueries.selectDechargeItemsByDechargeId(decharge.id)
                .executeAsList()
                .map { it.toDomain() }
            _uiState.update { it.copy(selectedDecharge = decharge, selectedDechargeItems = items.toImmutableList()) }
        }
    }

    fun createDechargeVoucherSync(
        referenceBon: String,
        organizationName: String,
        donorName: String,
        date: String,
        notes: String,
        lines: List<DechargeDraftLine>
    ): Boolean {
        if (organizationName.isBlank() || lines.isEmpty()) {
            _uiState.update { it.copy(message = "L'organisme et au moins un article sont requis") }
            return false
        }

        val taxonomy = categoryRepository.currentTaxonomy()
        for (line in lines) {
            val existing = database.appDatabaseQueries.selectStockArticleByCode(line.codeArticle).executeAsOneOrNull()
            if (existing == null) {
                val resolved = taxonomy.resolvePair(line.family, line.sousFamily)
                if (resolved == null) {
                    _uiState.update {
                        it.copy(
                            message = "Famille « ${line.family} » / sous-famille « ${line.sousFamily} » non configurée pour l'article « ${line.designation.ifBlank { line.codeArticle }} ». " +
                                "Sélectionnez une catégorie existante ou configurez-la dans Paramètres & Config."
                        )
                    }
                    return false
                }
            }
        }

        database.transaction {
            database.appDatabaseQueries.insertDecharge(
                reference_bon = referenceBon.trim(),
                organization_name = organizationName.trim(),
                donor_name = donorName.trim(),
                date = date,
                notes = notes.trim()
            )
            val dechargeId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

            lines.forEach { line ->
                // 1. Insert line item with STRICT 0.0 unit price (Zero financial valuation)
                database.appDatabaseQueries.insertDechargeItem(
                    decharge_id = dechargeId,
                    code_article = line.codeArticle,
                    designation = line.designation,
                    quantity = line.quantity
                )

                // 2. Atomically increase stock physical quantity or create new article with zero price
                val existing = database.appDatabaseQueries.selectStockArticleByCode(line.codeArticle).executeAsOneOrNull()
                if (existing != null) {
                    database.appDatabaseQueries.increaseStockQuantity(
                        quantity = line.quantity,
                        date = date,
                        code_article = line.codeArticle
                    )
                } else {
                    val (familyItem, sousFamilyItem) = taxonomy.resolvePair(line.family, line.sousFamily)
                        ?: error("Unresolved category pair for ${line.codeArticle}")
                    database.appDatabaseQueries.insertStockArticle(
                        code_article = line.codeArticle,
                        additional_reference_id = "",
                        designation = line.designation,
                        unite_quantite = line.unite.ifBlank { "U" },
                        family = familyItem.label,
                        sous_family = sousFamilyItem.label,
                        quantity = line.quantity,
                        unit_price = 0.0,
                        expiration_date = line.expirationDate,
                        is_archived = 0,
                        is_obsolete = 0,
                        date = date
                    )
                }
            }
        }

        closeCreateDialog()
        loadDataSync()
        _uiState.update { it.copy(message = "Papier de Décharge $referenceBon validé et stock incrémenté avec succès.") }
        return true
    }

    fun createDechargeVoucher(
        referenceBon: String,
        organizationName: String,
        donorName: String,
        date: String,
        notes: String,
        lines: List<DechargeDraftLine>
    ) {
        viewModelScope.launch {
            createDechargeVoucherSync(referenceBon, organizationName, donorName, date, notes, lines)
        }
    }
}
