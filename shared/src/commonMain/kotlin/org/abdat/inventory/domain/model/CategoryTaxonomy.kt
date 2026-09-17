package org.abdat.inventory.domain.model

import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

const val CATEGORY_FAMILY = "FAMILY"
const val CATEGORY_SOUS_FAMILY = "SOUS_FAMILY"

/**
 * The single matching rule for category labels. Articles store labels as free text, so every
 * comparison between an article and the taxonomy — validation, reconciliation, the delete guard,
 * the filter chips — must go through here or they will disagree about what "the same" means.
 */
fun normalizeCategoryLabel(label: String): String = label.trim().lowercase()

fun categoryLabelsMatch(a: String, b: String): Boolean =
    normalizeCategoryLabel(a) == normalizeCategoryLabel(b)

/**
 * The Family → Sous-family hierarchy held in `dynamic_config_item`.
 *
 * [items] carries every row regardless of activity so the configuration screen can still manage
 * inactive ones; everything used for selection or validation is narrowed to active rows. A
 * sous-family belongs to the family whose `code` equals its `parent_code`.
 */
@Stable
data class CategoryTaxonomy(
    val items: ImmutableList<DynamicConfigItem> = persistentListOf()
) {
    val allFamilies: ImmutableList<DynamicConfigItem> =
        items.filter { it.category == CATEGORY_FAMILY }.toImmutableList()

    val allSousFamilies: ImmutableList<DynamicConfigItem> =
        items.filter { it.category == CATEGORY_SOUS_FAMILY }.toImmutableList()

    /** Families offered for selection. */
    val families: ImmutableList<DynamicConfigItem> =
        allFamilies.filter { it.isActive }.toImmutableList()

    fun sousFamiliesOf(familyCode: String): ImmutableList<DynamicConfigItem> =
        allSousFamilies
            .filter { it.isActive && codesMatch(it.parentCode, familyCode) }
            .toImmutableList()

    fun findFamilyByLabel(label: String): DynamicConfigItem? =
        families.firstOrNull { categoryLabelsMatch(it.label, label) }

    fun findFamilyByCode(code: String): DynamicConfigItem? =
        families.firstOrNull { codesMatch(it.code, code) }

    fun findSousFamily(familyCode: String, label: String): DynamicConfigItem? =
        sousFamiliesOf(familyCode).firstOrNull { categoryLabelsMatch(it.label, label) }

    /**
     * Resolves an article's stored labels to the configured pair, or null when either side is not
     * a configured active category. Blank labels never resolve.
     */
    fun resolvePair(familyLabel: String, sousFamilyLabel: String): Pair<DynamicConfigItem, DynamicConfigItem>? {
        if (familyLabel.isBlank() || sousFamilyLabel.isBlank()) return null
        val family = findFamilyByLabel(familyLabel) ?: return null
        val sousFamily = findSousFamily(family.code, sousFamilyLabel) ?: return null
        return family to sousFamily
    }

    fun isValidPair(familyLabel: String, sousFamilyLabel: String): Boolean =
        resolvePair(familyLabel, sousFamilyLabel) != null

    /** Sous-families of every activity state attached to [familyCode]; any of them blocks deletion. */
    fun childrenOf(familyCode: String): List<DynamicConfigItem> =
        allSousFamilies.filter { codesMatch(it.parentCode, familyCode) }

    /**
     * Articles referencing [item] according to [usage]. A family counts every article carrying its
     * label; a sous-family counts only articles carrying its label under its own parent family.
     */
    fun articleCountFor(item: DynamicConfigItem, usage: List<ArticleCategoryUsage>): Long = when (item.category) {
        CATEGORY_FAMILY -> usage
            .filter { categoryLabelsMatch(it.family, item.label) }
            .sumOf { it.articleCount }
        CATEGORY_SOUS_FAMILY -> {
            val parent = allFamilies.firstOrNull { codesMatch(it.code, item.parentCode) }
            if (parent == null) 0L else usage
                .filter { categoryLabelsMatch(it.family, parent.label) && categoryLabelsMatch(it.sousFamily, item.label) }
                .sumOf { it.articleCount }
        }
        else -> 0L
    }

    private fun codesMatch(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)
}

/** How many articles carry a given raw family / sous-family label pair. */
data class ArticleCategoryUsage(
    val family: String,
    val sousFamily: String,
    val articleCount: Long
)

object CategoryCodes {
    private const val MAX_LENGTH = 15

    /** The code the configuration screen has always derived from a label. Kept for compatibility. */
    fun fromLabel(label: String): String =
        label.trim().uppercase().replace(" ", "_").take(MAX_LENGTH)

    /**
     * A code for [label] that is not already in [takenCodes], suffixing `_2`, `_3`… when the
     * truncated form collides. Used where refusing is not an option (baseline and reconciliation).
     */
    fun uniqueFromLabel(label: String, takenCodes: Collection<String>): String {
        val taken = takenCodes.map { it.trim().uppercase() }.toSet()
        val base = fromLabel(label)
        if (base !in taken) return base
        var index = 2
        while (true) {
            val suffix = "_$index"
            val candidate = base.take(MAX_LENGTH - suffix.length) + suffix
            if (candidate !in taken) return candidate
            index++
        }
    }
}
