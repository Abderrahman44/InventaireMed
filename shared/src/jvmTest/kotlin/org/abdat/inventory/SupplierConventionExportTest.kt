package org.abdat.inventory

import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.ConventionItem
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance
import org.abdat.inventory.ui.suppliers.SupplierExportDataMapper
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.PdfExporter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupplierConventionExportTest {

    private val sampleSupplier = Fournisseur(
        id = 1,
        fullName = "Mohamed Benali",
        societe = "PharmAlgerie SARL",
        phone = "+213 555 123 456",
        responsablePhone = "+213 666 789 012",
        ville = "Alger",
        email = "contact@pharmalgerie.dz",
        otherLinks = "WhatsApp: +213555123456"
    )

    private val sampleConventions = listOf(
        Convention(
            id = 10,
            fournisseurId = 1,
            ordNumber = 2026001,
            date = "2026-01-15",
            totalMontant = 1500000.0,
            tvaRate = 19.0,
            items = listOf(
                ConventionItem(id = 101, conventionId = 10, designation = "Amoxicilline 500mg", unite = "Boîte", quantity = 1000.0, minQuantity = 50.0)
            )
        ),
        Convention(
            id = 11,
            fournisseurId = 1,
            ordNumber = 2026002,
            date = "2026-02-20",
            totalMontant = 500000.0,
            tvaRate = 0.0,
            items = emptyList()
        )
    )

    private val sampleBalances = listOf(
        ConventionQuotaBalance(
            conventionItemId = 101,
            conventionId = 10,
            designation = "Amoxicilline 500mg",
            unite = "Boîte",
            agreedQuantity = 1000.0,
            minQuantity = 50.0,
            orderedQuantity = 400.0,
            remainingQuantity = 600.0,
            unitPrice = 250.0
        ),
        ConventionQuotaBalance(
            conventionItemId = 102,
            conventionId = 10,
            designation = "Sérum Physiologique",
            unite = "Flacon",
            agreedQuantity = 2500.0,
            minQuantity = 100.0,
            orderedQuantity = 2450.0,
            remainingQuantity = 50.0,
            unitPrice = 80.0
        )
    )

    @Test
    fun testSupplierConventionsCsvMapping() {
        val (headers, rows) = SupplierExportDataMapper.toSupplierConventionsCsv(
            supplier = sampleSupplier,
            conventions = sampleConventions
        )

        assertEquals(6, headers.size)
        assertEquals(listOf("Numéro Convention", "Date Signature", "Plafond DZD", "Taux TVA", "Nombre Articles", "Statut"), headers)
        assertEquals(2, rows.size)

        val firstRow = rows[0]
        assertEquals("Conv N° 2026001", firstRow[0])
        assertEquals("2026-01-15", firstRow[1])
        assertEquals("1500000.0", firstRow[2])
        assertEquals("19.0%", firstRow[3])
        assertEquals("1", firstRow[4])
        assertEquals("Active", firstRow[5])
    }

    @Test
    fun testConventionArticlesCsvMapping() {
        val (headers, rows) = SupplierExportDataMapper.toConventionArticlesCsv(
            convention = sampleConventions[0],
            supplier = sampleSupplier,
            balances = sampleBalances
        )

        assertEquals(7, headers.size)
        assertEquals(listOf("Code / ID", "Désignation", "Unité de Mesure", "Quota Convenu", "Quantité Commandée", "Solde Restant", "Seuil Alerte"), headers)
        assertEquals(2, rows.size)

        val firstArticle = rows[0]
        assertEquals("101", firstArticle[0])
        assertEquals("Amoxicilline 500mg", firstArticle[1])
        assertEquals("Boîte", firstArticle[2])
        assertEquals("1000", firstArticle[3])
        assertEquals("400", firstArticle[4])
        assertEquals("600", firstArticle[5])
        assertEquals("50", firstArticle[6])
    }

    @Test
    fun testCsvFileExportExecution() {
        val tempFile = File.createTempFile("test_supplier_export_", ".csv")
        try {
            val (headers, rows) = SupplierExportDataMapper.toSupplierConventionsCsv(
                supplier = sampleSupplier,
                conventions = sampleConventions
            )
            CsvExporter.writeCsv(tempFile.absolutePath, headers, rows)

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0)

            val lines = tempFile.readLines()
            assertTrue(lines.isNotEmpty())
            assertTrue(lines[0].contains("Numéro Convention"))
            assertTrue(lines[1].contains("Conv N° 2026001"))
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testSupplierDossierPdfGeneration() {
        val tempPdf = File.createTempFile("test_supplier_dossier_", ".pdf")
        try {
            PdfExporter.generateSupplierDossierPdf(
                filePath = tempPdf.absolutePath,
                supplier = sampleSupplier,
                conventions = sampleConventions,
                balancesMap = mapOf(10L to sampleBalances)
            )

            assertTrue(tempPdf.exists())
            assertTrue(tempPdf.length() > 0)

            val bytes = tempPdf.readBytes()
            // Valid PDF header starts with %PDF
            val header = String(bytes.sliceArray(0 until 4))
            assertEquals("%PDF", header)
        } finally {
            tempPdf.delete()
        }
    }

    @Test
    fun testConventionDetailPdfGeneration() {
        val tempPdf = File.createTempFile("test_convention_detail_", ".pdf")
        try {
            PdfExporter.generateConventionDetailPdf(
                filePath = tempPdf.absolutePath,
                convention = sampleConventions[0],
                supplier = sampleSupplier,
                balances = sampleBalances
            )

            assertTrue(tempPdf.exists())
            assertTrue(tempPdf.length() > 0)

            val bytes = tempPdf.readBytes()
            val header = String(bytes.sliceArray(0 until 4))
            assertEquals("%PDF", header)
        } finally {
            tempPdf.delete()
        }
    }
}
