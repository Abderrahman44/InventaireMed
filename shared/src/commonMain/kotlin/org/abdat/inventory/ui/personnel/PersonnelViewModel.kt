package org.abdat.inventory.ui.personnel

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
import org.abdat.inventory.domain.model.EquipmentAssignment
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.Service
import org.abdat.inventory.domain.model.StockArticle

@Stable
data class PersonnelUiState(
    val persons: ImmutableList<Person> = persistentListOf(),
    val filteredPersons: ImmutableList<Person> = persistentListOf(),
    val services: ImmutableList<Service> = persistentListOf(),
    val selectedServiceId: Long? = null,
    val searchQuery: String = "",
    val assignments: ImmutableList<EquipmentAssignment> = persistentListOf(),
    val selectedPerson: Person? = null,
    val personAssignments: ImmutableList<EquipmentAssignment> = persistentListOf(),
    val availableArticles: ImmutableList<StockArticle> = persistentListOf(),

    // Operational KPIs
    val totalStaffCount: Int = 0,
    val activeChefsCount: Int = 0,
    val totalAssignmentsCount: Int = 0,
    val activeAssignmentsCount: Int = 0,
    val returnedAssignmentsCount: Int = 0,

    // Dialog & Flow States
    val isAddPersonDialogOpen: Boolean = false,
    val isEditPersonDialogOpen: Boolean = false,
    val personPendingEdit: Person? = null,
    val isDeletePersonDialogOpen: Boolean = false,
    val personPendingDelete: Person? = null,
    val isAssignEquipmentDialogOpen: Boolean = false,
    val assignPersonTarget: Person? = null,
    val isReturnDialogOpen: Boolean = false,
    val assignmentPendingReturn: EquipmentAssignment? = null,

    // Feedback
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isLoading: Boolean = false
)

class PersonnelViewModel(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonnelUiState())
    val uiState: StateFlow<PersonnelUiState> = _uiState.asStateFlow()

    init {
        loadDataSync()

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllPersons().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllServices().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllAssignments().asFlow().mapToList(dispatcher)
            ) { _, _, _ -> Unit }
                .collect {
                    loadDataSync()
                }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            loadDataSync()
        }
    }

    fun loadDataSync() {
        val dbPersons = database.appDatabaseQueries.selectAllPersons().executeAsList().map { it.toDomain() }
        val dbServices = database.appDatabaseQueries.selectAllServices().executeAsList().map { it.toDomain() }
        val dbAssignments = database.appDatabaseQueries.selectAllAssignments().executeAsList().map { it.toDomain() }
        val dbArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .map { it.toDomain() }
            .filter { !it.isArchived && !it.isObsolete }

        val currentSelectedPersonId = _uiState.value.selectedPerson?.id
        val selectedPerson = if (currentSelectedPersonId != null) {
            dbPersons.firstOrNull { it.id == currentSelectedPersonId } ?: dbPersons.firstOrNull()
        } else {
            dbPersons.firstOrNull()
        }

        val personAssignments = if (selectedPerson != null) {
            dbAssignments.filter { it.personId == selectedPerson.id }
        } else {
            emptyList()
        }

        val activeCount = dbAssignments.count { it.status == "ACTIVE" }
        val returnedCount = dbAssignments.count { it.status == "RETURNED" }
        val chefsCount = dbPersons.count { it.isChef }

        _uiState.update { current ->
            current.copy(
                persons = dbPersons.toImmutableList(),
                services = dbServices.toImmutableList(),
                assignments = dbAssignments.toImmutableList(),
                availableArticles = dbArticles.toImmutableList(),
                selectedPerson = selectedPerson,
                personAssignments = personAssignments.toImmutableList(),
                totalStaffCount = dbPersons.size,
                activeChefsCount = chefsCount,
                totalAssignmentsCount = dbAssignments.size,
                activeAssignmentsCount = activeCount,
                returnedAssignmentsCount = returnedCount,
                isLoading = false
            )
        }
        applyFilters()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onServiceFilterChange(serviceId: Long?) {
        _uiState.update { it.copy(selectedServiceId = serviceId) }
        applyFilters()
    }

    fun selectPerson(person: Person?) {
        val assignments = if (person != null) {
            _uiState.value.assignments.filter { it.personId == person.id }
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                selectedPerson = person,
                personAssignments = assignments.toImmutableList()
            )
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val serviceId = state.selectedServiceId

        val filtered = state.persons.filter { person ->
            val matchesService = serviceId == null || person.serviceId == serviceId
            val matchesQuery = query.isEmpty() ||
                person.fullName.lowercase().contains(query) ||
                person.serviceName.lowercase().contains(query) ||
                person.notes.lowercase().contains(query)
            matchesService && matchesQuery
        }

        _uiState.update { it.copy(filteredPersons = filtered.toImmutableList()) }
    }

    // ------------------------------------------------------------------------
    // Staff Directory Operations
    // ------------------------------------------------------------------------

    fun addPersonSync(serviceId: Long, fullName: String, birthDate: String, notes: String): Boolean {
        if (fullName.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le nom complet du collaborateur est requis.") }
            return false
        }

        database.appDatabaseQueries.insertPerson(
            service_id = serviceId,
            full_name = fullName.trim(),
            birth_date = birthDate.trim(),
            notes = notes.trim()
        )

        loadDataSync()

        val newlyCreated = _uiState.value.persons.lastOrNull { it.fullName == fullName.trim() }
        if (newlyCreated != null) {
            selectPerson(newlyCreated)
        }

        _uiState.update {
            it.copy(
                isAddPersonDialogOpen = false,
                actionSuccessMessage = "Collaborateur « $fullName » ajouté avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun addPerson(serviceId: Long, fullName: String, birthDate: String, notes: String) {
        viewModelScope.launch {
            addPersonSync(serviceId, fullName, birthDate, notes)
        }
    }

    fun updatePersonSync(id: Long, serviceId: Long, fullName: String, birthDate: String, notes: String): Boolean {
        if (fullName.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le nom complet du collaborateur est requis.") }
            return false
        }

        database.appDatabaseQueries.updatePerson(
            service_id = serviceId,
            full_name = fullName.trim(),
            birth_date = birthDate.trim(),
            notes = notes.trim(),
            id = id
        )

        loadDataSync()

        _uiState.update {
            it.copy(
                isEditPersonDialogOpen = false,
                personPendingEdit = null,
                actionSuccessMessage = "Fiche collaborateur mise à jour.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun updatePerson(id: Long, serviceId: Long, fullName: String, birthDate: String, notes: String) {
        viewModelScope.launch {
            updatePersonSync(id, serviceId, fullName, birthDate, notes)
        }
    }

    fun deletePersonSync(id: Long): Boolean {
        val activeAssignments = _uiState.value.assignments.filter { it.personId == id && it.status == "ACTIVE" }
        if (activeAssignments.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    isDeletePersonDialogOpen = false,
                    personPendingDelete = null,
                    actionErrorMessage = "Impossible de supprimer ce collaborateur : ${activeAssignments.size} dotation(s) de matériel active(s) en cours."
                )
            }
            return false
        }

        return try {
            database.appDatabaseQueries.deletePerson(id)
            loadDataSync()
            _uiState.update {
                it.copy(
                    isDeletePersonDialogOpen = false,
                    personPendingDelete = null,
                    actionSuccessMessage = "Collaborateur retiré avec succès.",
                    actionErrorMessage = null
                )
            }
            true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isDeletePersonDialogOpen = false,
                    personPendingDelete = null,
                    actionErrorMessage = "Erreur lors de la suppression : ${e.message}"
                )
            }
            false
        }
    }

    fun deletePerson(id: Long) {
        viewModelScope.launch {
            deletePersonSync(id)
        }
    }

    fun toggleChefStatusSync(person: Person) {
        if (person.isChef) {
            database.appDatabaseQueries.clearServiceChef(person.serviceId)
            _uiState.update {
                it.copy(
                    actionSuccessMessage = "${person.fullName} n'est plus Chef de Service.",
                    actionErrorMessage = null
                )
            }
        } else {
            database.appDatabaseQueries.updateServiceChef(chef_id = person.id, id = person.serviceId)
            _uiState.update {
                it.copy(
                    actionSuccessMessage = "${person.fullName} est désigné Chef de Service pour « ${person.serviceName.ifBlank { "son service" }} ».",
                    actionErrorMessage = null
                )
            }
        }
        loadDataSync()
    }

    fun toggleChefStatus(person: Person) {
        viewModelScope.launch {
            toggleChefStatusSync(person)
        }
    }

    // ------------------------------------------------------------------------
    // Equipment Dotation Operations
    // ------------------------------------------------------------------------

    fun assignEquipmentSync(
        personId: Long,
        articleId: Long,
        quantity: Double,
        assignedDate: String,
        notes: String
    ): Boolean {
        if (quantity <= 0.0) {
            _uiState.update { it.copy(actionErrorMessage = "La quantité dotée doit être supérieure à 0.") }
            return false
        }

        val article = _uiState.value.availableArticles.firstOrNull { it.id == articleId }
        if (article == null) {
            _uiState.update { it.copy(actionErrorMessage = "Article introuvable dans le stock.") }
            return false
        }

        if (article.quantity < quantity) {
            _uiState.update {
                it.copy(
                    actionErrorMessage = "Stock insuffisant pour « ${article.designation} ». Disponible: ${article.quantity} ${article.uniteQuantite}."
                )
            }
            return false
        }

        database.transaction {
            database.appDatabaseQueries.insertEquipmentAssignment(
                person_id = personId,
                article_id = articleId,
                quantity = quantity,
                assigned_date = assignedDate.trim(),
                return_date = null,
                status = "ACTIVE",
                notes = notes.trim()
            )
            database.appDatabaseQueries.decreaseStockQuantityById(
                quantity = quantity,
                date = assignedDate.trim(),
                id = articleId
            )
        }

        loadDataSync()

        _uiState.update {
            it.copy(
                isAssignEquipmentDialogOpen = false,
                assignPersonTarget = null,
                actionSuccessMessage = "Dotation de ${quantity} ${article.uniteQuantite} « ${article.designation} » enregistrée avec succès.",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun assignEquipment(
        personId: Long,
        articleId: Long,
        quantity: Double,
        assignedDate: String,
        notes: String
    ) {
        viewModelScope.launch {
            assignEquipmentSync(personId, articleId, quantity, assignedDate, notes)
        }
    }

    fun returnEquipmentSync(assignmentId: Long, returnDate: String): Boolean {
        val assignment = _uiState.value.assignments.firstOrNull { it.id == assignmentId }
        if (assignment == null) {
            _uiState.update { it.copy(actionErrorMessage = "Dotation introuvable.") }
            return false
        }

        if (assignment.status == "RETURNED") {
            _uiState.update { it.copy(actionErrorMessage = "Cet équipement a déjà été restitué.") }
            return false
        }

        database.transaction {
            database.appDatabaseQueries.returnEquipmentAssignment(
                return_date = returnDate.trim(),
                id = assignmentId
            )
            database.appDatabaseQueries.increaseStockQuantityById(
                quantity = assignment.quantity,
                date = returnDate.trim(),
                id = assignment.articleId
            )
        }

        loadDataSync()

        _uiState.update {
            it.copy(
                isReturnDialogOpen = false,
                assignmentPendingReturn = null,
                actionSuccessMessage = "Restitution de l'équipement enregistrée (Stock réintégré: +${assignment.quantity} ${assignment.unite}).",
                actionErrorMessage = null
            )
        }
        return true
    }

    fun returnEquipment(assignmentId: Long, returnDate: String) {
        viewModelScope.launch {
            returnEquipmentSync(assignmentId, returnDate)
        }
    }

    // ------------------------------------------------------------------------
    // Dialog Triggers
    // ------------------------------------------------------------------------

    fun openAddPersonDialog() {
        _uiState.update { it.copy(isAddPersonDialogOpen = true, actionErrorMessage = null) }
    }

    fun closeAddPersonDialog() {
        _uiState.update { it.copy(isAddPersonDialogOpen = false) }
    }

    fun openEditPersonDialog(person: Person) {
        _uiState.update { it.copy(isEditPersonDialogOpen = true, personPendingEdit = person, actionErrorMessage = null) }
    }

    fun closeEditPersonDialog() {
        _uiState.update { it.copy(isEditPersonDialogOpen = false, personPendingEdit = null) }
    }

    fun openDeletePersonDialog(person: Person) {
        _uiState.update { it.copy(isDeletePersonDialogOpen = true, personPendingDelete = person) }
    }

    fun closeDeletePersonDialog() {
        _uiState.update { it.copy(isDeletePersonDialogOpen = false, personPendingDelete = null) }
    }

    fun openAssignEquipmentDialog(person: Person) {
        _uiState.update { it.copy(isAssignEquipmentDialogOpen = true, assignPersonTarget = person, actionErrorMessage = null) }
    }

    fun closeAssignEquipmentDialog() {
        _uiState.update { it.copy(isAssignEquipmentDialogOpen = false, assignPersonTarget = null) }
    }

    fun openReturnDialog(assignment: EquipmentAssignment) {
        _uiState.update { it.copy(isReturnDialogOpen = true, assignmentPendingReturn = assignment, actionErrorMessage = null) }
    }

    fun closeReturnDialog() {
        _uiState.update { it.copy(isReturnDialogOpen = false, assignmentPendingReturn = null) }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }
}
