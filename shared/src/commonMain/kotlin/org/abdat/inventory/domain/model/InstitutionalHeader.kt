package org.abdat.inventory.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class InstitutionalHeader(
    val line1: String = DEFAULT_LINE_1,
    val line2: String = DEFAULT_LINE_2,
    val line3: String = DEFAULT_LINE_3,
    val line4: String = DEFAULT_LINE_4
) {
    fun toLines(): List<String> = listOf(line1, line2, line3, line4)

    val isBlank: Boolean
        get() = line1.isBlank() && line2.isBlank() && line3.isBlank() && line4.isBlank()

    companion object {
        const val DEFAULT_LINE_1 = "Établissement Public Hospitalier"
        const val DEFAULT_LINE_2 = "El Attaf, Aïn Defla"
        const val DEFAULT_LINE_3 = "Département des Finances et des Moyens"
        const val DEFAULT_LINE_4 = "Service Magasin Central"

        val DEFAULT = InstitutionalHeader(
            line1 = DEFAULT_LINE_1,
            line2 = DEFAULT_LINE_2,
            line3 = DEFAULT_LINE_3,
            line4 = DEFAULT_LINE_4
        )
    }
}

/**
 * Global holder storing the current active institutional header in memory for export generators.
 */
object InstitutionalHeaderHolder {
    @Volatile
    var current: InstitutionalHeader = InstitutionalHeader.DEFAULT
}
