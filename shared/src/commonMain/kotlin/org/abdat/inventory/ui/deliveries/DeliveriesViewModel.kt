package org.abdat.inventory.ui.deliveries

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
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.domain.model.Person
import org.abdat.inventory.domain.model.Service

data class DeliveryVerificationLine(
    val commandeItemId: Long,
    val codeArticle: String,
    val designation: String,
    val orderedQuantity: Double,
    val previouslyDeliveredQuantity: Double = 0.0,
    val remainingQuantity: Double = (orderedQuantity - previouslyDeliveredQuantity).coerceAtLeast(0.0),
    val receivedQuantity: Double = remainingQuantity,
    val lotNumber: String = "",
    val targetLocation: String = "Pharmacie Centrale - Réserve Générale",
    val unitPrice: Double = 0.0,
    val qualityCheckStatus: String = "Visuel OK"
) {
    val isConforming: Boolean get() = receivedQuantity == remainingQuantity
    val isDeficit: Boolean get() = receivedQuantity < remainingQuantity
    val isSurplus: Boolean get() = receivedQuantity > remainingQuantity
    val gap: Double get() = receivedQuantity - remainingQuantity
    val totalAmount: Double get() = receivedQuantity * unitPrice
}

data class DeliveryDisplayItem(
    val id: Long,
    val receptionNumber: String,
    val paperBlReference: String,
    val commandeReference: String,
    val supplierName: String,
    val supplierConvention: String,
    val serviceName: String = "Pharmacie Centrale",
    val beneficiaireName: String = "Personnel non spécifié",
    val chefDeServiceName: String = "",
    val dateLivraison: String,
    val itemsCount: Int,
    val totalAmountDzd: Double,
    val statusLabel: String = "VALIDÉ & EN STOCK"
)

enum class ReceptionType(val label: String) {
    COMPLETE("Réception Complète / Définitive"),
    PARTIAL("Réception Partielle avec Reliquat")
}

@Stable
data class DeliveriesUiState(
    val deliveries: ImmutableList<DeliveryDisplayItem> = persistentListOf(),
    val filteredDeliveries: ImmutableList<DeliveryDisplayItem> = persistentListOf(),
    val searchQuery: String = "",
    
    // KPI metrics
    val plannedDeliveriesCount: Int = 0,
    val conformingDeliveriesCount: Int = 0,
    val inControlDeliveriesCount: Int = 0,
    val monthlyVolumeDzd: Double = 0.0,
    val pendingReconciliationCount: Int = 0,
    val activeAnomaliesCount: Int = 0,
    
    // Inbound Form fields
    val paperBlReference: String = "BL-SCH-994182",
    val receptionDate: String = "2026-09-08",
    val availableCommandes: ImmutableList<BonDeCommande> = persistentListOf(),
    val selectedCommandeId: Long? = null,
    val selectedCommandeReference: String = "",
    val supplierName: String = "",
    val supplierConvention: String = "",
    val availableServices: ImmutableList<Service> = persistentListOf(),
    val selectedServiceId: Long? = null,
    val selectedServiceName: String = "",
    val availableBeneficiaries: ImmutableList<Person> = persistentListOf(),
    val selectedBeneficiaryId: Long? = null,
    val selectedBeneficiaryName: String = "",
    val chefDeServiceName: String = "",
    val receptionType: ReceptionType = ReceptionType.COMPLETE,
    
    // Quantitative Verification Lines for selected BC
    val verificationLines: ImmutableList<DeliveryVerificationLine> = persistentListOf(),
    
    // Discrepancy details
    val discrepancyNote: String = "",
    
    val totalCalculatedDzd: Double = 0.0,
    val isLoading: Boolean = false,
    val actionSuccessMessage: String? = null,
    val actionErrorMessage: String? = null
)

class DeliveriesViewModel(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveriesUiState())
    val uiState: StateFlow<DeliveriesUiState> = _uiState.asStateFlow()

    init {
        loadDeliveriesSync()

        viewModelScope.launch {
            combine(
                database.appDatabaseQueries.selectAllLivraisons().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllCommandes().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllFournisseurs().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllServices().asFlow().mapToList(dispatcher),
                database.appDatabaseQueries.selectAllPersons().asFlow().mapToList(dispatcher)
            ) { _, _, _, _, _ -> Unit }
                .collect {
                    loadDeliveriesSync()
                }
        }
    }

    fun loadDeliveries() {
        viewModelScope.launch {
            loadDeliveriesSync()
        }
    }

    fun loadDeliveriesSync() {
        val dbLivraisons = database.appDatabaseQueries.selectAllLivraisons().executeAsList()
        val dbCommandes = database.appDatabaseQueries.selectAllCommandes().executeAsList().map { it.toDomain() }
        val dbSuppliers = database.appDatabaseQueries.selectAllFournisseurs().executeAsList().map { it.toDomain() }
        val dbServices = database.appDatabaseQueries.selectAllServices().executeAsList().map { it.toDomain() }
        val dbPersons = database.appDatabaseQueries.selectAllPersons().executeAsList().map { it.toDomain() }

        val supplierMap = dbSuppliers.associateBy { it.id }
        val commandeMap = dbCommandes.associateBy { it.id }
        val serviceMap = dbServices.associateBy { it.id }
        val personMap = dbPersons.associateBy { it.id }

        val displayItems = dbLivraisons.map { bl ->
            val items = database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(bl.id).executeAsList()
            val totalAmount = items.sumOf { it.quantity * it.unit_price }
            val supplier = supplierMap[bl.fournisseur_id]
            val supplierName = supplier?.fullName ?: "Fournisseur Inconnu"
            val conventions = database.appDatabaseQueries.selectConventionsByFournisseur(bl.fournisseur_id).executeAsList()
            val conventionLabel = conventions.firstOrNull()?.let { "Convention CC-${it.date.take(4)}-${it.ord_number.toString().padStart(2, '0')}" } ?: "Marché Direct"
            val commandeRef = bl.commande_id?.let { commandeMap[it]?.referenceBon } ?: "Sans B.C."
            val receptionNumber = "REC-2026-${bl.id.toString().padStart(3, '0')}"

            val service = bl.service_id?.let { serviceMap[it] }
            val serviceName = service?.name ?: "Pharmacie Centrale & Distribution"
            val beneficiaire = bl.beneficiaire_id?.let { personMap[it] }
            val beneficiaireName = beneficiaire?.fullName ?: "Personnel non spécifié"

            val targetServiceId = bl.service_id ?: service?.id ?: dbServices.firstOrNull()?.id
            val targetService = targetServiceId?.let { serviceMap[it] }
            val chef = targetService?.chefId?.let { personMap[it] }
                ?: dbPersons.firstOrNull { it.serviceId == targetServiceId && it.isChef }
                ?: dbPersons.firstOrNull {
                    it.serviceId == targetServiceId &&
                            (it.notes.contains("Chef", ignoreCase = true) || it.notes.contains("Responsable", ignoreCase = true))
                } ?: dbPersons.firstOrNull { it.serviceId == targetServiceId }
            val chefName = chef?.let { "${it.fullName} (${it.notes.ifBlank { "Chef de Service" }})" } ?: "Dr. Amina Mansouri (Chef de Service)"

            DeliveryDisplayItem(
                id = bl.id,
                receptionNumber = receptionNumber,
                paperBlReference = bl.reference_bon,
                commandeReference = commandeRef,
                supplierName = supplierName,
                supplierConvention = conventionLabel,
                serviceName = serviceName,
                beneficiaireName = beneficiaireName,
                chefDeServiceName = chefName,
                dateLivraison = bl.date_livraison,
                itemsCount = items.size,
                totalAmountDzd = totalAmount,
                statusLabel = "VALIDÉ & EN STOCK"
            )
        }

        val totalVolume = displayItems.sumOf { it.totalAmountDzd }

        // Filter out orders that are already 100% fulfilled (Soldés / Clôturés)
        val pendingCommandes = dbCommandes.filter { cmd ->
            val items = database.appDatabaseQueries.selectCommandeItemsByCommandeId(cmd.id).executeAsList()
            if (items.isEmpty()) false
            else {
                val linkedBls = dbLivraisons.filter { it.commande_id == cmd.id }
                val deliveredItems = linkedBls.flatMap { database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(it.id).executeAsList() }
                val deliveredMap = mutableMapOf<String, Double>()
                deliveredItems.forEach { d ->
                    val key = if (d.code_article.isNotBlank()) d.code_article.lowercase().trim() else d.designation.lowercase().trim()
                    deliveredMap[key] = (deliveredMap[key] ?: 0.0) + d.quantity
                }
                items.any { item ->
                    val key = if (item.code_article.isNotBlank()) item.code_article.lowercase().trim() else item.designation.lowercase().trim()
                    val delivered = deliveredMap[key] ?: 0.0
                    (item.quantity - delivered) > 0.001
                }
            }
        }

        val initialCommandeId = _uiState.value.selectedCommandeId?.takeIf { id -> pendingCommandes.any { it.id == id } }
            ?: pendingCommandes.firstOrNull()?.id
            ?: dbCommandes.firstOrNull()?.id

        val initialServiceId = _uiState.value.selectedServiceId?.takeIf { id -> dbServices.any { it.id == id } }
            ?: dbServices.firstOrNull()?.id
        val serviceStaff = dbPersons.filter { it.serviceId == initialServiceId }
        val initialBeneficiaryId = _uiState.value.selectedBeneficiaryId?.takeIf { id -> serviceStaff.any { it.id == id } }
            ?: serviceStaff.firstOrNull()?.id
        val initialService = initialServiceId?.let { serviceMap[it] }
        val initialChef = initialService?.chefId?.let { personMap[it] }
            ?: serviceStaff.firstOrNull { it.isChef }
            ?: serviceStaff.firstOrNull {
                it.notes.contains("Chef", ignoreCase = true) || it.notes.contains("Responsable", ignoreCase = true)
            } ?: serviceStaff.firstOrNull()

        val initialServiceName = initialServiceId?.let { serviceMap[it]?.name } ?: ""
        val initialBeneficiaryName = initialBeneficiaryId?.let { personMap[it]?.fullName } ?: ""
        val initialChefName = initialChef?.let { "${it.fullName} (${it.notes.ifBlank { "Chef de Service" }})" } ?: "Non assigné"

        _uiState.update { current ->
            current.copy(
                deliveries = displayItems.toImmutableList(),
                availableCommandes = pendingCommandes.ifEmpty { dbCommandes }.toImmutableList(),
                availableServices = dbServices.toImmutableList(),
                selectedServiceId = initialServiceId,
                selectedServiceName = initialServiceName,
                availableBeneficiaries = serviceStaff.toImmutableList(),
                selectedBeneficiaryId = initialBeneficiaryId,
                selectedBeneficiaryName = initialBeneficiaryName,
                chefDeServiceName = initialChefName,
                plannedDeliveriesCount = (displayItems.size + pendingCommandes.size).coerceAtLeast(4),
                conformingDeliveriesCount = displayItems.size.coerceAtLeast(3),
                inControlDeliveriesCount = pendingCommandes.size.coerceAtLeast(1),
                monthlyVolumeDzd = totalVolume.takeIf { it > 0 } ?: 14850000.0,
                pendingReconciliationCount = pendingCommandes.size,
                activeAnomaliesCount = if (current.verificationLines.any { it.isDeficit }) 1 else 0,
                isLoading = false
            )
        }

        if (initialCommandeId != null) {
            selectPurchaseOrder(initialCommandeId)
        } else {
            applyFilters()
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()

        val filtered = state.deliveries.filter { item ->
            query.isEmpty() ||
                    item.receptionNumber.lowercase().contains(query) ||
                    item.paperBlReference.lowercase().contains(query) ||
                    item.commandeReference.lowercase().contains(query) ||
                    item.supplierName.lowercase().contains(query) ||
                    item.serviceName.lowercase().contains(query) ||
                    item.beneficiaireName.lowercase().contains(query) ||
                    item.chefDeServiceName.lowercase().contains(query)
        }

        _uiState.update { it.copy(filteredDeliveries = filtered.toImmutableList()) }
    }

    fun selectPurchaseOrder(bcId: Long) {
        val commande = database.appDatabaseQueries.selectCommandeById(bcId).executeAsOneOrNull() ?: return
        val supplier = database.appDatabaseQueries.selectFournisseurById(commande.fournisseur_id).executeAsOneOrNull()
        val conventions = database.appDatabaseQueries.selectConventionsByFournisseur(commande.fournisseur_id).executeAsList()
        val conventionRef = conventions.firstOrNull()?.let { "CC-${it.date.take(4)}-${it.ord_number.toString().padStart(2, '0')}" } ?: "Standard"

        val dbItems = database.appDatabaseQueries.selectCommandeItemsByCommandeId(bcId).executeAsList()

        // Aggregate previously delivered quantities for this order across past deliveries
        val pastDeliveries = database.appDatabaseQueries.selectAllLivraisons().executeAsList().filter { it.commande_id == bcId }
        val pastLivraisonItems = pastDeliveries.flatMap { bl ->
            database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(bl.id).executeAsList()
        }
        val deliveredQtyMap = mutableMapOf<String, Double>()
        pastLivraisonItems.forEach { d ->
            val key = if (d.code_article.isNotBlank()) d.code_article.lowercase().trim() else d.designation.lowercase().trim()
            deliveredQtyMap[key] = (deliveredQtyMap[key] ?: 0.0) + d.quantity
        }

        val verificationLines = dbItems.mapIndexed { index, item ->
            val defaultLoc = when {
                item.family.contains("Matériel", ignoreCase = true) -> "Allée B • C07"
                item.family.contains("Consommable", ignoreCase = true) -> "Allée A • R02"
                else -> "Allée B • C04"
            }
            val defaultLot = "LT-24${(10 + index)}-" + (item.code_article.takeLast(3).ifBlank { "001" })

            val key = if (item.code_article.isNotBlank()) item.code_article.lowercase().trim() else item.designation.lowercase().trim()
            val prevDelivered = deliveredQtyMap[key] ?: 0.0
            val remaining = (item.quantity - prevDelivered).coerceAtLeast(0.0)

            DeliveryVerificationLine(
                commandeItemId = item.id,
                codeArticle = item.code_article,
                designation = item.designation,
                orderedQuantity = item.quantity,
                previouslyDeliveredQuantity = prevDelivered,
                remainingQuantity = remaining,
                receivedQuantity = remaining,
                lotNumber = defaultLot,
                targetLocation = defaultLoc,
                unitPrice = item.unit_price,
                qualityCheckStatus = "Visuel OK"
            )
        }

        val totalCalculated = verificationLines.sumOf { it.totalAmount }
        val generatedBl = "BL-${supplier?.societe?.take(3)?.uppercase() ?: "DEP"}-${commande.reference_bon.takeLast(4)}"

        _uiState.update { current ->
            current.copy(
                selectedCommandeId = bcId,
                selectedCommandeReference = commande.reference_bon,
                supplierName = supplier?.full_name ?: "Fournisseur",
                supplierConvention = "Convention-cadre : $conventionRef",
                paperBlReference = current.paperBlReference.ifBlank { generatedBl },
                verificationLines = verificationLines.toImmutableList(),
                totalCalculatedDzd = totalCalculated,
                receptionType = ReceptionType.COMPLETE,
                discrepancyNote = "",
                activeAnomaliesCount = 0
            )
        }
        applyFilters()
    }

    fun updateReceivedQuantity(commandeItemId: Long, qty: Double) {
        val safeQty = qty.coerceAtLeast(0.0)
        _uiState.update { current ->
            val updatedLines = current.verificationLines.map { line ->
                if (line.commandeItemId == commandeItemId) {
                    val status = when {
                        safeQty == line.remainingQuantity -> "Visuel OK"
                        safeQty < line.remainingQuantity -> "Réserve Émise"
                        else -> "Surplus Constaté"
                    }
                    line.copy(receivedQuantity = safeQty, qualityCheckStatus = status)
                } else {
                    line
                }
            }
            val totalCalculated = updatedLines.sumOf { it.totalAmount }
            val hasDeficit = updatedLines.any { it.isDeficit }
            val deficitLine = updatedLines.firstOrNull { it.isDeficit }

            val note = if (deficitLine != null) {
                "Déficit quantitatif constaté sur ${deficitLine.designation} (${deficitLine.receivedQuantity.toInt()} reçus vs ${deficitLine.remainingQuantity.toInt()} attendus du reliquat). Réserve transporteur à apposer."
            } else {
                ""
            }

            current.copy(
                verificationLines = updatedLines.toImmutableList(),
                totalCalculatedDzd = totalCalculated,
                receptionType = if (hasDeficit) ReceptionType.PARTIAL else current.receptionType,
                discrepancyNote = note,
                activeAnomaliesCount = if (hasDeficit) 1 else 0
            )
        }
    }

    fun updateLotNumber(commandeItemId: Long, lot: String) {
        _uiState.update { current ->
            val updatedLines = current.verificationLines.map { line ->
                if (line.commandeItemId == commandeItemId) line.copy(lotNumber = lot) else line
            }
            current.copy(verificationLines = updatedLines.toImmutableList())
        }
    }

    fun updateTargetLocation(commandeItemId: Long, location: String) {
        _uiState.update { current ->
            val updatedLines = current.verificationLines.map { line ->
                if (line.commandeItemId == commandeItemId) line.copy(targetLocation = location) else line
            }
            current.copy(verificationLines = updatedLines.toImmutableList())
        }
    }

    fun updatePaperBlReference(reference: String) {
        _uiState.update { it.copy(paperBlReference = reference) }
    }

    fun updateReceptionDate(date: String) {
        _uiState.update { it.copy(receptionDate = date) }
    }

    fun selectService(serviceId: Long) {
        val allPersons = database.appDatabaseQueries.selectAllPersons().executeAsList().map { it.toDomain() }
        val servicePersons = allPersons.filter { it.serviceId == serviceId }
        val selectedPerson = servicePersons.firstOrNull()
        val svc = database.appDatabaseQueries.selectServiceById(serviceId).executeAsOneOrNull()
        val chef = servicePersons.firstOrNull { it.id == svc?.chef_id }
            ?: servicePersons.firstOrNull { it.isChef }
            ?: servicePersons.firstOrNull {
                it.notes.contains("Chef", ignoreCase = true) || it.notes.contains("Responsable", ignoreCase = true)
            } ?: servicePersons.firstOrNull()

        _uiState.update { current ->
            current.copy(
                selectedServiceId = serviceId,
                selectedServiceName = svc?.name ?: "",
                availableBeneficiaries = servicePersons.toImmutableList(),
                selectedBeneficiaryId = selectedPerson?.id,
                selectedBeneficiaryName = selectedPerson?.fullName ?: "",
                chefDeServiceName = chef?.let { "${it.fullName} (${it.notes.ifBlank { "Chef de Service" }})" } ?: "Non assigné"
            )
        }
    }

    fun selectBeneficiary(personId: Long) {
        val person = database.appDatabaseQueries.selectPersonById(personId).executeAsOneOrNull()
        _uiState.update { current ->
            current.copy(
                selectedBeneficiaryId = personId,
                selectedBeneficiaryName = person?.full_name ?: ""
            )
        }
    }

    fun updateReceptionType(type: ReceptionType) {
        _uiState.update { it.copy(receptionType = type) }
    }

    fun clearNotifications() {
        _uiState.update { it.copy(actionSuccessMessage = null, actionErrorMessage = null) }
    }

    fun validateDelivery(
        referenceBon: String,
        date: String,
        serviceId: Long? = _uiState.value.selectedServiceId,
        beneficiaireId: Long? = _uiState.value.selectedBeneficiaryId,
        isPartial: Boolean = _uiState.value.receptionType == ReceptionType.PARTIAL
    ) {
        viewModelScope.launch {
            validateDeliverySync(referenceBon, date, serviceId, beneficiaireId, isPartial)
        }
    }

    fun validateDeliverySync(
        referenceBon: String,
        date: String,
        serviceId: Long? = _uiState.value.selectedServiceId,
        beneficiaireId: Long? = _uiState.value.selectedBeneficiaryId,
        isPartial: Boolean = _uiState.value.receptionType == ReceptionType.PARTIAL
    ): Boolean {
        val state = _uiState.value
        val commandeId = state.selectedCommandeId

        if (referenceBon.isBlank()) {
            _uiState.update { it.copy(actionErrorMessage = "Le numéro de BL papier est obligatoire.") }
            return false
        }

        if (state.verificationLines.isEmpty()) {
            _uiState.update { it.copy(actionErrorMessage = "Aucun article à réceptionner pour cette livraison.") }
            return false
        }

        val receptionDate = date.ifBlank { "2026-09-08" }

        try {
            database.transaction {
                val supplierId = if (commandeId != null) {
                    val cmd = database.appDatabaseQueries.selectCommandeById(commandeId).executeAsOne()
                    cmd.fournisseur_id
                } else {
                    database.appDatabaseQueries.selectAllFournisseurs().executeAsList().first().id
                }

                // 1. Insert bon_de_livraison
                database.appDatabaseQueries.insertLivraison(
                    reference_bon = referenceBon.trim(),
                    fournisseur_id = supplierId,
                    date_livraison = receptionDate,
                    commande_id = commandeId,
                    service_id = serviceId,
                    beneficiaire_id = beneficiaireId
                )
                val livraisonId = database.appDatabaseQueries.selectLastInsertedRowId().executeAsOne()

                // 2. Insert items and increment physical stock
                state.verificationLines.forEach { line ->
                    database.appDatabaseQueries.insertLivraisonItem(
                        livraison_id = livraisonId,
                        code_article = line.codeArticle,
                        designation = line.designation,
                        quantity = line.receivedQuantity,
                        unit_price = line.unitPrice
                    )

                    // Increment physical stock article
                    if (line.codeArticle.isNotBlank()) {
                        val existing = database.appDatabaseQueries.selectStockArticleByCode(line.codeArticle).executeAsOneOrNull()
                        if (existing != null) {
                            database.appDatabaseQueries.increaseStockQuantity(
                                quantity = line.receivedQuantity,
                                date = receptionDate,
                                code_article = line.codeArticle
                            )
                        } else {
                            database.appDatabaseQueries.increaseStockQuantityByDesignation(
                                quantity = line.receivedQuantity,
                                date = receptionDate,
                                designation = line.designation
                            )
                        }
                    } else {
                        database.appDatabaseQueries.increaseStockQuantityByDesignation(
                            quantity = line.receivedQuantity,
                            date = receptionDate,
                            designation = line.designation
                        )
                    }
                }
            }

            _uiState.update {
                it.copy(
                    actionSuccessMessage = "Livraison $referenceBon validée et stock augmenté avec succès.",
                    actionErrorMessage = null,
                    paperBlReference = "BL-${(1000..9999).random()}"
                )
            }
            loadDeliveriesSync()
            return true
        } catch (e: Exception) {
            _uiState.update {
                it.copy(actionErrorMessage = "Erreur lors de la validation: ${e.message}")
            }
            return false
        }
    }
}
