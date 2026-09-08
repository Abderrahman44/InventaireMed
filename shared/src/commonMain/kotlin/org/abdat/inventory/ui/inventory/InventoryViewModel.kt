package org.abdat.inventory.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.StockArticle

data class InventoryUiState(
    val articles: List<StockArticle> = emptyList(),
    val filteredArticles: List<StockArticle> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "Tous",
    val availableCategories: List<String> = listOf("Tous", "Médicaments", "Consommables", "Matériel", "Réactifs"),
    val totalArticlesCount: Int = 0,
    val totalStockValueDzd: Double = 0.0,
    val outOfStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val activeOrdersCount: Int = 0,
    val isAddEditDialogOpen: Boolean = false,
    val articleToEdit: StockArticle? = null,
    val isLoading: Boolean = false
)

class InventoryViewModel(
    private val database: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadArticlesSync()
    }

    fun loadArticles() {
        viewModelScope.launch {
            loadArticlesSync()
        }
    }

    fun loadArticlesSync() {
        val dbArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
        val domainArticles = dbArticles.map { it.toDomain() }

        val totalValuation = domainArticles.sumOf { it.quantity * it.unitPrice }
        val outOfStock = domainArticles.count { it.quantity <= 0 }
        val lowStock = domainArticles.count { it.quantity in 0.001..10.0 }
        val activeOrders = database.appDatabaseQueries.selectAllCommandes().executeAsList().size

        val categories = listOf("Tous") + domainArticles.map { it.family.ifBlank { "Général" } }.distinct()

        _uiState.update { current ->
            current.copy(
                articles = domainArticles,
                availableCategories = if (categories.size > 1) categories else current.availableCategories,
                totalArticlesCount = domainArticles.size,
                totalStockValueDzd = totalValuation,
                outOfStockCount = outOfStock,
                lowStockCount = lowStock,
                activeOrdersCount = activeOrders,
                isLoading = false
            )
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

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val category = state.selectedCategory

        val filtered = state.articles.filter { article ->
            val matchesQuery = query.isEmpty() ||
                    article.designation.lowercase().contains(query) ||
                    article.codeArticle.lowercase().contains(query) ||
                    article.family.lowercase().contains(query)

            val matchesCategory = category == "Tous" ||
                    article.family.equals(category, ignoreCase = true) ||
                    (category == "Général" && article.family.isBlank())

            matchesQuery && matchesCategory
        }

        _uiState.update { it.copy(filteredArticles = filtered) }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = true, articleToEdit = null) }
    }

    fun openEditDialog(article: StockArticle) {
        _uiState.update { it.copy(isAddEditDialogOpen = true, articleToEdit = article) }
    }

    fun closeAddEditDialog() {
        _uiState.update { it.copy(isAddEditDialogOpen = false, articleToEdit = null) }
    }

    fun saveArticle(
        code: String,
        designation: String,
        unite: String,
        family: String,
        sousFamily: String,
        quantity: Double,
        unitPrice: Double,
        date: String
    ) {
        viewModelScope.launch {
            val articleToEdit = _uiState.value.articleToEdit
            if (articleToEdit == null) {
                database.appDatabaseQueries.insertStockArticle(
                    code_article = code,
                    designation = designation,
                    unite_quantite = unite,
                    family = family,
                    sous_family = sousFamily,
                    quantity = quantity,
                    unit_price = unitPrice,
                    expiration_date = null,
                    date = date
                )
            } else {
                database.appDatabaseQueries.updateStockArticle(
                    code_article = code,
                    designation = designation,
                    unite_quantite = unite,
                    family = family,
                    sous_family = sousFamily,
                    quantity = quantity,
                    unit_price = unitPrice,
                    expiration_date = null,
                    date = date,
                    id = articleToEdit.id
                )
            }
            closeAddEditDialog()
            loadArticlesSync()
        }
    }

    fun deleteArticle(id: Long) {
        viewModelScope.launch {
            database.appDatabaseQueries.deleteStockArticle(id)
            loadArticlesSync()
        }
    }
}
