package org.abdat.inventory.database

import org.abdat.inventory.domain.model.CATEGORY_FAMILY
import org.abdat.inventory.domain.model.CATEGORY_SOUS_FAMILY
import org.abdat.inventory.domain.model.CategoryCodes
import org.abdat.inventory.domain.model.categoryLabelsMatch

/**
 * Bump when [CATEGORY_BASELINE] gains entries. Each version is applied once and recorded in
 * `app_metadata`, so a category the user later deletes is not resurrected on the next launch.
 */
const val CATEGORY_BASELINE_VERSION = 1

internal const val CATEGORY_BASELINE_VERSION_KEY = "category_baseline_version"

/**
 * Hospital-wide reference taxonomy: medical stock alongside general stock (IT, office, hardware,
 * printed forms). This is reference data, not mock data — it ships in every build, independent of
 * the development seeder.
 */
val CATEGORY_BASELINE: List<Pair<String, List<String>>> = listOf(
    "Médicaments" to listOf("Antalgique", "Antibiotique", "Hormones"),
    "Consommables" to listOf("Injection", "Perfusion", "Pansements"),
    "Réactifs" to listOf("Diagnostic"),
    "Matériel" to listOf("Monitoring", "Mobilier médical"),
    "Quincaillerie générale" to listOf("Outillage", "Électricité", "Plomberie"),
    "Accessoire informatique" to listOf("Ordinateurs", "Périphériques", "Réseau", "Consommables informatiques"),
    "Accessoire bureautique" to listOf("Fournitures de bureau", "Papeterie", "Classement"),
    "Imprimés divers" to listOf("Formulaires administratifs", "Registres", "Étiquettes")
)

/**
 * Brings the category taxonomy to a usable state on every startup:
 *
 * 1. Applies [CATEGORY_BASELINE] once per [CATEGORY_BASELINE_VERSION], reusing rows that already
 *    carry a matching label rather than duplicating them.
 * 2. Registers every non-blank family / sous-family label already used by an article but missing
 *    from the taxonomy, so no existing article becomes invalid under the selection-only rule.
 *
 * Both steps only ever insert. They run in one transaction, before any query is observed, so no
 * half-applied taxonomy is ever emitted.
 */
fun ensureCategoryTaxonomy(database: AppDatabase) {
    val queries = database.appDatabaseQueries
    database.transaction {
        val writer = TaxonomyWriter(queries)

        val appliedVersion = queries.getMetadata(CATEGORY_BASELINE_VERSION_KEY)
            .executeAsOneOrNull()?.toIntOrNull() ?: 0
        if (appliedVersion < CATEGORY_BASELINE_VERSION) {
            CATEGORY_BASELINE.forEach { (familyLabel, sousFamilies) ->
                val familyCode = writer.ensureFamily(familyLabel)
                sousFamilies.forEach { writer.ensureSousFamily(familyCode, it) }
            }
            queries.setMetadata(CATEGORY_BASELINE_VERSION_KEY, CATEGORY_BASELINE_VERSION.toString())
        }

        queries.selectArticleCategoryUsage().executeAsList().forEach { usage ->
            if (usage.family.isBlank()) return@forEach
            val familyCode = writer.ensureFamily(usage.family)
            if (usage.sous_family.isNotBlank()) {
                writer.ensureSousFamily(familyCode, usage.sous_family)
            }
        }
    }
}

/**
 * Insert-if-absent over an in-memory copy of the taxonomy rows, kept in step with its own inserts so
 * a label seen twice in one run is only written once. Matches rows of every activity state: an
 * inactive category is still a category, and must not be duplicated.
 */
private class TaxonomyWriter(private val queries: AppDatabaseQueries) {

    private data class Row(val category: String, val code: String, val label: String, val parentCode: String)

    private val rows: MutableList<Row> = queries.selectAllCategoryItems().executeAsList()
        .map { Row(it.category, it.code, it.label, it.parent_code) }
        .toMutableList()

    /** Returns the code of the family labelled [label], inserting it when absent. */
    fun ensureFamily(label: String): String {
        rows.firstOrNull { it.category == CATEGORY_FAMILY && categoryLabelsMatch(it.label, label) }
            ?.let { return it.code }

        val code = CategoryCodes.uniqueFromLabel(
            label,
            rows.filter { it.category == CATEGORY_FAMILY }.map { it.code }
        )
        insert(Row(CATEGORY_FAMILY, code, label.trim(), parentCode = ""))
        return code
    }

    fun ensureSousFamily(familyCode: String, label: String) {
        val siblings = rows.filter {
            it.category == CATEGORY_SOUS_FAMILY && it.parentCode.equals(familyCode, ignoreCase = true)
        }
        if (siblings.any { categoryLabelsMatch(it.label, label) }) return

        val code = CategoryCodes.uniqueFromLabel(label, siblings.map { it.code })
        insert(Row(CATEGORY_SOUS_FAMILY, code, label.trim(), parentCode = familyCode))
    }

    private fun insert(row: Row) {
        queries.insertConfigItem(
            category = row.category,
            code = row.code,
            label = row.label,
            parent_category = if (row.category == CATEGORY_SOUS_FAMILY) CATEGORY_FAMILY else "",
            parent_code = row.parentCode,
            is_active = 1L
        )
        rows += row
    }
}
