package com.jonathaxs.gymnutshell.ui.achievements

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.ProgressColors
import com.jonathaxs.gymnutshell.ui.today.CheckInSelector
import com.jonathaxs.gymnutshell.ui.today.TodayCategoryUi
import com.jonathaxs.gymnutshell.ui.today.TodayGoalUi
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton
import kotlin.math.roundToInt

/**
 * Edição de uma conquista (DailyRecord) recente — porte da EditTodayView (iOS).
 * Cabeçalho com data e tier recalculado ao vivo (5 níveis), sem pontos.
 * Suporta modo Stepper (com botões -/+ redondos RoundStepButton) ou Slider conforme o controlStyle salvo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecordScreen(
    epochDay: Long,
    onBack: () -> Unit,
    viewModel: EditRecordViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = Color(state.accentArgb)
    LaunchedEffect(epochDay) { viewModel.load(epochDay) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_record_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save(onBack) }, enabled = state.loaded) {
                        Text(stringResource(R.string.edit_record_save))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "header") { EditHeaderCard(state, accent) }

            state.sections.forEach { section ->
                item(key = "header_${section.id}") {
                    EditCategoryHeader(
                        section = section,
                        accent = accent,
                        onToggle = { viewModel.toggleCategory(section.id) },
                    )
                }
                if (!section.collapsed) {
                    items(section.goals, key = { it.key }) { goal ->
                        EditGoalRow(
                            goal = goal,
                            controlStyle = state.controlStyle,
                            accent = accent,
                            onSet = { viewModel.setIntake(goal, it) },
                            onToggleRest = { viewModel.toggleRestDay(goal) },
                        )
                    }
                }
            }

            if (state.uncategorizedGoals.isNotEmpty()) {
                items(state.uncategorizedGoals, key = { it.key }) { goal ->
                    EditGoalRow(
                        goal = goal,
                        controlStyle = state.controlStyle,
                        accent = accent,
                        onSet = { viewModel.setIntake(goal, it) },
                        onToggleRest = { viewModel.toggleRestDay(goal) },
                    )
                }
            }
        }
    }
}

/** Cabeçalho: emoji do tier + data + "Level N · X%" — espelha o card de resumo da EditTodayView (sem pontos). */
@Composable
private fun EditHeaderCard(state: EditRecordUiState, accent: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(state.tierEmoji, fontSize = 40.sp, modifier = Modifier.clearAndSetSemantics {})
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    state.dateLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "${stringResource(state.tierNameRes)} · ${state.percent}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Cabeçalho de categoria, clicável para recolher/expandir. */
@Composable
private fun EditCategoryHeader(
    section: TodayCategoryUi,
    accent: Color,
    onToggle: () -> Unit,
) {
    val title = section.titleRes?.let { stringResource(it) } ?: section.title.orEmpty()
    val shape = if (section.collapsed) {
        RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
    } else {
        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    }
    val bg = if (section.collapsed) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f)
    } else {
        accent.copy(alpha = 0.22f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (section.collapsed) "▸" else "▾", modifier = Modifier.clearAndSetSemantics {})
        Spacer(Modifier.width(6.dp))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

/** Linha de meta com suporte a Check-in, Stepper (-/+) com RoundStepButton ou Slider conforme controlStyle. */
@Composable
private fun EditGoalRow(
    goal: TodayGoalUi,
    controlStyle: ControlStyle,
    accent: Color,
    onSet: (Int) -> Unit,
    onToggleRest: () -> Unit,
) {
    val title = goal.title ?: stringResource(goalTitleRes(goal.key))
    val restLabel = stringResource(R.string.rest_day)

    var sliderValue by remember(goal.key) { mutableFloatStateOf(goal.intake.toFloat()) }
    LaunchedEffect(goal.intake) { sliderValue = goal.intake.toFloat() }
    val shownValue = snapToIncrement(sliderValue, goal.increment, goal.target)

    val progressFraction = if (goal.target > 0) (shownValue.toFloat() / goal.target.toFloat()).coerceIn(0f, 1f) else 0f
    val progressColor = Color(ProgressColors.ringArgb(progressFraction.toDouble()))
    val checkInDone = CategoryKind.isCheckInDone(goal.intake, goal.target)
    val checkInStateLabel = stringResource(if (checkInDone) R.string.today_checkin_done else R.string.today_checkin_pending)
    val valueDesc = when {
        goal.isCheckIn -> "$checkInStateLabel, ${goal.target} ${goal.unit}"
        goal.isRestDay -> stringResource(R.string.a11y_goal_restday)
        else -> stringResource(R.string.a11y_goal_value, shownValue, goal.target, goal.unit)
    }
    val headerDesc = "${stringResource(R.string.a11y_goal_label, title)}, $valueDesc"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { contentDescription = headerDesc },
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(goal.emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
                    Spacer(Modifier.width(12.dp))
                    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (goal.isCheckIn) "${goal.target} ${goal.unit}" else "$shownValue/${goal.target} ${goal.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!goal.isCheckIn && goal.supportsRestDay) {
                    Spacer(Modifier.width(10.dp))
                    EditRestDayToggle(isRestDay = goal.isRestDay, accent = accent, onClick = onToggleRest)
                }
            }

            if (goal.isCheckIn) {
                CheckInSelector(
                    title = title,
                    done = checkInDone,
                    accent = accent,
                    onSelect = { newDone ->
                        onSet(CategoryKind.checkInValue(done = newDone, goal = goal.target))
                    },
                )
            } else if (goal.isRestDay) {
                Text(
                    restLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clearAndSetSemantics {},
                )
            } else if (controlStyle == ControlStyle.Stepper) {
                // Modo Stepper: barra de progresso proporcional + botões redondos RoundStepButton (- / +).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(progressColor.copy(alpha = 0.24f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(progressColor),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val decDesc = stringResource(R.string.cd_decrease, title)
                        val incDesc = stringResource(R.string.cd_increase, title)
                        RoundStepButton(
                            symbol = "−",
                            contentDescription = decDesc,
                            onClick = { onSet((goal.intake - goal.increment).coerceAtLeast(0)) },
                            enabled = goal.intake > 0,
                            color = accent,
                            size = 40.dp,
                        )
                        RoundStepButton(
                            symbol = "+",
                            contentDescription = incDesc,
                            onClick = { onSet((goal.intake + goal.increment).coerceAtMost(goal.target)) },
                            enabled = goal.intake < goal.target,
                            color = accent,
                            size = 40.dp,
                        )
                    }
                }
            } else {
                // Modo Slider padrão.
                val maxTarget = goal.target.toFloat().coerceAtLeast(1f)
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onSet(snapToIncrement(sliderValue, goal.increment, goal.target)) },
                    valueRange = 0f..maxTarget,
                    colors = SliderDefaults.colors(
                        thumbColor = progressColor,
                        activeTrackColor = progressColor,
                        inactiveTrackColor = progressColor.copy(alpha = 0.24f),
                    ),
                )
            }
        }
    }
}

/** Botão ON/OFF de dia de descanso. */
@Composable
private fun EditRestDayToggle(isRestDay: Boolean, accent: Color, onClick: () -> Unit) {
    val label = if (isRestDay) stringResource(R.string.rest_day_off) else stringResource(R.string.rest_day_on)
    val textColor = if (isRestDay) MaterialTheme.colorScheme.onSurfaceVariant else accent
    val bgColor = if (isRestDay) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val toggleDesc = if (isRestDay) stringResource(R.string.a11y_rest_day_on) else stringResource(R.string.a11y_rest_day_off)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .semantics { role = Role.Button; contentDescription = toggleDesc },
    )
}

private fun snapToIncrement(value: Float, increment: Int, target: Int): Int {
    val safeInc = increment.coerceAtLeast(1)
    return ((value / safeInc).roundToInt() * safeInc).coerceIn(0, target)
}

@StringRes
private fun goalTitleRes(key: String): Int = when (key) {
    "tracking.workout" -> R.string.today_workout
    "tracking.cardio" -> R.string.today_cardio
    "tracking.sleep" -> R.string.today_sleep
    "tracking.water" -> R.string.today_water
    "tracking.protein" -> R.string.today_protein
    "tracking.carbs" -> R.string.today_carbs
    "tracking.goodFat" -> R.string.today_good_fat
    "tracking.fiber" -> R.string.today_fiber
    "tracking.creatine" -> R.string.today_creatine
    else -> R.string.app_name
}

