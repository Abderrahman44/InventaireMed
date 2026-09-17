package org.abdat.inventory.ui.inventory

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.data.repository.StockSnapshot
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.domain.model.categoryLabelsMatch

/** Filter chip meaning "no family filter". */
const val CATEGORY_FILTER_ALL = "Tous"

/** Filter chip for articles with a blank family; not a configured category. */
const val CATEGORY_FILTER_UNCLASSIFIED = "Non classé"

@Stable
data class InventoryUiState(
    val articles: ImmutableList<StockArticle> = persistentListOf(),
    val filteredArticles: ImmutableList<StockArticle> = persistentListOf(),
    val searchQuery: String = "",
    val selectedCategory: String = CATEGORY_FILTER_ALL,
    val availableCategories: ImmutableList<String> = persistentListOf(CATEGORY_FILTER_ALL),
    val taxonomy: CategoryTaxonomy = CategoryTaxonomy(),
    val statusFilter: String = "Actifs", // Actifs, Tous, Archivés, Obsolètes
    val totalArticlesCount: Int = 0,
    val totalStockValueDzd: Double = 0.0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val activeOrdersCount: Int = 0,
    val isAddEditDialogOpen: Boolean = false,
    val articleToEdit: StockArticle? = null,
    val articleSaveError: String? = null,
    val selectedArticleForDetail: StockArticle? = null,
    val isLoading: Boolean = true
)

class InventoryViewModel(
    private val database: AppDatabase,
    private val stockRepository: StockRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        // Prime synchronously so the first composition has data rather than a spinner.
        onTaxonomy(categoryRepository.currentTaxonomy())
        onStockSnapshot(stockRepository.currentSnapshot())

        // Then stay reactive: any screen's committed mutation re-emits here, so the table and the
        // KPI strip stay live without a reload call or a screen-entry effect.
        viewModelScope.launch {
            stockRepository.snapshots.collect { snapshot ->
                onStockSnapshot(snapshot)
            }
        }

        // A family or sous-family added in Config reaches the filter chips and an open article dialog
        // through this collector, independently of stock emissions.
        viewModelScope.launch {
            categoryRepository.taxonomy.collect { taxonomy ->
                onTaxonomy(taxonomy)
            }
        }

        viewModelScope.launch {
            database.appDatabaseQueries.selectAllCommandes().asFlow().mapToList(Dispatchers.IO).collect { cmds ->
                _uiState.update { it.copy(activeOrdersCount = cmds.size) }
            }
        }
    }

    private fun onTaxonomy(taxonomy: CategoryTaxonomy) {
        _uiState.update { current -> withCategoryChips(current.copy(taxonomy = taxonomy)) }
        applyFilters()
    }

    /**
     * Chips come from the configured families, not from whatever labels articles happen to carry, plus
     * [CATEGORY_FILTER_UNCLASSIFIED] while any article lacks a family. Recomputed from both collectors,
     * since either can change the result. A selection that no longer exists falls back to "Tous".
     */
    private fun withCategoryChips(state: InventoryUiState): InventoryUiState {
        val chips = buildList {
            add(CATEGORY_FILTER_ALL)
            addAll(state.taxonomy.families.map { it.label })
            if (state.articles.any { it.family.isBlank() }) add(CATEGORY_FILTER_UNCLASSIFIED)
        }
        val selectionStillExists = chips.any { categoryLabelsMatch(it, state.selectedCategory) }
        return state.copy(
            availableCategories = chips.toImmutableList(),
            selectedCategory = if (selectionStillExists) state.selectedCategory else CATEGORY_FILTER_ALL
        )
    }

    /**
     * Folds an emitted stock snapshot into the existing state with [InventoryUiState.copy], so the
     * UI state this ViewModel owns — search text, filters, dialog visibility, the article being
     * edited — survives an emission triggered by an unrelated screen.
     */
    private fun onStockSnapshot(snapshot: StockSnapshot) {
        val domainArticles = snapshot.articles
        val activeOrders = database.appDatabaseQueries.selectAllCommandes().executeAsList().size

        _uiState.update { current ->
            withCategoryChips(current.copy(
                articles = domainArticles,
                totalArticlesCount = snapshot.activeArticlesCount,
                totalStockValueDzd = snapshot.totalValuationDzd,
                outOfStockCount = snapshot.outOfStockCount,
                lowStockCount = snapshot.lowStockCount,
                activeOrdersCount = activeOrders,
                // Only the first emission clears the loading state; later emissions update the
                // populated table in place rather than replacing it with a spinner.
                isLoading = false
            ))
        }
        applyFilters()
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        applyFilters()
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun onStatusFilterChange(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val category = state.selectedCategory
        val status = state.statusFilter

        val filtered = state.articles.filter { article ->
            val matchesQuery = query.isEmpty() ||
                    article.designation.lowercase().contains(query) ||
                    article.codeArticle.lowercase().contains(query) ||
                    article.additionalReferenceId.lowercase().contains(query) ||
                    article.family.lowercase().contains(query) ||
                    article.sousFamily.lowercase().contains(query)

            val matchesCategory = when (category) {
                CATEGORY_FILTER_ALL -> true
                CATEGORY_FILTER_UNCLASSIFIED -> article.family.isBlank()
                else -> categoryLabelsMatch(article.family, category)
            }

            val matchesStatus = when (status) {
                "Actifs" -> !article.isArchived && !article.isObsolete
                "Archivés" -> article.isArchived
                "Obsolètes" -> article.isObsolete
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }

        _uiState.update { it.copy(filteredArticles = filtered.toImmutableList()) }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, articleToEdit = null, articleSaveError = null) }
    }

    fun openEditDialog(article: StockArticle) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, articleToEdit = article, articleSaveError = null) }
    }

    fun closeAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, articleToEdit = null, articleSaveError = null) }
    }

    fun selectArticleForDetail(article: StockArticle?) {
        _uiState.update { it.copy(selectedArticleForDetail = article) }
    }

    fun saveArticle(
        code: String,
        designation: String,
        unite: String,
        family: String,
        sousFamily: String,
        quantity: Double,
        unitPrice: Double,
        date: String,
        additionalReferenceId: String = "",
        expirationDate: String? = null,
        auditReason: String = ""
    ) {
        viewModelScope.launch {
            saveArticleSync(
                code, designation, unite, family, sousFamily, quantity, unitPrice, date,
                additionalReferenceId, expirationDate, auditReason
            )
        }
    }

    /**
     * Inserts or updates the article, returning false without writing when [family] / [sousFamily]
     * is not a configured active pair. Validated against committed taxonomy rather than the observed
     * state, so a category deleted a moment ago cannot slip through; the configured labels are what
     * get stored, so letter case always matches the configuration.
     */
    fun saveArticleSync(
        code: String,
        designation: String,
        unite: String,
        family: String,
        sousFamily: String,
        quantity: Double,
        unitPrice: Double,
        date: String,
        additionalReferenceId: String = "",
        expirationDate: String? = null,
        auditReason: String = ""
    ): Boolean {
        val resolved = categoryRepository.currentTaxonomy().resolvePair(family, sousFamily)
        if (resolved == null) {
            _uiState.update {
                it.copy(
                    articleSaveError = "Famille « $family » / sous-famille « $sousFamily » non configurée. " +
                        "Sélectionnez une catégorie existante ou ajoutez-la dans Paramètres & Config."
                )
            }
            return false
        }
        val (familyItem, sousFamilyItem) = resolved

        val articleToEdit = _uiState.value.articleToEdit
        if (articleToEdit == null) {
            database.appDatabaseQueries.insertStockArticle(
                code_article = code,
                additional_reference_id = additionalReferenceId,
                designation = designation,
                unite_quantite = unite,
                family = familyItem.label,
                sous_family = sousFamilyItem.label,
                quantity = quantity,
                unit_price = unitPrice,
                expiration_date = expirationDate,
                is_archived = 0L,
                is_obsolete = 0L,
                date = date
            )
        } else {
            database.appDatabaseQueries.updateStockArticle(
                code_article = code,
                additional_reference_id = additionalReferenceId,
                designation = designation,
                unite_quantite = unite,
                family = familyItem.label,
                sous_family = sousFamilyItem.label,
                quantity = quantity,
                unit_price = unitPrice,
                expiration_date = expirationDate,
                is_archived = if (articleToEdit.isArchived) 1L else 0L,
                is_obsolete = if (articleToEdit.isObsolete) 1L else 0L,
                date = date,
                id = articleToEdit.id
            )
            if (auditReason.isNotBlank()) {
                database.appDatabaseQueries.insertArticleAuditLog(
                    article_id = articleToEdit.id,
                    field_name = "fiche_article",
                    old_value = "${articleToEdit.designation} [${articleToEdit.uniteQuantite}]",
                    new_value = "$designation [$unite]",
                    reason = auditReason,
                    user_name = "Responsable Stock",
                    edit_date = date
                )
            }
        }
        // No reload: the write notifies the observed query, which re-emits into onStockSnapshot.
        closeAddEditDialog()
        return true
    }

    fun archiveArticle(id: Long) {
        viewModelScope.launch {
            database.appDatabaseQueries.archiveStockArticle(id)
        }
    }

    fun unarchiveArticle(id: Long) {
        viewModelScope.launch {
            database.appDatabaseQueries.unarchiveStockArticle(id)
        }
    }

    fun markArticleObsolete(id: Long) {
        viewModelScope.launch {
            database.appDatabaseQueries.markStockArticleObsolete(id)
        }
    }

    fun deleteArticle(id: Long) {
        // Soft-delete: mark as archived to preserve historical data
        archiveArticle(id)
    }
}
