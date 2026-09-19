package org.abdat.inventory.util

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
}

