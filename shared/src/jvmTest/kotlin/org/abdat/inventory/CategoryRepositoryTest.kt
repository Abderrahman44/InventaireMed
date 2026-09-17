package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.abdat.inventory.data.repository.CategoryRepository
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.CategoryCodes
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.DynamicConfigItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers `article-category-taxonomy`: the single label-matching rule and the reactive taxonomy flow
 * that lets a category added in Config reach every open dropdown.
 */
class CategoryRepositoryTest {

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

    private fun family(id: Long, code: String, label: String, active: Boolean = true) =
        DynamicConfigItem(id = id, category = "FAMILY", code = code, label = label, isActive = active)

    private fun sousFamily(id: Long, label: String, parentCode: String, active: Boolean = true) =
        DynamicConfigItem(
            id = id,
            category = "SOUS_FAMILY",
            code = label.uppercase(),
            label = label,
            parentCategory = "FAMILY",
            parentCode = parentCode,
            isActive = active
        )

    private val sampleTaxonomy = CategoryTaxonomy(
        persistentListOf(
            family(1, "MÉDICAMENTS", "Médicaments"),
            family(2, "ACCESSOIRE_INFO", "Accessoire informatique"),
            family(3, "ARCHIVE_FAM", "Ancienne famille", active = false),
            sousFamily(10, "Antalgique", "MÉDICAMENTS"),
            sousFamily(11, "Consommables", "MÉDICAMENTS"),
            sousFamily(12, "Consommables", "ACCESSOIRE_INFO"),
            sousFamily(13, "Ordinateurs", "ACCESSOIRE_INFO"),
            sousFamily(14, "Retiré", "ACCESSOIRE_INFO", active = false)
        )
    )

    private suspend fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(5)
        }
    }

    private fun insertCategory(category: String, code: String, label: String, parentCode: String = "") {
        database.appDatabaseQueries.insertConfigItem(
            category = category,
            code = code,
            label = label,
            parent_category = if (category == "SOUS_FAMILY") "FAMILY" else "",
            parent_code = parentCode,
            is_active = 1L
        )
    }

    @Test
    fun labelMatchingIgnoresCaseAndSurroundingWhitespace() {
        assertEquals("Médicaments", sampleTaxonomy.findFamilyByLabel("  médicaments ")?.label)
        assertEquals("Médicaments", sampleTaxonomy.findFamilyByLabel("MÉDICAMENTS")?.label)
        assertTrue(sampleTaxonomy.isValidPair("accessoire INFORMATIQUE", " ordinateurs"))
    }

    @Test
    fun sousFamilyResolvesOnlyUnderItsOwnParent() {
        assertNotNull(sampleTaxonomy.findSousFamily("ACCESSOIRE_INFO", "Ordinateurs"))
        assertNull(sampleTaxonomy.findSousFamily("MÉDICAMENTS", "Ordinateurs"))
        assertFalse(sampleTaxonomy.isValidPair("Médicaments", "Ordinateurs"))
    }

    @Test
    fun sameSousFamilyLabelUnderTwoFamiliesResolvesToDistinctRows() {
        val underMedical = sampleTaxonomy.findSousFamily("MÉDICAMENTS", "Consommables")
        val underIt = sampleTaxonomy.findSousFamily("ACCESSOIRE_INFO", "Consommables")

        assertNotNull(underMedical)
        assertNotNull(underIt)
        assertNotEquals(underMedical.id, underIt.id)
        assertEquals(2, sampleTaxonomy.sousFamiliesOf("MÉDICAMENTS").size)
    }

    @Test
    fun inactiveAndBlankCategoriesNeverValidate() {
        assertFalse(sampleTaxonomy.isValidPair("Ancienne famille", "Antalgique"))
        assertFalse(sampleTaxonomy.isValidPair("Accessoire informatique", "Retiré"))
        assertFalse(sampleTaxonomy.isValidPair("Médicaments", ""))
        assertFalse(sampleTaxonomy.isValidPair("", "Antalgique"))
        assertEquals(2, sampleTaxonomy.families.size, "inactive families are not offered")
        assertEquals(3, sampleTaxonomy.allFamilies.size, "but remain manageable in Config")
    }

    @Test
    fun uniqueCodeGenerationSuffixesTruncatedCollisions() {
        val first = CategoryCodes.fromLabel("Accessoire informatique")
        assertEquals("ACCESSOIRE_INFO", first)

        val second = CategoryCodes.uniqueFromLabel("Accessoire informatique réseau", listOf(first))
        assertEquals("ACCESSOIRE_IN_2", second)
        assertTrue(second.length <= 15)
    }

    @Test
    fun flowEmitsCurrentTaxonomyOnSubscription() = runBlocking {
        insertCategory("FAMILY", "IMPRIMÉS_DIVERS", "Imprimés divers")
        insertCategory("UNIT", "BOITE", "Boîte")

        val repository = CategoryRepository(database, Dispatchers.IO)
        val received = mutableListOf<CategoryTaxonomy>()
        val job = launch { repository.taxonomy.collect { received += it } }

        awaitUntil { received.isNotEmpty() }

        assertEquals(listOf("Imprimés divers"), received.first().families.map { it.label })
        assertTrue(received.first().items.none { it.category == "UNIT" }, "units are not categories")

        job.cancel()
    }

    @Test
    fun flowReEmitsAfterInsertAndDelete() = runBlocking {
        insertCategory("FAMILY", "QUINCAILLERIE_", "Quincaillerie générale")

        val repository = CategoryRepository(database, Dispatchers.IO)
        val received = mutableListOf<CategoryTaxonomy>()
        val job = launch { repository.taxonomy.collect { received += it } }
        awaitUntil { received.isNotEmpty() }

        insertCategory("SOUS_FAMILY", "OUTILLAGE", "Outillage", parentCode = "QUINCAILLERIE_")
        awaitUntil { received.last().sousFamiliesOf("QUINCAILLERIE_").isNotEmpty() }

        val outillageId = received.last().allSousFamilies.single().id
        database.appDatabaseQueries.deleteConfigItem(outillageId)
        awaitUntil { received.last().allSousFamilies.isEmpty() }

        assertEquals(1, received.last().families.size)

        job.cancel()
    }
}
