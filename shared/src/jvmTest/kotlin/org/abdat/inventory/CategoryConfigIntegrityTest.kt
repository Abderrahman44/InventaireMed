package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.domain.model.DynamicConfigItem
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `Managed Family and Sous-family Hierarchy` and `Protection of Categories in Use` through the
 * configuration screen's view model. Refusals are checked against the database, not the UI lists.
 */
class CategoryConfigIntegrityTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: CategoryRepository
    private lateinit var viewModel: DynamicConfigViewModel

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        ensureCategoryTaxonomy(database)
        repository = CategoryRepository(database, Dispatchers.IO)
        viewModel = DynamicConfigViewModel(database, repository)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun taxonomy() = repository.currentTaxonomy()

    private fun family(label: String): DynamicConfigItem = assertNotNull(taxonomy().findFamilyByLabel(label))

    private fun sousFamily(familyLabel: String, label: String): DynamicConfigItem =
        assertNotNull(taxonomy().findSousFamily(family(familyLabel).code, label))

    private fun insertArticle(code: String, family: String, sousFamily: String, archived: Long = 0L) {
        database.appDatabaseQueries.insertStockArticle(
            code_article = code,
            additional_reference_id = "",
            designation = "Article $code",
            unite_quantite = "PIECE",
            family = family,
            sous_family = sousFamily,
            quantity = 1.0,
            unit_price = 10.0,
            expiration_date = null,
            is_archived = archived,
            is_obsolete = 0L,
            date = "2026-09-15"
        )
    }

    @Test
    fun familyUsedByArticlesIsNotDeleted() {
        insertArticle("MED-001", "médicaments", "Antalgique")
        insertArticle("MED-002", "Médicaments", "Hormones")
        val medicaments = family("Médicaments")

        assertFalse(viewModel.deleteConfigItemSync(medicaments.id))

        assertNotNull(taxonomy().findFamilyByLabel("Médicaments"))
        val error = assertNotNull(viewModel.uiState.value.actionErrorMessage)
        assertTrue("2 article(s)" in error, "error names the article count: $error")
    }

    @Test
    fun familyWithSousFamiliesIsNotDeleted() {
        val printed = family("Imprimés divers")
        val childCount = taxonomy().childrenOf(printed.code).size

        assertFalse(viewModel.deleteConfigItemSync(printed.id))

        assertNotNull(taxonomy().findFamilyByLabel("Imprimés divers"))
        val error = assertNotNull(viewModel.uiState.value.actionErrorMessage)
        assertTrue("$childCount sous-famille(s)" in error, "error names the sous-family count: $error")
    }

    @Test
    fun sousFamilyUsedByAnArchivedArticleIsNotDeleted() {
        insertArticle("PC-001", "Accessoire informatique", "Ordinateurs", archived = 1L)
        val computers = sousFamily("Accessoire informatique", "Ordinateurs")

        assertFalse(viewModel.deleteConfigItemSync(computers.id))

        assertNotNull(taxonomy().findSousFamily(family("Accessoire informatique").code, "Ordinateurs"))
        assertTrue("1 article(s)" in assertNotNull(viewModel.uiState.value.actionErrorMessage))
    }

    @Test
    fun sousFamilyLabelUsedUnderAnotherFamilyDoesNotBlockDeletion() {
        insertArticle("MED-010", "Médicaments", "Consommables informatiques")
        val itConsumables = sousFamily("Accessoire informatique", "Consommables informatiques")

        assertTrue(viewModel.deleteConfigItemSync(itConsumables.id))
    }

    @Test
    fun unusedSousFamilyAndThenItsEmptyFamilyAreDeleted() {
        val printed = family("Imprimés divers")
        taxonomy().childrenOf(printed.code).forEach { child ->
            assertTrue(viewModel.deleteConfigItemSync(child.id), "unused sous-family ${child.label}")
        }

        assertTrue(viewModel.deleteConfigItemSync(printed.id))
        assertNull(taxonomy().findFamilyByLabel("Imprimés divers"))
        assertNull(viewModel.uiState.value.actionErrorMessage)
    }

    @Test
    fun duplicateFamilyIsRefusedByLabelAndByCode() {
        val before = taxonomy().allFamilies.size

        assertFalse(viewModel.addConfigItemSync("FAMILY", code = "", label = "  accessoire INFORMATIQUE "))
        assertNotNull(viewModel.uiState.value.actionErrorMessage)

        val printedCode = family("Imprimés divers").code
        assertFalse(viewModel.addConfigItemSync("FAMILY", code = printedCode, label = "Formulaires"))

        assertEquals(before, taxonomy().allFamilies.size)
    }

    @Test
    fun duplicateSousFamilyIsRefusedOnlyWithinTheSameFamily() {
        val itCode = family("Accessoire informatique").code
        val officeCode = family("Accessoire bureautique").code

        assertFalse(
            viewModel.addConfigItemSync("SOUS_FAMILY", "", "ordinateurs", parentCategory = "FAMILY", parentCode = itCode)
        )
        assertNotNull(viewModel.uiState.value.actionErrorMessage)
        assertEquals(1, taxonomy().childrenOf(itCode).count { it.label.equals("Ordinateurs", ignoreCase = true) })

        assertTrue(
            viewModel.addConfigItemSync("SOUS_FAMILY", "", "Ordinateurs", parentCategory = "FAMILY", parentCode = officeCode)
        )
        assertNotNull(taxonomy().findSousFamily(officeCode, "Ordinateurs"))
    }

    @Test
    fun sousFamilyParentCodeIsStoredVerbatim() {
        val medicamentsCode = family("Médicaments").code

        assertTrue(
            viewModel.addConfigItemSync("SOUS_FAMILY", "", "Antiseptique", parentCategory = "FAMILY", parentCode = medicamentsCode)
        )

        assertEquals(medicamentsCode, sousFamily("Médicaments", "Antiseptique").parentCode)
    }

    @Test
    fun familyNeverStoresAParentEvenWhenTheDialogSendsOne() {
        val preselected = family("Consommables").code

        assertTrue(
            viewModel.addConfigItemSync("FAMILY", "", "Linge hospitalier", parentCategory = "", parentCode = preselected)
        )

        val linen = family("Linge hospitalier")
        assertEquals("", linen.parentCode)
        assertEquals("", linen.parentCategory)
    }
}
