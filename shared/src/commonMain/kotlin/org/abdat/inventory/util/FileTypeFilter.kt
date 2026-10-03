package org.abdat.inventory.util

/**
 * Describes a file type filter for native save/open dialogs.
 * @param description Human-readable label (e.g. "CSV Files")
 * @param extension File extension without the dot (e.g. "csv")
 */
data class FileTypeFilter(
    val description: String,
    val extension: String
) {
    companion object {
        val CSV = FileTypeFilter("Fichiers CSV (*.csv)", "csv")
        val PDF = FileTypeFilter("Documents PDF (*.pdf)", "pdf")
        val DOCX = FileTypeFilter("Documents Word (*.docx)", "docx")
        val SQLITE_DB = FileTypeFilter("Base de données SQLite (*.db)", "db")
        val SQLITE_SQLITE = FileTypeFilter("Base de données SQLite (*.sqlite)", "sqlite")
        val SQLITE_BAK = FileTypeFilter("Sauvegarde SQLite (*.bak)", "bak")
        val BACKUP_FILTERS = listOf(SQLITE_DB, SQLITE_SQLITE, SQLITE_BAK)
    }
}
