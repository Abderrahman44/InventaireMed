package org.abdat.inventory.util

/**
 * Opens the OS-native file-open dialog to select an existing file.
 *
 * @param title   Dialog window title
 * @param filters Allowed file type filters
 * @return The absolute path of the chosen file, or `null` if cancelled.
 */
expect suspend fun showOpenFileDialog(
    title: String,
    filters: List<FileTypeFilter>
): String?
