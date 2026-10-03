package org.abdat.inventory.util

import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder

expect object CsvExporter {
    fun writeCsv(
        filePath: String,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    )
}
