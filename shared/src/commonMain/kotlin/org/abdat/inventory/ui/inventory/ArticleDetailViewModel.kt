package org.abdat.inventory.ui.inventory

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
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.ArticleAuditLog
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.StockArticle

@Stable
data class ArticleDetailUiState(
    val article: StockArticle? = null,
    val auditLogs: ImmutableList<ArticleAuditLog> = persistentListOf(),
    val isLoading: Boolean = false,
    val isEditDialogOpen: Boolean = false,
    val taxonomy: CategoryTaxonomy = CategoryTaxonomy(),
    val errorMessage: String? = null
)

class ArticleDetailViewModel(
    private val database: AppDatabase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArticleDetailUiState(taxonomy = categoryRepository.currentTaxonomy()))
    val uiState: StateFlow<ArticleDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.taxonomy.collect { taxonomy ->
                _uiState.update { it.copy(taxonomy = taxonomy) }
            }
        }
    }

    fun loadArticle(articleId: Long) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val dbArticle = database.appDatabaseQueries.selectStockArticleById(articleId).executeAsOneOrNull()
            if (dbArticle != null) {
                val domainArticle = dbArticle.toDomain()
                val dbLogs = database.appDatabaseQueries.selectAuditLogsByArticleId(articleId).executeAsList()
                val domainLogs = dbLogs.map { it.toDomain(domainArticle.designation, domainArticle.codeArticle) }
                _uiState.update {
                    it.copy(
                        article = domainArticle,
                        auditLogs = domainLogs.toImmutableList(),
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Article introuvable") }
            }
        }
    }

    fun openEditDialog() {
        _uiState.update { it.copy(isEditDialogOpen = true, errorMessage = null) }
    }

    fun closeEditDialog() {
        _uiState.update { it.copy(isEditDialogOpen = false, errorMessage = null) }
    }

    fun saveArticleModifications(
        designation: String,
        additionalReferenceId: String,
        unite: String,
        family: String,
        sousFamily: String,
        unitPrice: Double,
        expirationDate: String?,
        reason: String,
        currentDate: String
    ) {
        val currentArticle = _uiState.value.article ?: return
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Le motif de modification est obligatoire") }
            return
        }

        // Same rule as the inventory dialog: a configured active pair, checked against committed rows,
        // stored with the configured labels.
        val resolved = categoryRepository.currentTaxonomy().resolvePair(family, sousFamily)
        if (resolved == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Famille « $family » / sous-famille « $sousFamily » non configurée. " +
                        "Sélectionnez une catégorie existante ou ajoutez-la dans Paramètres & Config."
                )
            }
            return
        }
        val familyLabel = resolved.first.label
        val sousFamilyLabel = resolved.second.label

        viewModelScope.launch {
            database.appDatabaseQueries.updateStockArticle(
                code_article = currentArticle.codeArticle,
                additional_reference_id = additionalReferenceId,
                designation = designation,
                unite_quantite = unite,
                family = familyLabel,
                sous_family = sousFamilyLabel,
                quantity = currentArticle.quantity,
                unit_price = unitPrice,
                expiration_date = expirationDate,
                is_archived = if (currentArticle.isArchived) 1L else 0L,
                is_obsolete = if (currentArticle.isObsolete) 1L else 0L,
                date = currentDate,
                id = currentArticle.id
            )

            // Insert detailed audit entry
            val oldVal = "${currentArticle.designation} | Ref: ${currentArticle.additionalReferenceId} | PU: ${currentArticle.unitPrice} DZD" +
                " | Fam: ${currentArticle.family} / ${currentArticle.sousFamily}"
            val newVal = "$designation | Ref: $additionalReferenceId | PU: $unitPrice DZD | Fam: $familyLabel / $sousFamilyLabel"
            database.appDatabaseQueries.insertArticleAuditLog(
                article_id = currentArticle.id,
                field_name = "fiche_technique",
                old_value = oldVal,
                new_value = newVal,
                reason = reason.trim(),
                user_name = "Responsable Stock",
                edit_date = currentDate
            )

            closeEditDialog()
            loadArticle(currentArticle.id)
        }
    }

    fun toggleArchive() {
        val current = _uiState.value.article ?: return
        viewModelScope.launch {
            if (current.isArchived) {
                database.appDatabaseQueries.unarchiveStockArticle(current.id)
            } else {
                database.appDatabaseQueries.archiveStockArticle(current.id)
            }
            loadArticle(current.id)
        }
    }

    fun markObsolete() {
        val current = _uiState.value.article ?: return
        viewModelScope.launch {
            database.appDatabaseQueries.markStockArticleObsolete(current.id)
            loadArticle(current.id)
        }
    }
}
