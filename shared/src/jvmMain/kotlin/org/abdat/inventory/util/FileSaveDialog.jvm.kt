package org.abdat.inventory.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * JVM Desktop implementation using [java.awt.FileDialog] in SAVE mode
 * for a native OS look-and-feel on Windows, macOS, and Linux.
 */
actual suspend fun showSaveFileDialog(
    title: String,
    defaultFileName: String,
    filters: List<FileTypeFilter>
): String? = withContext(Dispatchers.Main) {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply {
        file = defaultFileName

        // Apply the first filter's extension as the filename filter
        if (filters.isNotEmpty()) {
            val allowedExtensions = filters.map { ".${it.extension}" }
            filenameFilter = FilenameFilter { _, name ->
                allowedExtensions.any { ext -> name.endsWith(ext, ignoreCase = true) }
            }
        }

        isVisible = true
    }

    val dir = dialog.directory
    val file = dialog.file

    if (dir != null && file != null) {
        // Ensure the file has the correct extension
        val expectedExt = filters.firstOrNull()?.extension
        val result = if (expectedExt != null && !file.endsWith(".$expectedExt", ignoreCase = true)) {
            "$dir$file.$expectedExt"
        } else {
            "$dir$file"
        }
        File(result).absolutePath
    } else {
        null
    }
}
