package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BonDeCommande(
    val id: Long = 0,
    val referenceBon: String,
    val fournisseurId: Long,
    val date: String,
    val isDebt: Boolean = false,
    val items: List<CommandeItem> = emptyList()
)

@Serializable
data class CommandeItem(
    val id: Long = 0,
    val commandeId: Long = 0,
    val conventionItemId: Long? = null,
    val codeArticle: String = "",
    val designation: String,
    val type: String,
    val family: String = "",
    val quantity: Double,
    val unitPrice: Double = 0.0
)

@Serializable
data class BonDeLivraison(
    val id: Long = 0,
    val referenceBon: String,
    val fournisseurId: Long,
    val dateLivraison: String,
    val commandeId: Long? = null,
    val items: List<LivraisonItem> = emptyList()
)

@Serializable
data class LivraisonItem(
    val id: Long = 0,
    val livraisonId: Long = 0,
    val codeArticle: String,
    val designation: String,
    val quantity: Double,
    val unitPrice: Double = 0.0
)

@Serializable
data class BonDeSortie(
    val id: Long = 0,
    val referenceBon: String,
    val serviceId: Long,
    val localId: Long? = null,
    val date: String,
    val items: List<SortieItem> = emptyList()
)

@Serializable
data class SortieItem(
    val id: Long = 0,
    val sortieId: Long = 0,
    val codeArticle: String,
    val designation: String,
    val quantity: Double
)
