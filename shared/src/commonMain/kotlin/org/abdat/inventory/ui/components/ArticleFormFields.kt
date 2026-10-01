package org.abdat.inventory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.StockArticle

/** Values of the article form once validated; [family] / [sousFamily] are the configured labels. */
data class ArticleFormValues(
    val code: String,
    val barcode: String,
    val designation: String,
    val unite: String,
    val family: String,
    val sousFamily: String,
    val quantity: Double,
    val unitPrice: Double,
    val expirationDate: String?
)

/** State of the shared article form (Gestion de stock "Nouvel Article" and convention "Ajouter un Article"). */
@Stable
class ArticleFormState internal constructor(
    article: StockArticle?,
    val categorySelection: ArticleCategorySelection
) {
    var code by mutableStateOf(article?.codeArticle ?: "")
    var barcode by mutableStateOf(article?.additionalReferenceId ?: "")
    var designation by mutableStateOf(article?.designation ?: "")
    var unite by mutableStateOf(article?.uniteQuantite ?: "BOITE")
    var quantityText by mutableStateOf(article?.quantity?.toString() ?: "0")
    var priceText by mutableStateOf(article?.unitPrice?.toString() ?: "0")
    var expirationDate by mutableStateOf(article?.expirationDate ?: "")

    var codeError by mutableStateOf(false)
    var designationError by mutableStateOf(false)
    var showCategoryErrors by mutableStateOf(false)

    /** Flags every invalid field; returns the values to persist, or null while anything is invalid. */
    fun validate(taxonomy: CategoryTaxonomy): ArticleFormValues? {
        codeError = code.isBlank()
        designationError = designation.isBlank()
        val labels = categorySelection.resolvedLabels(taxonomy)
        showCategoryErrors = labels == null
        if (codeError || designationError || labels == null) return null
        return ArticleFormValues(
            code = code.trim(),
            barcode = barcode.trim(),
            designation = designation.trim(),
            unite = unite.trim(),
            family = labels.first,
            sousFamily = labels.second,
            quantity = quantityText.toDoubleOrNull() ?: 0.0,
            unitPrice = priceText.toDoubleOrNull() ?: 0.0,
            expirationDate = expirationDate.ifBlank { null }
        )
    }
}

@Composable
fun rememberArticleFormState(article: StockArticle?, taxonomy: CategoryTaxonomy): ArticleFormState {
    // No default category: creation starts empty; editing preselects only what still resolves.
    val selection = rememberArticleCategorySelection(
        family = article?.family ?: "",
        sousFamily = article?.sousFamily ?: "",
        taxonomy = taxonomy
    )
    return remember { ArticleFormState(article, selection) }
}

@Composable
fun ArticleFormFields(
    state: ArticleFormState,
    taxonomy: CategoryTaxonomy,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.code,
                onValueChange = { state.code = it; state.codeError = false },
                label = { Text("Code Article (ex: ART-101)") },
                isError = state.codeError,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.barcode,
                onValueChange = { state.barcode = it },
                label = { Text("Réf. Barcode / Fabriquant") },
                modifier = Modifier.weight(1.2f)
            )
        }

        OutlinedTextField(
            value = state.designation,
            onValueChange = { state.designation = it; state.designationError = false },
            label = { Text("Désignation de l'Article") },
            isError = state.designationError,
            modifier = Modifier.fillMaxWidth()
        )

        ArticleCategoryPicker(
            taxonomy = taxonomy,
            selection = state.categorySelection,
            showErrors = state.showCategoryErrors,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.unite,
                onValueChange = { state.unite = it },
                label = { Text("Unité (Pièce, Boîte, Rame...)") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.expirationDate,
                onValueChange = { state.expirationDate = it },
                label = { Text("Péremption (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.quantityText,
                onValueChange = { state.quantityText = it },
                label = { Text("Quantité Stock") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.priceText,
                onValueChange = { state.priceText = it },
                label = { Text("Prix Unitaire (DZD)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
