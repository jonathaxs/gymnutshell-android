package com.jonathaxs.gymnutshell.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.CategoryKind

/**
 * Seletor do tipo de categoria (Padrão, Dia de descanso, Check-in) com texto explicativo abaixo —
 * reutilizado na criação inline de categoria (AddGoalScreen) e na edição (CategoriesScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryKindPicker(
    selected: CategoryKind,
    onSelect: (CategoryKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = stringResource(kindTitleRes(selected)),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_category_kind)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CategoryKind.entries.forEach { kind ->
                    DropdownMenuItem(
                        text = { Text(stringResource(kindTitleRes(kind))) },
                        onClick = {
                            onSelect(kind)
                            expanded = false
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(kindFooterRes(selected)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@StringRes
private fun kindTitleRes(kind: CategoryKind): Int = when (kind) {
    CategoryKind.Standard -> R.string.category_kind_standard
    CategoryKind.RestDay -> R.string.category_kind_rest_day
    CategoryKind.CheckIn -> R.string.category_kind_check_in
}

@StringRes
private fun kindFooterRes(kind: CategoryKind): Int = when (kind) {
    CategoryKind.Standard -> R.string.category_kind_standard_footer
    CategoryKind.RestDay -> R.string.category_kind_rest_day_footer
    CategoryKind.CheckIn -> R.string.category_kind_check_in_footer
}
