package org.abdat.inventory.ui.consumption

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.abdat.inventory.domain.model.Service

@Immutable
data class ConsumptionItem(
    val id: Long,
    val sortieId: Long,
    val referenceBon: String,
    val date: String,
    val serviceId: Long,
    val serviceName: String,
    val localId: Long? = null,
    val localName: String = "",
    val codeArticle: String,
    val designation: String,
    val family: String = "",
    val sousFamily: String = "",
    val quantity: Double,
    val unitPrice: Double = 0.0,
    val totalValue: Double = quantity * unitPrice,
    val demandeur: String = "",
    val recepteurInterne: String = "",
    val motif: String = "",
    val chefService: String = ""
)

@Immutable
data class ConsumptionVoucherItem(
    val id: Long,
    val codeArticle: String,
    val designation: String,
    val family: String = "",
    val sousFamily: String = "",
    val quantity: Double,
    val unitPrice: Double = 0.0,
    val totalCost: Double = quantity * unitPrice
)

@Immutable
data class ConsumptionDetailRecord(
    val sortieId: Long,
    val referenceBon: String,
    val date: String,
    val serviceId: Long,
    val serviceName: String,
    val localName: String,
    val demandeur: String = "",
    val recepteurInterne: String = "",
    val motif: String = "",
    val chefService: String = "",
    val beneficiaireNames: String = "",
    val items: ImmutableList<ConsumptionVoucherItem> = persistentListOf(),
    val totalVoucherCost: Double = 0.0,
    val totalVoucherQuantity: Double = 0.0
)

@Immutable
data class ConsumptionFilterState(
    val dateFrom: String = "",
    val dateTo: String = "",
    val selectedServiceId: Long? = null,
    val selectedFamily: String? = null,
    val selectedSousFamily: String? = null,
    val searchQuery: String = ""
) {
    val hasActiveFilters: Boolean
        get() = dateFrom.isNotBlank() ||
                dateTo.isNotBlank() ||
                selectedServiceId != null ||
                !selectedFamily.isNullOrBlank() ||
                !selectedSousFamily.isNullOrBlank() ||
                searchQuery.isNotBlank()
}

@Immutable
data class ConsumptionUiState(
    val isLoading: Boolean = true,
    val allItems: ImmutableList<ConsumptionItem> = persistentListOf(),
    val filteredItems: ImmutableList<ConsumptionItem> = persistentListOf(),
    val filterState: ConsumptionFilterState = ConsumptionFilterState(),
    val services: ImmutableList<Service> = persistentListOf(),
    val families: ImmutableList<String> = persistentListOf(),
    val availableSousFamilies: ImmutableList<String> = persistentListOf(),
    val selectedDetailRecord: ConsumptionDetailRecord? = null,
    val totalLinesCount: Int = 0,
    val totalQuantitySum: Double = 0.0,
    val totalFinancialValueDzd: Double = 0.0,
    val isExportingPdf: Boolean = false,
    val exportSuccessMessage: String? = null,
    val exportErrorMessage: String? = null
)
