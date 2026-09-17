package org.abdat.inventory.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import org.abdat.inventory.data.mapper.toDomain
import org.abdat.inventory.database.AppDatabase
import org.abdat.inventory.domain.model.ArticleCategoryUsage
import org.abdat.inventory.domain.model.CategoryTaxonomy

/**
 * The single reactive source of truth for the article category taxonomy.
 *
 * Mirrors [StockRepository]: a family or sous-family written from the configuration screen notifies
 * this live query, so every dropdown and filter observing [taxonomy] updates without a reload call.
 * The same caveats apply — writes must go through the shared [AppDatabase] instance.
 */
class CategoryRepository(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher
) {
    private val _refreshTrigger = MutableStateFlow(0L)

    val taxonomy: Flow<CategoryTaxonomy> = combine(
        database.appDatabaseQueries.selectAllCategoryItems()
            .asFlow()
            .mapToList(dispatcher),
        _refreshTrigger
    ) { rows, _ ->
        CategoryTaxonomy(rows.map { it.toDomain() }.toImmutableList())
    }.distinctUntilChanged()

    fun notifyTaxonomyChanged() {
        _refreshTrigger.update { it + 1 }
    }

    /** Synchronous read used only to prime a ViewModel's first frame; observers use [taxonomy]. */
    fun currentTaxonomy(): CategoryTaxonomy = CategoryTaxonomy(
        database.appDatabaseQueries.selectAllCategoryItems()
            .executeAsList()
            .map { it.toDomain() }
            .toImmutableList()
    )

    /** Raw label pairs in use by articles, archived and obsolete included. */
    fun articleCategoryUsage(): List<ArticleCategoryUsage> =
        database.appDatabaseQueries.selectArticleCategoryUsage()
            .executeAsList()
            .map { ArticleCategoryUsage(it.family, it.sous_family, it.article_count) }
}
