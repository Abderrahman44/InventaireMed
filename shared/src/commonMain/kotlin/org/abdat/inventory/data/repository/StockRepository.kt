package org.abdat.inventory.data.repository

import androidx.compose.runtime.Stable
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.StockArticle

/**
 * A consistent view of `stock_article` plus every aggregate derived from it.
 *
 * The aggregates live here rather than in each ViewModel so the six screens that display stock
 * metrics cannot drift into deriving them differently. They are computed once at construction;
 * `equals` intentionally covers only [articles], since everything else is a function of it.
 */
@Stable
data class StockSnapshot(
    val articles: ImmutableList<StockArticle> = persistentListOf()
) {
    private val activeArticles: List<StockArticle> =
        articles.filter { !it.isArchived && !it.isObsolete }

    val activeArticlesCount: Int = activeArticles.size
    val totalValuationDzd: Double = activeArticles.sumOf { it.quantity * it.unitPrice }
    val outOfStockCount: Int = activeArticles.count { it.quantity <= 0.0 }
    val lowStockCount: Int = activeArticles.count { it.quantity in LOW_STOCK_RANGE }

    /** Articles at or below [threshold], across all articles rather than active ones only. */
    fun countAtOrBelow(threshold: Double): Int = articles.count { it.quantity <= threshold }

    companion object {
        /** Stock is "low" once it drops to this level while still being present. */
        const val LOW_STOCK_CEILING: Double = 10.0
        private val LOW_STOCK_RANGE = 0.001..LOW_STOCK_CEILING
    }
}

/**
 * The single reactive source of truth for stock quantities.
 *
 * Every stock read in the app goes through [snapshots]. SQLDelight notifies each live query
 * registered against `stock_article` when a transaction touching that table commits, so a
 * mutation made on any screen reaches every observer without the writer knowing who is listening.
 *
 * Two caveats this relies on: notification only covers writes made through the same [AppDatabase]
 * instance (Koin provides it as a `single`), and raw `SqlDriver.execute` calls bypass notification
 * entirely — which is safe for the schema migrations in `JvmDatabaseDriverFactory` only because
 * they run before any query is registered.
 */
class StockRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher
) {
    /** Stock plus its derived aggregates, re-emitted whenever `stock_article` changes. */
    val snapshots: Flow<StockSnapshot> =
        database.appDatabaseQueries.selectAllStockArticles()
            .asFlow()
            .mapToList(dispatcher)
            .map { rows -> rows.map { it.toDomain() }.toImmutableList() }
            // SQLDelight notifies on any write to the table, including ones that leave these rows
            // untouched; drop those so collectors only wake for changes they can see.
            .distinctUntilChanged()
            .map { StockSnapshot(it) }

    /** Convenience view for readers that need the articles but none of the aggregates. */
    val articles: Flow<ImmutableList<StockArticle>> =
        snapshots.map { it.articles }.distinctUntilChanged()

    /**
     * A synchronous read of current stock, used only to prime a ViewModel's state at construction
     * so the first frame has data instead of a spinner.
     *
     * Observers still get every subsequent change through [snapshots]; this is a head start, not a
     * substitute. Stock reads stay funnelled through this class so the aggregate derivation has a
     * single definition — ViewModels must not query `stock_article` themselves.
     */
    fun currentSnapshot(): StockSnapshot = StockSnapshot(
        database.appDatabaseQueries.selectAllStockArticles()
            .executeAsList()
            .map { it.toDomain() }
            .toImmutableList()
    )
}
