package org.abdat.inventory.ui.services

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
import org.abdat.inventory.domain.model.Local
import org.abdat.inventory.domain.model.Service

@Stable
data class ServicesUiState(
    val services: ImmutableList<Service> = persistentListOf(),
    val filteredServices: ImmutableList<Service> = persistentListOf(),
    val selectedService: Service? = null,
    val localsForSelectedService: ImmutableList<Local> = persistentListOf(),
    val localCountsByServiceId: Map<Long, Int> = emptyMap(),
    val sortieCountsByServiceId: Map<Long, Int> = emptyMap(),
    val searchQuery: String = "",

    // Operational KPI Metrics
    val totalServicesCount: Int = 0,
    val totalLocalsCount: Int = 0,
    val totalSortiesCount: Int = 0,
    val assignmentRatePercent: Int = 100,

    // Dialog & Action States
    val isAddServiceDialogOpen: Boolean = false,
    val isEditServiceDialogOpen: Boolean = false,
    val isDeleteServiceDialogOpen: Boolean = false,
    val servicePendingDelete: Service? = null,

    val isAddLocalDialogOpen: Boolean = false,
    val isEditLocalDialogOpen: Boolean = false,
    val localPendingEdit: Local? = null,
    val isDeleteLocalDialogOpen: Boolean = false,
    val localPendingDelete: Local? = null,

    // Feedback notifications
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isLoading: Boolean = false
)

class ServicesViewModel(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesUiState())
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    init {
        loadServicesSync()

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllServices().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllLocals().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllSorties().asFlow().mapToList(dispatcher)
            ) { _, _, _ -> Unit }
                .collect {
                    loadServicesSync()
                }
        }
    }

    fun loadServices() {
        viewModelScope.launch {
            loadServicesSync()
        }
    }

    fun loadServicesSync() {
        val dbServices = database.appDatabaseQueries.selectAllServices().executeAsList().map { it.toDomain() }
        val dbLocals = database.appDatabaseQueries.selectAllLocals().executeAsList().map { it.toDomain() }
        val dbSorties = database.appDatabaseQueries.selectAllSorties().executeAsList()

        val localCounts = dbLocals.groupBy { it.serviceId }.mapValues { it.value.size }
        val sortieCounts = dbSorties.groupBy { it.service_id }.mapValues { it.value.size }

        val previousSelected = _uiState.value.selectedService
        val selectedService = if (previousSelected != null && dbServices.any { it.id == previousSelected.id }) {
            dbServices.first { it.id == previousSelected.id }
        } else {
            dbServices.firstOrNull()
        }

        val localsForSelected = if (selectedService != null) {
            dbLocals.filter { it.serviceId == selectedService.id }
        } else {
            emptyList()
        }

        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) {
            dbServices
        } else {
            dbServices.filter { service ->
                service.name.contains(query, ignoreCase = true) ||
                    service.localNum.contains(query, ignoreCase = true) ||
                    service.id.toString().contains(query)
            }
        }

        val servicesWithLocalsCount = dbServices.count { srv -> (localCounts[srv.id] ?: 0) > 0 }
        val assignmentRate = if (dbServices.isNotEmpty()) {
            ((servicesWithLocalsCount.toDouble() / dbServices.size.toDouble()) * 100.0).toInt()
        } else {
            100
        }

        _uiState.update { current ->
            current.copy(
                services = dbServices.toImmutableList(),
                filteredServices = filtered.toImmutableList(),
                selectedService = selectedService,
                localsForSelectedService = localsForSelected.toImmutableList(),
                localCountsByServiceId = localCounts,
                sortieCountsByServiceId = sortieCounts,
                totalServicesCount = dbServices.size,
                totalLocalsCount = dbLocals.size,
                totalSortiesCount = dbSorties.size,
                assignmentRatePercent = assignmentRate,
                isLoading = false
            )
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val filtered = if (query.isBlank()) {
                current.services
            } else {
                current.services.filter { service ->
                    service.name.contains(query, ignoreCase = true) ||
                        service.localNum.contains(query, ignoreCase = true) ||
                        service.id.toString().contains(query)
                }
            }
            current.copy(
                searchQuery = query,
                filteredServices = filtered.toImmutableList()
            )
        }
    }

    fun selectService(service: Service) {
        val dbLocals = database.appDatabaseQueries.selectLocalsByServiceId(service.id).executeAsList().map { it.toDomain() }
        _uiState.update { current ->
            current.copy(
                selectedService = service,
                localsForSelectedService = dbLocals.toImmutableList()
            )
        }
    }

    // ------------------------------------------------------------------------
    // Service Operations
    // ------------------------------------------------------------------------

    fun addServiceSync(name: String, localNum: String): Boolean {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le nom du service ne peut pas être vide.") }
            return false
        }

        database.appDatabaseQueries.insertService(
            name = name.trim(),
            local_num = localNum.trim()
        )

        loadServicesSync()

        // Auto select the newly inserted service
        val newlyCreated = _uiState.value.services.lastOrNull { it.name == name.trim() }
        if (newlyCreated != null) {
            selectService(newlyCreated)
        }

        _uiState.update {
            it.copy(
                isAddServiceDialogOpen = false,
                actionSuccessMessage = "Service « $name » créé avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun addService(name: String, localNum: String) {
        viewModelScope.launch {
            addServiceSync(name, localNum)
        }
    }

    fun updateServiceSync(id: Long, name: String, localNum: String): Boolean {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le nom du service ne peut pas être vide.") }
            return false
        }

        database.appDatabaseQueries.updateService(
            name = name.trim(),
            local_num = localNum.trim(),
            id = id
        )

        loadServicesSync()

        _uiState.update {
            it.copy(
                isEditServiceDialogOpen = false,
                actionSuccessMessage = "Service mis à jour avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun updateService(id: Long, name: String, localNum: String) {
        viewModelScope.launch {
            updateServiceSync(id, name, localNum)
        }
    }

    fun deleteServiceSync(id: Long): Boolean {
        // Enforce referential integrity: Check if service has issue vouchers (bon_de_sortie)
        val linkedSorties = database.appDatabaseQueries.selectSortiesByServiceId(id).executeAsList()
        if (linkedSorties.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    isDeleteServiceDialogOpen = false,
                    servicePendingDelete = null,
                    actionErrorMessage = "Impossible de supprimer ce service car il est référencé dans ${linkedSorties.size} bon(s) de sortie."
                )
            }
            return false
        }

        return try {
            database.appDatabaseQueries.deleteService(id)
            loadServicesSync()
            _uiState.update {
                it.copy(
                    isDeleteServiceDialogOpen = false,
                    servicePendingDelete = null,
                    actionSuccessMessage = "Service supprimé avec succès.",
                    actionErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isDeleteServiceDialogOpen = false,
                    servicePendingDelete = null,
                    actionErrorMessage = "Erreur lors de la suppression du service: ${e.message}"
                )
            }
            false
        }
    }

    fun deleteService(id: Long) {
        viewModelScope.launch {
            deleteServiceSync(id)
        }
    }

    // ------------------------------------------------------------------------
    // Local Room Operations
    // ------------------------------------------------------------------------

    fun addLocalSync(serviceId: Long, name: String): Boolean {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "La désignation du local ne peut pas être vide.") }
            return false
        }

        database.appDatabaseQueries.insertLocal(
            service_id = serviceId,
            name = name.trim()
        )

        loadServicesSync()

        _uiState.update {
            it.copy(
                isAddLocalDialogOpen = false,
                actionSuccessMessage = "Local « $name » ajouté avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun addLocal(serviceId: Long, name: String) {
        viewModelScope.launch {
            addLocalSync(serviceId, name)
        }
    }

    fun updateLocalSync(id: Long, serviceId: Long, name: String): Boolean {
        if (name.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "La désignation du local ne peut pas être vide.") }
            return false
        }

        database.appDatabaseQueries.updateLocal(
            service_id = serviceId,
            name = name.trim(),
            id = id
        )

        loadServicesSync()

        _uiState.update {
            it.copy(
                isEditLocalDialogOpen = false,
                localPendingEdit = null,
                actionSuccessMessage = "Local mis à jour avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun updateLocal(id: Long, serviceId: Long, name: String) {
        viewModelScope.launch {
            updateLocalSync(id, serviceId, name)
        }
    }

    fun deleteLocalSync(id: Long): Boolean {
        return try {
            database.appDatabaseQueries.deleteLocal(id)
            loadServicesSync()
            _uiState.update {
                it.copy(
                    isDeleteLocalDialogOpen = false,
                    localPendingDelete = null,
                    actionSuccessMessage = "Local supprimé avec succès.",
                    actionErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isDeleteLocalDialogOpen = false,
                    localPendingDelete = null,
                    actionErrorMessage = "Erreur lors de la suppression du local: ${e.message}"
                )
            }
            false
        }
    }

    fun deleteLocal(id: Long) {
        viewModelScope.launch {
            deleteLocalSync(id)
        }
    }

    // ------------------------------------------------------------------------
    // Dialog Controls & Feedback
    // ------------------------------------------------------------------------

    fun openAddServiceDialog() {
        _uiState.update { it.copy(isAddServiceDialogOpen = true, actionErrorMessage = null) }
    }

    fun closeAddServiceDialog() {
        _uiState.update { it.copy(isAddServiceDialogOpen = false) }
    }

    fun openEditServiceDialog(service: Service) {
        _uiState.update { it.copy(isEditServiceDialogOpen = true, actionErrorMessage = null) }
    }

    fun closeEditServiceDialog() {
        _uiState.update { it.copy(isEditServiceDialogOpen = false) }
    }

    fun openDeleteServiceDialog(service: Service) {
        _uiState.update { it.copy(isDeleteServiceDialogOpen = true, servicePendingDelete = service) }
    }

    fun closeDeleteServiceDialog() {
        _uiState.update { it.copy(isDeleteServiceDialogOpen = false, servicePendingDelete = null) }
    }

    fun openAddLocalDialog() {
        _uiState.update { it.copy(isAddLocalDialogOpen = true, actionErrorMessage = null) }
    }

    fun closeAddLocalDialog() {
        _uiState.update { it.copy(isAddLocalDialogOpen = false) }
    }

    fun openEditLocalDialog(local: Local) {
        _uiState.update { it.copy(isEditLocalDialogOpen = true, localPendingEdit = local, actionErrorMessage = null) }
    }

    fun closeEditLocalDialog() {
        _uiState.update { it.copy(isEditLocalDialogOpen = false, localPendingEdit = null) }
    }

    fun openDeleteLocalDialog(local: Local) {
        _uiState.update { it.copy(isDeleteLocalDialogOpen = true, localPendingDelete = local) }
    }

    fun closeDeleteLocalDialog() {
        _uiState.update { it.copy(isDeleteLocalDialogOpen = false, localPendingDelete = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }
}
