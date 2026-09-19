package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.database.seedInitialDataIfEmpty
import org.abdat.inventory.ui.deliveries.DeliveriesViewModel
import org.abdat.inventory.ui.deliveries.ReceptionType
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.abdat.inventory.ui.orders.OrderDraftItem
import org.abdat.inventory.ui.orders.OrderStatusFilter
import org.abdat.inventory.ui.orders.OrdersViewModel
import org.abdat.inventory.ui.services.ServicesViewModel
import org.abdat.inventory.ui.sorties.SortieMotive
import org.abdat.inventory.ui.sorties.SortiesViewModel
import org.abdat.inventory.ui.decharges.DechargeDraftLine
import org.abdat.inventory.ui.decharges.DechargeViewModel
import org.abdat.inventory.ui.personnel.PersonnelViewModel
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
        // Mirrors the Koin factory: reference taxonomy is applied independently of the mock seeder.
        ensureCategoryTaxonomy(database)
    }

    /** Stock-reading ViewModels observe this rather than querying `stock_article` themselves. */
    private fun stockRepo() = StockRepository(database, Dispatchers.IO)

    private fun categoryRepo() = CategoryRepository(database, Dispatchers.IO)

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
        val viewModel = InventoryViewModel(database, stockRepo(), categoryRepo())
        val state = viewModel.uiState.value

        assertEquals(20, state.totalArticlesCount)
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
    fun testConventionCreationWithTvaRateAndOrderTaxCalculation() {
        val suppliersVm = SuppliersViewModel(database)
        val supplier = suppliersVm.uiState.value.suppliers.first()
        suppliersVm.selectSupplier(supplier)

        val articleDraft = listOf(
            ConventionDraftArticle("Gants Examen Nitrile", "BOITE", 100.0, 500.0)
        )

        suppliersVm.addConvention(
            ordNumber = 101L,
            date = "2026-09-15",
            totalMontant = 500000.0,
            tvaRate = 19.0,
            items = articleDraft
        )

        val updatedConventions = database.appDatabaseQueries.selectConventionsByFournisseur(supplier.id).executeAsList()
        val createdConv = updatedConventions.firstOrNull { it.ord_number == 101L }
        assertNotNull(createdConv)
        assertEquals(19.0, createdConv.tva_rate)

        val ordersVm = OrdersViewModel(database)
        ordersVm.selectSupplierForNewOrder(supplier.id)
        ordersVm.selectConventionForNewOrder(createdConv.id)

        val articles = ordersVm.uiState.value.articlesForConvention
        val article = articles.first { it.designation == "Gants Examen Nitrile" }

        val draftLines = listOf(
            OrderDraftItem(
                conventionItemId = article.conventionItemId,
                designation = article.designation,
                unite = article.unite,
                quantity = 10.0,
                unitPrice = 500.0
            )
        )

        ordersVm.createOrder(
            reference = "BC-TVA-TEST",
            supplierId = supplier.id,
            date = "2026-09-15",
            isDebt = false,
            items = draftLines
        )

        val createdOrder = ordersVm.uiState.value.orders.firstOrNull { it.commande.referenceBon == "BC-TVA-TEST" }
        assertNotNull(createdOrder)
        assertEquals(5000.0, createdOrder.subtotalHtDzd)
        assertEquals(19.0, createdOrder.tvaRate)
        assertEquals(950.0, createdOrder.tvaAmountDzd)
        assertEquals(5950.0, createdOrder.totalAmountDzd)
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

        val expectedSubtotalHt = (100.0 * article1.unitPrice) + (50.0 * article2.unitPrice)
        val convTvaRate = orderState.conventionsForSupplier.first { it.id == article1.conventionId }.tvaRate
        val expectedTtc = expectedSubtotalHt + (expectedSubtotalHt * (convTvaRate / 100.0))

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
        assertEquals(expectedSubtotalHt, newOrder.subtotalHtDzd)
        assertEquals(convTvaRate, newOrder.tvaRate)
        assertEquals(expectedTtc, newOrder.totalAmountDzd)
    }

    @Test
    fun testOrdersViewModelAndFilter() {
        val viewModel = OrdersViewModel(database)
        val state = viewModel.uiState.value

        assertTrue(state.orders.size >= 4)
        assertEquals(3, state.activeOrdersCount, "Expected 3 active unclosed orders")
        assertTrue(state.totalCommittedDzd > 0.0)

        // Status filtering
        viewModel.onFilterChange(OrderStatusFilter.VALIDATED)
        val filteredState = viewModel.uiState.value
        assertTrue(filteredState.filteredOrders.all { it.statusLabel.contains("Validé") || it.statusLabel.contains("Dette") })
    }

    @Test
    fun testOrderDetailSelectionAndLineFulfillmentReconciliation() {
        val viewModel = OrdersViewModel(database)
        val state = viewModel.uiState.value

        val targetCmd = state.orders.first { it.commande.referenceBon == "BC-2026-0841" }
        viewModel.selectOrderForDetail(targetCmd.commande.id)

        val detailData = viewModel.uiState.value.selectedOrderForDetail
        assertNotNull(detailData, "selectedOrderForDetail should not be null after selection")
        assertEquals(targetCmd.commande.id, detailData.displayItem.commande.id)
        assertEquals("BC-2026-0841", detailData.displayItem.commande.referenceBon)

        // 2 line items in BC-2026-0841
        assertEquals(2, detailData.lines.size)

        // Line 1: MED-001 Paracétamol (500 ordered, 300 + 200 delivered -> 500 -> Livré)
        val line1 = detailData.lines.first { it.item.codeArticle == "MED-001" }
        assertEquals(500.0, line1.item.quantity)
        assertEquals(500.0, line1.deliveredQuantity)
        assertEquals(0.0, line1.remainingQuantity)
        assertTrue(line1.isFulfilled)
        assertEquals("Livré", line1.statusLabel)

        // Line 2: MED-002 Amoxicilline (200 ordered, 100 delivered -> Pour livraison partielle)
        val line2 = detailData.lines.first { it.item.codeArticle == "MED-002" }
        assertEquals(200.0, line2.item.quantity)
        assertEquals(100.0, line2.deliveredQuantity)
        assertEquals(100.0, line2.remainingQuantity)
        assertFalse(line2.isFulfilled)
        assertEquals("Pour livraison partielle", line2.statusLabel)

        // 2 linked BLs
        assertEquals(2, detailData.linkedDeliveries.size)
        assertTrue(detailData.linkedDeliveries.any { it.referenceBon == "BL-2026-0310" })
        assertTrue(detailData.linkedDeliveries.any { it.referenceBon == "BL-2026-0312" })

        // Test unfulfilled order (BC-2026-0843 - no deliveries)
        val unfulfilledCmd = state.orders.first { it.commande.referenceBon == "BC-2026-0843" }
        viewModel.selectOrderForDetail(unfulfilledCmd.commande.id)
        val unfulfilledDetail = viewModel.uiState.value.selectedOrderForDetail
        assertNotNull(unfulfilledDetail)
        assertEquals(1, unfulfilledDetail.lines.size)
        val unfLine = unfulfilledDetail.lines.first()
        assertEquals(0.0, unfLine.deliveredQuantity)
        assertEquals(50.0, unfLine.remainingQuantity)
        assertFalse(unfLine.isFulfilled)
        assertEquals("Pour livraison partielle", unfLine.statusLabel)
        assertTrue(unfulfilledDetail.linkedDeliveries.isEmpty())

        // Test deselecting order returns to null
        viewModel.selectOrderForDetail(null)
        assertNull(viewModel.uiState.value.selectedOrderForDetail)

        // Verify that filters and search query survive order selection and back navigation
        viewModel.onSearchQueryChange("BC-2026")
        viewModel.onFilterChange(OrderStatusFilter.VALIDATED)
        viewModel.selectOrderForDetail(targetCmd.commande.id)
        assertNotNull(viewModel.uiState.value.selectedOrderForDetail)

        // Navigate back
        viewModel.selectOrderForDetail(null)
        assertEquals("BC-2026", viewModel.uiState.value.searchQuery)
        assertEquals(OrderStatusFilter.VALIDATED, viewModel.uiState.value.selectedFilter)
        assertNull(viewModel.uiState.value.selectedOrderForDetail)
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

    @Test
    fun testDeliveriesViewModelInitialStateAndReconciliation() {
        val viewModel = DeliveriesViewModel(database)
        val state = viewModel.uiState.value

        assertTrue(state.deliveries.isNotEmpty(), "Expected seeded delivery receipts in history")
        assertTrue(state.monthlyVolumeDzd > 0.0, "Expected positive monthly intake volume")
        assertTrue(state.availableCommandes.isNotEmpty(), "Expected available purchase orders")
        assertNotNull(state.selectedCommandeId, "Expected selected purchase order ID")
        assertTrue(state.verificationLines.isNotEmpty(), "Expected verification lines loaded for selected BC")

        val lineWithRemaining = state.verificationLines.first { it.remainingQuantity > 0.0 }
        assertTrue(lineWithRemaining.orderedQuantity > 0.0)
        assertEquals(lineWithRemaining.remainingQuantity, lineWithRemaining.receivedQuantity)
        assertTrue(lineWithRemaining.isConforming)
        assertFalse(lineWithRemaining.isDeficit)
        assertFalse(lineWithRemaining.isSurplus)
        assertEquals(0.0, lineWithRemaining.gap)
    }

    @Test
    fun testDeliveriesConformityCalculationAndDeficit() {
        val viewModel = DeliveriesViewModel(database)
        val initialLines = viewModel.uiState.value.verificationLines
        assertTrue(initialLines.isNotEmpty())

        val targetLine = initialLines.first { it.remainingQuantity > 5.0 }

        // 1. Reduce received quantity -> Deficit
        viewModel.updateReceivedQuantity(targetLine.commandeItemId, targetLine.remainingQuantity - 2.0)
        val deficitState = viewModel.uiState.value
        val updatedLine = deficitState.verificationLines.first { it.commandeItemId == targetLine.commandeItemId }

        assertFalse(updatedLine.isConforming)
        assertTrue(updatedLine.isDeficit)
        assertFalse(updatedLine.isSurplus)
        assertEquals(-2.0, updatedLine.gap)
        assertEquals("Réserve Émise", updatedLine.qualityCheckStatus)
        assertEquals(ReceptionType.PARTIAL, deficitState.receptionType)
        assertEquals(1, deficitState.activeAnomaliesCount)
        assertTrue(deficitState.discrepancyNote.isNotBlank())

        // 2. Increase received quantity -> Surplus
        viewModel.updateReceivedQuantity(targetLine.commandeItemId, targetLine.remainingQuantity + 3.0)
        val surplusState = viewModel.uiState.value
        val surplusLine = surplusState.verificationLines.first { it.commandeItemId == targetLine.commandeItemId }

        assertFalse(surplusLine.isConforming)
        assertFalse(surplusLine.isDeficit)
        assertTrue(surplusLine.isSurplus)
        assertEquals(3.0, surplusLine.gap)
        assertEquals("Surplus Constaté", surplusLine.qualityCheckStatus)

        // 3. Reset to matching quantity -> Conforming
        viewModel.updateReceivedQuantity(targetLine.commandeItemId, targetLine.remainingQuantity)
        val conformingState = viewModel.uiState.value
        val conformingLine = conformingState.verificationLines.first { it.commandeItemId == targetLine.commandeItemId }

        assertTrue(conformingLine.isConforming)
        assertFalse(conformingLine.isDeficit)
        assertFalse(conformingLine.isSurplus)
        assertEquals(0.0, conformingLine.gap)
        assertEquals("Visuel OK", conformingLine.qualityCheckStatus)
    }

    @Test
    fun testDeliveriesValidationStockIncrementAndHistory() {
        val viewModel = DeliveriesViewModel(database)
        val state = viewModel.uiState.value

        val targetLine = state.verificationLines.first { it.remainingQuantity > 0.0 }
        val initialStockRecord = database.appDatabaseQueries.selectStockArticleByCode(targetLine.codeArticle).executeAsOne()
        val initialStockQty = initialStockRecord.quantity

        val receivedQty = 50.0
        viewModel.updateReceivedQuantity(targetLine.commandeItemId, receivedQty)

        val service = state.availableServices.firstOrNull()
        val beneficiaire = state.availableBeneficiaries.firstOrNull()
        val uniqueBlRef = "BL-TEST-7788"
        val success = viewModel.validateDeliverySync(
            referenceBon = uniqueBlRef,
            date = "2026-09-08",
            serviceId = service?.id,
            beneficiaireId = beneficiaire?.id,
            isPartial = false
        )

        assertTrue(success, "Delivery validation should succeed")

        // Verify stock quantity was incremented atomically
        val updatedStockRecord = database.appDatabaseQueries.selectStockArticleByCode(targetLine.codeArticle).executeAsOne()
        assertEquals(initialStockQty + receivedQty, updatedStockRecord.quantity)

        // Verify delivery record exists in DB with service and beneficiary IDs
        val allLivraisons = database.appDatabaseQueries.selectAllLivraisons().executeAsList()
        val createdDelivery = allLivraisons.firstOrNull { it.reference_bon == uniqueBlRef }
        assertNotNull(createdDelivery)
        assertEquals(service?.id, createdDelivery.service_id)
        assertEquals(beneficiaire?.id, createdDelivery.beneficiaire_id)

        // Verify delivery items
        val deliveryItems = database.appDatabaseQueries.selectLivraisonItemsByLivraisonId(createdDelivery.id).executeAsList()
        assertTrue(deliveryItems.isNotEmpty())

        // Verify UI state historical register
        val updatedState = viewModel.uiState.value
        val historyDelivery = updatedState.deliveries.firstOrNull { it.paperBlReference == uniqueBlRef }
        assertNotNull(historyDelivery)
        if (service != null) {
            assertEquals(service.name, historyDelivery.serviceName)
        }
        if (beneficiaire != null) {
            assertEquals(beneficiaire.fullName, historyDelivery.beneficiaireName)
        }
    }

    @Test
    fun testDeliveriesBeneficiarySelectionAndChefDeServiceResolution() {
        val viewModel = DeliveriesViewModel(database)
        val state = viewModel.uiState.value

        // 1. Initial services and beneficiaries should be loaded
        assertTrue(state.availableServices.isNotEmpty(), "Available services should not be empty")
        assertNotNull(state.selectedServiceId, "A service should be selected by default")
        assertNotNull(state.selectedServiceName, "Service name should be populated")

        // 2. Select a specific service
        val targetService = state.availableServices.first { 
            it.name.contains("Pharmacie", ignoreCase = true) || it.name.contains("Urgences", ignoreCase = true)
        }
        viewModel.selectService(targetService.id)

        val updatedState = viewModel.uiState.value
        assertEquals(targetService.id, updatedState.selectedServiceId)
        assertEquals(targetService.name, updatedState.selectedServiceName)

        // 3. Chef de service should be resolved for this service
        val personsForService = database.appDatabaseQueries.selectPersonsByServiceId(targetService.id).executeAsList()
        val expectedChef = personsForService.firstOrNull { 
            it.notes.contains("Chef", ignoreCase = true) || 
            it.notes.contains("Responsable", ignoreCase = true)
        } ?: personsForService.firstOrNull()

        if (expectedChef != null) {
            assertTrue(updatedState.chefDeServiceName.contains(expectedChef.full_name))
        }

        // 4. Beneficiaries should belong to selected service
        assertTrue(updatedState.availableBeneficiaries.all { it.serviceId == targetService.id })

        // 5. Select a specific beneficiary
        if (updatedState.availableBeneficiaries.isNotEmpty()) {
            val targetBeneficiary = updatedState.availableBeneficiaries.first()
            viewModel.selectBeneficiary(targetBeneficiary.id)

            val finalState = viewModel.uiState.value
            assertEquals(targetBeneficiary.id, finalState.selectedBeneficiaryId)
            assertEquals(targetBeneficiary.fullName, finalState.selectedBeneficiaryName)
        }
    }

    @Test
    fun testDeliveriesSearchFilter() {
        val viewModel = DeliveriesViewModel(database)
        val initialCount = viewModel.uiState.value.deliveries.size
        assertTrue(initialCount >= 2)

        // Filter by BL reference
        viewModel.onSearchQueryChange("BL-2026-0312")
        assertEquals(1, viewModel.uiState.value.filteredDeliveries.size)
        assertEquals("BL-2026-0312", viewModel.uiState.value.filteredDeliveries.first().paperBlReference)

        // Clear query
        viewModel.onSearchQueryChange("")
        assertEquals(initialCount, viewModel.uiState.value.filteredDeliveries.size)
    }

    @Test
    fun testSortiesViewModelInitialStateAndKPIs() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val state = viewModel.uiState.value

        assertTrue(state.sorties.isNotEmpty(), "Expected seeded issue slips in history")
        assertTrue(state.services.isNotEmpty(), "Expected seeded services")
        assertNotNull(state.selectedService, "Expected default selected service")
        assertTrue(state.localsForSelectedService.isNotEmpty(), "Expected locals for default selected service")
        assertTrue(state.availableArticles.isNotEmpty(), "Expected available stock articles")
        assertTrue(state.draftLines.isEmpty(), "Expected initial draft lines to be empty")
        assertTrue(state.todaySortiesCount > 0, "Expected positive sorties today KPI")
        assertTrue(state.monthlyDistributedDzd > 0.0, "Expected positive monthly distributed KPI")
        assertTrue(state.activeServicesCount > 0, "Expected active services count")
    }

    @Test
    fun testSortiesServiceToLocalCascadingSelection() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val allServices = viewModel.uiState.value.services
        assertTrue(allServices.size >= 2)

        val targetService = allServices.first { it.name.contains("Chirurgie", ignoreCase = true) }
        viewModel.selectService(targetService.id)

        val updatedState = viewModel.uiState.value
        assertEquals(targetService.id, updatedState.selectedService?.id)
        assertTrue(updatedState.localsForSelectedService.isNotEmpty())
        assertTrue(updatedState.localsForSelectedService.all { it.serviceId == targetService.id })
        assertNotNull(updatedState.selectedLocal)
        assertEquals(targetService.id, updatedState.selectedLocal?.serviceId)
    }

    @Test
    fun testSortiesDraftLineManagementAndStatusBadges() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val article = viewModel.uiState.value.availableArticles.first { it.quantity > 50.0 }

        // Remove initial lines to test from scratch
        viewModel.uiState.value.draftLines.forEach { viewModel.removeDraftLine(it.articleId) }
        assertEquals(0, viewModel.uiState.value.draftLines.size)

        // 1. Add article
        viewModel.addDraftLine(article, 5.0)
        val lineAfterAdd = viewModel.uiState.value.draftLines.first()
        assertEquals(article.id, lineAfterAdd.articleId)
        assertEquals(5.0, lineAfterAdd.issueQuantity)
        assertTrue(lineAfterAdd.isSufficient)
        assertFalse(lineAfterAdd.isBelowMin)
        assertFalse(lineAfterAdd.isExceeded)

        // 2. Update quantity to trigger below min threshold
        val nearExhaustionQty = article.quantity - 10.0
        viewModel.updateIssueQuantity(article.id, nearExhaustionQty)
        val lineNearExhaustion = viewModel.uiState.value.draftLines.first()
        assertFalse(lineNearExhaustion.isSufficient)
        assertTrue(lineNearExhaustion.isBelowMin)
        assertFalse(lineNearExhaustion.isExceeded)

        // 3. Update quantity exceeding available stock -> Error status
        val excessiveQty = article.quantity + 50.0
        viewModel.updateIssueQuantity(article.id, excessiveQty)
        val lineExceeded = viewModel.uiState.value.draftLines.first()
        assertTrue(lineExceeded.isExceeded)
        assertTrue(viewModel.uiState.value.hasExceededLines)
        assertFalse(viewModel.uiState.value.canSubmit)

        // 4. Remove line
        viewModel.removeDraftLine(article.id)
        assertEquals(0, viewModel.uiState.value.draftLines.size)
    }

    @Test
    fun testSortiesAtomicValidationAndStockDecrement() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val article = viewModel.uiState.value.availableArticles.first { it.quantity > 20.0 }
        val initialStockQty = article.quantity

        // Clear draft and add clean line
        viewModel.uiState.value.draftLines.forEach { viewModel.removeDraftLine(it.articleId) }
        val issueQty = 8.0
        viewModel.addDraftLine(article, issueQty)

        val service = viewModel.uiState.value.selectedService!!
        val local = viewModel.uiState.value.selectedLocal
        val testRef = "BS-TEST-ATOMIC-01"

        val success = viewModel.validateSortieSync(
            referenceBon = testRef,
            serviceId = service.id,
            localId = local?.id,
            recipient = "Dr. Test Requester",
            date = "2026-09-08 11:00"
        )

        assertTrue(success, "Sortie validation should succeed")

        // Verify stock in database was decremented by exactly issueQty
        val updatedStock = database.appDatabaseQueries.selectStockArticleByCode(article.codeArticle).executeAsOne()
        assertEquals(initialStockQty - issueQty, updatedStock.quantity)

        // Verify sortie record exists in DB
        val allSorties = database.appDatabaseQueries.selectAllSorties().executeAsList()
        val createdSortie = allSorties.firstOrNull { it.reference_bon == testRef }
        assertNotNull(createdSortie)

        // Verify sortie items
        val sortieItems = database.appDatabaseQueries.selectSortieItemsBySortieId(createdSortie.id).executeAsList()
        assertEquals(1, sortieItems.size)
        assertEquals(issueQty, sortieItems.first().quantity)

        // Verify UI state historical register
        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.sorties.any { it.referenceBon == testRef })
    }

    @Test
    fun testSortiesOverAllocationPrevention() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val article = viewModel.uiState.value.availableArticles.first()
        val initialStockQty = article.quantity

        // Clear draft and add line with excessive quantity
        viewModel.uiState.value.draftLines.forEach { viewModel.removeDraftLine(it.articleId) }
        val impossibleQty = initialStockQty + 1000.0
        viewModel.addDraftLine(article, 1.0)
        viewModel.updateIssueQuantity(article.id, impossibleQty)

        val service = viewModel.uiState.value.selectedService!!
        val testRef = "BS-TEST-OVERALLOC-01"

        val success = viewModel.validateSortieSync(
            referenceBon = testRef,
            serviceId = service.id,
            localId = null,
            recipient = "Dr. Test",
            date = "2026-09-08 11:00"
        )

        assertFalse(success, "Sortie validation should fail when quantity exceeds available stock")
        assertNotNull(viewModel.uiState.value.actionErrorMessage)

        // Ensure stock was NOT decremented
        val stockCheck = database.appDatabaseQueries.selectStockArticleByCode(article.codeArticle).executeAsOne()
        assertEquals(initialStockQty, stockCheck.quantity)
    }

    @Test
    fun testSortiesSearchQueryFiltering() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val initialCount = viewModel.uiState.value.sorties.size
        assertTrue(initialCount >= 2)

        // Filter by reference
        viewModel.onSearchQueryChange("BS-2026-001")
        assertEquals(1, viewModel.uiState.value.filteredSorties.size)
        assertEquals("BS-2026-001", viewModel.uiState.value.filteredSorties.first().referenceBon)

        // Filter by service
        viewModel.onSearchQueryChange("Chirurgie")
        assertTrue(viewModel.uiState.value.filteredSorties.all { it.serviceName.contains("Chirurgie", ignoreCase = true) })

        // Clear query
        viewModel.onSearchQueryChange("")
    }

    @Test
    fun testSortiesChefDeServiceAndBeneficiaryPersonnelAllocation() {
        val viewModel = SortiesViewModel(database, stockRepo())
        val services = viewModel.uiState.value.services
        assertTrue(services.isNotEmpty())

        val testService = services.first()
        viewModel.selectService(testService.id)

        val stateAfterSelect = viewModel.uiState.value
        assertEquals(testService.id, stateAfterSelect.selectedService?.id)
        assertTrue(stateAfterSelect.chefDeServiceName.isNotBlank(), "Chef de Service must be automatically resolved")
        val resolvedChef = stateAfterSelect.chefDeServiceName

        val staff = stateAfterSelect.availableServiceStaff
        assertTrue(staff.isNotEmpty(), "Selected service should have staff available")

        // 1. Scenario: Empty beneficiary selection -> Whole service sortie
        viewModel.clearBeneficiaries()
        assertTrue(viewModel.uiState.value.selectedBeneficiaryIds.isEmpty())

        val article = viewModel.uiState.value.availableArticles.first { it.quantity >= 10.0 }
        viewModel.addDraftLine(article, 2.0)

        val collectiveRef = "BS-TEST-COLLECTIVE"
        val success1 = viewModel.validateSortieSync(
            referenceBon = collectiveRef,
            serviceId = testService.id,
            localId = stateAfterSelect.selectedLocal?.id,
            demandeur = "",
            recepteurInterne = "",
            motif = "Dotation collective pour tout le service",
            date = "2026-09-18 10:00"
        )
        assertTrue(success1)

        val sortie1 = viewModel.uiState.value.sorties.first { it.referenceBon == collectiveRef }
        assertEquals(resolvedChef, sortie1.chefDeServiceName)
        assertTrue(sortie1.beneficiaireNames.contains("Ensemble du service"))

        // 2. Scenario: Single beneficiary selection (e.g. 1 person for uniform/clothes)
        val singlePerson = staff.first()
        viewModel.selectService(testService.id)
        viewModel.toggleBeneficiary(singlePerson.id)
        assertEquals(1, viewModel.uiState.value.selectedBeneficiaryIds.size)
        assertEquals(listOf(singlePerson.id), viewModel.uiState.value.selectedBeneficiaryIds)

        viewModel.addDraftLine(article, 1.0)
        val singleRef = "BS-TEST-SINGLE"
        val success2 = viewModel.validateSortieSync(
            referenceBon = singleRef,
            serviceId = testService.id,
            localId = stateAfterSelect.selectedLocal?.id,
            demandeur = "",
            recepteurInterne = "",
            motif = "Blouse médicale nominative",
            date = "2026-09-18 10:15"
        )
        assertTrue(success2)

        val sortie2 = viewModel.uiState.value.sorties.first { it.referenceBon == singleRef }
        assertEquals(resolvedChef, sortie2.chefDeServiceName)
        assertEquals(singlePerson.fullName, sortie2.beneficiaireNames)

        // 3. Scenario: Multiple beneficiary selection (e.g. 2-3 persons sharing a printer/device)
        if (staff.size >= 2) {
            val p1 = staff[0]
            val p2 = staff[1]
            viewModel.selectService(testService.id)
            viewModel.toggleBeneficiary(p1.id)
            viewModel.toggleBeneficiary(p2.id)
            assertEquals(2, viewModel.uiState.value.selectedBeneficiaryIds.size)

            viewModel.addDraftLine(article, 1.0)
            val multiRef = "BS-TEST-MULTI"
            val success3 = viewModel.validateSortieSync(
                referenceBon = multiRef,
                serviceId = testService.id,
                localId = stateAfterSelect.selectedLocal?.id,
                demandeur = "",
                recepteurInterne = "",
                motif = "Imprimante laser partagée",
                date = "2026-09-18 10:30"
            )
            assertTrue(success3)

            val sortie3 = viewModel.uiState.value.sorties.first { it.referenceBon == multiRef }
            assertEquals(resolvedChef, sortie3.chefDeServiceName)
            assertTrue(sortie3.beneficiaireNames.contains(p1.fullName))
            assertTrue(sortie3.beneficiaireNames.contains(p2.fullName))
        }

        // 4. Scenario: Verify CSV export mapper contains Chef de Service and Bénéficiaires columns
        val (headers, rows) = org.abdat.inventory.util.SortiesCsvMapper.toHeadersAndRows(viewModel.uiState.value.sorties)
        assertTrue(headers.contains("Chef de Service"))
        assertTrue(headers.contains("Bénéficiaires"))
        val chefIdx = headers.indexOf("Chef de Service")
        val benefIdx = headers.indexOf("Bénéficiaires")
        assertTrue(chefIdx >= 0)
        assertTrue(benefIdx >= 0)
        val matchingRow = rows.first { it[headers.indexOf("Numéro BS")] == singleRef }
        assertEquals(resolvedChef, matchingRow[chefIdx])
        assertEquals(singlePerson.fullName, matchingRow[benefIdx])
    }

    @Test
    fun testOrderLifecycleMathematicalProgressionAndStatusTransitions() {
        val ordersVm = OrdersViewModel(database)
        val orders = ordersVm.uiState.value.orders

        // 1. Check BC-2026-0841 (partial delivery: 300+200 Paracetamol delivered out of 500, 100 Amoxicilline delivered out of 200)
        // Total delivered = 600, total ordered = 700 -> ~85% progress
        val partialCmd = orders.first { it.commande.referenceBon == "BC-2026-0841" }
        assertEquals("Partiellement Reçu", partialCmd.statusLabel)
        assertEquals(85, partialCmd.progressPercent)
        assertEquals(2, partialCmd.linkedDeliveriesCount)
        assertTrue(partialCmd.linkedDeliveryRefs.contains("BL-2026-0310"))
        assertTrue(partialCmd.linkedDeliveryRefs.contains("BL-2026-0312"))

        // 2. Check BC-2026-0842 (full delivery: 50/50 Seringues delivered)
        val closedCmd = orders.first { it.commande.referenceBon == "BC-2026-0842" }
        assertEquals("Soldé / Clôturé", closedCmd.statusLabel)
        assertEquals(100, closedCmd.progressPercent)
        assertEquals(1, closedCmd.linkedDeliveriesCount)
        assertTrue(closedCmd.linkedDeliveryRefs.contains("BL-2026-0315"))

        // 3. Check BC-2026-0843 (urgent debt with zero deliveries)
        val urgentCmd = orders.first { it.commande.referenceBon == "BC-2026-0843" }
        assertEquals("Dette Urgente", urgentCmd.statusLabel)
        assertEquals(0, urgentCmd.progressPercent)
        assertEquals(0, urgentCmd.linkedDeliveriesCount)

        // 4. Test status filters
        ordersVm.onFilterChange(OrderStatusFilter.CLOSED)
        val closedFiltered = ordersVm.uiState.value.filteredOrders
        assertEquals(1, closedFiltered.size)
        assertEquals("BC-2026-0842", closedFiltered.first().commande.referenceBon)

        ordersVm.onFilterChange(OrderStatusFilter.RECEIVING)
        val receivingFiltered = ordersVm.uiState.value.filteredOrders
        assertEquals(1, receivingFiltered.size)
        assertEquals("BC-2026-0841", receivingFiltered.first().commande.referenceBon)

        ordersVm.onFilterChange(OrderStatusFilter.VALIDATED)
        val validatedFiltered = ordersVm.uiState.value.filteredOrders
        assertTrue(validatedFiltered.any { it.commande.referenceBon == "BC-2026-0843" })
    }

    @Test
    fun testDeliveriesReliquatCalculationAndDockSelectorFiltering() {
        val deliveriesVm = DeliveriesViewModel(database)
        val availableCmds = deliveriesVm.uiState.value.availableCommandes

        // Closed order BC-2026-0842 must be excluded from dock dropdown
        assertFalse(availableCmds.any { it.referenceBon == "BC-2026-0842" }, "Closed order BC-2026-0842 should not appear in dock dropdown")

        // Partially fulfilled and pending orders must be available
        assertTrue(availableCmds.any { it.referenceBon == "BC-2026-0841" })
        assertTrue(availableCmds.any { it.referenceBon == "BC-2026-0843" })

        // Select BC-2026-0841 to verify reliquat calculations
        val targetCmd = availableCmds.first { it.referenceBon == "BC-2026-0841" }
        deliveriesVm.selectPurchaseOrder(targetCmd.id)

        val lines = deliveriesVm.uiState.value.verificationLines
        assertEquals(2, lines.size)

        val paracetamolLine = lines.first { it.codeArticle == "MED-001" }
        assertEquals(500.0, paracetamolLine.orderedQuantity)
        assertEquals(500.0, paracetamolLine.previouslyDeliveredQuantity)
        assertEquals(0.0, paracetamolLine.remainingQuantity)
        assertEquals(0.0, paracetamolLine.receivedQuantity)
        assertTrue(paracetamolLine.isConforming)

        val amoxLine = lines.first { it.codeArticle == "MED-002" }
        assertEquals(200.0, amoxLine.orderedQuantity)
        assertEquals(100.0, amoxLine.previouslyDeliveredQuantity)
        assertEquals(100.0, amoxLine.remainingQuantity)
        assertEquals(100.0, amoxLine.receivedQuantity, "Received quantity should default to remaining reliquat")
        assertTrue(amoxLine.isConforming)
    }

    @Test
    fun testMultiDeliveryFulfillmentClosesOrderAndUpdatesStock() {
        val deliveriesVm = DeliveriesViewModel(database)
        val ordersVm = OrdersViewModel(database)

        // Select partially received BC-2026-0841
        val availableCmds = deliveriesVm.uiState.value.availableCommandes
        val targetCmd = availableCmds.first { it.referenceBon == "BC-2026-0841" }
        deliveriesVm.selectPurchaseOrder(targetCmd.id)

        // Get initial stock for Amoxicilline (MED-002)
        val initialStockAmox = database.appDatabaseQueries.selectStockArticleByCode("MED-002").executeAsOne().quantity

        // Validate final delivery covering remaining 100 units of Amoxicilline
        val closingBlRef = "BL-2026-0320"
        val success = deliveriesVm.validateDeliverySync(
            referenceBon = closingBlRef,
            date = "2026-09-08",
            isPartial = false
        )
        assertTrue(success, "Closing delivery should validate successfully")

        // 1. Stock of MED-002 should increase by exactly the received 100 units
        val updatedStockAmox = database.appDatabaseQueries.selectStockArticleByCode("MED-002").executeAsOne().quantity
        assertEquals(initialStockAmox + 100.0, updatedStockAmox)

        // 2. Reload OrdersViewModel and verify BC-2026-0841 is now 100% Soldé / Clôturé
        ordersVm.loadOrdersSync()
        val closedCmd = ordersVm.uiState.value.orders.first { it.commande.referenceBon == "BC-2026-0841" }
        assertEquals("Soldé / Clôturé", closedCmd.statusLabel)
        assertEquals(100, closedCmd.progressPercent)
        assertEquals(3, closedCmd.linkedDeliveriesCount)
        assertTrue(closedCmd.linkedDeliveryRefs.contains(closingBlRef))

        // 3. BC-2026-0841 should now disappear from availableCommandes in DeliveriesViewModel
        deliveriesVm.loadDeliveriesSync()
        assertFalse(
            deliveriesVm.uiState.value.availableCommandes.any { it.referenceBon == "BC-2026-0841" },
            "BC-2026-0841 should be removed from dock dropdown once fully closed"
        )
    }

    @Test
    fun testServicesViewModelInitialStateAndKpiMetrics() {
        val viewModel = ServicesViewModel(database)
        val state = viewModel.uiState.value

        assertTrue(state.totalServicesCount >= 3, "Expected seeded services in initial state")
        assertTrue(state.totalLocalsCount >= 3, "Expected seeded locals in initial state")
        assertTrue(state.totalSortiesCount >= 2, "Expected seeded sorties in initial state")
        assertTrue(state.assignmentRatePercent > 0, "Assignment rate percentage should be positive")
        assertNotNull(state.selectedService, "Initial selected service should not be null")
        assertTrue(state.localsForSelectedService.isNotEmpty(), "Selected service should have associated locals")
    }

    @Test
    fun testServicesSearchQueryFiltering() {
        val viewModel = ServicesViewModel(database)
        val initialCount = viewModel.uiState.value.services.size

        // Filter by service name
        viewModel.onSearchQueryChange("Chirurgie")
        val filtered = viewModel.uiState.value.filteredServices
        assertTrue(filtered.isNotEmpty(), "Should find matching service")
        assertTrue(filtered.all { it.name.contains("Chirurgie", ignoreCase = true) })

        // Clear query
        viewModel.onSearchQueryChange("")
        assertEquals(initialCount, viewModel.uiState.value.filteredServices.size)
    }

    @Test
    fun testServiceCreationAndUpdating() {
        val viewModel = ServicesViewModel(database)
        val initialCount = viewModel.uiState.value.services.size

        // Add service
        val newServiceName = "Service Neurologie"
        val newLocalNum = "SRV-NEURO-01"
        val addSuccess = viewModel.addServiceSync(newServiceName, newLocalNum)
        assertTrue(addSuccess, "Adding service should succeed")

        val stateAfterAdd = viewModel.uiState.value
        assertEquals(initialCount + 1, stateAfterAdd.services.size)
        assertEquals(newServiceName, stateAfterAdd.selectedService?.name)

        // Verify in DB
        val dbService = database.appDatabaseQueries.selectAllServices().executeAsList().firstOrNull { it.name == newServiceName }
        assertNotNull(dbService)
        assertEquals(newLocalNum, dbService.local_num)

        // Update service
        val updatedName = "Service Neurologie & Neurochirurgie"
        val updatedLocalNum = "SRV-NEURO-02"
        val updateSuccess = viewModel.updateServiceSync(dbService.id, updatedName, updatedLocalNum)
        assertTrue(updateSuccess, "Updating service should succeed")

        val stateAfterUpdate = viewModel.uiState.value
        val updatedDbService = database.appDatabaseQueries.selectServiceById(dbService.id).executeAsOne()
        assertEquals(updatedName, updatedDbService.name)
        assertEquals(updatedLocalNum, updatedDbService.local_num)
    }

    @Test
    fun testLocalCreationUpdateAndDelete() {
        val viewModel = ServicesViewModel(database)
        val service = viewModel.uiState.value.selectedService!!
        val initialLocalsCount = viewModel.uiState.value.localsForSelectedService.size

        // Add Local
        val newLocalName = "Salle Explorations EEG"
        val addSuccess = viewModel.addLocalSync(service.id, newLocalName)
        assertTrue(addSuccess, "Adding local should succeed")

        viewModel.selectService(service)
        val stateAfterAdd = viewModel.uiState.value
        assertEquals(initialLocalsCount + 1, stateAfterAdd.localsForSelectedService.size)

        val dbLocal = database.appDatabaseQueries.selectLocalsByServiceId(service.id).executeAsList().firstOrNull { it.name == newLocalName }
        assertNotNull(dbLocal)

        // Update Local
        val updatedLocalName = "Salle Explorations Fonctionnelles & EEG"
        val updateSuccess = viewModel.updateLocalSync(dbLocal.id, service.id, updatedLocalName)
        assertTrue(updateSuccess, "Updating local should succeed")

        val updatedDbLocal = database.appDatabaseQueries.selectLocalById(dbLocal.id).executeAsOne()
        assertEquals(updatedLocalName, updatedDbLocal.name)

        // Delete Local
        val deleteSuccess = viewModel.deleteLocalSync(dbLocal.id)
        assertTrue(deleteSuccess, "Deleting local should succeed")

        val remainingLocals = database.appDatabaseQueries.selectLocalsByServiceId(service.id).executeAsList()
        assertFalse(remainingLocals.any { it.id == dbLocal.id })
    }

    @Test
    fun testServiceDeletionCascadeAndRestrictIntegrity() {
        val viewModel = ServicesViewModel(database)

        // 1. Create an isolated service with child locals (not referenced in bon_de_sortie)
        viewModel.addServiceSync("Service Temporaire", "SRV-TEMP-99")
        val createdService = viewModel.uiState.value.services.first { it.name == "Service Temporaire" }
        viewModel.addLocalSync(createdService.id, "Local Temp A")
        viewModel.addLocalSync(createdService.id, "Local Temp B")

        // Verify child locals exist
        val localsBefore = database.appDatabaseQueries.selectLocalsByServiceId(createdService.id).executeAsList()
        assertEquals(2, localsBefore.size)

        // Delete unreferenced service -> should succeed and cascade delete child locals
        val deleteSuccess = viewModel.deleteServiceSync(createdService.id)
        assertTrue(deleteSuccess, "Deleting unreferenced service should succeed")

        val localsAfter = database.appDatabaseQueries.selectLocalsByServiceId(createdService.id).executeAsList()
        assertEquals(0, localsAfter.size, "Child locals should be cascade deleted with parent service")

        // 2. Try to delete a service that IS referenced in a bon_de_sortie
        // Seeded service "Chirurgie Générale" or similar has sorties in DB
        val allSorties = database.appDatabaseQueries.selectAllSorties().executeAsList()
        val referencedServiceId = allSorties.first().service_id

        val deleteReferencedSuccess = viewModel.deleteServiceSync(referencedServiceId)
        assertFalse(deleteReferencedSuccess, "Deleting a service referenced in bon_de_sortie must be prevented")
        assertNotNull(viewModel.uiState.value.actionErrorMessage)
        assertTrue(viewModel.uiState.value.actionErrorMessage!!.contains("référencé", ignoreCase = true))

        // Ensure service still exists in DB
        val serviceCheck = database.appDatabaseQueries.selectServiceById(referencedServiceId).executeAsOneOrNull()
        assertNotNull(serviceCheck, "Referenced service must not be deleted")
    }

    @Test
    fun testServicesSyncWithSortiesViewModelWorkflow() {
        val servicesVm = ServicesViewModel(database)

        // Add a brand new department with a local
        val newDeptName = "Service Pédiatrie"
        val newDeptCode = "SRV-PED-01"
        servicesVm.addServiceSync(newDeptName, newDeptCode)
        val createdService = servicesVm.uiState.value.services.first { it.name == newDeptName }

        val newRoomName = "Salle de Soins Péd B"
        servicesVm.addLocalSync(createdService.id, newRoomName)

        // Initialize / reload SortiesViewModel to verify workflow integration
        val sortiesVm = SortiesViewModel(database, stockRepo())
        sortiesVm.loadSortiesSync()

        val sortiesServices = sortiesVm.uiState.value.services
        assertTrue(
            sortiesServices.any { it.name == newDeptName },
            "Newly created service should be available in SortiesViewModel"
        )

        // Select the newly created service in SortiesViewModel
        val targetService = sortiesServices.first { it.name == newDeptName }
        sortiesVm.selectService(targetService.id)

        val sortiesLocals = sortiesVm.uiState.value.localsForSelectedService
        assertTrue(
            sortiesLocals.any { it.name == newRoomName },
            "Newly created room should be available in SortiesViewModel when parent service is selected"
        )
    }

    @Test
    fun testPersonnelViewModelStaffManagementAndChefAppointment() {
        val personnelVm = PersonnelViewModel(database)
        personnelVm.loadDataSync()

        val initialState = personnelVm.uiState.value
        val initialStaffCount = initialState.totalStaffCount
        assertTrue(initialStaffCount > 0, "Staff should be seeded")

        val targetService = initialState.services.first()

        // 1. Add new staff member
        val newStaffName = "Dr. Nouria Benkhelil"
        val added = personnelVm.addPersonSync(
            serviceId = targetService.id,
            fullName = newStaffName,
            birthDate = "1985-07-22",
            notes = "Médecin Pédiatre"
        )
        assertTrue(added)

        val stateAfterAdd = personnelVm.uiState.value
        assertEquals(initialStaffCount + 1, stateAfterAdd.totalStaffCount)
        val addedPerson = stateAfterAdd.persons.first { it.fullName == newStaffName }
        assertEquals(targetService.id, addedPerson.serviceId)
        assertEquals("1985-07-22", addedPerson.birthDate)
        assertFalse(addedPerson.isChef)

        // 2. Edit staff member
        val updated = personnelVm.updatePersonSync(
            id = addedPerson.id,
            serviceId = targetService.id,
            fullName = "$newStaffName (Spécialiste)",
            birthDate = "1985-07-22",
            notes = "Médecin Spécialiste Pédiatre"
        )
        assertTrue(updated)
        val stateAfterEdit = personnelVm.uiState.value
        val editedPerson = stateAfterEdit.persons.first { it.id == addedPerson.id }
        assertEquals("$newStaffName (Spécialiste)", editedPerson.fullName)

        // 3. Appoint as Chef de Service
        personnelVm.toggleChefStatusSync(editedPerson)
        val stateAfterChef = personnelVm.uiState.value
        val chefPerson = stateAfterChef.persons.first { it.id == addedPerson.id }
        assertTrue(chefPerson.isChef, "Person should now be marked as Chef")

        // 4. Remove Chef status
        personnelVm.toggleChefStatusSync(chefPerson)
        val stateAfterDemis = personnelVm.uiState.value
        val demisPerson = stateAfterDemis.persons.first { it.id == addedPerson.id }
        assertFalse(demisPerson.isChef, "Person should no longer be Chef")

        // 5. Delete staff member
        val deleted = personnelVm.deletePersonSync(addedPerson.id)
        assertTrue(deleted)
        val stateAfterDelete = personnelVm.uiState.value
        assertEquals(initialStaffCount, stateAfterDelete.totalStaffCount)
        assertFalse(stateAfterDelete.persons.any { it.id == addedPerson.id })
    }

    @Test
    fun testDechargeViewModelAtomicStockMutationAndZeroValuation() {
        val dechargeVm = DechargeViewModel(database, categoryRepo())
        dechargeVm.loadDataSync()

        val initialState = dechargeVm.uiState.value
        val initialDechargesCount = initialState.decharges.size
        val existingArticle = initialState.availableArticles.first()
        val initialStockQty = existingArticle.quantity

        // Create voucher with 1 existing article and 1 brand-new donation article
        val newCode = "DON-TEST-001"
        val newDesig = "Stéthoscope Haute Précision"
        val lines = listOf(
            DechargeDraftLine(article = existingArticle, quantity = 5.0),
            DechargeDraftLine(
                codeArticle = newCode,
                designation = newDesig,
                unite = "U",
                family = "Matériel",
                sousFamily = "Monitoring",
                quantity = 10.0,
                expirationDate = "2030-12-31",
                article = null
            )
        )

        val docRef = "DCH-VM-2026-99"
        val success = dechargeVm.createDechargeVoucherSync(
            referenceBon = docRef,
            organizationName = "Fondation Santé Pour Tous",
            donorName = "Direction Générale",
            date = "2026-09-17 11:00",
            notes = "Donation de matériel diagnostic",
            lines = lines
        )
        assertTrue(success)

        val updatedState = dechargeVm.uiState.value
        assertEquals(initialDechargesCount + 1, updatedState.decharges.size)
        val createdVoucher = updatedState.decharges.first { it.referenceBon == docRef }
        assertEquals("Fondation Santé Pour Tous", createdVoucher.organizationName)

        // Verify stock mutation: existing article incremented
        val updatedExisting = database.appDatabaseQueries.selectStockArticleByCode(existingArticle.codeArticle).executeAsOne()
        assertEquals(initialStockQty + 5.0, updatedExisting.quantity)

        // Verify stock mutation: new donation article created with strictly 0.0 unit price
        val brandNewArticle = database.appDatabaseQueries.selectStockArticleByCode(newCode).executeAsOne()
        assertEquals(newDesig, brandNewArticle.designation)
        assertEquals(10.0, brandNewArticle.quantity)
        assertEquals(0.0, brandNewArticle.unit_price, "Donation article unit_price must strictly be 0.0")
        assertEquals("2030-12-31", brandNewArticle.expiration_date)
    }
}
