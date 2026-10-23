package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.GoalsCalculator
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.ui.components.GroupCard
import com.jonathaxs.gymnutshell.ui.components.GroupInset
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.welcome.WelcomeCard
import kotlin.math.roundToInt

/** Cores dos objetivos fitness: laranja (bulking), destaque do usuário (manutenção) e verde (cutting). */
private val GoalBulkingColor = Color(0xFFFF9500)
private val GoalCuttingColor = Color(0xFF34C759)

/**
 * Tela para alterar o objetivo fitness (porte de UserGoalChangeView do iOS).
 * Lista de objetivos em cards, prévia proporcional "atual → nova ±X%" e dois botões de ação fixos.
 */
@Composable
fun UserGoalScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val accent by viewModel.accentColor.collectAsStateWithLifecycle()
    val overrides by viewModel.goalValueOverrides.collectAsStateWithLifecycle()

    // O objetivo salvo vazio ou não reconhecido conta como manutenção
    val savedGoal = profile.goal

    var selected by remember { mutableStateOf(savedGoal) }
    LaunchedEffect(savedGoal) {
        selected = savedGoal
    }

    val hasChanged = selected != savedGoal

    // Metas atuais (tabela base + overrides salvos do usuário)
    val base = GoalsCalculator.calculate(profile.sex, savedGoal)
    val currentGoals = base.copy(
        protein = overrides["tracking.protein"] ?: base.protein,
        carbs = overrides["tracking.carbs"] ?: base.carbs,
        goodFat = overrides["tracking.goodFat"] ?: base.goodFat,
        fiber = overrides["tracking.fiber"] ?: base.fiber,
    )

    // Metas reescaladas proporcionalmente com o novo objetivo
    val rescaledGoals = GoalsCalculator.rescale(
        current = currentGoals,
        sex = profile.sex,
        from = savedGoal,
        to = selected,
    )

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_fitness_goal),
                onBack = onBack,
            )
        },
        bottomBar = {
            // Botões de ação fixos na parte inferior
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = GroupInset, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Botão 1: Salvar e recalcular
                    Button(
                        onClick = {
                            viewModel.saveGoalWithRecalculate(selected, rescaledGoals)
                            onBack()
                        },
                        enabled = hasChanged,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent.color,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(52.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_goalchange_save_recalc),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasChanged) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }

                    // Botão 2: Salvar sem recalcular
                    OutlinedButton(
                        onClick = {
                            viewModel.saveGoalKeep(selected)
                            onBack()
                        },
                        enabled = hasChanged,
                        shape = CircleShape,
                        border = if (hasChanged) BorderStroke(1.5.dp, accent.color)
                        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(52.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_goalchange_save_keep),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (hasChanged) accent.color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 1. Seção de escolha do objetivo em cards
                Column(
                    modifier = Modifier.padding(horizontal = GroupInset),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_goalchange_section_pick),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    // Bulking (Ganho de massa)
                    GoalOptionCard(
                        title = stringResource(R.string.goal_bulking),
                        description = stringResource(R.string.welcome_goal_bulking_desc),
                        iconRes = R.drawable.ic_goal_bulking,
                        cardColor = GoalBulkingColor,
                        isSelected = selected == UserGoal.Bulking,
                        onClick = { selected = UserGoal.Bulking },
                    )

                    // Manutenção (Se manter)
                    GoalOptionCard(
                        title = stringResource(R.string.goal_maintenance),
                        description = stringResource(R.string.welcome_goal_maintenance_desc),
                        iconRes = R.drawable.ic_goal_maintenance,
                        cardColor = accent.color,
                        isSelected = selected == UserGoal.Maintenance,
                        onClick = { selected = UserGoal.Maintenance },
                    )

                    // Cutting (Emagrecimento / Definição)
                    GoalOptionCard(
                        title = stringResource(R.string.goal_cutting),
                        description = stringResource(R.string.welcome_goal_cutting_desc),
                        iconRes = R.drawable.ic_goal_cutting,
                        cardColor = GoalCuttingColor,
                        isSelected = selected == UserGoal.Cutting,
                        onClick = { selected = UserGoal.Cutting },
                    )
                }

                // 2. Prévia das metas recalculadas (aparece quando o objetivo muda)
                AnimatedVisibility(
                    visible = hasChanged,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_goalchange_section_preview),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = GroupInset + 20.dp, end = GroupInset),
                        )

                        GroupCard {
                            GoalPreviewRow(
                                label = stringResource(R.string.today_protein),
                                old = currentGoals.protein,
                                new = rescaledGoals.protein,
                                unit = " g",
                            )
                            GroupRowDivider(startInset = 16.dp)
                            GoalPreviewRow(
                                label = stringResource(R.string.today_carbs),
                                old = currentGoals.carbs,
                                new = rescaledGoals.carbs,
                                unit = " g",
                            )
                            GroupRowDivider(startInset = 16.dp)
                            GoalPreviewRow(
                                label = stringResource(R.string.today_good_fat),
                                old = currentGoals.goodFat,
                                new = rescaledGoals.goodFat,
                                unit = " g",
                            )
                            GroupRowDivider(startInset = 16.dp)
                            GoalPreviewRow(
                                label = stringResource(R.string.today_fiber),
                                old = currentGoals.fiber,
                                new = rescaledGoals.fiber,
                                unit = " g",
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Card de opção de objetivo fitness com ícone e seleção. */
@Composable
private fun GoalOptionCard(
    title: String,
    description: String,
    iconRes: Int,
    cardColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    WelcomeCard(
        isSelected = isSelected,
        accentColor = cardColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                contentDescription = null,
                tint = if (isSelected) cardColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Linha de prévia: "Proteína   150 → 165 g   +10%". */
@Composable
private fun GoalPreviewRow(
    label: String,
    old: Int,
    new: Int,
    unit: String,
) {
    val percent = if (old > 0) ((new.toDouble() - old.toDouble()) / old.toDouble() * 100.0).roundToInt() else 0
    val percentText = if (percent == 0) "0%" else if (percent > 0) "+$percent%" else "$percent%"
    val percentColor = when {
        percent > 0 -> Color(0xFF34C759)
        percent < 0 -> Color(0xFFFF9500)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f, fill = false),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$old → $new$unit",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = percentText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                color = percentColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 42.dp),
            )
        }
    }
}
