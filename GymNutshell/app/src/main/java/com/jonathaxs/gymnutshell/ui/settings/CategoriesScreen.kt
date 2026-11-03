package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.data.CustomGoalCategory
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.ui.components.CategoryKindPicker
import com.jonathaxs.gymnutshell.ui.components.CategorySymbolPicker
import com.jonathaxs.gymnutshell.ui.components.GroupRow
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.components.GroupSection

/** Subtela de Categorias — porte de CategoriesSettingsView (iOS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = Color(state.accentArgb)
    var editing by remember { mutableStateOf<CustomGoalCategory?>(null) }

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.goals_categories_title), onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                GroupSection(title = stringResource(R.string.goals_category_order)) {
                    state.items.forEachIndexed { idx, item ->
                        if (idx > 0) GroupRowDivider()
                        GroupRow(
                            title = item.titleRes?.let { stringResource(it) } ?: item.title.orEmpty(),
                            trailing = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.isCustom) {
                                        IconButton(onClick = { editing = item.custom }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.a11y_edit_category),
                                                tint = accent,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        IconButton(onClick = { item.custom?.let { viewModel.deleteCustom(it.id) } }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cd_delete),
                                                tint = Color(0xFFFF3B30),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    ReorderArrows(
                                        canUp = idx > 0,
                                        canDown = idx < state.items.lastIndex,
                                        onMoveUp = { viewModel.move(item.id, up = true) },
                                        onMoveDown = { viewModel.move(item.id, up = false) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    editing?.let { category ->
        EditCategorySheet(
            category = category,
            existingCategories = state.items.mapNotNull { it.custom },
            accent = accent,
            onDismiss = { editing = null },
            onSave = { updated ->
                viewModel.saveCategory(updated)
                editing = null
            },
        )
    }
}

/** Setas ↑/↓ de reordenação de categoria (desabilitadas nos extremos). */
@Composable
private fun ReorderArrows(canUp: Boolean, canDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMoveUp, enabled = canUp) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.cd_move_up),
                tint = if (canUp) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onMoveDown, enabled = canDown) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_move_down),
                tint = if (canDown) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
    }
}

/**
 * Sheet de criar ou editar categoria personalizada (nome + tipo + ícone) —
 * porte de EditCustomGoalCategoryView (iOS). Quando `category` é nulo, cria uma nova.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCategorySheet(
    category: CustomGoalCategory?,
    existingCategories: List<CustomGoalCategory>,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (CustomGoalCategory) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var kind by remember(category?.id) { mutableStateOf(category?.kind ?: CategoryKind.Standard) }
    var symbol by remember(category?.id) { mutableStateOf(category?.symbolName ?: CustomGoalCategory.DEFAULT_SYMBOL_NAME) }

    val trimmedName = name.trim()
    val builtinNames = GoalCategory.entries.map { stringResource(GoalsViewModel.categoryTitleRes(it)) }
    val isNameConflict = trimmedName.isNotEmpty() && (
        builtinNames.any { it.equals(trimmedName, ignoreCase = true) } ||
            existingCategories.any { it.id != category?.id && it.name.equals(trimmedName, ignoreCase = true) }
    )
    val canSave = trimmedName.isNotEmpty() && !isNameConflict

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = category?.name ?: stringResource(R.string.addgoal_category_new_button),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.addgoal_category_new_section)) },
                placeholder = { Text(stringResource(R.string.addgoal_category_new_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (isNameConflict) {
                Text(
                    text = stringResource(R.string.addgoal_warning_category_duplicate),
                    color = Color(0xFFFF3B30),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CategoryKindPicker(
                selected = kind,
                onSelect = { kind = it },
            )
            Text(stringResource(R.string.settings_category_icon_section), style = MaterialTheme.typography.titleMedium)
            CategorySymbolPicker(
                selection = symbol,
                onSelect = { symbol = it },
                accentColor = accent,
            )
            Button(
                onClick = {
                    val base = category?.copy(name = trimmedName, symbolName = symbol)
                        ?: CustomGoalCategory(name = trimmedName, symbolName = symbol)
                    onSave(base.withKind(kind))
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }
}
