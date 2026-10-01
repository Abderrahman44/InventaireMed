package org.abdat.inventory.util

import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

expect object PdfExporter {
    fun generateStockReportPdf(
        filePath: String,
        reportTitle: String,
        dateRange: String,
        familiesFilter: String,
        serviceFilter: String,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )

    fun generateSupplierDossierPdf(
        filePath: String,
        supplier: Fournisseur,
        conventions: List<Convention>,
        balancesMap: Map<Long, List<ConventionQuotaBalance>> = emptyMap(),
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )

    fun generateConventionDetailPdf(
        filePath: String,
        convention: Convention,
        supplier: Fournisseur,
        balances: List<ConventionQuotaBalance>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )

    fun generateGenericTablePdf(
        filePath: String,
        documentTitle: String,
        subtitle: String = "",
        metadata: Map<String, String> = emptyMap(),
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )
}
