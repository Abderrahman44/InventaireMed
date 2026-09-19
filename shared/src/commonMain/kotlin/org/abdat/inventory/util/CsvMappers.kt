package org.abdat.inventory.util

import org.abdat.inventory.ui.orders.OrderDisplayItem
import org.abdat.inventory.ui.sorties.SortieDisplayItem

/**
 * Maps [OrderDisplayItem] list to CSV-ready headers + rows.
 */
object OrdersCsvMapper {

    private val headers = listOf(
        "Numéro",
        "Date",
        "Fournisseur",
        "Statut",
        "Nombre Articles",
        "Montant Total (DZD)"
    )

    fun toHeadersAndRows(orders: List<OrderDisplayItem>): Pair<List<String>, List<List<String>>> {
        val rows = orders.map { order ->
            val formattedAmount = (order.totalAmountDzd).toString()
            listOf(
                order.commande.referenceBon,
                order.commande.date,
                order.supplierName,
                order.statusLabel,
                order.itemsCount.toString(),
                formattedAmount
            )
        }
        return headers to rows
    }
}

/**
 * Maps [SortieDisplayItem] list to CSV-ready headers + rows.
 */
object SortiesCsvMapper {

    private val headers = listOf(
        "Numéro BS",
        "Date",
        "Service",
        "Chef de Service",
        "Local",
        "Bénéficiaires",
        "Demandeur",
        "Récepteur",
        "Motif",
        "Nb Articles",
        "Montant (DZD)",
        "Statut"
    )

    fun toHeadersAndRows(sorties: List<SortieDisplayItem>): Pair<List<String>, List<List<String>>> {
        val rows = sorties.map { sortie ->
            listOf(
                sortie.referenceBon,
                sortie.date,
                sortie.serviceName,
                sortie.chefDeServiceName,
                sortie.localName,
                sortie.beneficiaireNames.ifBlank { "Ensemble du service" },
                sortie.demandeur,
                sortie.recepteurInterne,
                sortie.motif,
                sortie.itemsCount.toString(),
                sortie.totalAmountDzd.toString(),
                sortie.statusLabel
            )
        }
        return headers to rows
    }
}
