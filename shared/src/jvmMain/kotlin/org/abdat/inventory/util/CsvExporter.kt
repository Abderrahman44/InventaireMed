package org.abdat.inventory.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File

actual object CsvExporter {
    actual fun writeCsv(filePath: String, headers: List<String>, rows: List<List<String>>) {
        csvWriter().open(File(filePath)) {
            writeRow(headers)
            rows.forEach { row -> writeRow(row) }
        }
    }

    fun writeCsv(file: File, headers: List<String>, rows: List<List<String>>) {
        writeCsv(file.absolutePath, headers, rows)
    }
}
