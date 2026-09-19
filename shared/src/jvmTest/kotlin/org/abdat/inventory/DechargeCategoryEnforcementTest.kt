package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.ui.decharges.DechargeDraftLine
import org.abdat.inventory.ui.decharges.DechargeViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DechargeCategoryEnforcementTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: DechargeViewModel

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        ensureCategoryTaxonomy(database)
        categoryRepository = CategoryRepository(database, Dispatchers.IO)
        viewModel = DechargeViewModel(database, categoryRepository, Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun createVoucherWithLine(line: DechargeDraftLine): Boolean {
        return viewModel.createDechargeVoucherSync(
            referenceBon = "DCH-TEST-001",
            organizationName = "Croissant Rouge",
            donorName = "Dr. Dupont",
            date = "2026-09-17",
            notes = "Test donation",
            lines = listOf(line)
        )
    }

    @Test
    fun unconfiguredFamilyIsRefused() {
        val line = DechargeDraftLine(
            codeArticle = "NEW-ART-001",
            designation = "Novel Stethoscope",
            unite = "U",
            family = "Inconnue",
            sousFamily = "Divers",
            quantity = 5.0,
            expirationDate = null,
            article = null
        )

        val result = createVoucherWithLine(line)

        assertFalse(result, "Creation must fail when family is not configured")
        assertNull(
            database.appDatabaseQueries.selectStockArticleByCode("NEW-ART-001").executeAsOneOrNull(),
            "No article should be inserted into database"
        )
        val message = viewModel.uiState.value.message
        assertNotNull(message)
        assertTrue(message.contains("non configurée"), "Error message should mention unconfigured category")
    }

    @Test
    fun mismatchedSousFamilyIsRefused() {
        val line = DechargeDraftLine(
            codeArticle = "NEW-ART-002",
            designation = "Aspirine Informatique",
            unite = "Boîte",
            family = "Médicaments",
            sousFamily = "Ordinateurs",
            quantity = 2.0,
            expirationDate = null,
            article = null
        )

        val result = createVoucherWithLine(line)

        assertFalse(result, "Creation must fail when sous-family does not belong to family")
        assertNull(database.appDatabaseQueries.selectStockArticleByCode("NEW-ART-002").executeAsOneOrNull())
        assertTrue(viewModel.uiState.value.message?.contains("non configurée") == true)
    }

    @Test
    fun blankCategoryIsRefused() {
        val line = DechargeDraftLine(
            codeArticle = "NEW-ART-003",
            designation = "Unclassified Item",
            unite = "U",
            family = "Médicaments",
            sousFamily = "",
            quantity = 1.0,
            expirationDate = null,
            article = null
        )

        val result = createVoucherWithLine(line)

        assertFalse(result, "Creation must fail when sous-family is blank")
        assertNull(database.appDatabaseQueries.selectStockArticleByCode("NEW-ART-003").executeAsOneOrNull())
    }

    @Test
    fun validConfiguredPairIsCreatedWithConfiguredCasing() {
        val line = DechargeDraftLine(
            codeArticle = "NEW-PC-01",
            designation = "Ordinateur Portable Donné",
            unite = "Pièce",
            family = "  accessoire INFORMATIQUE ",
            sousFamily = " ordinateurs ",
            quantity = 3.0,
            expirationDate = null,
            article = null
        )

        val result = createVoucherWithLine(line)

        assertTrue(result, "Creation must succeed with valid configured taxonomy pair")
        val inserted = database.appDatabaseQueries.selectStockArticleByCode("NEW-PC-01").executeAsOneOrNull()
        assertNotNull(inserted, "Article must be inserted into stock")
        assertEquals("Accessoire informatique", inserted.family, "Family must use canonical configured casing")
        assertEquals("Ordinateurs", inserted.sous_family, "Sous-family must use canonical configured casing")
        assertEquals(0.0, inserted.unit_price, "Donation article unit price must be 0.0")
        assertEquals(3.0, inserted.quantity)
    }
}
