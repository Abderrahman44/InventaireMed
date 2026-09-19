package org.abdat.inventory.ui.inventory

import org.abdat.inventory.domain.model.StockArticle

object StockReportDataMapper {
    fun toHeadersAndRows(
        articles: List<StockArticle>,
        selectedColumns: Map<String, Boolean>
    ): Pair<List<String>, List<List<String>>> {
        val activeColumns = ReportColumn.entries.filter { selectedColumns[it.id] == true }
        val headers = activeColumns.map { it.label }
        val rows = articles.map { article ->
            activeColumns.map { col ->
                when (col) {
                    ReportColumn.CODE -> article.codeArticle
                    ReportColumn.BARCODE -> article.additionalReferenceId.ifBlank { "-" }
                    ReportColumn.DESIGNATION -> article.designation
                    ReportColumn.FAMILY -> article.family.ifBlank { CATEGORY_FILTER_UNCLASSIFIED }
                    ReportColumn.SOUS_FAMILY -> article.sousFamily.ifBlank { "-" }
                    ReportColumn.QUANTITY -> if (article.quantity % 1.0 == 0.0) article.quantity.toLong().toString() else article.quantity.toString()
                    ReportColumn.UNITE -> article.uniteQuantite
                    ReportColumn.UNIT_PRICE -> "${article.unitPrice}"
                    ReportColumn.TOTAL_VALUE -> "${article.quantity * article.unitPrice}"
                    ReportColumn.EXPIRATION -> article.expirationDate ?: "-"
                    ReportColumn.STATUS -> when {
                        article.quantity <= 0.0 -> "Rupture"
                        article.quantity <= 5.0 -> "Stock Faible"
                        else -> "Disponible"
                    }
                }
            }
        }
        return headers to rows
    }
}
