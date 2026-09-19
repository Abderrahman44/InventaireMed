package org.abdat.inventory.util

import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

expect object PdfExporter {
    fun generateStockReportPdf(
        filePath: String,
        reportTitle: String,
        dateRange: String,
        familiesFilter: String,
        serviceFilter: String,
        headers: List<String>,
        rows: List<List<String>>
    )

    fun generateSupplierDossierPdf(
        filePath: String,
        supplier: Fournisseur,
        conventions: List<Convention>,
        balancesMap: Map<Long, List<ConventionQuotaBalance>> = emptyMap()
    )

    fun generateConventionDetailPdf(
        filePath: String,
        convention: Convention,
        supplier: Fournisseur,
        balances: List<ConventionQuotaBalance>
    )
}

