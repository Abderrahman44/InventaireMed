package org.abdat.inventory

import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder
import org.abdat.inventory.util.DocxExporter
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DocxExporterTest {

    private val originalHeader = InstitutionalHeaderHolder.current

    @BeforeTest
    fun setUp() {
        InstitutionalHeaderHolder.current = InstitutionalHeader(
            line1 = "RÉPUBLIQUE ALGÉRIENNE DÉMOCRATIQUE ET POPULAIRE",
            line2 = "MINISTÈRE DE LA SANTÉ",
            line3 = "DIRECTION DE LA SANTÉ ET DE LA POPULATION D'AÏN DEFLA",
            line4 = "ÉTABLISSEMENT PUBLIC HOSPITALIER D'EL ATTAF"
        )
    }

    @AfterTest
    fun tearDown() {
        InstitutionalHeaderHolder.current = originalHeader
    }

    @Test
    fun testGenerateGenericTableDocxArchiveIntegrity() {
        val tempFile = File.createTempFile("test_generic_", ".docx")
        try {
            val headers = listOf("Code", "Désignation & Détails", "Quantité", "Prix <DZD>")
            val rows = listOf(
                listOf("ART-001", "Paracétamol 500mg & Vitamine C", "150", "120,50"),
                listOf("ART-002", "Sérum Physiologique > 0.9%", "300", "85,00")
            )
            val metadata = mapOf(
                "Filtre" to "Service Urgences & Pédiatrie",
                "Total Articles" to "2"
            )

            DocxExporter.generateGenericTableDocx(
                filePath = tempFile.absolutePath,
                documentTitle = "Registre des Opérations & Mouvements",
                subtitle = "Période : Septembre 2026",
                metadata = metadata,
                headers = headers,
                rows = rows
            )

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0)

            // Validate Zip archive structure
            ZipFile(tempFile).use { zip ->
                val contentTypes = zip.getEntry("[Content_Types].xml")
                assertNotNull(contentTypes, "[Content_Types].xml must exist in .docx")

                val rootRels = zip.getEntry("_rels/.rels")
                assertNotNull(rootRels, "_rels/.rels must exist in .docx")

                val docRels = zip.getEntry("word/_rels/document.xml.rels")
                assertNotNull(docRels, "word/_rels/document.xml.rels must exist in .docx")

                val documentXml = zip.getEntry("word/document.xml")
                assertNotNull(documentXml, "word/document.xml must exist in .docx")

                val xmlContent = zip.getInputStream(documentXml).bufferedReader().readText()

                // Check institutional header
                assertTrue(xmlContent.contains("RÉPUBLIQUE ALGÉRIENNE DÉMOCRATIQUE ET POPULAIRE"))
                assertTrue(xmlContent.contains("ÉTABLISSEMENT PUBLIC HOSPITALIER D&apos;EL ATTAF"))

                // Check title and subtitle
                assertTrue(xmlContent.contains("Registre des Opérations &amp; Mouvements"))
                assertTrue(xmlContent.contains("Période : Septembre 2026"))

                // Check XML escaping of special characters
                assertTrue(xmlContent.contains("Paracétamol 500mg &amp; Vitamine C"))
                assertTrue(xmlContent.contains("Prix &lt;DZD&gt;"))
                assertTrue(xmlContent.contains("Sérum Physiologique &gt; 0.9%"))
                assertTrue(xmlContent.contains("Service Urgences &amp; Pédiatrie"))

                // Check headers and rows
                assertTrue(xmlContent.contains("ART-001"))
                assertTrue(xmlContent.contains("ART-002"))
                assertTrue(xmlContent.contains("120,50"))
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testGenerateLandscapeDocxForLargeTables() {
        val tempFile = File.createTempFile("test_landscape_", ".docx")
        try {
            // More than 5 columns triggers landscape mode in DocxExporter
            val headers = listOf("N° Bon", "Date", "Service", "Local", "Demandeur", "Articles", "Montant DZD", "Statut")
            val rows = listOf(
                listOf("BS-001", "2026-09-01", "Pédiatrie", "Salle 1", "Dr. A", "10", "15 000,00", "LIVRÉ")
            )
            val metadata = mapOf("Statut" to "Validé")

            DocxExporter.generateGenericTableDocx(
                filePath = tempFile.absolutePath,
                documentTitle = "Registre Large des Bons de Sortie",
                subtitle = "Exercice 2026",
                metadata = metadata,
                headers = headers,
                rows = rows
            )

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0)

            ZipFile(tempFile).use { zip ->
                val documentXml = zip.getEntry("word/document.xml")
                assertNotNull(documentXml)
                val xml = zip.getInputStream(documentXml).bufferedReader().readText()

                assertTrue(xml.contains("landscape"))
                assertTrue(xml.contains("Registre Large des Bons de Sortie"))
                assertTrue(xml.contains("Pédiatrie"))
                assertTrue(xml.contains("BS-001"))
            }
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testGenerateDocxWithoutInstitutionalHeader() {
        val tempFile = File.createTempFile("test_no_inst_header_", ".docx")
        try {
            val emptyHeader = InstitutionalHeader("", "", "", "")
            DocxExporter.generateGenericTableDocx(
                filePath = tempFile.absolutePath,
                documentTitle = "Rapport Standard",
                subtitle = "Sans en-tête",
                metadata = emptyMap(),
                headers = listOf("ID", "Nom"),
                rows = listOf(listOf("1", "Article Simple")),
                institutionalHeader = emptyHeader
            )

            assertTrue(tempFile.exists())
            assertTrue(tempFile.length() > 0)

            ZipFile(tempFile).use { zip ->
                val documentXml = zip.getEntry("word/document.xml")
                assertNotNull(documentXml)
                val xml = zip.getInputStream(documentXml).bufferedReader().readText()

                // Must NOT contain default institutional text
                assertTrue(!xml.contains("RÉPUBLIQUE ALGÉRIENNE"))
                // Must contain title
                assertTrue(xml.contains("Rapport Standard"))
                assertTrue(xml.contains("Article Simple"))
            }
        } finally {
            tempFile.delete()
        }
    }
}
