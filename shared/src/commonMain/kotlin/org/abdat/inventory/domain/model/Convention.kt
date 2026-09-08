package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Fournisseur(
    val id: Long = 0,
    val fullName: String
)

@Serializable
data class Convention(
    val id: Long = 0,
    val fournisseurId: Long,
    val ordNumber: Long,
    val date: String,
    val globalUnitPrice: Double = 0.0,
    val totalMontant: Double = 0.0,
    val items: List<ConventionItem> = emptyList()
)

@Serializable
data class ConventionItem(
    val id: Long = 0,
    val conventionId: Long = 0,
    val designation: String,
    val unite: String,
    val quantity: Double,
    val minQuantity: Double = 0.0,
    val unitPrice: Double = 0.0
)
