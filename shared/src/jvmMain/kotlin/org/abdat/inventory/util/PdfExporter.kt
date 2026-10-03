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
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder
import org.abdat.inventory.ui.suppliers.ConventionQuotaBalance

actual object PdfExporter {
    private fun renderInstitutionalHeader(document: Document, header: InstitutionalHeader) {
        if (header.isBlank) return

        val institutionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, Color(30, 41, 59))
        val locationFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color(71, 85, 105))
        val departmentFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, Color(71, 85, 105))
        val serviceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color(37, 99, 235))

        val headerTable = PdfPTable(1).apply {
            widthPercentage = 100f
            setSpacingAfter(10f)
        }

        fun addHeaderLine(text: String, font: com.lowagie.text.Font, padding: Float = 1.2f) {
            val cell = PdfPCell(Phrase(text, font)).apply {
                horizontalAlignment = Element.ALIGN_CENTER
                verticalAlignment = Element.ALIGN_MIDDLE
                border = Rectangle.NO_BORDER
                paddingTop = padding
                paddingBottom = padding
            }
            headerTable.addCell(cell)
        }

        if (header.line1.isNotBlank()) addHeaderLine(header.line1.uppercase(), institutionFont, 2f)
        if (header.line2.isNotBlank()) addHeaderLine(header.line2, locationFont)
        if (header.line3.isNotBlank()) addHeaderLine(header.line3, departmentFont)
        if (header.line4.isNotBlank()) addHeaderLine(header.line4, serviceFont)

        val dividerCell = PdfPCell().apply {
            border = Rectangle.BOTTOM
            borderWidthBottom = 1.2f
            borderColorBottom = Color(203, 213, 225)
            paddingTop = 3f
            paddingBottom = 5f
        }
        headerTable.addCell(dividerCell)

        document.add(headerTable)
    }

    actual fun generateStockReportPdf(
        filePath: String,
        reportTitle: String,
        dateRange: String,
        familiesFilter: String,
        serviceFilter: String,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader
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
            renderInstitutionalHeader(document, institutionalHeader)

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

    actual fun generateSupplierDossierPdf(
        filePath: String,
        supplier: Fournisseur,
        conventions: List<Convention>,
        balancesMap: Map<Long, List<ConventionQuotaBalance>>,
        institutionalHeader: InstitutionalHeader
    ) {
        val document = Document(PageSize.A4, 30f, 30f, 30f, 30f)
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val outputStream = FileOutputStream(file)
        try {
            PdfWriter.getInstance(document, outputStream)

            val footer = HeaderFooter(Phrase("Page ", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)), true)
            footer.border = Rectangle.NO_BORDER
            footer.setAlignment(Element.ALIGN_RIGHT)
            document.setFooter(footer)

            document.open()
            renderInstitutionalHeader(document, institutionalHeader)

            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, Color(30, 41, 59))
            val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Color(37, 99, 235))
            val metaTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color(71, 85, 105))
            val metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, Color(15, 23, 42))
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)
            val dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color(30, 41, 59))

            // Title
            val titleParagraph = Paragraph("Dossier Fournisseur : ${supplier.fullName}", titleFont)
            titleParagraph.alignment = Element.ALIGN_CENTER
            titleParagraph.spacingAfter = 14f
            document.add(titleParagraph)

            // Supplier info table
            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setSpacingAfter(14f)

            fun addInfoRow(label1: String, val1: String, label2: String, val2: String) {
                val p1 = Paragraph().apply {
                    add(Phrase("$label1 : ", metaTitleFont))
                    add(Phrase(val1, metaValueFont))
                }
                val cell1 = PdfPCell(p1).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 5f
                }
                val p2 = Paragraph().apply {
                    add(Phrase("$label2 : ", metaTitleFont))
                    add(Phrase(val2, metaValueFont))
                }
                val cell2 = PdfPCell(p2).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 5f
                }
                infoTable.addCell(cell1)
                infoTable.addCell(cell2)
            }

            addInfoRow("Fournisseur", supplier.fullName, "Société", supplier.societe.ifBlank { "-" })
            addInfoRow("Téléphone", supplier.phone.ifBlank { "-" }, "Resp. Téléphone", supplier.responsablePhone.ifBlank { "-" })
            addInfoRow("Ville", supplier.ville.ifBlank { "-" }, "Email", supplier.email.ifBlank { "-" })
            addInfoRow("Total Conventions", conventions.size.toString(), "Autres Liens", supplier.otherLinks.ifBlank { "-" })

            document.add(infoTable)

            // Conventions section
            val convTitle = Paragraph("Liste des Conventions", sectionFont)
            convTitle.spacingAfter = 8f
            document.add(convTitle)

            if (conventions.isNotEmpty()) {
                val convTable = PdfPTable(6)
                convTable.widthPercentage = 100f
                convTable.headerRows = 1
                convTable.setWidths(floatArrayOf(2.5f, 2f, 2f, 1.5f, 2f, 2f))

                val primaryColor = Color(37, 99, 235)
                val zebraColor = Color(248, 250, 252)

                listOf("Numéro", "Date", "Plafond (DZD)", "TVA", "Articles", "Statut").forEach { h ->
                    val cell = PdfPCell(Phrase(h, headerFont)).apply {
                        backgroundColor = primaryColor
                        horizontalAlignment = Element.ALIGN_CENTER
                        verticalAlignment = Element.ALIGN_MIDDLE
                        paddingTop = 5f
                        paddingBottom = 5f
                    }
                    convTable.addCell(cell)
                }

                conventions.forEachIndexed { index, conv ->
                    val bgColor = if (index % 2 == 1) zebraColor else Color.WHITE
                    val itemsCount = balancesMap[conv.id]?.size ?: conv.items.size
                    listOf(
                        "Conv N° ${conv.ordNumber}",
                        conv.date,
                        "%.2f".format(conv.totalMontant),
                        "${conv.tvaRate}%",
                        itemsCount.toString(),
                        "Active"
                    ).forEach { text ->
                        val cell = PdfPCell(Phrase(text, dataFont)).apply {
                            backgroundColor = bgColor
                            paddingTop = 4f
                            paddingBottom = 4f
                            paddingLeft = 4f
                            paddingRight = 4f
                            borderColor = Color(226, 232, 240)
                            verticalAlignment = Element.ALIGN_MIDDLE
                        }
                        convTable.addCell(cell)
                    }
                }

                document.add(convTable)
            } else {
                document.add(Paragraph("Aucune convention enregistrée.", dataFont))
            }

        } finally {
            if (document.isOpen) {
                document.close()
            }
            outputStream.close()
        }
    }

    actual fun generateConventionDetailPdf(
        filePath: String,
        convention: Convention,
        supplier: Fournisseur,
        balances: List<ConventionQuotaBalance>,
        institutionalHeader: InstitutionalHeader
    ) {
        val document = Document(PageSize.A4, 30f, 30f, 30f, 30f)
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val outputStream = FileOutputStream(file)
        try {
            PdfWriter.getInstance(document, outputStream)

            val footer = HeaderFooter(Phrase("Page ", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)), true)
            footer.border = Rectangle.NO_BORDER
            footer.setAlignment(Element.ALIGN_RIGHT)
            document.setFooter(footer)

            document.open()
            renderInstitutionalHeader(document, institutionalHeader)

            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, Color(30, 41, 59))
            val sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f, Color(37, 99, 235))
            val metaTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color(71, 85, 105))
            val metaValueFont = FontFactory.getFont(FontFactory.HELVETICA, 9f, Color(15, 23, 42))
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, Color.WHITE)
            val dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color(30, 41, 59))

            // Title
            val titleParagraph = Paragraph("Fiche Convention N° ${convention.ordNumber}", titleFont)
            titleParagraph.alignment = Element.ALIGN_CENTER
            titleParagraph.spacingAfter = 14f
            document.add(titleParagraph)

            // Convention & Supplier info table
            val infoTable = PdfPTable(2)
            infoTable.widthPercentage = 100f
            infoTable.setSpacingAfter(14f)

            fun addInfoRow(label1: String, val1: String, label2: String, val2: String) {
                val p1 = Paragraph().apply {
                    add(Phrase("$label1 : ", metaTitleFont))
                    add(Phrase(val1, metaValueFont))
                }
                val cell1 = PdfPCell(p1).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 5f
                }
                val p2 = Paragraph().apply {
                    add(Phrase("$label2 : ", metaTitleFont))
                    add(Phrase(val2, metaValueFont))
                }
                val cell2 = PdfPCell(p2).apply {
                    border = Rectangle.NO_BORDER
                    paddingBottom = 5f
                }
                infoTable.addCell(cell1)
                infoTable.addCell(cell2)
            }

            addInfoRow("Fournisseur", supplier.fullName, "Société", supplier.societe.ifBlank { "-" })
            addInfoRow("Numéro Convention", "Conv N° ${convention.ordNumber}", "Date", convention.date)
            addInfoRow("Plafond Global", "%.2f DZD".format(convention.totalMontant), "Taux TVA", "${convention.tvaRate}%")

            document.add(infoTable)

            // Quotas / Balances section
            val quotaTitle = Paragraph("Articles Contractés et Suivi des Quotas", sectionFont)
            quotaTitle.spacingAfter = 8f
            document.add(quotaTitle)

            if (balances.isNotEmpty()) {
                val quotaTable = PdfPTable(7)
                quotaTable.widthPercentage = 100f
                quotaTable.headerRows = 1
                quotaTable.setWidths(floatArrayOf(1.2f, 3f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f))

                val primaryColor = Color(37, 99, 235)
                val zebraColor = Color(248, 250, 252)

                listOf("ID", "Désignation", "Unité", "Quota", "Commandé", "Restant", "Seuil Min").forEach { h ->
                    val cell = PdfPCell(Phrase(h, headerFont)).apply {
                        backgroundColor = primaryColor
                        horizontalAlignment = Element.ALIGN_CENTER
                        verticalAlignment = Element.ALIGN_MIDDLE
                        paddingTop = 5f
                        paddingBottom = 5f
                    }
                    quotaTable.addCell(cell)
                }

                fun formatQty(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

                balances.forEachIndexed { index, b ->
                    val bgColor = if (index % 2 == 1) zebraColor else Color.WHITE
                    listOf(
                        b.conventionItemId.toString(),
                        b.designation,
                        b.unite,
                        formatQty(b.agreedQuantity),
                        formatQty(b.orderedQuantity),
                        formatQty(b.remainingQuantity),
                        formatQty(b.minQuantity)
                    ).forEach { text ->
                        val cell = PdfPCell(Phrase(text, dataFont)).apply {
                            backgroundColor = bgColor
                            paddingTop = 4f
                            paddingBottom = 4f
                            paddingLeft = 4f
                            paddingRight = 4f
                            borderColor = Color(226, 232, 240)
                            verticalAlignment = Element.ALIGN_MIDDLE
                        }
                        quotaTable.addCell(cell)
                    }
                }

                document.add(quotaTable)
            } else {
                document.add(Paragraph("Aucun article ou quota défini pour cette convention.", dataFont))
            }

        } finally {
            if (document.isOpen) {
                document.close()
            }
            outputStream.close()
        }
    }

    actual fun generateGenericTablePdf(
        filePath: String,
        documentTitle: String,
        subtitle: String,
        metadata: Map<String, String>,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader
    ) {
        val colCount = headers.size.coerceAtLeast(1)
        val isLandscape = colCount > 5
        val pageSize = if (isLandscape) PageSize.A4.rotate() else PageSize.A4
        val document = Document(pageSize, 20f, 20f, 25f, 25f)
        val file = File(filePath)
        file.parentFile?.mkdirs()

        val outputStream = FileOutputStream(file)
        try {
            PdfWriter.getInstance(document, outputStream)

            val footer = HeaderFooter(Phrase("Page ", FontFactory.getFont(FontFactory.HELVETICA, 8f, Color.GRAY)), true)
            footer.border = Rectangle.NO_BORDER
            footer.setAlignment(Element.ALIGN_RIGHT)
            document.setFooter(footer)

            document.open()
            renderInstitutionalHeader(document, institutionalHeader)

            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, Color(15, 23, 42))
            val subFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color(100, 116, 139))
            val metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color(71, 85, 105))
            val metaValFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color(15, 23, 42))
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)
            val dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, Color(30, 41, 59))
            val zebraColor = Color(248, 250, 252)

            val titlePara = Paragraph(documentTitle, titleFont).apply {
                spacingAfter = if (subtitle.isNotBlank()) 2f else 8f
            }
            document.add(titlePara)

            if (subtitle.isNotBlank()) {
                val subPara = Paragraph(subtitle, subFont).apply {
                    spacingAfter = 8f
                }
                document.add(subPara)
            }

            if (metadata.isNotEmpty()) {
                val metaTable = PdfPTable(4).apply {
                    widthPercentage = 100f
                    setSpacingAfter(10f)
                }
                val metaEntries = metadata.entries.toList()
                val chunked = metaEntries.chunked(2)
                chunked.forEach { pair ->
                    val (k1, v1) = pair[0]
                    metaTable.addCell(PdfPCell(Phrase(k1, metaLabelFont)).apply {
                        backgroundColor = Color(241, 245, 249)
                        borderColor = Color(226, 232, 240)
                        setPadding(4f)
                    })
                    metaTable.addCell(PdfPCell(Phrase(v1, metaValFont)).apply {
                        borderColor = Color(226, 232, 240)
                        setPadding(4f)
                    })
                    if (pair.size > 1) {
                        val (k2, v2) = pair[1]
                        metaTable.addCell(PdfPCell(Phrase(k2, metaLabelFont)).apply {
                            backgroundColor = Color(241, 245, 249)
                            borderColor = Color(226, 232, 240)
                            setPadding(4f)
                        })
                        metaTable.addCell(PdfPCell(Phrase(v2, metaValFont)).apply {
                            borderColor = Color(226, 232, 240)
                            setPadding(4f)
                        })
                    } else {
                        metaTable.addCell(PdfPCell(Phrase("", metaLabelFont)).apply {
                            borderColor = Color(226, 232, 240)
                            setPadding(4f)
                        })
                        metaTable.addCell(PdfPCell(Phrase("", metaValFont)).apply {
                            borderColor = Color(226, 232, 240)
                            setPadding(4f)
                        })
                    }
                }
                document.add(metaTable)
            }

            val table = PdfPTable(colCount).apply {
                widthPercentage = 100f
                headerRows = 1
                setSpacingBefore(4f)
            }

            fun isNumeric(text: String): Boolean {
                val t = text.trim().replace(" ", "").replace("DZD", "").replace("DA", "").replace("%", "").replace(",", ".")
                return t.isNotEmpty() && t.toDoubleOrNull() != null
            }

            headers.forEach { h ->
                val cell = PdfPCell(Phrase(h, headerFont)).apply {
                    backgroundColor = Color(37, 99, 235)
                    horizontalAlignment = if (isNumeric(h)) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
                    verticalAlignment = Element.ALIGN_MIDDLE
                    paddingTop = 5f
                    paddingBottom = 5f
                    paddingLeft = 4f
                    paddingRight = 4f
                    borderColor = Color(29, 78, 216)
                }
                table.addCell(cell)
            }

            rows.forEachIndexed { rIdx, row ->
                val bgColor = if (rIdx % 2 == 1) zebraColor else Color.WHITE
                headers.indices.forEach { cIdx ->
                    val cellText = if (cIdx < row.size) row[cIdx] else ""
                    val align = if (isNumeric(cellText)) Element.ALIGN_RIGHT else Element.ALIGN_LEFT
                    val cell = PdfPCell(Phrase(cellText, dataFont)).apply {
                        backgroundColor = bgColor
                        horizontalAlignment = align
                        verticalAlignment = Element.ALIGN_MIDDLE
                        paddingTop = 4f
                        paddingBottom = 4f
                        paddingLeft = 4f
                        paddingRight = 4f
                        borderColor = Color(226, 232, 240)
                    }
                    table.addCell(cell)
                }
            }

            document.add(table)
        } finally {
            if (document.isOpen) {
                document.close()
            }
            outputStream.close()
        }
    }
}


