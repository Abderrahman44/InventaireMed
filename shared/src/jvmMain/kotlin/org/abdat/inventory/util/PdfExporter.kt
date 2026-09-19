package org.abdat.inventory.util

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.FontFactory
import com.lowagie.text.HeaderFooter
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.Rectangle
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.awt.Color
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

actual object PdfExporter {
    actual fun generateStockReportPdf(
        filePath: String,
        reportTitle: String,
        dateRange: String,
        familiesFilter: String,
        serviceFilter: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        val document = Document(PageSize.A4.rotate(), 20f, 20f, 25f, 25f)
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val outputStream = FileOutputStream(file)
        try {
            PdfWriter.getInstance(document, outputStream)

            // Header & Footer with Page Numbering
            val footer = HeaderFooter(Phrase("Page ", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)), true)
            footer.border = Rectangle.NO_BORDER
            footer.setAlignment(Element.ALIGN_RIGHT)
            document.setFooter(footer)

            document.open()

            // Styles
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15f, Color(30, 41, 59))
            val metaTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color(71, 85, 105))
            val metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color(15, 23, 42))
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)
            val dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color(30, 41, 59))
            val summaryFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color(15, 23, 42))

            // Title
            val titleParagraph = Paragraph(reportTitle.ifBlank { "État Général du Stock & Consommation" }, titleFont)
            titleParagraph.alignment = Element.ALIGN_CENTER
            titleParagraph.spacingAfter = 10f
            document.add(titleParagraph)

            // Metadata box
            val metaTable = PdfPTable(2)
            metaTable.widthPercentage = 100f
            metaTable.setSpacingAfter(12f)

            fun addMetaRow(label1: String, val1: String, label2: String, val2: String) {
                val p1 = Paragraph().apply {
                    add(Phrase("$label1 : ", metaTitleFont))
                    add(Phrase(val1, metaValueFont))
                }
                val cell1 = PdfPCell(p1).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 4f
                }
                val p2 = Paragraph().apply {
                    add(Phrase("$label2 : ", metaTitleFont))
                    add(Phrase(val2, metaValueFont))
                }
                val cell2 = PdfPCell(p2).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 4f
                }
                metaTable.addCell(cell1)
                metaTable.addCell(cell2)
            }

            val timestamp = try {
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            } catch (_: Exception) {
                ""
            }

            addMetaRow("Période", dateRange, "Date de Génération", timestamp)
            addMetaRow("Famille(s)", familiesFilter, "Service Destinataire", serviceFilter)
            document.add(metaTable)

            // Data Table
            if (headers.isNotEmpty()) {
                val dataTable = PdfPTable(headers.size)
                dataTable.widthPercentage = 100f
                dataTable.headerRows = 1

                val primaryColor = Color(37, 99, 235)
                val zebraColor = Color(248, 250, 252)

                // Header Row
                headers.forEach { headerText ->
                    val cell = PdfPCell(Phrase(headerText, headerFont)).apply {
                        backgroundColor = primaryColor
                        horizontalAlignment = Element.ALIGN_CENTER
                        verticalAlignment = Element.ALIGN_MIDDLE
                        paddingTop = 5f
                        paddingBottom = 5f
                        borderColor = Color(203, 213, 225)
                    }
                    dataTable.addCell(cell)
                }

                // Data Rows
                rows.forEachIndexed { index, row ->
                    val bgColor = if (index % 2 == 1) zebraColor else Color.WHITE
                    row.forEach { cellText ->
                        val cell = PdfPCell(Phrase(cellText, dataFont)).apply {
                            backgroundColor = bgColor
                            paddingTop = 4f
                            paddingBottom = 4f
                            paddingLeft = 4f
                            paddingRight = 4f
                            borderColor = Color(226, 232, 240)
                            verticalAlignment = Element.ALIGN_MIDDLE
                        }
                        dataTable.addCell(cell)
                    }
                }

                document.add(dataTable)
            }

            // Summary
            val summaryParagraph = Paragraph("\nTotal Articles inclus : ${rows.size}", summaryFont)
            summaryParagraph.alignment = Element.ALIGN_RIGHT
            document.add(summaryParagraph)

        } finally {
            if (document.isOpen) {
                document.close()
            }
            outputStream.close()
        }
    }
}

