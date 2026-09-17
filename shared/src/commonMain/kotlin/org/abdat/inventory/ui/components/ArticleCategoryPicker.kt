package org.abdat.inventory.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.abdat.inventory.domain.model.CategoryTaxonomy
import org.abdat.inventory.domain.model.DynamicConfigItem

private const val CONFIG_SCREEN_HINT = "à ajouter dans Paramètres & Config"

/**
 * Dialog-local selection of an article's family and sous-family.
 *
 * The family is held as its configured *code* so the dependent sous-family lookup is exact; labels
 * are produced only by [resolvedLabels], against the taxonomy current at save time. The article's
 * original labels are kept as [previousFamily] / [previousSousFamily] so an unreferenced legacy value
 * can be shown as a hint instead of silently vanishing.
 */
@Stable
class ArticleCategorySelection internal constructor(
    initialFamilyCode: String,
    initialSousFamilyLabel: String,
    val previousFamily: String,
    val previousSousFamily: String
) {
    var familyCode: String by mutableStateOf(initialFamilyCode)
        private set
    var sousFamilyLabel: String by mutableStateOf(initialSousFamilyLabel)
        private set

    /** Selecting a family that does not contain the current sous-family clears it. */
    fun selectFamily(code: String, taxonomy: CategoryTaxonomy) {
        familyCode = code
        if (taxonomy.findSousFamily(code, sousFamilyLabel) == null) sousFamilyLabel = ""
    }

    fun selectSousFamily(label: String) {
        sousFamilyLabel = label
    }

    /** The configured labels to persist, or null while the selection is not a valid configured pair. */
    fun resolvedLabels(taxonomy: CategoryTaxonomy): Pair<String, String>? {
        val family = taxonomy.findFamilyByCode(familyCode) ?: return null
        val sousFamily = taxonomy.findSousFamily(family.code, sousFamilyLabel) ?: return null
        return family.label to sousFamily.label
    }
}

/**
 * Preselects the article's current categories when they resolve against [taxonomy]; otherwise the
 * unresolved side starts empty and must be chosen. Creation passes blank labels and gets no default.
 */
@Composable
fun rememberArticleCategorySelection(
    family: String,
    sousFamily: String,
    taxonomy: CategoryTaxonomy
): ArticleCategorySelection = remember(family, sousFamily, taxonomy) {
    val resolvedFamily = taxonomy.findFamilyByLabel(family)
    val resolvedSousFamily = resolvedFamily?.let { taxonomy.findSousFamily(it.code, sousFamily) }
    ArticleCategorySelection(
        initialFamilyCode = resolvedFamily?.code ?: "",
        initialSousFamilyLabel = resolvedSousFamily?.label ?: "",
        // Only values that failed to resolve are hints; a valid value the user changed away from is not.
        previousFamily = if (resolvedFamily == null) family else "",
        previousSousFamily = if (resolvedSousFamily == null) sousFamily else ""
    )
}

/**
 * Selection-only Family → Sous-family inputs. Nothing can be typed: both fields are read-only anchors
 * for menus listing configured, active categories, and there is deliberately no inline "create"
 * action — new categories are added from the configuration screen.
 */
@Composable
fun ArticleCategoryPicker(
    taxonomy: CategoryTaxonomy,
    selection: ArticleCategorySelection,
    showErrors: Boolean,
    modifier: Modifier = Modifier
) {
    ArticleCategoryPicker(
        taxonomy = taxonomy,
        selectedFamilyCode = selection.familyCode,
        selectedSousFamilyLabel = selection.sousFamilyLabel,
        previousFamily = selection.previousFamily,
        previousSousFamily = selection.previousSousFamily,
        showErrors = showErrors,
        onFamilySelected = { selection.selectFamily(it, taxonomy) },
        onSousFamilySelected = { selection.selectSousFamily(it) },
        modifier = modifier
    )
}

@Composable
fun ArticleCategoryPicker(
    taxonomy: CategoryTaxonomy,
    selectedFamilyCode: String,
    selectedSousFamilyLabel: String,
    previousFamily: String,
    previousSousFamily: String,
    showErrors: Boolean,
    onFamilySelected: (String) -> Unit,
    onSousFamilySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val family = taxonomy.findFamilyByCode(selectedFamilyCode)
    val sousFamilies = family?.let { taxonomy.sousFamiliesOf(it.code) }.orEmpty()
    val sousFamily = family?.let { taxonomy.findSousFamily(it.code, selectedSousFamilyLabel) }

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CategoryDropdown(
            label = "Famille *",
            selectedLabel = family?.label.orEmpty(),
            options = taxonomy.families,
            enabled = true,
            emptyMessage = "Aucune famille configurée — $CONFIG_SCREEN_HINT",
            supportingLines = listOfNotNull(
                "Sélectionnez une famille".takeIf { showErrors && family == null },
                previousValueHint(previousFamily).takeIf { family == null },
                "Aucune famille configurée — $CONFIG_SCREEN_HINT".takeIf { taxonomy.families.isEmpty() }
            ),
            isError = showErrors && family == null,
            onSelected = { onFamilySelected(it.code) },
            modifier = Modifier.weight(1f)
        )
        CategoryDropdown(
            label = "Sous-Famille *",
            selectedLabel = sousFamily?.label.orEmpty(),
            options = sousFamilies,
            enabled = family != null,
            emptyMessage = "Aucune sous-famille — $CONFIG_SCREEN_HINT",
            supportingLines = listOfNotNull(
                "Sélectionnez une sous-famille".takeIf { showErrors && sousFamily == null },
                previousValueHint(previousSousFamily).takeIf { sousFamily == null },
                "Aucune sous-famille pour cette famille — $CONFIG_SCREEN_HINT"
                    .takeIf { family != null && sousFamilies.isEmpty() }
            ),
            isError = showErrors && sousFamily == null,
            onSelected = { onSousFamilySelected(it.label) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun previousValueHint(previous: String): String? =
    previous.trim().takeIf { it.isNotEmpty() }?.let { "Valeur précédente : « $it » (non référencée)" }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    label: String,
    selectedLabel: String,
    options: List<DynamicConfigItem>,
    enabled: Boolean,
    emptyMessage: String,
    supportingLines: List<String>,
    isError: Boolean,
    onSelected: (DynamicConfigItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            isError = isError,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            supportingText = if (supportingLines.isEmpty()) null else {
                { Column { supportingLines.forEach { Text(it) } } }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(text = { Text(emptyMessage) }, onClick = {}, enabled = false)
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
