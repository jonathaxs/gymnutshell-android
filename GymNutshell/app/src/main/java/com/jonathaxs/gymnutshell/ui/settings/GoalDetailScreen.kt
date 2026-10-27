package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.ui.components.GroupRow
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.components.GroupSection
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton

/** Detalhe de uma meta fixa: edita valor + passo — porte de TrackingGoalDetailView (iOS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(goalKey: String, onBack: () -> Unit, viewModel: GoalDetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load(goalKey) }

    // Estado local dos steppers, semeado quando os dados terminam de carregar.
    var value by remember { mutableIntStateOf(0) }
    var increment by remember { mutableIntStateOf(1) }
    LaunchedEffect(state.loaded) {
        if (state.loaded) {
            value = state.value
            increment = state.increment
        }
    }

    val isCheckIn = GoalCategory.defaultCategory(goalKey)?.kind == CategoryKind.CheckIn
    val title = "${state.emoji} ${stringResource(state.titleRes)}".trim()
    val canSave = if (isCheckIn) value >= 1 else (value >= increment && increment >= 1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            viewModel.save(goalKey, value, increment)
                            onBack()
                        },
                    ) { Text(stringResource(R.string.action_save)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            if (isCheckIn) {
                item {
                    GroupSection(footer = stringResource(R.string.addgoal_dose_footer)) {
                        StepperRow(
                            label = stringResource(R.string.addgoal_section_dose),
                            valueText = "$value ${state.unit}",
                            canDecrement = value > 1,
                            canIncrement = value < 99999,
                            onDecrement = { value = (value - 1).coerceAtLeast(1) },
                            onIncrement = { value = (value + 1).coerceAtMost(99999) },
                        )
                    }
                }
            } else {
                item {
                    GroupSection(footer = stringResource(R.string.goaldetail_footer)) {
                        StepperRow(
                            label = stringResource(R.string.goaldetail_stepper),
                            valueText = "$value ${state.unit}",
                            canDecrement = value - increment >= increment,
                            canIncrement = value + increment <= 99999,
                            onDecrement = { value = (value - increment).coerceAtLeast(increment) },
                            onIncrement = { value = (value + increment).coerceAtMost(99999) },
                        )
                        GroupRowDivider()
                        StepperRow(
                            label = stringResource(R.string.goaldetail_increment_stepper),
                            valueText = "$increment ${state.unit}",
                            canDecrement = increment > 1,
                            canIncrement = increment < 99999,
                            onDecrement = { increment = (increment - 1).coerceAtLeast(1) },
                            onIncrement = { increment = (increment + 1).coerceAtMost(99999) },
                        )
                    }
                }
                item {
                    Text(
                        stringResource(R.string.goaldetail_increment_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

/** Linha de stepper com rótulo, valor e botões redondos − / + agrupados à direita — espelha TrackingGoalDetailView (iOS). */
@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    canDecrement: Boolean,
    canIncrement: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    GroupRow(
        title = label,
        subtitle = valueText,
        trailing = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RoundStepButton(
                    symbol = "−",
                    contentDescription = stringResource(R.string.a11y_welcome_goal_decrease, label),
                    onClick = onDecrement,
                    enabled = canDecrement,
                )
                RoundStepButton(
                    symbol = "+",
                    contentDescription = stringResource(R.string.a11y_welcome_goal_increase, label),
                    onClick = onIncrement,
                    enabled = canIncrement,
                )
            }
        },
    )
}
