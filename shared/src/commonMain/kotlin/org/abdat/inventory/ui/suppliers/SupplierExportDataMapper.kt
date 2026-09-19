package org.abdat.inventory.ui.suppliers

import org.abdat.inventory.domain.model.Convention
import org.abdat.inventory.domain.model.Fournisseur

/**
 * Data mapper converting supplier and convention domain models into structured
 * headers and rows suitable for CSV exports and tabular reports.
 */
object SupplierExportDataMapper {

    val supplierConventionsHeaders = listOf(
        "Numéro Convention",
        "Date Signature",
        "Plafond DZD",
        "Taux TVA",
        "Nombre Articles",
        "Statut"
    )

    val conventionArticlesHeaders = listOf(
        "Code / ID",
        "Désignation",
        "Unité de Mesure",
        "Quota Convenu",
        "Quantité Commandée",
        "Solde Restant",
        "Seuil Alerte"
    )

    /**
     * Maps a supplier's conventions into CSV headers and rows.
     */
    fun toSupplierConventionsCsv(
        supplier: Fournisseur,
        conventions: List<Convention>,
        balancesMap: Map<Long, List<ConventionQuotaBalance>> = emptyMap()
    ): Pair<List<String>, List<List<String>>> {
        val rows = conventions.map { conv ->
            val itemsCount = balancesMap[conv.id]?.size ?: conv.items.size
            listOf(
                "Conv N° ${conv.ordNumber}",
                conv.date,
                conv.totalMontant.toString(),
                "${conv.tvaRate}%",
                itemsCount.toString(),
                "Active"
            )
        }
        return supplierConventionsHeaders to rows
    }

    /**
     * Maps a specific convention's contracted articles and quota balances into CSV headers and rows.
     */
    fun toConventionArticlesCsv(
        convention: Convention,
        supplier: Fournisseur,
        balances: List<ConventionQuotaBalance>
    ): Pair<List<String>, List<List<String>>> {
        val rows = balances.map { balance ->
            listOf(
                balance.conventionItemId.toString(),
                balance.designation,
                balance.unite,
                formatQuantity(balance.agreedQuantity),
                formatQuantity(balance.orderedQuantity),
                formatQuantity(balance.remainingQuantity),
                formatQuantity(balance.minQuantity)
            )
        }
        return conventionArticlesHeaders to rows
    }

    private fun formatQuantity(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }
}
