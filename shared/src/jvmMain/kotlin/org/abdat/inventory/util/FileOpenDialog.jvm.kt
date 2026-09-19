package org.abdat.inventory.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter

/**
 * JVM Desktop implementation using [java.awt.FileDialog] in LOAD mode
 * for a native OS look-and-feel on Windows, macOS, and Linux.
 */
actual suspend fun showOpenFileDialog(
    title: String,
    filters: List<FileTypeFilter>
): String? = withContext(Dispatchers.Main) {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
        if (filters.isNotEmpty()) {
            val allowedExtensions = filters.map { ".${it.extension.lowercase()}" }
            filenameFilter = FilenameFilter { _, name ->
                allowedExtensions.any { ext -> name.lowercase().endsWith(ext) }
            }
        }
        isVisible = true
    }

    val dir = dialog.directory
    val file = dialog.file

    if (dir != null && file != null) {
        val selected = File(dir, file)
        if (selected.exists()) selected.absolutePath else null
    } else {
        null
    }
}
