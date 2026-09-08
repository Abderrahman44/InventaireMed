package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PackagingUnit(val label: String) {
    UNITE("UNITE"),
    BIDON("BIDON"),
    FLACON("FLACON"),
    CARTON("CARTON"),
    KITE("KITE"),
    BOITE("BOITE"),
    ROULEAU("ROULEAU"),
    PAQUET("PAQUET"),
    AUTRE("AUTRE");

    companion object {
        fun fromString(value: String): PackagingUnit {
            return entries.firstOrNull { it.label.equals(value.trim(), ignoreCase = true) } ?: AUTRE
        }
    }
}

@Serializable
data class StockArticle(
    val id: Long = 0,
    val codeArticle: String,
    val designation: String,
    val uniteQuantite: String,
    val family: String = "",
    val sousFamily: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val expirationDate: String? = null,
    val date: String
)
