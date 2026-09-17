package org.abdat.inventory.util

/**
 * Opens the OS-native file-save dialog.
 *
 * @param title       Dialog window title
 * @param defaultFileName  Pre-filled filename (e.g. "report.pdf")
 * @param filters     Allowed file type filters
 * @return The absolute path chosen by the user, or `null` if cancelled.
 */
expect suspend fun showSaveFileDialog(
    title: String,
    defaultFileName: String,
    filters: List<FileTypeFilter>
): String?
