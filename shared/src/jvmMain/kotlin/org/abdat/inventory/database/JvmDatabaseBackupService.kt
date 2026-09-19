package org.abdat.inventory.database

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.abdat.inventory.data.repository.CategoryRepository
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.io.FileInputStream
import java.sql.DriverManager

class JvmDatabaseBackupService(
    private val database: AppDatabase,
    private val driver: SqlDriver,
    private val categoryRepository: CategoryRepository,
    private val dbFile: File = File(File(System.getProperty("user.home"), ".inventairemed"), "inventory.db")
) : DatabaseBackupService {

    companion object {
        private val SQLITE_HEADER_PREFIX = "SQLite format 3\u0000".toByteArray(Charsets.UTF_8)

        val TABLES_ORDER = listOf(
            "app_metadata",
            "fournisseur",
            "convention",
            "convention_item",
            "service",
            "local",
            "stock_article",
            "article_audit_log",
            "bon_de_commande",
            "commande_item",
            "bon_de_livraison",
            "livraison_item",
            "bon_de_sortie",
            "sortie_item",
            "papier_decharge",
            "decharge_item",
            "person",
            "equipment_assignment",
            "dynamic_config_item",
            "user_profile"
        )

        val MANDATORY_TABLES = listOf(
            "stock_article",
            "fournisseur",
            "service",
            "app_metadata"
        )
    }

    override suspend fun getBackupMetadata(): BackupMetadata = withContext(Dispatchers.IO) {
        val lastDate = database.appDatabaseQueries.getMetadata("last_backup_date").executeAsOneOrNull()
        val lastPath = database.appDatabaseQueries.getMetadata("last_backup_path").executeAsOneOrNull()
        val lastRestore = database.appDatabaseQueries.getMetadata("last_restore_date").executeAsOneOrNull()
        BackupMetadata(
            lastBackupDate = lastDate,
            lastBackupPath = lastPath,
            lastRestoreDate = lastRestore
        )
    }

    override suspend fun exportBackup(destinationPath: String): Result<BackupExportResult> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(destinationPath)
            destFile.parentFile?.mkdirs()

            if (destFile.exists()) {
                destFile.delete()
            }

            val escapedPath = destFile.absolutePath.replace("'", "''")

            // Flush WAL / checkpoint if needed
            try {
                driver.execute(null, "PRAGMA wal_checkpoint(FULL);", 0)
            } catch (_: Exception) {}

            // VACUUM INTO creates an exact, atomic, defragmented copy of the active database
            var vacuumSuccess = false
            try {
                driver.execute(null, "VACUUM INTO '$escapedPath';", 0)
                vacuumSuccess = destFile.exists() && destFile.length() > 0
            } catch (_: Exception) {
                vacuumSuccess = false
            }

            // Fallback: direct file copy if VACUUM INTO fails
            if (!vacuumSuccess) {
                if (dbFile.exists()) {
                    dbFile.copyTo(destFile, overwrite = true)
                } else {
                    return@withContext Result.failure(IllegalStateException("Le fichier source de base de données est introuvable."))
                }
            }

            val fileSize = destFile.length()
            val now = currentTimestamp()

            database.appDatabaseQueries.setMetadata("last_backup_date", now)
            database.appDatabaseQueries.setMetadata("last_backup_path", destFile.absolutePath)

            Result.success(
                BackupExportResult(
                    destinationPath = destFile.absolutePath,
                    timestamp = now,
                    fileSizeInBytes = fileSize
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateBackupFile(sourcePath: String): BackupValidationResult = withContext(Dispatchers.IO) {
        val file = File(sourcePath)
        if (!file.exists() || !file.isFile || !file.canRead()) {
            return@withContext BackupValidationResult.Invalid("Le fichier sélectionné est introuvable ou inaccessible en lecture.")
        }

        if (file.length() < 100) {
            return@withContext BackupValidationResult.Invalid("Le fichier est trop petit pour être une base SQLite valide.")
        }

        // 1. Verify SQLite magic header bytes
        val headerBytes = ByteArray(16)
        try {
            FileInputStream(file).use { input ->
                val read = input.read(headerBytes)
                if (read < 16 || !headerBytes.contentEquals(SQLITE_HEADER_PREFIX)) {
                    return@withContext BackupValidationResult.Invalid("Format de fichier non reconnu : ce n'est pas un fichier SQLite 3 valide.")
                }
            }
        } catch (e: Exception) {
            return@withContext BackupValidationResult.Invalid("Erreur lors de la lecture de l'en-tête du fichier : ${e.message}")
        }

        // 2. Open temporary JDBC connection to inspect tables
        try {
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { conn ->
                val foundTables = mutableSetOf<String>()
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';").use { rs ->
                        while (rs.next()) {
                            foundTables.add(rs.getString(1).lowercase())
                        }
                    }
                }

                val missingMandatory = MANDATORY_TABLES.filter { !foundTables.contains(it.lowercase()) }
                if (missingMandatory.isNotEmpty()) {
                    return@withContext BackupValidationResult.Invalid(
                        "Ce fichier SQLite n'est pas une sauvegarde InventaireMed compatible (tables manquantes : ${missingMandatory.joinToString(", ")})."
                    )
                }

                var articleCount = 0L
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT count(*) FROM stock_article;").use { rs ->
                        if (rs.next()) {
                            articleCount = rs.getLong(1)
                        }
                    }
                }

                BackupValidationResult.Valid(
                    tableCount = foundTables.size,
                    articleCount = articleCount,
                    details = "${foundTables.size} tables trouvées, $articleCount articles en stock."
                )
            }
        } catch (e: Exception) {
            BackupValidationResult.Invalid("Impossible d'analyser la base de données : ${e.message}")
        }
    }

    override suspend fun restoreBackup(sourcePath: String): Result<BackupRestoreResult> = withContext(Dispatchers.IO) {
        val validation = validateBackupFile(sourcePath)
        if (validation !is BackupValidationResult.Valid) {
            val reason = (validation as? BackupValidationResult.Invalid)?.reason ?: "Fichier invalide"
            return@withContext Result.failure(IllegalArgumentException(reason))
        }

        val sourceFile = File(sourcePath)
        val now = currentTimestamp()

        // 1. Create a pre-restore safety copy on disk
        var safetyBackupPath: String? = null
        try {
            if (dbFile.exists()) {
                val safetyFile = File(dbFile.parentFile, "inventory.db.pre_restore_bak")
                dbFile.copyTo(safetyFile, overwrite = true)
                safetyBackupPath = safetyFile.absolutePath
            }
        } catch (_: Exception) {}

        // 2. Perform atomic transactional copy using ATTACH DATABASE on a dedicated connection
        val escapedSource = sourceFile.absolutePath.replace("'", "''")

        try {
            // Checkpoint any pending writes
            try {
                driver.execute(null, "PRAGMA wal_checkpoint(FULL);", 0)
            } catch (_: Exception) {}

            val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
            conn.use { c ->
                c.createStatement().use { stmt ->
                    stmt.execute("PRAGMA busy_timeout = 5000;")
                    stmt.execute("ATTACH DATABASE '$escapedSource' AS backup_source;")

                    try {
                        c.autoCommit = false
                        stmt.execute("PRAGMA foreign_keys = OFF;")

                        for (table in TABLES_ORDER) {
                            val backupCols = mutableListOf<String>()
                            stmt.executeQuery("PRAGMA backup_source.table_info($table);").use { rs ->
                                while (rs.next()) {
                                    val colName = rs.getString("name")
                                    if (colName != null) backupCols.add(colName)
                                }
                            }

                            if (backupCols.isNotEmpty()) {
                                val activeCols = mutableListOf<String>()
                                stmt.executeQuery("PRAGMA table_info($table);").use { rs ->
                                    while (rs.next()) {
                                        val colName = rs.getString("name")
                                        if (colName != null) activeCols.add(colName)
                                    }
                                }

                                val commonCols = activeCols.intersect(backupCols.toSet())
                                if (commonCols.isNotEmpty()) {
                                    val colList = commonCols.joinToString(", ") { "`$it`" }
                                    stmt.executeUpdate("DELETE FROM `$table`;")
                                    stmt.executeUpdate("INSERT INTO `$table` ($colList) SELECT $colList FROM backup_source.`$table`;")
                                }
                            }
                        }

                        // Restore sqlite auto-increment sequence if present
                        try {
                            var hasSeq = false
                            stmt.executeQuery("PRAGMA backup_source.table_info(sqlite_sequence);").use { rs ->
                                if (rs.next()) hasSeq = true
                            }
                            if (hasSeq) {
                                stmt.executeUpdate("DELETE FROM sqlite_sequence;")
                                stmt.executeUpdate("INSERT OR REPLACE INTO sqlite_sequence SELECT * FROM backup_source.sqlite_sequence;")
                            }
                        } catch (_: Exception) {}

                        stmt.execute("PRAGMA foreign_keys = ON;")
                        c.commit()
                    } catch (e: Exception) {
                        c.rollback()
                        throw e
                    } finally {
                        c.autoCommit = true
                        try {
                            stmt.execute("DETACH DATABASE backup_source;")
                        } catch (_: Exception) {}
                    }
                }
            }

            // Checkpoint again after restore
            try {
                driver.execute(null, "PRAGMA wal_checkpoint(FULL);", 0)
            } catch (_: Exception) {}

            // Notify SQLDelight listeners
            (driver as? JdbcSqliteDriver)?.notifyListeners(*TABLES_ORDER.toTypedArray())

            // 3. Update restore metadata & trigger taxonomy reconciliation
            database.appDatabaseQueries.setMetadata("last_restore_date", now)
            ensureCategoryTaxonomy(database)
            categoryRepository.notifyTaxonomyChanged()

            Result.success(
                BackupRestoreResult(
                    sourcePath = sourceFile.absolutePath,
                    timestamp = now,
                    safetyBackupPath = safetyBackupPath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun currentTimestamp(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} " +
            "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
    }
}
