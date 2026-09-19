package org.abdat.inventory

import org.abdat.inventory.domain.model.StockArticle
import org.abdat.inventory.domain.model.categoryLabelsMatch
import org.abdat.inventory.ui.inventory.CATEGORY_FILTER_UNCLASSIFIED
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportExportFilteringTest {

    private fun sampleArticles(): List<StockArticle> = listOf(
        StockArticle(
            id = 1,
            codeArticle = "MED-001",
            designation = "Paracétamol",
            family = "Médicaments",
            sousFamily = "Antalgiques",
            quantity = 50.0,
            uniteQuantite = "Boîte",
            unitPrice = 120.0,
            date = "2026-01-01"
        ),
        StockArticle(
            id = 2,
            codeArticle = "CON-001",
            designation = "Gants Latex",
            family = "Consommables",
            sousFamily = "Protection",
            quantity = 200.0,
            uniteQuantite = "Boîte",
            unitPrice = 45.0,
            date = "2026-01-01"
        ),
        StockArticle(
            id = 3,
            codeArticle = "MAT-001",
            designation = "Tensiomètre",
            family = "Matériel",
            sousFamily = "Diagnostic",
            quantity = 5.0,
            uniteQuantite = "Pièce",
            unitPrice = 3500.0,
            date = "2026-01-01"
        ),
        StockArticle(
            id = 4,
            codeArticle = "UNC-001",
            designation = "Article Inconnu",
            family = "",
            sousFamily = "",
            quantity = 12.0,
            uniteQuantite = "Unité",
            unitPrice = 10.0,
            date = "2026-01-01"
        )
    )

    private fun filterArticles(
        articles: List<StockArticle>,
        selectedFamilies: Map<String, Boolean>
    ): List<StockArticle> {
        return articles.filter { article ->
            val fam = article.family.ifBlank { CATEGORY_FILTER_UNCLASSIFIED }
            selectedFamilies.entries.any { (familyLabel, checked) ->
                checked && categoryLabelsMatch(familyLabel, fam)
            }
        }
    }

    private fun formatFamilySummary(
        allFamilies: List<String>,
        selectedFamilies: Map<String, Boolean>
    ): String {
        val checked = allFamilies.filter { selectedFamilies[it] == true }
        return when {
            checked.isEmpty() -> "Aucune"
            checked.size == allFamilies.size -> "Toutes (${allFamilies.size})"
            else -> checked.joinToString(", ")
        }
    }

    @Test
    fun testAllFamiliesSelectedIncludesAllArticles() {
        val articles = sampleArticles()
        val allFamilies = listOf("Médicaments", "Consommables", "Matériel", CATEGORY_FILTER_UNCLASSIFIED)
        val selectedFamilies = allFamilies.associateWith { true }

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(4, filtered.size)
        assertEquals("Toutes (4)", formatFamilySummary(allFamilies, selectedFamilies))
    }

    @Test
    fun testSingleFamilySelected() {
        val articles = sampleArticles()
        val allFamilies = listOf("Médicaments", "Consommables", "Matériel", CATEGORY_FILTER_UNCLASSIFIED)
        val selectedFamilies = mapOf(
            "Médicaments" to true,
            "Consommables" to false,
            "Matériel" to false,
            CATEGORY_FILTER_UNCLASSIFIED to false
        )

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(1, filtered.size)
        assertEquals("MED-001", filtered.first().codeArticle)
        assertEquals("Médicaments", formatFamilySummary(allFamilies, selectedFamilies))
    }

    @Test
    fun testMultipleFamiliesSelected() {
        val articles = sampleArticles()
        val allFamilies = listOf("Médicaments", "Consommables", "Matériel", CATEGORY_FILTER_UNCLASSIFIED)
        val selectedFamilies = mapOf(
            "Médicaments" to true,
            "Consommables" to true,
            "Matériel" to false,
            CATEGORY_FILTER_UNCLASSIFIED to false
        )

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(2, filtered.size)
        assertTrue(filtered.any { it.codeArticle == "MED-001" })
        assertTrue(filtered.any { it.codeArticle == "CON-001" })
        assertEquals("Médicaments, Consommables", formatFamilySummary(allFamilies, selectedFamilies))
    }

    @Test
    fun testUnclassifiedFamilySelection() {
        val articles = sampleArticles()
        val selectedFamilies = mapOf(
            "Médicaments" to false,
            "Consommables" to false,
            "Matériel" to false,
            CATEGORY_FILTER_UNCLASSIFIED to true
        )

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(1, filtered.size)
        assertEquals("UNC-001", filtered.first().codeArticle)
    }

    @Test
    fun testDeselectAllReturnsEmptyList() {
        val articles = sampleArticles()
        val allFamilies = listOf("Médicaments", "Consommables", "Matériel", CATEGORY_FILTER_UNCLASSIFIED)
        val selectedFamilies = allFamilies.associateWith { false }

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(0, filtered.size)
        assertEquals("Aucune", formatFamilySummary(allFamilies, selectedFamilies))
    }

    @Test
    fun testCaseAndWhitespaceInsensitivity() {
        val articles = sampleArticles()
        val selectedFamilies = mapOf(
            "  médicaments  " to true
        )

        val filtered = filterArticles(articles, selectedFamilies)
        assertEquals(1, filtered.size)
        assertEquals("MED-001", filtered.first().codeArticle)
    }
}
