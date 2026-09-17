package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.PackagingUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.abdat.inventory.di.initDesktopKoin
import org.abdat.inventory.ui.inventory.InventoryViewModel
import org.koin.core.context.stopKoin

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
            total_montant = 150000.0,
            tva_rate = 19.0
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
            additional_reference_id = "",
            designation = "Amoxicilline 1g",
            unite_quantite = "BOITE",
            family = "Antibiotique",
            sous_family = "Oral",
            quantity = 50.0,
            unit_price = 45.0,
            expiration_date = "2027-12-31",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )

        // Attempting to insert another article with same code_article MUST fail
        assertFailsWith<Exception> {
            queries.insertStockArticle(
                code_article = "MED-001",
                additional_reference_id = "",
                designation = "Amoxicilline Duplicate",
                unite_quantite = "BOITE",
                family = "Antibiotique",
                sous_family = "Oral",
                quantity = 10.0,
                unit_price = 45.0,
                expiration_date = "2027-12-31",
                is_archived = 0L,
                is_obsolete = 0L,
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

        queries.insertConvention(fId, 1, "2026-01-01", 10.0, 10000.0, 0.0)
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
            additional_reference_id = "",
            designation = "Seringue 10ml",
            unite_quantite = "UNITE",
            family = "Materiel",
            sous_family = "Usage Unique",
            quantity = 100.0,
            unit_price = 2.5,
            expiration_date = "2028-06-30",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )

        // 1. Delivery Receipt: increases stock by 50
        queries.insertLivraison("BL-2026-55", fId, "2026-09-09", commande_id = null, service_id = sId, beneficiaire_id = null)
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
            additional_reference_id = "",
            designation = "Betadine 500ml",
            unite_quantite = "FLACON",
            family = "Antiseptique",
            sous_family = "Liquide",
            quantity = 25.0,
            unit_price = 180.0,
            expiration_date = "2027-01-01",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val dbArticle = queries.selectStockArticleByCode("BET-001").executeAsOne()
        val domainArticle = dbArticle.toDomain()

        assertEquals("BET-001", domainArticle.codeArticle)
        assertEquals("Betadine 500ml", domainArticle.designation)
        assertEquals(25.0, domainArticle.quantity)
        assertEquals(180.0, domainArticle.unitPrice)
    }

    @Test
    fun testDechargesGiftZeroValuationAndStockIncrement() {
        val queries = database.appDatabaseQueries

        // 1. Initial article
        queries.insertStockArticle(
            code_article = "DON-001",
            additional_reference_id = "6130009990011",
            designation = "Soluté Salé 0.9% 500ml",
            unite_quantite = "FLACON",
            family = "Solutés",
            sous_family = "Perfusion",
            quantity = 50.0,
            unit_price = 75.0,
            expiration_date = "2028-12-31",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val artId = queries.selectLastInsertedRowId().executeAsOne()

        // 2. Insert Papier de Décharge (Strictly Zero Valuation)
        queries.insertDecharge(
            reference_bon = "DCH-2026-001",
            organization_name = "Croissant Rouge Algérien",
            donor_name = "Donateur Anonyme",
            date = "2026-09-09",
            notes = "Don humanitaire"
        )
        val dchId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertDechargeItem(
            decharge_id = dchId,
            code_article = "DON-001",
            designation = "Soluté Salé 0.9% 500ml",
            quantity = 100.0
        )

        // Increment stock
        queries.increaseStockQuantityById(
            quantity = 100.0,
            date = "2026-09-09",
            id = artId
        )

        // Verify stock incremented
        val updatedArticle = queries.selectStockArticleById(artId).executeAsOne()
        assertEquals(150.0, updatedArticle.quantity)

        // Verify décharge records
        val allDecharges = queries.selectAllDecharges().executeAsList()
        assertEquals(1, allDecharges.size)
        assertEquals("Croissant Rouge Algérien", allDecharges.first().organization_name)

        val items = queries.selectDechargeItemsByDechargeId(dchId).executeAsList()
        assertEquals(1, items.size)
        assertEquals(100.0, items.first().quantity)
    }

    @Test
    fun testArticleAuditLogAndArchiveFlags() {
        val queries = database.appDatabaseQueries

        // Insert article
        queries.insertStockArticle(
            code_article = "AUD-001",
            additional_reference_id = "1234567890123",
            designation = "Gants Chirurgicaux T7",
            unite_quantite = "BOITE",
            family = "Consommables",
            sous_family = "Protection",
            quantity = 200.0,
            unit_price = 1200.0,
            expiration_date = "2028-01-01",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val artId = queries.selectLastInsertedRowId().executeAsOne()

        // Insert audit log
        queries.insertArticleAuditLog(
            article_id = artId,
            field_name = "unit_price",
            old_value = "1200.0",
            new_value = "1350.0",
            reason = "Ajustement tarifaire conventionnel 2026",
            user_name = "Dr. Amina Mansouri",
            edit_date = "2026-09-09 14:00"
        )

        val logs = queries.selectAuditLogsByArticleId(artId).executeAsList()
        assertEquals(1, logs.size)
        assertEquals("Ajustement tarifaire conventionnel 2026", logs.first().reason)
        assertEquals("1200.0", logs.first().old_value)
        assertEquals("1350.0", logs.first().new_value)

        // Soft archive instead of delete
        queries.archiveStockArticle(artId)
        val archivedArticle = queries.selectStockArticleById(artId).executeAsOne()
        assertEquals(1L, archivedArticle.is_archived)

        // Obsolete
        queries.markStockArticleObsolete(artId)
        val obsoleteArticle = queries.selectStockArticleById(artId).executeAsOne()
        assertEquals(1L, obsoleteArticle.is_obsolete)
    }

    @Test
    fun testPersonnelAndEquipmentAssignmentWorkflow() {
        val queries = database.appDatabaseQueries

        // Service & Person
        queries.insertService("Pédiatrie", "PED-01")
        val sId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertPerson(
            service_id = sId,
            full_name = "Dr. Samia Kaci",
            birth_date = "1985-05-20",
            notes = "Médecin Pédiatre"
        )
        val pId = queries.selectLastInsertedRowId().executeAsOne()

        // Article
        queries.insertStockArticle(
            code_article = "OTO-001",
            additional_reference_id = "5551112223334",
            designation = "Otoscope Diagnostic LED",
            unite_quantite = "PIECE",
            family = "Matériel",
            sous_family = "Diagnostic",
            quantity = 5.0,
            unit_price = 8500.0,
            expiration_date = "2032-12-31",
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-08"
        )
        val artId = queries.selectLastInsertedRowId().executeAsOne()

        // 1. Assign equipment -> Stock decrements from 5.0 to 4.0
        queries.insertEquipmentAssignment(
            person_id = pId,
            article_id = artId,
            quantity = 1.0,
            assigned_date = "2026-09-09",
            return_date = null,
            status = "ACTIVE",
            notes = "Dotation individuelle de consultation"
        )
        val assignmentId = queries.selectLastInsertedRowId().executeAsOne()
        queries.decreaseStockQuantityById(1.0, "2026-09-09", artId)

        val stockAfterAssign = queries.selectStockArticleById(artId).executeAsOne()
        assertEquals(4.0, stockAfterAssign.quantity)

        val assignments = queries.selectAssignmentsByPersonId(pId).executeAsList()
        assertEquals(1, assignments.size)
        assertEquals("ACTIVE", assignments.first().status)

        // 2. Return equipment -> Stock increments back to 5.0
        queries.returnEquipmentAssignment("2026-09-10", assignmentId)
        queries.increaseStockQuantityById(1.0, "2026-09-10", artId)

        val stockAfterReturn = queries.selectStockArticleById(artId).executeAsOne()
        assertEquals(5.0, stockAfterReturn.quantity)

        val returnedAssignment = queries.selectAssignmentsByPersonId(pId).executeAsList().first()
        assertEquals("RETURNED", returnedAssignment.status)
        assertEquals("2026-09-10", returnedAssignment.return_date)
    }

    @Test
    fun testDynamicConfigAndUserProfile() {
        val queries = database.appDatabaseQueries

        // 1. Dynamic Config Items
        queries.insertConfigItem(
            category = "UNIT",
            code = "AMPOULE",
            label = "Ampoule injectable",
            parent_category = "",
            parent_code = "",
            is_active = 1L
        )
        val units = queries.selectConfigItemsByCategory("UNIT").executeAsList()
        assertTrue(units.any { it.code == "AMPOULE" })

        // 2. User Profile
        queries.insertOrUpdateUserProfile(
            full_name = "Dr. Amina Mansouri",
            phone = "+213 550 00 00 00",
            email = "amina@inventairemed.dz",
            role = "Chef Pharmacien",
            theme_mode = "DARK",
            locale = "FR"
        )
        val profile = queries.getUserProfile().executeAsOne()
        assertEquals("Dr. Amina Mansouri", profile.full_name)
        assertEquals("DARK", profile.theme_mode)
        assertEquals("FR", profile.locale)
    }

    @Test
    fun testDeliveryBeneficiaryAndServiceRelationalKeys() {
        val queries = database.appDatabaseQueries

        // 1. Setup Supplier, Service, and Personnel
        queries.insertFournisseur("Laboratoire Pharmaceutique Central")
        val supplierId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertService("Pédiatrie", "PED-01")
        val serviceId = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertPerson(
            service_id = serviceId,
            full_name = "Dr. Rachid Benali",
            birth_date = "1980-05-12",
            notes = "Chef de Service Pédiatrie"
        )
        val staffId = queries.selectLastInsertedRowId().executeAsOne()

        // 2. Insert Delivery linked to Service and Beneficiary
        val blRef = "BL-PED-2026-001"
        queries.insertLivraison(
            reference_bon = blRef,
            fournisseur_id = supplierId,
            date_livraison = "2026-09-15",
            commande_id = null,
            service_id = serviceId,
            beneficiaire_id = staffId
        )
        val blId = queries.selectLastInsertedRowId().executeAsOne()

        // 3. Query and verify persistence by Service
        val deliveriesByService = queries.selectLivraisonsByService(serviceId).executeAsList()
        assertTrue(deliveriesByService.any { it.id == blId && it.reference_bon == blRef })

        // 4. Query and verify persistence by Beneficiary
        val deliveriesByBeneficiary = queries.selectLivraisonsByBeneficiaire(staffId).executeAsList()
        assertTrue(deliveriesByBeneficiary.any { it.id == blId && it.reference_bon == blRef })

        // 5. Verify domain mapping
        val deliveryRecord = queries.selectLivraisonById(blId).executeAsOne()
        val domainDelivery = deliveryRecord.toDomain()
        assertEquals(blRef, domainDelivery.referenceBon)
        assertEquals(serviceId, domainDelivery.serviceId)
        assertEquals(staffId, domainDelivery.beneficiaireId)

        // 6. Test updateLivraison with updated service/beneficiary
        queries.insertService("Chirurgie", "CHIR-01")
        val newServiceId = queries.selectLastInsertedRowId().executeAsOne()
        queries.updateLivraison(
            reference_bon = "BL-PED-2026-001-MOD",
            fournisseur_id = supplierId,
            date_livraison = "2026-09-16",
            commande_id = null,
            service_id = newServiceId,
            beneficiaire_id = null,
            id = blId
        )
        val updatedRecord = queries.selectLivraisonById(blId).executeAsOne()
        assertEquals("BL-PED-2026-001-MOD", updatedRecord.reference_bon)
        assertEquals(newServiceId, updatedRecord.service_id)
        assertNull(updatedRecord.beneficiaire_id)
    }

    @Test
    fun testPersonCrudAndServiceChefRelationalIntegrity() {
        val queries = database.appDatabaseQueries

        // 1. Create a Service
        queries.insertService(name = "Chirurgie Cardiovasculaire", local_num = "BLOC-CV-01")
        val serviceId = queries.selectLastInsertedRowId().executeAsOne()

        // 2. Create multiple Person records in this service
        queries.insertPerson(
            service_id = serviceId,
            full_name = "Pr. Salim Mansouri",
            birth_date = "1975-03-21",
            notes = "Chirurgien Principal"
        )
        val staff1Id = queries.selectLastInsertedRowId().executeAsOne()

        queries.insertPerson(
            service_id = serviceId,
            full_name = "Dr. Amina Bouzid",
            birth_date = "1988-11-14",
            notes = "Médecin Spécialiste"
        )
        val staff2Id = queries.selectLastInsertedRowId().executeAsOne()

        // 3. Verify initial state: neither is chef
        val personsBeforeChef = queries.selectAllPersons().executeAsList()
        val p1Before = personsBeforeChef.first { it.id == staff1Id }
        val p2Before = personsBeforeChef.first { it.id == staff2Id }
        assertEquals(0L, p1Before.is_chef)
        assertEquals(0L, p2Before.is_chef)

        // 4. Designate Pr. Salim Mansouri as Chef de Service (updateServiceChef)
        queries.updateServiceChef(chef_id = staff1Id, id = serviceId)

        // Verify selectAllPersons computes is_chef = 1 for staff1 and 0 for staff2
        val personsAfterChef = queries.selectAllPersons().executeAsList()
        val p1After = personsAfterChef.first { it.id == staff1Id }
        val p2After = personsAfterChef.first { it.id == staff2Id }
        assertEquals(1L, p1After.is_chef)
        assertEquals(0L, p2After.is_chef)

        // Verify domain mapping
        val domainP1 = p1After.toDomain()
        assertTrue(domainP1.isChef)
        val domainP2 = p2After.toDomain()
        assertFalse(domainP2.isChef)

        // Verify selectChefByServiceId
        val chefRecord = queries.selectChefByServiceId(serviceId).executeAsOne()
        assertEquals(staff1Id, chefRecord.id)
        assertEquals("Pr. Salim Mansouri", chefRecord.full_name)

        // Verify selectServiceByIdWithChef
        val svcWithChef = queries.selectServiceByIdWithChef(serviceId).executeAsOne()
        assertEquals(staff1Id, svcWithChef.chef_id)
        assertEquals("Pr. Salim Mansouri", svcWithChef.chef_name)

        // 5. Chef Reassignment: appoint Dr. Amina Bouzid as new Chef (1-to-1 integrity)
        queries.updateServiceChef(chef_id = staff2Id, id = serviceId)
        val personsReassigned = queries.selectAllPersons().executeAsList()
        val p1Reassigned = personsReassigned.first { it.id == staff1Id }
        val p2Reassigned = personsReassigned.first { it.id == staff2Id }
        assertEquals(0L, p1Reassigned.is_chef, "Previous chef must no longer be marked as chef")
        assertEquals(1L, p2Reassigned.is_chef, "Newly designated staff must be marked as chef")

        // 6. Test clearServiceChef
        queries.clearServiceChef(serviceId)
        val svcCleared = queries.selectServiceByIdWithChef(serviceId).executeAsOne()
        assertNull(svcCleared.chef_id)
        assertNull(svcCleared.chef_name)
        val chefAfterClear = queries.selectChefByServiceId(serviceId).executeAsOneOrNull()
        assertNull(chefAfterClear)

        // 7. Foreign Key ON DELETE SET NULL test:
        // Appoint staff2 as chef again, then delete staff2. The service.chef_id should automatically become null.
        queries.updateServiceChef(chef_id = staff2Id, id = serviceId)
        val svcBeforeDelete = queries.selectServiceById(serviceId).executeAsOne()
        assertEquals(staff2Id, svcBeforeDelete.chef_id)

        queries.deletePerson(staff2Id)
        val svcAfterDeletePerson = queries.selectServiceById(serviceId).executeAsOne()
        assertNull(svcAfterDeletePerson.chef_id, "Deleting a chef must set service.chef_id to null via ON DELETE SET NULL")

        // 8. Delete person cleanup
        queries.deletePerson(staff1Id)
        val p1Deleted = queries.selectPersonById(staff1Id).executeAsOneOrNull()
        assertNull(p1Deleted)
    }

    @Test
    fun testDonationStockIntakeWithZeroValuationAndAttributePersistence() {
        val queries = database.appDatabaseQueries

        // 1. Record a Papier de Décharge (Donation intake voucher)
        val docRef = "DCH-2026-DON-01"
        queries.insertDecharge(
            reference_bon = docRef,
            organization_name = "Croissant Rouge Algérien",
            donor_name = "Délégation Régionale",
            date = "2026-09-17 10:00",
            notes = "Don humanitaire d'équipements et consommables médicaux"
        )
        val dechargeId = queries.selectLastInsertedRowId().executeAsOne()

        // 2. Add line item with STRICT zero valuation (unit_price = 0.0)
        queries.insertDechargeItem(
            decharge_id = dechargeId,
            code_article = "DON-RESP-001",
            designation = "Concentrateur d'Oxygène 5L Mobile",
            quantity = 4.0
        )
        val itemId = queries.selectLastInsertedRowId().executeAsOne()

        val dechargeItems = queries.selectDechargeItemsByDechargeId(dechargeId).executeAsList()
        assertEquals(1, dechargeItems.size)
        val item = dechargeItems.first()
        assertEquals("DON-RESP-001", item.code_article)
        assertEquals(0.0, item.unit_price, "Donation line items must have strictly 0.0 unit price")
        assertEquals(4.0, item.quantity)

        // 3. Stock Intake - Insert new stock article with zero valuation and full attributes
        queries.insertStockArticle(
            code_article = "DON-RESP-001",
            additional_reference_id = "REF-DON-CRA-99",
            designation = "Concentrateur d'Oxygène 5L Mobile",
            unite_quantite = "Appareil",
            family = "Matériel Médical",
            sous_family = "Respiratoire",
            quantity = 4.0,
            unit_price = 0.0, // Zero financial valuation
            expiration_date = "2031-12-31",
            is_archived = 0,
            is_obsolete = 0,
            date = "2026-09-17"
        )

        val stockArticle = queries.selectStockArticleByCode("DON-RESP-001").executeAsOne()
        assertEquals("DON-RESP-001", stockArticle.code_article)
        assertEquals("Appareil", stockArticle.unite_quantite)
        assertEquals("Matériel Médical", stockArticle.family)
        assertEquals("Respiratoire", stockArticle.sous_family)
        assertEquals(4.0, stockArticle.quantity)
        assertEquals(0.0, stockArticle.unit_price, "Stock article from donation must have 0.0 unit price")
        assertEquals("2031-12-31", stockArticle.expiration_date)

        // 4. Test subsequent donation of existing article: increments quantity and preserves 0.0 valuation
        queries.increaseStockQuantity(
            quantity = 2.0,
            date = "2026-09-17 14:00",
            code_article = "DON-RESP-001"
        )
        val updatedStock = queries.selectStockArticleByCode("DON-RESP-001").executeAsOne()
        assertEquals(6.0, updatedStock.quantity)
        assertEquals(0.0, updatedStock.unit_price)

        // 5. Verify domain mapping for decharges
        val decharges = queries.selectAllDecharges().executeAsList()
        assertTrue(decharges.any { it.reference_bon == docRef })
        val domainDecharge = queries.selectDechargeById(dechargeId).executeAsOne().toDomain()
        assertEquals(docRef, domainDecharge.referenceBon)
        assertEquals("Croissant Rouge Algérien", domainDecharge.organizationName)
    }

    @Test
    fun testDesktopKoinInitializationWithRealDriver() {
        try {
            stopKoin()
        } catch (_: Exception) {}
        val app = initDesktopKoin()
        val db: AppDatabase = app.koin.get()
        org.abdat.inventory.database.seedInitialDataIfEmpty(db)
        val invVm: InventoryViewModel = app.koin.get()
        assertNotNull(invVm)
        assertTrue(invVm.uiState.value.articles.isNotEmpty(), "Articles should be seeded and loaded")
        stopKoin()
    }
}

