package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleAuditLog(
    val id: Long = 0,
    val articleId: Long,
    val fieldName: String,
    val oldValue: String = "",
    val newValue: String = "",
    val reason: String,
    val userName: String = "Admin",
    val editDate: String,
    val articleDesignation: String = "",
    val articleCode: String = ""
)
