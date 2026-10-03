package org.abdat.inventory.util

import org.abdat.inventory.domain.model.PapierDecharge
import org.abdat.inventory.ui.consumption.ConsumptionItem
import org.abdat.inventory.ui.deliveries.DeliveryDisplayItem
import org.abdat.inventory.ui.orders.OrderDisplayItem
import org.abdat.inventory.ui.sorties.SortieDisplayItem

/**
 * Common data structure for tabular exports (CSV, Word, PDF).
 */
data class ExportDataSet(
    val title: String,
    val subtitle: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val headers: List<String>,
    val rows: List<List<String>>
)

/**
 * Data mapper for Orders screen.
 */
object OrdersExportDataMapper {
    val headers = listOf(
        "N° Commande",
        "Date",
        "Fournisseur",
        "Statut",
        "Nombre Articles",
        "Montant Total (DZD)"
    )

    fun toExportDataSet(orders: List<OrderDisplayItem>, filterDescription: String = ""): ExportDataSet {
        val totalAmount = orders.sumOf { it.totalAmountDzd }
        val totalItems = orders.sumOf { it.itemsCount }

        val metadata = linkedMapOf(
            "Total Commandes" to "${orders.size}",
            "Articles Commandés" to "$totalItems",
            "Montant Total" to "${formatAmount(totalAmount)} DZD"
        )
        if (filterDescription.isNotBlank()) {
            metadata["Filtre actif"] = filterDescription
        }

        val rows = orders.map { order ->
            listOf(
                order.commande.referenceBon,
                order.commande.date,
                order.supplierName,
                order.statusLabel,
                order.itemsCount.toString(),
                formatAmount(order.totalAmountDzd)
            )
        }

        return ExportDataSet(
            title = "Registre des Bons de Commande",
            subtitle = if (filterDescription.isNotBlank()) "Filtres : $filterDescription" else "Tous les bons de commande enregistrés",
            metadata = metadata,
            headers = headers,
            rows = rows
        )
    }

    fun toHeadersAndRows(orders: List<OrderDisplayItem>): Pair<List<String>, List<List<String>>> {
        val set = toExportDataSet(orders)
        return set.headers to set.rows
    }
}

/**
 * Data mapper for Deliveries / Réceptions screen.
 */
object DeliveriesExportDataMapper {
    val headers = listOf(
        "N° Réception",
        "Date Réception",
        "N° Commande",
        "Réf. BL Papier",
        "Fournisseur",
        "Destination",
        "Articles",
        "Montant (DZD)",
        "Statut"
    )

    fun toExportDataSet(deliveries: List<DeliveryDisplayItem>, filterDescription: String = ""): ExportDataSet {
        val totalAmount = deliveries.sumOf { it.totalAmountDzd }
        val totalArticles = deliveries.sumOf { it.itemsCount }

        val metadata = linkedMapOf(
            "Total Bons de Réception" to "${deliveries.size}",
            "Articles Réceptionnés" to "$totalArticles",
            "Valeur Réceptions" to "${formatAmount(totalAmount)} DZD"
        )
        if (filterDescription.isNotBlank()) {
            metadata["Filtre actif"] = filterDescription
        }

        val rows = deliveries.map { d ->
            listOf(
                d.receptionNumber,
                d.dateLivraison,
                d.commandeReference,
                d.paperBlReference.ifBlank { "-" },
                d.supplierName,
                d.destinationStock,
                d.itemsCount.toString(),
                formatAmount(d.totalAmountDzd),
                d.statusLabel
            )
        }

        return ExportDataSet(
            title = "Registre des Bons de Réception & Livraisons",
            subtitle = if (filterDescription.isNotBlank()) "Filtres : $filterDescription" else "Registre officiel des entrées en stock",
            metadata = metadata,
            headers = headers,
            rows = rows
        )
    }

    fun toHeadersAndRows(deliveries: List<DeliveryDisplayItem>): Pair<List<String>, List<List<String>>> {
        val set = toExportDataSet(deliveries)
        return set.headers to set.rows
    }
}

/**
 * Data mapper for Sorties Matériel (material issues) screen.
 */
object SortiesExportDataMapper {
    val headers = listOf(
        "N° Bon Sortie",
        "Date Sortie",
        "Service Bénéficiaire",
        "Local / Emplacement",
        "Demandeur",
        "Récepteur",
        "Articles",
        "Montant (DZD)",
        "Statut"
    )

    fun toExportDataSet(sorties: List<SortieDisplayItem>, filterDescription: String = ""): ExportDataSet {
        val totalAmount = sorties.sumOf { it.totalAmountDzd }
        val totalArticles = sorties.sumOf { it.itemsCount }

        val metadata = linkedMapOf(
            "Total Bons de Sortie" to "${sorties.size}",
            "Articles Déstockés" to "$totalArticles",
            "Valeur Sorties" to "${formatAmount(totalAmount)} DZD"
        )
        if (filterDescription.isNotBlank()) {
            metadata["Filtre actif"] = filterDescription
        }

        val rows = sorties.map { s ->
            listOf(
                s.referenceBon,
                s.date,
                s.serviceName,
                s.localName.ifBlank { "-" },
                s.demandeur.ifBlank { "-" },
                s.recepteurInterne.ifBlank { "-" },
                s.itemsCount.toString(),
                formatAmount(s.totalAmountDzd),
                s.statusLabel
            )
        }

        return ExportDataSet(
            title = "Registre des Bons de Sortie Matériel",
            subtitle = if (filterDescription.isNotBlank()) "Filtres : $filterDescription" else "Registre officiel des dotations et consommations internes",
            metadata = metadata,
            headers = headers,
            rows = rows
        )
    }

    fun toHeadersAndRows(sorties: List<SortieDisplayItem>): Pair<List<String>, List<List<String>>> {
        val set = toExportDataSet(sorties)
        return set.headers to set.rows
    }
}

/**
 * Data mapper for Décharges & Dons screen.
 */
object DechargesExportDataMapper {
    val headers = listOf(
        "N° Décharge / Don",
        "Date",
        "Établissement / Organisme",
        "Donateur / Source",
        "Nb Lignes",
        "Observations"
    )

    fun toExportDataSet(decharges: List<PapierDecharge>, filterDescription: String = ""): ExportDataSet {
        val totalLines = decharges.sumOf { it.items.size }

        val metadata = linkedMapOf(
            "Total Décharges / Dons" to "${decharges.size}",
            "Lignes d'articles" to "$totalLines"
        )
        if (filterDescription.isNotBlank()) {
            metadata["Filtre actif"] = filterDescription
        }

        val rows = decharges.map { d ->
            listOf(
                d.referenceBon,
                d.date,
                d.organizationName,
                d.donorName.ifBlank { "-" },
                d.items.size.toString(),
                d.notes.ifBlank { "-" }
            )
        }

        return ExportDataSet(
            title = "Registre des Décharges & Donations",
            subtitle = if (filterDescription.isNotBlank()) "Filtres : $filterDescription" else "Entrées et affectations sans incidence budgétaire",
            metadata = metadata,
            headers = headers,
            rows = rows
        )
    }

    fun toHeadersAndRows(decharges: List<PapierDecharge>): Pair<List<String>, List<List<String>>> {
        val set = toExportDataSet(decharges)
        return set.headers to set.rows
    }
}

/**
 * Data mapper for Consommation & Statistiques screen.
 */
object ConsumptionExportDataMapper {
    val headers = listOf(
        "N° Bon Sortie",
        "Date",
        "Service Consommateur",
        "Local",
        "Code Article",
        "Désignation",
        "Famille",
        "Quantité",
        "P.U (DZD)",
        "Montant Total (DZD)"
    )

    fun toExportDataSet(items: List<ConsumptionItem>, filterDescription: String = ""): ExportDataSet {
        val totalValue = items.sumOf { it.totalValue }
        val totalQty = items.sumOf { it.quantity }

        val metadata = linkedMapOf(
            "Lignes de consommation" to "${items.size}",
            "Quantité totale distribuée" to if (totalQty % 1.0 == 0.0) totalQty.toLong().toString() else "$totalQty",
            "Coût Global Consommation" to "${formatAmount(totalValue)} DZD"
        )
        if (filterDescription.isNotBlank()) {
            metadata["Filtre actif"] = filterDescription
        }

        val rows = items.map { item ->
            val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toLong().toString() else item.quantity.toString()
            listOf(
                item.referenceBon,
                item.date,
                item.serviceName,
                item.localName.ifBlank { "-" },
                item.codeArticle,
                item.designation,
                item.family.ifBlank { "Non classé" },
                qtyStr,
                formatAmount(item.unitPrice),
                formatAmount(item.totalValue)
            )
        }

        return ExportDataSet(
            title = "Rapport Détaillé de Consommation & Dotations",
            subtitle = if (filterDescription.isNotBlank()) "Filtres : $filterDescription" else "Suivi analytique des consommations par service",
            metadata = metadata,
            headers = headers,
            rows = rows
        )
    }

    fun toHeadersAndRows(items: List<ConsumptionItem>): Pair<List<String>, List<List<String>>> {
        val set = toExportDataSet(items)
        return set.headers to set.rows
    }
}

private fun formatAmount(amount: Double): String {
    // Format double to 2 decimal places cleanly without java.text.DecimalFormat dependencies in commonMain
    val rounded = (amount * 100.0 + 0.5).toLong() / 100.0
    val wholePart = rounded.toLong()
    val fracPart = ((rounded - wholePart) * 100).toLong().coerceIn(0, 99)
    val fracStr = if (fracPart < 10) "0$fracPart" else "$fracPart"

    // Add thousands separator (space)
    val wholeStr = wholePart.toString().reversed().chunked(3).joinToString(" ").reversed()
    return "$wholeStr,$fracStr"
}
