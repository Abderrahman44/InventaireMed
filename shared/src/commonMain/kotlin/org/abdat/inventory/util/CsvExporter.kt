package org.abdat.inventory.util

expect object CsvExporter {
    fun writeCsv(filePath: String, headers: List<String>, rows: List<List<String>>)
}
