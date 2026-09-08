package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.orders.OrderStatusFilter
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.suppliers.SuppliersViewModel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreensViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        seedInitialDataIfEmpty(database)
    }

    @Test
    fun testDatabaseSeederPopulatesCoreEntities() {
        val articles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
        val suppliers = database.appDatabaseQueries.selectAllFournisseurs().executeAsList()
        val conventions = database.appDatabaseQueries.selectAllConventions().executeAsList()
        val orders = database.appDatabaseQueries.selectAllCommandes().executeAsList()

        assertTrue(articles.size >= 7, "Expected at least 7 seeded articles")
        assertTrue(suppliers.size >= 3, "Expected at least 3 seeded suppliers")
        assertTrue(conventions.size >= 2, "Expected seeded conventions")
        assertTrue(orders.size >= 3, "Expected seeded purchase orders")
    }

    @Test
    fun testInventoryViewModelStateAndFiltering() {
        val viewModel = InventoryViewModel(database)
        val state = viewModel.uiState.value

        assertEquals(7, state.totalArticlesCount)
        assertTrue(state.totalStockValueDzd > 0.0)
        assertEquals(1, state.outOfStockCount) // MED-003 Insuline has quantity 0

        // Search filtering
        viewModel.onSearchQueryChange("Paracétamol")
        val filteredState = viewModel.uiState.value
        assertEquals(1, filteredState.filteredArticles.size)
        assertEquals("MED-001", filteredState.filteredArticles.first().codeArticle)

        // Category filtering
        viewModel.onSearchQueryChange("")
        viewModel.onCategoryChange("Consommables")
        val categoryState = viewModel.uiState.value
        assertEquals(2, categoryState.filteredArticles.size)
    }

    @Test
    fun testSuppliersViewModelQuotaBalances() {
        val viewModel = SuppliersViewModel(database)
        val state = viewModel.uiState.value

        assertTrue(state.suppliers.isNotEmpty())
        assertNotNull(state.selectedSupplier)

        // Conventions for selected supplier
        viewModel.loadSupplierDetails(state.selectedSupplier!!.id)
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.conventions.isNotEmpty())

        val balances = updatedState.conventionBalances[updatedState.conventions.first().id]
        assertNotNull(balances)
        assertTrue(balances.isNotEmpty())
    }

    @Test
    fun testOrdersViewModelAndFilter() {
        val viewModel = OrdersViewModel(database)
        val state = viewModel.uiState.value

        assertEquals(3, state.activeOrdersCount)
        assertTrue(state.totalCommittedDzd > 0.0)

        // Status filtering
        viewModel.onFilterChange(OrderStatusFilter.VALIDATED)
        val filteredState = viewModel.uiState.value
        assertTrue(filteredState.filteredOrders.all { it.statusLabel.contains("Validé") })
    }
}
