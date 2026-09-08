package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.orders.OrderDraftItem
import org.abdat.inventory.ui.orders.OrderStatusFilter
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.suppliers.ConventionDraftArticle
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
        viewModel.loadSupplierDetailsSync(state.selectedSupplier.id)
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.conventions.isNotEmpty())

        val balances = updatedState.conventionBalances[updatedState.conventions.first().id]
        assertNotNull(balances)
        assertTrue(balances.isNotEmpty())
    }

    @Test
    fun testMultiArticleConventionCreation() {
        val suppliersVm = SuppliersViewModel(database)
        val supplier = suppliersVm.uiState.value.suppliers.first()
        suppliersVm.selectSupplier(supplier)

        val multiArticles = listOf(
            ConventionDraftArticle("Gants Examen Latex M", "BOITE", 300.0),
            ConventionDraftArticle("Compresses Stériles 10x10", "PAQUET", 500.0),
            ConventionDraftArticle("Alcool Chirurgical 70°", "FLACON", 150.0)
        )

        suppliersVm.addConvention(
            ordNumber = 99L,
            date = "2026-09-08",
            totalMontant = 1200000.0,
            items = multiArticles
        )

        val updatedConventions = database.appDatabaseQueries.selectConventionsByFournisseur(supplier.id).executeAsList()
        val newConv = updatedConventions.firstOrNull { it.ord_number == 99L }
        assertNotNull(newConv)

        val convItems = database.appDatabaseQueries.selectConventionItemsByConventionId(newConv.id).executeAsList()
        assertEquals(3, convItems.size, "Expected 3 articles under the new convention")
    }

    @Test
    fun testOrderConventionArticleSelectionAndMultiLineCreation() {
        val ordersVm = OrdersViewModel(database)
        val suppliers = ordersVm.uiState.value.suppliers
        val biopharm = suppliers.first { it.fullName.contains("BIOPHARM") }

        ordersVm.selectSupplierForNewOrder(biopharm.id)
        val orderState = ordersVm.uiState.value

        assertTrue(orderState.conventionsForSupplier.isNotEmpty(), "Expected conventions for BIOPHARM")
        assertTrue(orderState.articlesForConvention.isNotEmpty(), "Expected convention articles for BIOPHARM")

        val article1 = orderState.articlesForConvention[0]
        val article2 = orderState.articlesForConvention[1]

        // Create multi-line purchase order
        val draftLines = listOf(
            OrderDraftItem(
                conventionItemId = article1.conventionItemId,
                designation = article1.designation,
                unite = article1.unite,
                quantity = 100.0,
                unitPrice = article1.unitPrice
            ),
            OrderDraftItem(
                conventionItemId = article2.conventionItemId,
                designation = article2.designation,
                unite = article2.unite,
                quantity = 50.0,
                unitPrice = article2.unitPrice
            )
        )

        val expectedTotal = (100.0 * article1.unitPrice) + (50.0 * article2.unitPrice)

        ordersVm.createOrder(
            reference = "BC-2026-TEST-MULTI",
            supplierId = biopharm.id,
            date = "2026-09-08",
            isDebt = false,
            items = draftLines
        )

        val newOrder = ordersVm.uiState.value.orders.firstOrNull { it.commande.referenceBon == "BC-2026-TEST-MULTI" }
        assertNotNull(newOrder)
        assertEquals(2, newOrder.itemsCount)
        assertEquals(expectedTotal, newOrder.totalAmountDzd)
    }

    @Test
    fun testOrdersViewModelAndFilter() {
        val viewModel = OrdersViewModel(database)
        val state = viewModel.uiState.value

        assertTrue(state.activeOrdersCount >= 3)
        assertTrue(state.totalCommittedDzd > 0.0)

        // Status filtering
        viewModel.onFilterChange(OrderStatusFilter.VALIDATED)
        val filteredState = viewModel.uiState.value
        assertTrue(filteredState.filteredOrders.all { it.statusLabel.contains("Validé") })
    }
}
