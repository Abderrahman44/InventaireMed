package org.abdat.inventory.ui.config

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
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.BackupValidationResult
import org.abdat.inventory.database.DatabaseBackupService
import org.abdat.inventory.domain.model.CATEGORY_FAMILY
import org.abdat.inventory.domain.model.CATEGORY_SOUS_FAMILY
import org.abdat.inventory.domain.model.CategoryCodes
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.DynamicConfigItem
import org.abdat.inventory.domain.model.UserProfile
import org.abdat.inventory.domain.model.categoryLabelsMatch
import org.abdat.inventory.util.FileTypeFilter
import org.abdat.inventory.util.showOpenFileDialog
import org.abdat.inventory.util.showSaveFileDialog

enum class ConfigTab(val label: String) {
    TYPES("Listes & Unités"),
    THRESHOLDS("Seuils d'Alerte"),
    PREFIXES("Numérotation & Préfixes"),
    PROFILE("Profil Utilisateur"),
    BACKUP("Sauvegarde & Données")
}

@Stable
data class DynamicConfigUiState(
    val selectedTab: ConfigTab = ConfigTab.TYPES,
    val units: ImmutableList<DynamicConfigItem> = persistentListOf(),
    val families: ImmutableList<DynamicConfigItem> = persistentListOf(),
    val sousFamilies: ImmutableList<DynamicConfigItem> = persistentListOf(),
    val defaultMinQuantity: Double = 5.0,
    val prefixCommande: String = "CMD-2026-",
    val prefixLivraison: String = "BL-2026-",
    val prefixSortie: String = "BS-2026-",
    val prefixDecharge: String = "DCH-2026-",
    val userProfile: UserProfile = UserProfile(),

    // Backup & Restore
    val lastBackupDate: String? = null,
    val lastBackupPath: String? = null,
    val lastRestoreDate: String? = null,
    val isRestoreConfirmationDialogOpen: Boolean = false,
    val pendingRestoreFilePath: String? = null,
    val pendingRestoreValidationDetails: String? = null,
    val isBackupOperationInProgress: Boolean = false,

    // Dialog States
    val isAddOptionDialogOpen: Boolean = false,
    val targetCategoryForAdd: String = "UNIT",
    val isDeleteOptionDialogOpen: Boolean = false,
    val optionPendingDelete: DynamicConfigItem? = null,
    val isEditProfileDialogOpen: Boolean = false,

    // Feedback
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isLoading: Boolean = false
)

class DynamicConfigViewModel(
    private val database: AppDatabase,
    private val categoryRepository: CategoryRepository,
    private val backupService: DatabaseBackupService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(DynamicConfigUiState())
    val uiState: StateFlow<DynamicConfigUiState> = _uiState.asStateFlow()

    init {
        loadDataSync()

        // Families and sous-families are observed rather than loaded, so this screen's lists and every
        // other screen's dropdowns follow the same committed writes.
        onTaxonomy(categoryRepository.currentTaxonomy())
        viewModelScope.launch {
            categoryRepository.taxonomy.collect { onTaxonomy(it) }
        }
    }

    private fun onTaxonomy(taxonomy: CategoryTaxonomy) {
        _uiState.update {
            it.copy(families = taxonomy.allFamilies, sousFamilies = taxonomy.allSousFamilies)
        }
    }

    fun loadData() {
        viewModelScope.launch {
            loadDataSync()
        }
    }

    fun selectTab(tab: ConfigTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadDataSync() {
        val taxonomy = categoryRepository.currentTaxonomy()
        val allConfigItems = database.appDatabaseQueries.selectAllConfigItems().executeAsList().map { it.toDomain() }
        val units = allConfigItems.filter { it.category == "UNIT" }

        val minQtyStr = database.appDatabaseQueries.getMetadata("default_min_quantity").executeAsOneOrNull() ?: "5.0"
        val minQty = minQtyStr.toDoubleOrNull() ?: 5.0

        val pfxCmd = database.appDatabaseQueries.getMetadata("prefix_commande").executeAsOneOrNull() ?: "CMD-2026-"
        val pfxLiv = database.appDatabaseQueries.getMetadata("prefix_livraison").executeAsOneOrNull() ?: "BL-2026-"
        val pfxSortie = database.appDatabaseQueries.getMetadata("prefix_sortie").executeAsOneOrNull() ?: "BS-2026-"
        val pfxDecharge = database.appDatabaseQueries.getMetadata("prefix_decharge").executeAsOneOrNull() ?: "DCH-2026-"

        val dbProfile = database.appDatabaseQueries.getUserProfile().executeAsOneOrNull()
        val profile = dbProfile?.toDomain() ?: UserProfile()

        val lastBackupDate = database.appDatabaseQueries.getMetadata("last_backup_date").executeAsOneOrNull()
        val lastBackupPath = database.appDatabaseQueries.getMetadata("last_backup_path").executeAsOneOrNull()
        val lastRestoreDate = database.appDatabaseQueries.getMetadata("last_restore_date").executeAsOneOrNull()

        _uiState.update { current ->
            current.copy(
                units = units.toImmutableList(),
                families = taxonomy.allFamilies,
                sousFamilies = taxonomy.allSousFamilies,
                defaultMinQuantity = minQty,
                prefixCommande = pfxCmd,
                prefixLivraison = pfxLiv,
                prefixSortie = pfxSortie,
                prefixDecharge = pfxDecharge,
                userProfile = profile,
                lastBackupDate = lastBackupDate,
                lastBackupPath = lastBackupPath,
                lastRestoreDate = lastRestoreDate,
                isLoading = false
            )
        }
    }

    // ------------------------------------------------------------------------
    // Dynamic Dropdown Config Options
    // ------------------------------------------------------------------------

    fun addConfigItemSync(
        category: String,
        code: String,
        label: String,
        parentCategory: String = "",
        parentCode: String = ""
    ): Boolean {
        if (label.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le libellé ne peut pas être vide.") }
            return false
        }

        val normalizedCategory = category.trim().uppercase()
        val generatedCode = if (code.isBlank()) {
            CategoryCodes.fromLabel(label)
        } else {
            code.trim().uppercase()
        }

        // Validated against committed rows, not the observed lists, which may trail a write just made.
        val refusal = when (normalizedCategory) {
            CATEGORY_FAMILY -> familyAdditionRefusal(label, generatedCode)
            CATEGORY_SOUS_FAMILY -> sousFamilyAdditionRefusal(label, parentCode)
            else -> null
        }
        if (refusal != null) {
            _uiState.update { it.copy(actionErrorMessage = refusal, actionSuccessMessage = null) }
            return false
        }

        database.appDatabaseQueries.insertConfigItem(
            category = normalizedCategory,
            code = generatedCode,
            label = label.trim(),
            parent_category = if (normalizedCategory == CATEGORY_SOUS_FAMILY) parentCategory.trim().uppercase() else "",
            // Only a sous-family has a parent; the add dialog preselects one for every category, which
            // used to leave families carrying a meaningless parent_code. Taken verbatim from an existing
            // family's code: upper-casing again is not a guaranteed no-op for accented letters.
            parent_code = if (normalizedCategory == CATEGORY_SOUS_FAMILY) parentCode.trim() else "",
            is_active = 1L
        )

        categoryRepository.notifyTaxonomyChanged()
        loadDataSync()

        _uiState.update {
            it.copy(
                isAddOptionDialogOpen = false,
                actionSuccessMessage = "Option « $label » ajoutée avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun addConfigItem(
        category: String,
        code: String,
        label: String,
        parentCategory: String = "",
        parentCode: String = ""
    ) {
        viewModelScope.launch {
            addConfigItemSync(category, code, label, parentCategory, parentCode)
        }
    }

    private fun familyAdditionRefusal(label: String, code: String): String? {
        val existing = categoryRepository.currentTaxonomy().allFamilies.firstOrNull {
            categoryLabelsMatch(it.label, label) || it.code.equals(code, ignoreCase = true)
        } ?: return null
        return "La famille « ${label.trim()} » est en conflit avec la famille existante « ${existing.label} » " +
            "(code ${existing.code}). Choisissez un autre libellé ou un code identifiant distinct."
    }

    private fun sousFamilyAdditionRefusal(label: String, parentCode: String): String? {
        val taxonomy = categoryRepository.currentTaxonomy()
        val parent = taxonomy.allFamilies.firstOrNull { it.code.equals(parentCode.trim(), ignoreCase = true) }
            ?: return "Sélectionnez la famille parente de cette sous-famille."
        val duplicate = taxonomy.childrenOf(parent.code).any { categoryLabelsMatch(it.label, label) }
        return if (duplicate) "La sous-famille « ${label.trim()} » existe déjà dans la famille « ${parent.label} »." else null
    }

    /**
     * Why [item] cannot be deleted, or null when it can. Categories still referenced by articles —
     * archived and obsolete included — or still holding sous-families are protected, since articles
     * store category labels and would otherwise point at a category that no longer exists.
     */
    private fun deletionRefusal(item: DynamicConfigItem): String? {
        if (item.category != CATEGORY_FAMILY && item.category != CATEGORY_SOUS_FAMILY) return null

        val taxonomy = categoryRepository.currentTaxonomy()
        val articleCount = taxonomy.articleCountFor(item, categoryRepository.articleCategoryUsage())
        val noun = if (item.category == CATEGORY_FAMILY) "cette famille" else "cette sous-famille"
        if (articleCount > 0) {
            return "Impossible de supprimer « ${item.label} » : $articleCount article(s) utilisent $noun."
        }

        if (item.category == CATEGORY_FAMILY) {
            val childCount = taxonomy.childrenOf(item.code).size
            if (childCount > 0) {
                return "Impossible de supprimer « ${item.label} » : elle contient $childCount sous-famille(s). " +
                    "Supprimez-les ou réaffectez-les d'abord."
            }
        }
        return null
    }

    fun deleteConfigItemSync(id: Long): Boolean {
        return try {
            // Check and delete in one transaction so no article can take the label in between.
            val refusal = database.transactionWithResult {
                val item = database.appDatabaseQueries.selectAllConfigItems().executeAsList()
                    .firstOrNull { it.id == id }?.toDomain()
                val reason = item?.let { deletionRefusal(it) }
                if (reason == null) database.appDatabaseQueries.deleteConfigItem(id)
                reason
            }

            if (refusal != null) {
                _uiState.update {
                    it.copy(
                        isDeleteOptionDialogOpen = false,
                        optionPendingDelete = null,
                        actionSuccessMessage = null,
                        actionErrorMessage = refusal
                    )
                }
                return false
            }

            categoryRepository.notifyTaxonomyChanged()
            loadDataSync()
            _uiState.update {
                it.copy(
                    isDeleteOptionDialogOpen = false,
                    optionPendingDelete = null,
                    actionSuccessMessage = "Option supprimée.",
                    actionErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isDeleteOptionDialogOpen = false,
                    optionPendingDelete = null,
                    actionErrorMessage = "Erreur lors de la suppression : ${e.message}"
                )
            }
            false
        }
    }

    fun deleteConfigItem(id: Long) {
        viewModelScope.launch {
            deleteConfigItemSync(id)
        }
    }

    // ------------------------------------------------------------------------
    // Global Threshold & Document Prefixes
    // ------------------------------------------------------------------------

    fun saveGlobalMinQuantitySync(threshold: Double): Boolean {
        if (threshold < 0) {
            _uiState.update { it.copy(actionErrorMessage = "Le seuil minimum ne peut pas être négatif.") }
            return false
        }

        database.appDatabaseQueries.setMetadata("default_min_quantity", threshold.toString())
        loadDataSync()

        _uiState.update {
            it.copy(
                actionSuccessMessage = "Seuil global d'alerte configuré à $threshold unités.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun saveGlobalMinQuantity(threshold: Double) {
        viewModelScope.launch {
            saveGlobalMinQuantitySync(threshold)
        }
    }

    fun savePrefixesSync(cmd: String, bl: String, bs: String, dch: String): Boolean {
        database.transaction {
            database.appDatabaseQueries.setMetadata("prefix_commande", cmd.trim())
            database.appDatabaseQueries.setMetadata("prefix_livraison", bl.trim())
            database.appDatabaseQueries.setMetadata("prefix_sortie", bs.trim())
            database.appDatabaseQueries.setMetadata("prefix_decharge", dch.trim())
        }

        loadDataSync()

        _uiState.update {
            it.copy(
                actionSuccessMessage = "Préfixes de numérotation automatique mis à jour avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun savePrefixes(cmd: String, bl: String, bs: String, dch: String) {
        viewModelScope.launch {
            savePrefixesSync(cmd, bl, bs, dch)
        }
    }

    // ------------------------------------------------------------------------
    // User Profile Management
    // ------------------------------------------------------------------------

    fun saveUserProfileSync(
        fullName: String,
        phone: String,
        email: String,
        role: String,
        themeMode: String = _uiState.value.userProfile.themeMode,
        locale: String = _uiState.value.userProfile.locale
    ): Boolean {
        if (fullName.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le nom complet est obligatoire.") }
            return false
        }

        database.appDatabaseQueries.insertOrUpdateUserProfile(
            full_name = fullName.trim(),
            phone = phone.trim(),
            email = email.trim(),
            role = role.trim(),
            theme_mode = themeMode,
            locale = locale
        )

        loadDataSync()

        _uiState.update {
            it.copy(
                isEditProfileDialogOpen = false,
                actionSuccessMessage = "Profil utilisateur mis à jour.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun saveUserProfile(
        fullName: String,
        phone: String,
        email: String,
        role: String
    ) {
        viewModelScope.launch {
            saveUserProfileSync(fullName, phone, email, role)
        }
    }

    // ------------------------------------------------------------------------
    // Dialog Controls
    // ------------------------------------------------------------------------

    fun openAddOptionDialog(category: String) {
        _uiState.update { it.copy(isAddOptionDialogOpen = true, targetCategoryForAdd = category, actionErrorMessage = null) }
    }

    fun closeAddOptionDialog() {
        _uiState.update { it.copy(isAddOptionDialogOpen = false) }
    }

    fun openDeleteOptionDialog(item: DynamicConfigItem) {
        _uiState.update { it.copy(isDeleteOptionDialogOpen = true, optionPendingDelete = item) }
    }

    fun closeDeleteOptionDialog() {
        _uiState.update { it.copy(isDeleteOptionDialogOpen = false, optionPendingDelete = null) }
    }

    fun openEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = true, actionErrorMessage = null) }
    }

    fun closeEditProfileDialog() {
        _uiState.update { it.copy(isEditProfileDialogOpen = false) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }

    // ------------------------------------------------------------------------
    // Backup & Restore Operations
    // ------------------------------------------------------------------------

    fun exportBackup() {
        viewModelScope.launch {
            exportBackupSync()
        }
    }

    suspend fun exportBackupSync(): Boolean {
        if (backupService == null) {
            _uiState.update { it.copy(actionErrorMessage = "Le service de sauvegarde n'est pas disponible.") }
            return false
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val datePart = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
        val defaultName = "inventairemed_backup_$datePart.db"

        val targetPath = showSaveFileDialog(
            title = "Exporter une sauvegarde de la base de données",
            defaultFileName = defaultName,
            filters = FileTypeFilter.BACKUP_FILTERS
        ) ?: return false

        _uiState.update {
            it.copy(isBackupOperationInProgress = true, actionErrorMessage = null, actionSuccessMessage = null)
        }

        val result = backupService.exportBackup(targetPath)
        return result.fold(
            onSuccess = { exportResult ->
                loadDataSync()
                val sizeKb = (exportResult.fileSizeInBytes / 1024).coerceAtLeast(1)
                _uiState.update {
                    it.copy(
                        isBackupOperationInProgress = false,
                        actionSuccessMessage = "Sauvegarde exportée avec succès ($sizeKb Ko).",
                        actionErrorMessage = null
                    )
                }
                true
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        isBackupOperationInProgress = false,
                        actionErrorMessage = "Échec de l'exportation : ${error.message}",
                        actionSuccessMessage = null
                    )
                }
                false
            }
        )
    }

    fun requestImportBackup() {
        viewModelScope.launch {
            requestImportBackupSync()
        }
    }

    suspend fun requestImportBackupSync() {
        if (backupService == null) {
            _uiState.update { it.copy(actionErrorMessage = "Le service de sauvegarde n'est pas disponible.") }
            return
        }

        val selectedPath = showOpenFileDialog(
            title = "Sélectionner un fichier de sauvegarde à restaurer",
            filters = FileTypeFilter.BACKUP_FILTERS
        ) ?: return

        val validation = backupService.validateBackupFile(selectedPath)
        when (validation) {
            is BackupValidationResult.Valid -> {
                _uiState.update {
                    it.copy(
                        isRestoreConfirmationDialogOpen = true,
                        pendingRestoreFilePath = selectedPath,
                        pendingRestoreValidationDetails = validation.details,
                        actionErrorMessage = null
                    )
                }
            }
            is BackupValidationResult.Invalid -> {
                _uiState.update {
                    it.copy(
                        actionErrorMessage = "Fichier de sauvegarde invalide : ${validation.reason}",
                        actionSuccessMessage = null
                    )
                }
            }
        }
    }

    fun confirmRestore() {
        viewModelScope.launch {
            val path = _uiState.value.pendingRestoreFilePath ?: return@launch
            _uiState.update {
                it.copy(
                    isRestoreConfirmationDialogOpen = false,
                    isBackupOperationInProgress = true,
                    actionErrorMessage = null,
                    actionSuccessMessage = null
                )
            }

            val result = backupService?.restoreBackup(path)
            if (result != null && result.isSuccess) {
                loadDataSync()
                _uiState.update {
                    it.copy(
                        isBackupOperationInProgress = false,
                        pendingRestoreFilePath = null,
                        pendingRestoreValidationDetails = null,
                        actionSuccessMessage = "Base de données restaurée avec succès.",
                        actionErrorMessage = null
                    )
                }
            } else {
                val errorMsg = result?.exceptionOrNull()?.message ?: "Erreur inconnue lors de la restauration."
                _uiState.update {
                    it.copy(
                        isBackupOperationInProgress = false,
                        pendingRestoreFilePath = null,
                        pendingRestoreValidationDetails = null,
                        actionErrorMessage = "Échec de la restauration : $errorMsg",
                        actionSuccessMessage = null
                    )
                }
            }
        }
    }

    fun cancelRestore() {
        _uiState.update {
            it.copy(
                isRestoreConfirmationDialogOpen = false,
                pendingRestoreFilePath = null,
                pendingRestoreValidationDetails = null
            )
        }
    }
}
