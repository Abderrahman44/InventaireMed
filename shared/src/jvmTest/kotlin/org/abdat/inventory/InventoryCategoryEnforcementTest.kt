package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.ui.config.DynamicConfigViewModel
import org.abdat.inventory.ui.inventory.ArticleDetailViewModel
import org.abdat.inventory.ui.inventory.CATEGORY_FILTER_ALL
import org.abdat.inventory.ui.inventory.CATEGORY_FILTER_UNCLASSIFIED
import org.abdat.inventory.ui.inventory.InventoryViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the modified `inventory-master-screen` requirements: selection-only categories enforced by
 * the view models, and family filter chips driven by the observed taxonomy.
 */
class InventoryCategoryEnforcementTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var categoryRepository: CategoryRepository

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
        ensureCategoryTaxonomy(database)
        categoryRepository = CategoryRepository(database, Dispatchers.IO)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun inventoryViewModel() =
        InventoryViewModel(database, StockRepository(database, Dispatchers.IO), categoryRepository)

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

    private fun articles() = database.appDatabaseQueries.selectAllStockArticles().executeAsList().map { it.toDomain() }

    private fun InventoryViewModel.save(code: String, family: String, sousFamily: String): Boolean = saveArticleSync(
        code = code,
        designation = "Article $code",
        unite = "Pièce",
        family = family,
        sousFamily = sousFamily,
        quantity = 3.0,
        unitPrice = 85_000.0,
        date = "2026-09-15"
    )

    private fun insertArticle(code: String, family: String, sousFamily: String) {
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
            is_archived = 0L,
            is_obsolete = 0L,
            date = "2026-09-15"
        )
    }

    @Test
    fun unconfiguredFamilyIsRefusedWithoutWriting() {
        val viewModel = inventoryViewModel()
        viewModel.openAddDialog()

        assertFalse(viewModel.save("ART-001", "Pharmacie centrale", "Antalgique"))

        assertTrue(articles().isEmpty())
        val state = viewModel.uiState.value
        assertNotNull(state.articleSaveError)
        assertTrue(state.isAddEditDialogOpen, "the dialog stays open so the user can correct the selection")
    }

    @Test
    fun sousFamilyFromAnotherFamilyIsRefused() {
        val viewModel = inventoryViewModel()
        viewModel.openAddDialog()

        assertFalse(viewModel.save("ART-002", "Médicaments", "Ordinateurs"))
        assertTrue(articles().isEmpty())
    }

    @Test
    fun blankSousFamilyIsRefused() {
        val viewModel = inventoryViewModel()
        viewModel.openAddDialog()

        assertFalse(viewModel.save("ART-003", "Médicaments", ""))
        assertTrue(articles().isEmpty())
    }

    @Test
    fun validPairIsInsertedWithConfiguredLabelCasing() {
        val viewModel = inventoryViewModel()
        viewModel.openAddDialog()

        assertTrue(viewModel.save("PC-001", "  accessoire INFORMATIQUE", "ordinateurs "))

        val stored = articles().single()
        assertEquals("Accessoire informatique", stored.family)
        assertEquals("Ordinateurs", stored.sousFamily)
        val state = viewModel.uiState.value
        assertFalse(state.isAddEditDialogOpen)
        assertNull(state.articleSaveError)
    }

    @Test
    fun editingALegacyArticleRequiresAValidCategory() {
        insertArticle("OLD-001", "Médicaments", "")
        val viewModel = inventoryViewModel()
        val legacy = articles().single()
        viewModel.openEditDialog(legacy)

        assertFalse(viewModel.save("OLD-001", "Médicaments", ""))
        assertEquals("", articles().single().sousFamily, "no update was written")

        assertTrue(viewModel.save("OLD-001", "Médicaments", "Antalgique"))
        assertEquals("Antalgique", articles().single().sousFamily)
    }

    @Test
    fun articleDetailEditEnforcesTheSameRule() {
        insertArticle("MED-001", "Médicaments", "Antalgique")
        val article = articles().single()
        val detailViewModel = ArticleDetailViewModel(database, categoryRepository)
        detailViewModel.loadArticle(article.id)
        detailViewModel.uiState.await { it.article != null }
        detailViewModel.openEditDialog()

        detailViewModel.saveArticleModifications(
            designation = article.designation,
            additionalReferenceId = "",
            unite = article.uniteQuantite,
            family = "Médicaments",
            sousFamily = "Ordinateurs",
            unitPrice = article.unitPrice,
            expirationDate = null,
            reason = "Reclassement",
            currentDate = "2026-09-15 10:00"
        )

        assertNotNull(detailViewModel.uiState.value.errorMessage)
        assertTrue(detailViewModel.uiState.value.isEditDialogOpen)
        assertEquals("Antalgique", articles().single().sousFamily)
        assertTrue(
            database.appDatabaseQueries.selectAuditLogsByArticleId(article.id).executeAsList().isEmpty(),
            "a refused edit leaves no audit entry"
        )
    }

    @Test
    fun filterChipsComeFromTheConfiguredTaxonomy() {
        insertArticle("GEN-001", "", "")
        val viewModel = inventoryViewModel()

        val chips = viewModel.uiState.value.availableCategories
        assertEquals(CATEGORY_FILTER_ALL, chips.first())
        assertTrue("Imprimés divers" in chips, "a family with no articles still gets a chip")
        assertTrue("Accessoire informatique" in chips)
        assertEquals(CATEGORY_FILTER_UNCLASSIFIED, chips.last())

        viewModel.onCategoryChange(CATEGORY_FILTER_UNCLASSIFIED)
        assertEquals(listOf("GEN-001"), viewModel.uiState.value.filteredArticles.map { it.codeArticle })
    }

    @Test
    fun noHardcodedChipsWithoutATaxonomy() {
        database.appDatabaseQueries.selectAllCategoryItems().executeAsList()
            .forEach { database.appDatabaseQueries.deleteConfigItem(it.id) }

        val viewModel = inventoryViewModel()

        assertEquals(listOf(CATEGORY_FILTER_ALL), viewModel.uiState.value.availableCategories)
    }

    @Test
    fun familyAddedInConfigReachesInventoryWithoutReload() {
        val inventoryVm = inventoryViewModel()
        val configVm = DynamicConfigViewModel(database, categoryRepository)

        assertTrue(configVm.addConfigItemSync("FAMILY", code = "", label = "Linge hospitalier"))
        val familyCode = assertNotNull(categoryRepository.currentTaxonomy().findFamilyByLabel("Linge hospitalier")).code
        assertTrue(
            configVm.addConfigItemSync("SOUS_FAMILY", "", "Draps", parentCategory = "FAMILY", parentCode = familyCode)
        )

        val state = inventoryVm.uiState.await { current ->
            "Linge hospitalier" in current.availableCategories &&
                current.taxonomy.isValidPair("Linge hospitalier", "Draps")
        }
        assertNotNull(state.taxonomy.findFamilyByLabel("Linge hospitalier"))
    }

    @Test
    fun deletedSelectedFamilyResetsTheFilter() {
        val inventoryVm = inventoryViewModel()
        val configVm = DynamicConfigViewModel(database, categoryRepository)
        val printed = assertNotNull(categoryRepository.currentTaxonomy().findFamilyByLabel("Imprimés divers"))

        inventoryVm.onCategoryChange(printed.label)
        categoryRepository.currentTaxonomy().childrenOf(printed.code).forEach { configVm.deleteConfigItemSync(it.id) }
        assertTrue(configVm.deleteConfigItemSync(printed.id))

        val state = inventoryVm.uiState.await { printed.label !in it.availableCategories }
        assertEquals(CATEGORY_FILTER_ALL, state.selectedCategory)
    }
}
