package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DynamicConfigItem(
    val id: Long = 0,
    val category: String, // UNIT, FAMILY, SOUS_FAMILY
    val code: String,
    val label: String,
    val parentCategory: String = "",
    val parentCode: String = "",
    val isActive: Boolean = true
)

@Serializable
data class UserProfile(
    val id: Long = 1,
    val fullName: String = "Administrateur",
    val phone: String = "",
    val email: String = "",
    val role: String = "Responsable Stock",
    val themeMode: String = "LIGHT", // LIGHT, DARK
    val locale: String = "FR" // FR, EN
)
