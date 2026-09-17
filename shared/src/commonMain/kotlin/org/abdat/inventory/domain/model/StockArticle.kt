package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PackagingUnit(val label: String) {
    PIECE("Pièce"),
    BOITE("Boîte"),
    CARTON("Carton"),
    FLACON("Flacon"),
    LITRE("Litre"),
    RAME("Rame"),
    BIDON("Bidon"),
    ROULEAU("Rouleau"),
    PAQUET("Paquet"),
    KITE("Kit"),
    AUTRE("Autre");

    companion object {
        fun fromString(value: String): PackagingUnit {
            return entries.firstOrNull { 
                it.name.equals(value.trim(), ignoreCase = true) || 
                it.label.equals(value.trim(), ignoreCase = true) 
            } ?: AUTRE
        }
    }
}

@Serializable
data class StockArticle(
    val id: Long = 0,
    val codeArticle: String,
    val additionalReferenceId: String = "",
    val designation: String,
    val uniteQuantite: String,
    val family: String = "",
    val sousFamily: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val expirationDate: String? = null,
    val isArchived: Boolean = false,
    val isObsolete: Boolean = false,
    val date: String
)
