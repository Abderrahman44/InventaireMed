package org.abdat.inventory

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.data.repository.StockRepository
import org.abdat.inventory.data.repository.StockSnapshot
import org.abdat.inventory.database.AppDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers `reactive-stock-observation`: the repository flow emits on subscription and re-emits when
 * `stock_article` is mutated, which is the invalidation channel the six reader ViewModels rely on.
 */
class StockRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var driver: JdbcSqliteDriver

    /** Injected rather than referenced from common code — see StockRepository's dispatcher param. */
    private val testDispatcher: CoroutineDispatcher = Dispatchers.IO

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

    private fun insertArticle(
        code: String,
        designation: String,
        quantity: Double,
        unitPrice: Double = 100.0,
        archived: Long = 0L,
        obsolete: Long = 0L
    ) {
        database.appDatabaseQueries.insertStockArticle(
            code_article = code,
            additional_reference_id = "",
            designation = designation,
            unite_quantite = "PIECE",
            family = "Consommables",
            sous_family = "",
            quantity = quantity,
            unit_price = unitPrice,
            expiration_date = null,
            is_archived = archived,
            is_obsolete = obsolete,
            date = "2026-09-13"
        )
    }

    /** Polls until [condition] holds, so the test does not depend on emission timing. */
    private suspend fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!condition()) delay(5)
        }
    }

    @Test
    fun flowEmitsCurrentStockOnSubscription() = runBlocking {
        insertArticle("ART-001", "Compresses stériles", quantity = 40.0)

        val repository = StockRepository(database, testDispatcher)
        val received = mutableListOf<StockSnapshot>()
        val job = launch { repository.snapshots.collect { received += it } }

        awaitUntil { received.isNotEmpty() }

        assertEquals(1, received.first().articles.size)
        assertEquals(40.0, received.first().articles.first().quantity)

        job.cancel()
    }

    @Test
    fun flowReEmitsAfterStockDecrement() = runBlocking {
        insertArticle("ART-001", "Compresses stériles", quantity = 40.0)

        val repository = StockRepository(database, testDispatcher)
        val received = mutableListOf<StockSnapshot>()
        val job = launch { repository.snapshots.collect { received += it } }

        awaitUntil { received.isNotEmpty() }

        // A write made through the same AppDatabase instance must notify the live query.
        database.appDatabaseQueries.decreaseStockQuantity(
            quantity = 10.0,
            date = "2026-09-13",
            code_article = "ART-001"
        )

        awaitUntil { received.size >= 2 }

        assertEquals(30.0, received.last().articles.first().quantity)

        job.cancel()
    }

    @Test
    fun flowReEmitsAfterInsertAndReflectsNewArticle() = runBlocking {
        insertArticle("ART-001", "Compresses stériles", quantity = 40.0)

        val repository = StockRepository(database, testDispatcher)
        val received = mutableListOf<StockSnapshot>()
        val job = launch { repository.snapshots.collect { received += it } }

        awaitUntil { received.isNotEmpty() }
        insertArticle("ART-002", "Gants nitrile", quantity = 5.0)
        awaitUntil { received.last().articles.size == 2 }

        assertTrue(received.last().articles.any { it.codeArticle == "ART-002" })

        job.cancel()
    }

    @Test
    fun aggregatesAreDerivedFromActiveArticlesOnly() {
        insertArticle("ART-001", "Actif en stock", quantity = 40.0, unitPrice = 100.0)
        insertArticle("ART-002", "Actif en rupture", quantity = 0.0, unitPrice = 50.0)
        insertArticle("ART-003", "Actif stock bas", quantity = 4.0, unitPrice = 25.0)
        insertArticle("ART-004", "Archivé", quantity = 999.0, unitPrice = 10.0, archived = 1L)
        insertArticle("ART-005", "Obsolète", quantity = 999.0, unitPrice = 10.0, obsolete = 1L)

        val articles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .map { it.toDomain() }

        val snapshot = StockSnapshot(articles.toImmutableList())

        assertEquals(5, snapshot.articles.size, "all rows are carried, including inactive ones")
        assertEquals(3, snapshot.activeArticlesCount, "archived and obsolete are excluded")
        assertEquals(40.0 * 100.0 + 0.0 + 4.0 * 25.0, snapshot.totalValuationDzd)
        assertEquals(1, snapshot.outOfStockCount)
        assertEquals(1, snapshot.lowStockCount, "0 quantity is rupture, not low stock")
    }

    @Test
    fun countAtOrBelowSpansAllArticlesForScreenSpecificThresholds() {
        insertArticle("ART-001", "Au dessus", quantity = 40.0)
        insertArticle("ART-002", "En dessous", quantity = 12.0)
        insertArticle("ART-003", "Archivé bas", quantity = 3.0, archived = 1L)

        val articles = database.appDatabaseQueries.selectAllStockArticles().executeAsList()
            .map { it.toDomain() }
        val snapshot = StockSnapshot(articles.toImmutableList())

        // Sorties uses a 15.0 post-issue alert threshold over all articles, distinct from the
        // inventory low-stock definition; both read the same emitted list.
        assertEquals(2, snapshot.countAtOrBelow(15.0))
    }
}
