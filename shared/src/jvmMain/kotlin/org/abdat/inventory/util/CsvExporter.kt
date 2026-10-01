package org.abdat.inventory.util

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File
import org.abdat.inventory.domain.model.InstitutionalHeader
import org.abdat.inventory.domain.model.InstitutionalHeaderHolder

actual object CsvExporter {
    actual fun writeCsv(
        filePath: String,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader
    ) {
        val colCount = headers.size.coerceAtLeast(1)
        fun padRow(cell: String): List<String> {
            if (colCount <= 1) return listOf(cell)
            val list = ArrayList<String>(colCount)
            list.add(cell)
            for (i in 1 until colCount) {
                list.add("")
            }
            return list
        }

        val file = File(filePath)
        file.parentFile?.mkdirs()

        csvWriter().open(file) {
            if (!institutionalHeader.isBlank) {
                if (institutionalHeader.line1.isNotBlank()) writeRow(padRow(institutionalHeader.line1))
                if (institutionalHeader.line2.isNotBlank()) writeRow(padRow(institutionalHeader.line2))
                if (institutionalHeader.line3.isNotBlank()) writeRow(padRow(institutionalHeader.line3))
                if (institutionalHeader.line4.isNotBlank()) writeRow(padRow(institutionalHeader.line4))
                writeRow(padRow(""))
            }
            writeRow(headers)
            rows.forEach { row -> writeRow(row) }
        }
    }

    fun writeCsv(
        file: File,
        headers: List<String>,
        rows: List<List<String>>,
        institutionalHeader: InstitutionalHeader = InstitutionalHeaderHolder.current
    ) {
        writeCsv(file.absolutePath, headers, rows, institutionalHeader)
    }
}
