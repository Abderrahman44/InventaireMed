package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.database.CATEGORY_BASELINE
import org.abdat.inventory.database.ensureCategoryTaxonomy
import org.abdat.inventory.domain.model.CategoryTaxonomy
import kotlinx.collections.immutable.toImmutableList
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the `Hospital-Wide Baseline Taxonomy` and `Legacy Article Category Reconciliation`
 * requirements: what `ensureCategoryTaxonomy` adds on fresh and pre-existing databases, and what it
 * must leave alone.
 */
class CategoryTaxonomyBaselineTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        database = AppDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private val queries get() = database.appDatabaseQueries

    private fun taxonomy() = CategoryTaxonomy(
        queries.selectAllCategoryItems().executeAsList().map { it.toDomain() }.toImmutableList()
    )

    private fun categoryRowCount() = queries.selectAllCategoryItems().executeAsList().size

    private fun insertArticle(code: String, family: String, sousFamily: String) {
        queries.insertStockArticle(
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

    /** Reproduces the taxonomy the pre-change mock seeder wrote, codes and all. */
    private fun insertLegacyPharmaceuticalTaxonomy() {
        listOf("Médicaments", "Consommables", "Réactifs", "Matériel").forEach { fam ->
            queries.insertConfigItem("FAMILY", fam.uppercase(), fam, "", "", 1L)
        }
        listOf(
            "Antalgique" to "MÉDICAMENTS",
            "Antibiotique" to "MÉDICAMENTS",
            "Hormones" to "MÉDICAMENTS",
            "Injection" to "CONSOMMABLES",
            "Perfusion" to "CONSOMMABLES",
            "Diagnostic" to "RÉACTIFS",
            "Monitoring" to "MATÉRIEL"
        ).forEach { (sub, parent) ->
            queries.insertConfigItem("SOUS_FAMILY", sub.uppercase(), sub, "FAMILY", parent, 1L)
        }
    }

    @Test
    fun freshDatabaseReceivesEveryBaselineCategory() {
        ensureCategoryTaxonomy(database)

        val taxonomy = taxonomy()
        CATEGORY_BASELINE.forEach { (familyLabel, sousFamilies) ->
            val family = assertNotNull(taxonomy.findFamilyByLabel(familyLabel), "missing family $familyLabel")
            sousFamilies.forEach { sub ->
                assertNotNull(taxonomy.findSousFamily(family.code, sub), "missing $familyLabel / $sub")
            }
        }
        assertTrue(taxonomy.isValidPair("Accessoire informatique", "Ordinateurs"), "a PC can be categorised")
    }

    @Test
    fun preExistingDatabaseGainsGeneralStockWithoutTouchingExistingRows() {
        insertLegacyPharmaceuticalTaxonomy()
        insertArticle("MED-001", "Médicaments", "Antalgique")
        val before = queries.selectAllCategoryItems().executeAsList()

        ensureCategoryTaxonomy(database)

        val after = queries.selectAllCategoryItems().executeAsList()
        before.forEach { original ->
            assertEquals(original, after.single { it.id == original.id }, "existing row ${original.label} changed")
        }

        val taxonomy = taxonomy()
        assertEquals(1, taxonomy.allFamilies.count { it.label == "Médicaments" }, "no duplicate by label")
        assertNotNull(taxonomy.findFamilyByLabel("Imprimés divers"))
        // A new baseline sous-family lands under the existing family's legacy code.
        assertNotNull(taxonomy.findSousFamily("CONSOMMABLES", "Pansements"))

        val countAfterFirstRun = categoryRowCount()
        ensureCategoryTaxonomy(database)
        assertEquals(countAfterFirstRun, categoryRowCount(), "second run must not duplicate")
    }

    @Test
    fun deletedBaselineCategoriesAreNotRecreated() {
        ensureCategoryTaxonomy(database)

        val taxonomy = taxonomy()
        val printed = assertNotNull(taxonomy.findFamilyByLabel("Imprimés divers"))
        taxonomy.allSousFamilies.filter { it.parentCode == printed.code }.forEach { queries.deleteConfigItem(it.id) }
        queries.deleteConfigItem(printed.id)
        val office = assertNotNull(taxonomy.findFamilyByLabel("Accessoire bureautique"))
        queries.deleteConfigItem(assertNotNull(taxonomy.findSousFamily(office.code, "Papeterie")).id)

        ensureCategoryTaxonomy(database)

        val reloaded = taxonomy()
        assertNull(reloaded.findFamilyByLabel("Imprimés divers"))
        assertNull(reloaded.findSousFamily(office.code, "Papeterie"))
    }

    @Test
    fun reconciliationRegistersLabelsUsedByArticles() {
        insertLegacyPharmaceuticalTaxonomy()
        insertArticle("PRO-001", "Protection", "")
        insertArticle("MAT-900", "Matériel", "Usage Unique")
        insertArticle("MED-900", "médicaments", "Antalgique")
        insertArticle("GEN-001", "", "")
        insertArticle("GEN-002", "", "Orpheline")

        ensureCategoryTaxonomy(database)

        val taxonomy = taxonomy()
        assertNotNull(taxonomy.findFamilyByLabel("Protection"))
        assertNotNull(taxonomy.findSousFamily("MATÉRIEL", "Usage Unique"), "registered under Matériel's code")
        assertEquals(1, taxonomy.allFamilies.count { it.label.equals("médicaments", ignoreCase = true) })
        assertTrue(taxonomy.items.none { it.label.isBlank() }, "blank labels are never registered")
        assertTrue(taxonomy.allSousFamilies.none { it.label == "Orpheline" }, "no parent, so nothing to attach to")

        // Every article with both labels set is now valid under the selection-only rule.
        assertTrue(taxonomy.isValidPair("Matériel", "Usage Unique"))
        assertTrue(taxonomy.isValidPair("médicaments", "Antalgique"))
    }
}
