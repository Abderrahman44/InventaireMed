package org.abdat.inventory.util

import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder

expect object DocxExporter {
    fun generateGenericTableDocx(
        filePath: String,
        documentTitle: String,
        subtitle: String = "",
        metadata: Map<String, String> = emptyMap(),
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )
}
