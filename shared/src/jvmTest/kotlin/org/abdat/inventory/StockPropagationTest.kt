package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.sorties.SortiesViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Covers `reactive-stock-observation`: a mutation committed by one ViewModel reaches another
 * ViewModel's state with no reload call between them.
 *
 * This is the regression test for the reported bug — validating a bon de sortie left the Gestion de
 * Stock screen showing the pre-sortie quantity until the user opened an article and pressed Back.
 */
class StockPropagationTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: StockRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        seedInitialDataIfEmpty(database)
        // Mirrors the Koin factory: reference taxonomy is applied independently of the mock seeder.
        ensureCategoryTaxonomy(database)
        // One shared instance, exactly as Koin provides it — SQLDelight only notifies queries
        // registered against the same AppDatabase.
        repository = StockRepository(database, Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun <T> StateFlow<T>.await(timeoutMs: Long = 5_000, predicate: (T) -> Boolean): T =
        runBlocking {
            withTimeout(timeoutMs) {
                var current = value
                while (!predicate(current)) {
                    delay(5)
                    current = value
                }
                current
            }
        }

    @Test
    fun sortieDecrementReachesInventoryWithoutAnyReloadCall() {
        val inventoryVm = InventoryViewModel(database, repository, CategoryRepository(database, Dispatchers.IO))
        val sortiesVm = SortiesViewModel(database, repository)

        val article = inventoryVm.uiState.value.articles.first { it.quantity >= 10.0 }
        val startingQuantity = article.quantity
        val startingValuation = inventoryVm.uiState.value.totalStockValueDzd

        val service = sortiesVm.uiState.value.services.firstOrNull()
        assertNotNull(service, "seeded data should provide at least one service")

        // Build a single-line voucher for 10 units of the chosen article.
        sortiesVm.updateIssueQuantity(article.id, 0.0)
        sortiesVm.uiState.value.draftLines.forEach { sortiesVm.removeDraftLine(it.articleId) }
        sortiesVm.addDraftLine(article, initialQuantity = 10.0)

        val issued = sortiesVm.validateSortieSync(
            referenceBon = "BS-TEST-001",
            serviceId = service.id,
            localId = null,
            recipient = "Test",
            date = "2026-09-13 10:00"
        )
        assertTrue(issued, "voucher should validate: 10 units requested of $startingQuantity")

        // The crux: no inventoryVm.loadArticles() call anywhere in this test.
        val updated = inventoryVm.uiState.await { state ->
            state.articles.firstOrNull { it.id == article.id }?.quantity == startingQuantity - 10.0
        }

        val updatedArticle = updated.articles.first { it.id == article.id }
        assertEquals(startingQuantity - 10.0, updatedArticle.quantity)
        assertTrue(
            updated.totalStockValueDzd < startingValuation,
            "KPI valuation should fall with the issued stock"
        )
    }

    @Test
    fun inventoryFiltersAndDialogStateSurviveAStockEmission() {
        val inventoryVm = InventoryViewModel(database, repository, CategoryRepository(database, Dispatchers.IO))
        val articleToEdit = inventoryVm.uiState.value.articles.first()

        // User is mid-interaction: searching, filtering, with the edit dialog open.
        inventoryVm.onSearchQueryChange("Parac")
        inventoryVm.onStatusFilterChange("Tous")
        inventoryVm.openEditDialog(articleToEdit)

        // An unrelated screen changes stock.
        database.appDatabaseQueries.decreaseStockQuantity(
            quantity = 1.0,
            date = "2026-09-13",
            code_article = inventoryVm.uiState.value.articles.first { it.quantity >= 1.0 }.codeArticle
        )

        val after = inventoryVm.uiState.await { it.articles.isNotEmpty() && !it.isLoading }
        // Give the emission a chance to land before asserting nothing was clobbered.
        runBlocking { delay(200) }
        val settled = inventoryVm.uiState.value

        assertEquals("Parac", settled.searchQuery, "search text must survive a stock emission")
        assertEquals("Tous", settled.statusFilter, "filter must survive a stock emission")
        assertTrue(settled.isAddEditDialogOpen, "open dialog must survive a stock emission")
        assertEquals(articleToEdit.id, settled.articleToEdit?.id, "edit target must survive")
        assertTrue(after.articles.isNotEmpty())
    }

    @Test
    fun sortieDraftSurvivesAStockEmission() {
        val sortiesVm = SortiesViewModel(database, repository)
        val article = sortiesVm.uiState.value.availableArticles.first { it.quantity >= 5.0 }

        sortiesVm.uiState.value.draftLines.forEach { sortiesVm.removeDraftLine(it.articleId) }
        sortiesVm.addDraftLine(article, initialQuantity = 3.0)
        sortiesVm.updatePaperStubReference("BSP-MANUAL-9999")
        sortiesVm.updateRecipientName("Dr. Test Recipient")

        // Another screen receives stock while this draft is in progress.
        database.appDatabaseQueries.increaseStockQuantityById(
            quantity = 25.0,
            date = "2026-09-13",
            id = article.id
        )

        sortiesVm.uiState.await { state ->
            state.availableArticles.firstOrNull { it.id == article.id }
                ?.let { it.quantity >= article.quantity + 25.0 } == true
        }

        val settled = sortiesVm.uiState.value
        assertEquals(1, settled.draftLines.size, "draft line must survive a stock emission")
        assertEquals(3.0, settled.draftLines.first().issueQuantity)
        assertEquals("BSP-MANUAL-9999", settled.paperStubReference, "typed form field must survive")
        assertEquals("Dr. Test Recipient", settled.recipientName, "typed form field must survive")
    }

    @Test
    fun inventoryIsNotLoadingAfterConstructionBecauseStateIsPrimed() {
        val inventoryVm = InventoryViewModel(database, repository, CategoryRepository(database, Dispatchers.IO))
        val state = inventoryVm.uiState.value

        assertTrue(state.articles.isNotEmpty(), "primed synchronously, so the first frame has data")
        assertEquals(false, state.isLoading, "a primed screen must not show a spinner")
    }
}
