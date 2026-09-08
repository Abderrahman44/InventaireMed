package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.PackagingUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InventoryDatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
    }

    @Test
    fun testMasterEntitiesAndForeignKeysCascade() {
        val queries = database.appDatabaseQueries

        // 1. Insert Fournisseur
        queries.insertFournisseur("Pharma Distri SARL")
        val fournisseurId = queries.selectLastInsertedRowId().executeAsOne()

        val suppliers = queries.selectAllFournisseurs().executeAsList()
        assertEquals(1, suppliers.size)
        assertEquals("Pharma Distri SARL", suppliers.first().full_name)

        // 2. Insert Convention
        queries.insertConvention(
            fournisseur_id = fournisseurId,
            ord_number = 1,
            date = "2026-01-15",
            global_unit_price = 150.0,
            total_montant = 150000.0
        )
        val conventionId = queries.selectLastInsertedRowId().executeAsOne()

        // 3. Insert Convention Items
        queries.insertConventionItem(
            convention_id = conventionId,
            designation = "Paracetamol 500mg",
            unite = PackagingUnit.BOITE.label,
            quantity = 1000.0,
            min_quantity = 100.0
        )
        val conventionItemId = queries.selectLastInsertedRowId().executeAsOne()

        val items = queries.selectConventionItemsByConventionId(conventionId).executeAsList()
        assertEquals(1, items.size)
        assertEquals("Paracetamol 500mg", items.first().designation)

        // 4. Test Cascade Delete: Deleting Convention cascades to convention_item
        queries.deleteConvention(conventionId)
        val itemsAfterDelete = queries.selectConventionItemsByConventionId(conventionId).executeAsList()
        assertTrue(itemsAfterDelete.isEmpty())
    }

    @Test
    fun testInternalOrganizationHierarchy() {
        val queries = database.appDatabaseQueries

        // Insert Service
        queries.insertService(name = "Cardiologie", local_num = "B-204")
        val serviceId = queries.selectLastInsertedRowId().executeAsOne()

        // Insert Locals
        queries.insertLocal(service_id = serviceId, name = "Salle de Soins")
        queries.insertLocal(service_id = serviceId, name = "Pharmacie Centrale")

        val locals = queries.selectLocalsByServiceId(serviceId).executeAsList()
        assertEquals(2, locals.size)

        // Delete Service -> Locals cascade deleted
        queries.deleteService(serviceId)
        val localsAfter = queries.selectLocalsByServiceId(serviceId).executeAsList()
        assertTrue(localsAfter.isEmpty())
    }

    @Test
    fun testStockArticleUniqueCodeConstraint() {
        val queries = database.appDatabaseQueries

        queries.insertStockArticle(
            code_article = "MED-001",
            designation = "Amoxicilline 1g",
            unite_quantite = "BOITE",
            family = "Antibiotique",
            sous_family = "Oral",
            quantity = 50.0,
            unit_price = 45.0,
            expiration_date = "2027-12-31",
            date = "2026-09-08"
        )

        // Attempting to insert another article with same code_article MUST fail
        assertFailsWith<Exception> {
            queries.insertStockArticle(
                code_article = "MED-001",
                designation = "Amoxicilline Duplicate",
                unite_quantite = "BOITE",
                family = "Antibiotique",
                sous_family = "Oral",
                quantity = 10.0,
                unit_price = 45.0,
                expiration_date = "2027-12-31",
                date = "2026-09-08"
            )
        }
    }

    @Test
    fun testConventionQuotaBalanceAndUrgentDebtOrders() {
        val queries = database.appDatabaseQueries

        // Supplier & Convention
        queries.insertFournisseur("Fournisseur General")
        val fId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertConvention(fId, 1, "2026-01-01", 10.0, 10000.0)
        val convId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertConventionItem(convId, "Gants Latex M", "BOITE", 1000.0, 50.0)
        val convItemId = queries.selectLastInsertedRowId().executeAsOne()

        // Initial balance: 0 ordered, 1000 remaining
        val initialBalance = queries.selectConventionItemBalance(convItemId).executeAsOne()
        assertEquals(1000.0, initialBalance.agreedQuantity)
        assertEquals(0.0, initialBalance.orderedQuantity)
        assertEquals(1000.0, initialBalance.remainingQuantity)

        // 1. Standard Purchase Order for 200 units
        queries.insertCommande("BC-2026-001", fId, "2026-02-01", is_debt = 0)
        val cmd1Id = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertCommandeItem(
            commande_id = cmd1Id,
            convention_item_id = convItemId,
            code_article = "GNT-001",
            designation = "Gants Latex M",
            type = "BOITE",
            family = "Protection",
            quantity = 200.0,
            unit_price = 10.0
        )

        val balanceAfterCmd1 = queries.selectConventionItemBalance(convItemId).executeAsOne()
        assertEquals(200.0, balanceAfterCmd1.orderedQuantity)
        assertEquals(800.0, balanceAfterCmd1.remainingQuantity)

        // 2. Urgent Debt Order (bypasses quota balance calculation)
        queries.insertCommande("BC-DEBT-001", fId, "2026-03-01", is_debt = 1)
        val debtCmdId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertCommandeItem(
            commande_id = debtCmdId,
            convention_item_id = convItemId,
            code_article = "GNT-001",
            designation = "Gants Latex M",
            type = "BOITE",
            family = "Protection",
            quantity = 500.0,
            unit_price = 12.5 // Custom editable price during price fluctuation
        )

        // Verify that the standard convention quota balance did NOT count the debt order
        val balanceAfterDebt = queries.selectConventionItemBalance(convItemId).executeAsOne()
        assertEquals(200.0, balanceAfterDebt.orderedQuantity)
        assertEquals(800.0, balanceAfterDebt.remainingQuantity)

        // Query by convention list balances
        val balances = queries.selectConventionBalancesByConventionId(convId).executeAsList()
        assertEquals(1, balances.size)
        assertEquals(800.0, balances.first().remainingQuantity)
    }

    @Test
    fun testAtomicStockMutationsOnDeliveryAndExit() {
        val queries = database.appDatabaseQueries

        // Setup Supplier & Service
        queries.insertFournisseur("Fournisseur Medical")
        val fId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertService("Urgences", "URG-1")
        val sId = queries.selectLastInsertedRowId().executeAsOne()

        // Create initial stock article
        queries.insertStockArticle(
            code_article = "SER-001",
            designation = "Seringue 10ml",
            unite_quantite = "UNITE",
            family = "Materiel",
            sous_family = "Usage Unique",
            quantity = 100.0,
            unit_price = 2.5,
            expiration_date = "2028-06-30",
            date = "2026-09-08"
        )

        // 1. Delivery Receipt: increases stock by 50
        queries.insertLivraison("BL-2026-55", fId, "2026-09-09", commande_id = null)
        val blId = queries.selectLastInsertedRowId().executeAsOne()
        queries.insertLivraisonItem(blId, "SER-001", "Seringue 10ml", 50.0, 2.5)

        // Execute atomic stock increase
        queries.increaseStockQuantity(quantity = 50.0, date = "2026-09-09", code_article = "SER-001")

        val articleAfterDelivery = queries.selectStockArticleByCode("SER-001").executeAsOne()
        assertEquals(150.0, articleAfterDelivery.quantity)

        // 2. Issue Voucher (Bon de Sortie): decreases stock by 40
        queries.insertSortie("BS-2026-101", sId, local_id = null, date = "2026-09-10")
        val bsId = queries.selectLastInsertedRowId().executeAsOne()
        queries.insertSortieItem(bsId, "SER-001", "Seringue 10ml", 40.0)

        // Execute atomic stock decrease
        queries.decreaseStockQuantity(quantity = 40.0, date = "2026-09-10", code_article = "SER-001")

        val articleAfterExit = queries.selectStockArticleByCode("SER-001").executeAsOne()
        assertEquals(110.0, articleAfterExit.quantity)
    }

    @Test
    fun testDomainMappers() {
        val queries = database.appDatabaseQueries

        queries.insertFournisseur("BioTech Algerie")
        val fId = queries.selectLastInsertedRowId().executeAsOne()

        val dbFournisseur = queries.selectFournisseurById(fId).executeAsOne()
        val domainFournisseur = dbFournisseur.toDomain()
        assertEquals("BioTech Algerie", domainFournisseur.fullName)
        assertEquals(fId, domainFournisseur.id)

        queries.insertStockArticle(
            code_article = "BET-001",
            designation = "Betadine 500ml",
            unite_quantite = "FLACON",
            family = "Antiseptique",
            sous_family = "Liquide",
            quantity = 25.0,
            unit_price = 180.0,
            expiration_date = "2027-01-01",
            date = "2026-09-08"
        )
        val dbArticle = queries.selectStockArticleByCode("BET-001").executeAsOne()
        val domainArticle = dbArticle.toDomain()

        assertEquals("BET-001", domainArticle.codeArticle)
        assertEquals("Betadine 500ml", domainArticle.designation)
        assertEquals(25.0, domainArticle.quantity)
        assertEquals(180.0, domainArticle.unitPrice)
    }
}
