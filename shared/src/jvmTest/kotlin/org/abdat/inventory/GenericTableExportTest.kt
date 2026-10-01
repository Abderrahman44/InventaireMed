package org.abdat.inventory

import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder
import org.abdat.inventory.util.CsvExporter
import org.abdat.inventory.util.PdfExporter
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericTableExportTest {

    private val originalHeader = InstitutionalHeaderHolder.current

    @BeforeTest
    fun setUp() {
        InstitutionalHeaderHolder.current = InstitutionalHeader(
            line1 = "RÉPUBLIQUE ALGÉRIENNE DÉMOCRATIQUE ET POPULAIRE",
            line2 = "MINISTÈRE DE LA SANTÉ",
            line3 = "DIRECTION DE LA SANTÉ D'AÏN DEFLA",
            line4 = "ÉTABLISSEMENT PUBLIC HOSPITALIER D'EL ATTAF"
        )
    }

    @AfterTest
    fun tearDown() {
        InstitutionalHeaderHolder.current = originalHeader
    }

    @Test
    fun testGenerateGenericTablePdfValidHeaderAndStructure() {
        val tempPdf = File.createTempFile("test_generic_table_", ".pdf")
        try {
            val headers = listOf("N° Commande", "Date", "Fournisseur", "Articles", "Montant DZD", "Statut")
            val rows = listOf(
                listOf("CMD-2026-001", "2026-09-01", "PharmAlgerie", "15", "1 250 000,00", "LIVRÉE"),
                listOf("CMD-2026-002", "2026-09-05", "BioMed SARL", "4", "340 000,00", "EN ATTENTE")
            )
            val metadata = mapOf(
                "Total Commandes" to "2",
                "Valeur Globale" to "1 590 000,00 DZD",
                "Statut Filtre" to "Tous"
            )

            PdfExporter.generateGenericTablePdf(
                filePath = tempPdf.absolutePath,
                documentTitle = "Registre Général des Commandes",
                subtitle = "Exercice Budgétaire 2026",
                metadata = metadata,
                headers = headers,
                rows = rows
            )

            assertTrue(tempPdf.exists(), "PDF file must exist")
            assertTrue(tempPdf.length() > 0, "PDF file must not be empty")

            val bytes = tempPdf.readBytes()
            val magic = String(bytes.sliceArray(0 until 4))
            assertEquals("%PDF", magic, "File must start with %PDF magic header")
        } finally {
            tempPdf.delete()
        }
    }

    @Test
    fun testGenerateGenericTablePdfWithoutInstitutionalHeader() {
        InstitutionalHeaderHolder.current = InstitutionalHeader("", "", "", "")
        val tempPdf = File.createTempFile("test_generic_no_header_", ".pdf")
        try {
            PdfExporter.generateGenericTablePdf(
                filePath = tempPdf.absolutePath,
                documentTitle = "Rapport Sans En-tête",
                headers = listOf("Col 1", "Col 2"),
                rows = listOf(listOf("Val 1", "Val 2"))
            )

            assertTrue(tempPdf.exists())
            assertTrue(tempPdf.length() > 0)
            val magic = String(tempPdf.readBytes().sliceArray(0 until 4))
            assertEquals("%PDF", magic)
        } finally {
            tempPdf.delete()
        }
    }

    @Test
    fun testGenericCsvExportWithInstitutionalHeader() {
        val tempCsv = File.createTempFile("test_generic_table_", ".csv")
        try {
            val headers = listOf("Réf", "Désignation", "Stock", "P.U")
            val rows = listOf(
                listOf("MED-01", "Insuline Rapide 100UI", "50", "450.00"),
                listOf("MED-02", "Doliprane 1000mg, Boîte de 8", "120", "130.00")
            )

            CsvExporter.writeCsv(
                filePath = tempCsv.absolutePath,
                headers = headers,
                rows = rows
            )

            assertTrue(tempCsv.exists())
            val lines = tempCsv.readLines()
            assertTrue(lines.size >= 7, "CSV must contain institutional header lines and data")

            // First lines are institutional header
            assertTrue(lines[0].contains("RÉPUBLIQUE ALGÉRIENNE"))
            assertTrue(lines[1].contains("MINISTÈRE DE LA SANTÉ"))
            assertTrue(lines[2].contains("DIRECTION DE LA SANTÉ"))
            assertTrue(lines[3].contains("ÉTABLISSEMENT PUBLIC HOSPITALIER"))

            // Header row
            val headerLine = lines[5]
            assertTrue(headerLine.contains("Réf"))
            assertTrue(headerLine.contains("Désignation"))

            // Data rows - Doliprane contains comma so must be quoted
            val data1 = lines[6]
            assertTrue(data1.contains("MED-01"))
            assertTrue(data1.contains("Insuline Rapide 100UI"))

            val data2 = lines[7]
            assertTrue(data2.contains("MED-02"))
            assertTrue(data2.contains("\"Doliprane 1000mg, Boîte de 8\""))
        } finally {
            tempCsv.delete()
        }
    }
}
