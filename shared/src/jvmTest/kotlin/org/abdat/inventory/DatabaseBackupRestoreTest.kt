package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.BackupValidationResult
import org.abdat.inventory.database.JvmDatabaseBackupService
import org.abdat.inventory.ui.config.ConfigTab
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseBackupRestoreTest {

    private lateinit var tempDir: File
    private lateinit var activeDbFile: File
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: AppDatabase
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var backupService: JvmDatabaseBackupService

    @BeforeTest
    fun setUp() {
        tempDir = File.createTempFile("inv_test_dir_", "").apply {
            delete()
            mkdirs()
        }
        activeDbFile = File(tempDir, "inventory.db")
        driver = JdbcSqliteDriver("jdbc:sqlite:${activeDbFile.absolutePath}")
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        categoryRepository = CategoryRepository(database, Dispatchers.Unconfined)
        backupService = JvmDatabaseBackupService(
            database = database,
            driver = driver,
            categoryRepository = categoryRepository,
            dbFile = activeDbFile
        )
    }

    @AfterTest
    fun tearDown() {
        try {
            driver.close()
        } catch (_: Exception) {}
        tempDir.deleteRecursively()
    }

    @Test
    fun testBackupExportAndMetadata() {
        runBlocking {
            // Seed initial article
            database.appDatabaseQueries.insertStockArticle(
                code_article = "ART-TEST-001",
                additional_reference_id = "REF-001",
                designation = "Paracétamol 500mg",
                unite_quantite = "Boîte",
                family = "Médicaments",
                sous_family = "Antalgiques",
                quantity = 50.0,
                unit_price = 120.0,
                expiration_date = "2027-12-31",
                is_archived = 0L,
                is_obsolete = 0L,
                date = "2026-09-18"
            )

            val targetExportFile = File(tempDir, "export_test.db")
            val result = backupService.exportBackup(targetExportFile.absolutePath)

            assertTrue(result.isSuccess, "Export should succeed")
            assertTrue(targetExportFile.exists(), "Export file should exist")
            assertTrue(targetExportFile.length() > 0, "Export file should not be empty")

            val metadata = backupService.getBackupMetadata()
            assertNotNull(metadata.lastBackupDate, "Last backup date must be recorded")
            assertEquals(targetExportFile.absolutePath, metadata.lastBackupPath)
        }
    }

    @Test
    fun testBackupValidationChecks() {
        runBlocking {
            // 1. Valid backup validation
            val validBackupFile = File(tempDir, "valid_backup.db")
            backupService.exportBackup(validBackupFile.absolutePath)

            val validCheck = backupService.validateBackupFile(validBackupFile.absolutePath)
            assertTrue(validCheck is BackupValidationResult.Valid, "Valid backup should pass validation")
            assertTrue(validCheck.tableCount >= 4)

            // 2. Non-existent file
            val missingFile = File(tempDir, "does_not_exist.db")
            val missingCheck = backupService.validateBackupFile(missingFile.absolutePath)
            assertTrue(missingCheck is BackupValidationResult.Invalid)

            // 3. Corrupted / non-sqlite file
            val textFile = File(tempDir, "plain_text.txt").apply {
                writeText("This is not a SQLite database file at all.")
            }
            val textCheck = backupService.validateBackupFile(textFile.absolutePath)
            assertTrue(textCheck is BackupValidationResult.Invalid)

            // 4. Incompatible SQLite database lacking InventaireMed tables
            val foreignDbFile = File(tempDir, "other_app.db")
            val foreignDriver = JdbcSqliteDriver("jdbc:sqlite:${foreignDbFile.absolutePath}")
            foreignDriver.execute(null, "CREATE TABLE unrelated_users (id INTEGER PRIMARY KEY, name TEXT);", 0)
            foreignDriver.close()

            val foreignCheck = backupService.validateBackupFile(foreignDbFile.absolutePath)
            assertTrue(foreignCheck is BackupValidationResult.Invalid)
            assertTrue(foreignCheck.reason.contains("manquantes"))
        }
    }

    @Test
    fun testTransactionalRestoreWithSafetyCopy() {
        runBlocking {
            // Step 1: Populate initial state with 2 articles
            database.appDatabaseQueries.insertStockArticle(
                code_article = "ART-INITIAL-01",
                additional_reference_id = "",
                designation = "Amoxicilline 500mg",
                unite_quantite = "Boîte",
                family = "Médicaments",
                sous_family = "Antibiotiques",
                quantity = 25.0,
                unit_price = 350.0,
                expiration_date = "2027-06-30",
                is_archived = 0L,
                is_obsolete = 0L,
                date = "2026-09-18"
            )
            database.appDatabaseQueries.insertStockArticle(
                code_article = "ART-INITIAL-02",
                additional_reference_id = "",
                designation = "Ibuprofène 400mg",
                unite_quantite = "Boîte",
                family = "Médicaments",
                sous_family = "Anti-inflammatoires",
                quantity = 40.0,
                unit_price = 210.0,
                expiration_date = "2027-08-31",
                is_archived = 0L,
                is_obsolete = 0L,
                date = "2026-09-18"
            )

            // Step 2: Export backup
            val snapshotBackup = File(tempDir, "snapshot_restore_test.db")
            val exportResult = backupService.exportBackup(snapshotBackup.absolutePath)
            assertTrue(exportResult.isSuccess)

            // Step 3: Modify the active database (delete all and add different article)
            driver.execute(null, "DELETE FROM stock_article;", 0)
            database.appDatabaseQueries.insertStockArticle(
                code_article = "ART-MODIFIED-99",
                additional_reference_id = "",
                designation = "Serum Salé 500ml",
                unite_quantite = "Flacon",
                family = "Solutés",
                sous_family = "Perfusions",
                quantity = 100.0,
                unit_price = 150.0,
                expiration_date = "2028-01-01",
                is_archived = 0L,
                is_obsolete = 0L,
                date = "2026-09-18"
            )

            val beforeRestoreArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            assertEquals(1, beforeRestoreArticles.size)
            assertEquals("ART-MODIFIED-99", beforeRestoreArticles.first().code_article)

            // Step 4: Restore from the backup snapshot
            val restoreResult = backupService.restoreBackup(snapshotBackup.absolutePath)
            assertTrue(restoreResult.isSuccess, "Restore operation should succeed")

            // Verify articles restored
            val afterRestoreArticles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            assertEquals(2, afterRestoreArticles.size)
            val codes = afterRestoreArticles.map { it.code_article }
            assertTrue(codes.contains("ART-INITIAL-01"))
            assertTrue(codes.contains("ART-INITIAL-02"))

            // Verify pre-restore safety file exists
            val safetyFile = File(tempDir, "inventory.db.pre_restore_bak")
            assertTrue(safetyFile.exists(), "Safety backup file inventory.db.pre_restore_bak should have been created")

            // Verify last restore date is set in metadata
            val metadata = backupService.getBackupMetadata()
            assertNotNull(metadata.lastRestoreDate)
        }
    }

    @Test
    fun testDynamicConfigViewModelBackupFlow() {
        runBlocking {
            val viewModel = DynamicConfigViewModel(
                database = database,
                categoryRepository = categoryRepository,
                backupService = backupService
            )

            // Check tab selection
            viewModel.selectTab(ConfigTab.BACKUP)
            assertEquals(ConfigTab.BACKUP, viewModel.uiState.value.selectedTab)

            // Simulate request import with invalid file
            val invalidFile = File(tempDir, "corrupted.db").apply { writeText("garbage") }
            val check = backupService.validateBackupFile(invalidFile.absolutePath)
            assertTrue(check is BackupValidationResult.Invalid)

            // Dismiss messages
            viewModel.dismissMessages()
            assertNull(viewModel.uiState.value.actionErrorMessage)
            assertNull(viewModel.uiState.value.actionSuccessMessage)

            // Cancel restore dialog state
            viewModel.cancelRestore()
            assertFalse(viewModel.uiState.value.isRestoreConfirmationDialogOpen)
            assertNull(viewModel.uiState.value.pendingRestoreFilePath)
        }
    }
}
