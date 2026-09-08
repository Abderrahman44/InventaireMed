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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun testAddArticleToExistingConvention() {
        val suppliersVm = SuppliersViewModel(database)
        val supplier = suppliersVm.uiState.value.suppliers.first()
        suppliersVm.selectSupplier(supplier)

        val conv = suppliersVm.uiState.value.conventions.first()
        suppliersVm.selectConventionForDetail(conv)

        assertEquals(conv.id, suppliersVm.uiState.value.selectedConventionForDetail?.id)

        val initialCount = suppliersVm.uiState.value.conventionBalances[conv.id]?.size ?: 0

        // Append new article to the active convention
        suppliersVm.addArticleToConventionSync(
            conventionId = conv.id,
            designation = "Bétadine Dermique 10%",
            unite = "FLACON",
            quantity = 400.0,
            minQuantity = 20.0
        )

        val updatedBalances = suppliersVm.uiState.value.conventionBalances[conv.id]
        assertNotNull(updatedBalances)
        assertEquals(initialCount + 1, updatedBalances.size)

        val newArticleBalance = updatedBalances.firstOrNull { it.designation == "Bétadine Dermique 10%" }
        assertNotNull(newArticleBalance)
        assertEquals("FLACON", newArticleBalance.unite)
        assertEquals(400.0, newArticleBalance.agreedQuantity)
        assertEquals(400.0, newArticleBalance.remainingQuantity)
        assertEquals(20.0, newArticleBalance.minQuantity)
    }

    @Test
    fun testSensitiveEditArticleWorkflow() {
        val suppliersVm = SuppliersViewModel(database)
        val supplier = suppliersVm.uiState.value.suppliers.first()
        suppliersVm.selectSupplier(supplier)

        val conv = suppliersVm.uiState.value.conventions.first()
        suppliersVm.selectConventionForDetail(conv)

        val article = suppliersVm.uiState.value.conventionBalances[conv.id]!!.first()

        // 1. User requests edit -> sensitive warning alert opens
        suppliersVm.requestEditArticle(article)
        val warningState = suppliersVm.uiState.value
        assertTrue(warningState.isSensitiveWarningDialogOpen)
        assertFalse(warningState.isEditArticleDialogOpen)
        assertEquals(article.conventionItemId, warningState.articlePendingEdit?.conventionItemId)

        // 2. User confirms sensitive alert -> edit dialog opens
        suppliersVm.confirmSensitiveWarning()
        val editState = suppliersVm.uiState.value
        assertFalse(editState.isSensitiveWarningDialogOpen)
        assertTrue(editState.isEditArticleDialogOpen)

        // 3. User saves modifications
        val newQuota = article.agreedQuantity + 250.0
        suppliersVm.updateConventionArticleSync(
            itemId = article.conventionItemId,
            conventionId = conv.id,
            designation = "${article.designation} (Avenant)",
            unite = article.unite,
            quantity = newQuota,
            minQuantity = 15.0
        )

        val finalState = suppliersVm.uiState.value
        assertFalse(finalState.isEditArticleDialogOpen)

        val updatedArticle = finalState.conventionBalances[conv.id]!!
            .first { it.conventionItemId == article.conventionItemId }

        assertEquals("${article.designation} (Avenant)", updatedArticle.designation)
        assertEquals(newQuota, updatedArticle.agreedQuantity)
        assertEquals(15.0, updatedArticle.minQuantity)

        // 4. Test back navigation to suppliers overview
        suppliersVm.closeConventionDetail()
        assertNull(suppliersVm.uiState.value.selectedConventionForDetail)
    }

    @Test
    fun testSupplierProfileDetailsInspectionAndSeeding() {
        val suppliersVm = SuppliersViewModel(database)
        val state = suppliersVm.uiState.value

        val biopharm = state.suppliers.firstOrNull { it.fullName.contains("BIOPHARM", ignoreCase = true) }
        assertNotNull(biopharm, "Expected BIOPHARM supplier in seeded database")
        assertEquals("Biopharm Distribution Logistique SPA", biopharm.societe)
        assertEquals("+213 21 78 90 12", biopharm.phone)
        assertEquals("+213 661 45 67 89", biopharm.responsablePhone)
        assertEquals("Oued Smar, Alger", biopharm.ville)
        assertEquals("commandes@biopharm-dz.com", biopharm.email)
        assertEquals("wa.me/213661456789 | t.me/BiopharmSupply", biopharm.otherLinks)
    }

    @Test
    fun testSupplierProfileNavigationAndEditWorkflow() {
        val suppliersVm = SuppliersViewModel(database)
        val initialSupplier = suppliersVm.uiState.value.suppliers.first()
        suppliersVm.selectSupplier(initialSupplier)

        // 1. Open profile screen
        suppliersVm.openSupplierProfile()
        assertTrue(suppliersVm.uiState.value.isProfileViewOpen)
        assertFalse(suppliersVm.uiState.value.isEditProfileDialogOpen)

        // 2. Open edit profile dialog
        suppliersVm.openEditProfileDialog()
        assertTrue(suppliersVm.uiState.value.isEditProfileDialogOpen)

        // 3. Save profile updates
        suppliersVm.updateSupplierProfileSync(
            id = initialSupplier.id,
            fullName = "SARL INNOMED HEALTHCARE",
            societe = "INNOMED ALGERIE SPA",
            phone = "023 45 67 89",
            responsablePhone = "0661 99 88 77",
            ville = "Blida (Zone Industrielle)",
            email = "contact@innomed.dz",
            otherLinks = "@innomed_dz_telegram"
        )

        val updatedState = suppliersVm.uiState.value
        assertFalse(updatedState.isEditProfileDialogOpen)

        // Verify selected supplier reflects update
        val updatedSelected = updatedState.selectedSupplier
        assertNotNull(updatedSelected)
        assertEquals("SARL INNOMED HEALTHCARE", updatedSelected.fullName)
        assertEquals("INNOMED ALGERIE SPA", updatedSelected.societe)
        assertEquals("023 45 67 89", updatedSelected.phone)
        assertEquals("0661 99 88 77", updatedSelected.responsablePhone)
        assertEquals("Blida (Zone Industrielle)", updatedSelected.ville)
        assertEquals("contact@innomed.dz", updatedSelected.email)
        assertEquals("@innomed_dz_telegram", updatedSelected.otherLinks)

        // Verify direct DB query reflects update
        val dbRecord = database.appDatabaseQueries.selectFournisseurById(initialSupplier.id).executeAsOne()
        assertEquals("SARL INNOMED HEALTHCARE", dbRecord.full_name)
        assertEquals("INNOMED ALGERIE SPA", dbRecord.societe)
        assertEquals("023 45 67 89", dbRecord.phone)
        assertEquals("0661 99 88 77", dbRecord.responsable_phone)
        assertEquals("Blida (Zone Industrielle)", dbRecord.ville)
        assertEquals("contact@innomed.dz", dbRecord.email)
        assertEquals("@innomed_dz_telegram", dbRecord.other_links)

        // 4. Close profile view
        suppliersVm.closeSupplierProfile()
        assertFalse(suppliersVm.uiState.value.isProfileViewOpen)
    }

    @Test
    fun testAddSupplierWithFullDetails() {
        val suppliersVm = SuppliersViewModel(database)
        val initialCount = suppliersVm.uiState.value.suppliers.size

        suppliersVm.addSupplierFullSync(
            name = "LABORATOIRES MERCK ALGERIE",
            societe = "MERCK SANTE SPA",
            phone = "021 54 32 10",
            responsablePhone = "0770 11 22 33",
            ville = "Oran",
            email = "info@merck.dz",
            otherLinks = "https://t.me/merck_algerie"
        )

        val newState = suppliersVm.uiState.value
        assertEquals(initialCount + 1, newState.suppliers.size)

        val created = newState.suppliers.firstOrNull { it.fullName == "LABORATOIRES MERCK ALGERIE" }
        assertNotNull(created)
        assertEquals("MERCK SANTE SPA", created.societe)
        assertEquals("021 54 32 10", created.phone)
        assertEquals("0770 11 22 33", created.responsablePhone)
        assertEquals("Oran", created.ville)
        assertEquals("info@merck.dz", created.email)
        assertEquals("https://t.me/merck_algerie", created.otherLinks)
    }
}
