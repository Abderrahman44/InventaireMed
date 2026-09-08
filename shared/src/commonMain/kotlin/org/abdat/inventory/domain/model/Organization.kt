package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: Long = 0,
    val name: String,
    val localNum: String = ""
)

@Serializable
data class Local(
    val id: Long = 0,
    val serviceId: Long,
    val name: String
)
