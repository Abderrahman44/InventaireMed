package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: Long = 0,
    val name: String,
    val localNum: String = "",
    val chefId: Long? = null,
    val chefName: String? = null
)

@Serializable
data class Local(
    val id: Long = 0,
    val serviceId: Long,
    val name: String
)

@Serializable
data class Person(
    val id: Long = 0,
    val serviceId: Long,
    val fullName: String,
    val birthDate: String = "",
    val notes: String = "",
    val serviceName: String = "",
    val isChef: Boolean = false
)

@Serializable
data class EquipmentAssignment(
    val id: Long = 0,
    val personId: Long,
    val articleId: Long,
    val quantity: Double = 1.0,
    val assignedDate: String,
    val returnDate: String? = null,
    val status: String = "ACTIVE",
    val notes: String = "",
    val personName: String = "",
    val articleDesignation: String = "",
    val articleCode: String = "",
    val unite: String = ""
)
