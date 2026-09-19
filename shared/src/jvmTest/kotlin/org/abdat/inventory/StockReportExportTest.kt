package org.abdat.inventory

import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.ui.inventory.ReportColumn
import org.abdat.inventory.ui.inventory.StockReportDataMapper
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.PdfExporter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StockReportExportTest {

    private fun sampleArticles(): List<StockArticle> = listOf(
        StockArticle(
            id = 1,
            codeArticle = "MED-001",
            additionalReferenceId = "1234567890",
            designation = "Paracétamol 500mg",
            family = "Médicaments",
            sousFamily = "Antalgiques",
            quantity = 50.0,
            uniteQuantite = "Boîte",
            unitPrice = 120.0,
            date = "2026-01-01",
            expirationDate = "2027-12-31"
        ),
        StockArticle(
            id = 2,
            codeArticle = "CON-001",
            additionalReferenceId = "",
            designation = "Gants Latex",
            family = "Consommables",
            sousFamily = "",
            quantity = 3.0, // Low stock (<= 5)
            uniteQuantite = "Boîte",
            unitPrice = 45.0,
            date = "2026-01-01"
        ),
        StockArticle(
            id = 3,
            codeArticle = "MAT-001",
            additionalReferenceId = "",
            designation = "Tensiomètre",
            family = "",
            sousFamily = "",
            quantity = 0.0, // Out of stock (<= 0)
            uniteQuantite = "Pièce",
            unitPrice = 3500.0,
            date = "2026-01-01"
        )
    )

    @Test
    fun testStockReportDataMapperAllColumns() {
        val articles = sampleArticles()
        val allColumnsMap = ReportColumn.entries.associate { it.id to true }

        val (headers, rows) = StockReportDataMapper.toHeadersAndRows(articles, allColumnsMap)

        assertEquals(ReportColumn.entries.size, headers.size)
        assertEquals(articles.size, rows.size)

        // Verify first article mapping
        val medRow = rows[0]
        assertEquals("MED-001", medRow[0])
        assertEquals("1234567890", medRow[1])
        assertEquals("Paracétamol 500mg", medRow[2])
        assertEquals("Médicaments", medRow[3])
        assertEquals("Antalgiques", medRow[4])
        assertEquals("50", medRow[5]) // Formatted without decimal
        assertEquals("Boîte", medRow[6])
        assertEquals("120.0", medRow[7])
        assertEquals("6000.0", medRow[8]) // 50 * 120
        assertEquals("2027-12-31", medRow[9])
        assertEquals("Disponible", medRow[10])

        // Verify low stock status
        val conRow = rows[1]
        assertEquals("Stock Faible", conRow[10])

        // Verify out of stock status & blank fallback
        val matRow = rows[2]
        assertEquals("Rupture", matRow[10])
        assertEquals("Non classé", matRow[3])
        assertEquals("-", matRow[4])
    }

    @Test
    fun testStockReportDataMapperSubsetColumns() {
        val articles = sampleArticles()
        val selectedColumns = mapOf(
            ReportColumn.CODE.id to true,
            ReportColumn.DESIGNATION.id to true,
            ReportColumn.QUANTITY.id to true
        )

        val (headers, rows) = StockReportDataMapper.toHeadersAndRows(articles, selectedColumns)

        assertEquals(listOf("Code Article", "Désignation Article", "Quantité en Stock"), headers)
        assertEquals(3, rows.size)
        assertEquals(listOf("MED-001", "Paracétamol 500mg", "50"), rows[0])
    }

    @Test
    fun testPdfExporterGeneratesValidPdfFile() {
        val articles = sampleArticles()
        val allColumnsMap = ReportColumn.entries.associate { it.id to true }
        val (headers, rows) = StockReportDataMapper.toHeadersAndRows(articles, allColumnsMap)

        val tempPdf = File.createTempFile("test_stock_report_", ".pdf")
        tempPdf.deleteOnExit()

        PdfExporter.generateStockReportPdf(
            filePath = tempPdf.absolutePath,
            reportTitle = "Rapport Test Unitaire",
            dateRange = "01/01/2026 à 31/12/2026",
            familiesFilter = "Médicaments, Consommables",
            serviceFilter = "Pharmacie Centrale",
            headers = headers,
            rows = rows
        )

        assertTrue(tempPdf.exists(), "PDF file must exist")
        assertTrue(tempPdf.length() > 500, "PDF file must be non-empty (actual size: ${tempPdf.length()} bytes)")

        // Verify standard PDF header magic bytes: %PDF-
        val headerBytes = ByteArray(5)
        tempPdf.inputStream().use { it.read(headerBytes) }
        val headerString = String(headerBytes)
        assertEquals("%PDF-", headerString, "Generated file must start with '%PDF-' magic header")
    }

    @Test
    fun testCsvExporterGeneratesValidCsvFile() {
        val articles = sampleArticles()
        val selectedColumns = mapOf(
            ReportColumn.CODE.id to true,
            ReportColumn.DESIGNATION.id to true,
            ReportColumn.QUANTITY.id to true
        )
        val (headers, rows) = StockReportDataMapper.toHeadersAndRows(articles, selectedColumns)

        val tempCsv = File.createTempFile("test_stock_report_", ".csv")
        tempCsv.deleteOnExit()

        CsvExporter.writeCsv(tempCsv.absolutePath, headers, rows)

        assertTrue(tempCsv.exists(), "CSV file must exist")
        val parsed = com.github.doyaaaaaken.kotlincsv.dsl.csvReader().readAll(tempCsv)
        assertEquals(4, parsed.size, "CSV should contain 1 header line + 3 data rows")
        assertEquals(listOf("Code Article", "Désignation Article", "Quantité en Stock"), parsed[0])
        assertEquals(listOf("MED-001", "Paracétamol 500mg", "50"), parsed[1])
        assertEquals(listOf("CON-001", "Gants Latex", "3"), parsed[2])
        assertEquals(listOf("MAT-001", "Tensiomètre", "0"), parsed[3])
    }
}
