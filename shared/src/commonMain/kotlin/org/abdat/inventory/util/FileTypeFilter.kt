package org.abdat.inventory.util

/**
 * Describes a file type filter for native save/open dialogs.
 * @param description Human-readable label (e.g. "CSV Files")
 * @param extension File extension without the dot (e.g. "csv")
 */
data class FileTypeFilter(
    val description: String,
    val extension: String
)
