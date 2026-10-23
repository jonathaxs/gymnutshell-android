package com.jonathaxs.gymnutshell.ui.progress

import androidx.compose.ui.res.vectorResource
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.components.IconBadge
import com.jonathaxs.gymnutshell.ui.settings.TierInfoSheet
import com.jonathaxs.gymnutshell.ui.util.Breakpoints
import java.time.LocalDate

/**
 * Aba Histórico (Registro) — porte fiel da ProgressOverView 2.0 (iOS).
 * Seções: últimos 7 dias, distribuição por tier (5 níveis) com barra proporcional,
 * blocos de atividade (treino/cardio) e metas ativas (objetivo + metas rastreadas).
 * Sem pontos, sem bônus de sequência, sem dados físicos.
 */
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    onOpenTheme: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenUserGoal: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    viewModel: ProgressViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = Color(state.accentArgb)
    var showTierInfo by remember { mutableStateOf(false) }

    // Atividade → Conquistas no modo Lista; tocar num dos últimos 7 dias → Conquistas no calendário naquele dia (iOS).
    val onActivityClick = {
        viewModel.prepareAchievementsList()
        onOpenAchievements()
    }
    val onRecentDayClick: (Long) -> Unit = { day ->
        viewModel.prepareAchievementsDay(day)
        onOpenAchievements()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= Breakpoints.WideThreshold
        if (isWide) {
            Column(
                modifier = Modifier
                    .widthIn(max = 860.dp)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Últimos 7 dias ocupa a largura inteira acima das 2 colunas.
                RecentActivityCard(state, accent, onRecentDayClick)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    // Coluna esquerda: Conquistas por Níveis
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TiersCard(state, accent) { showTierInfo = true }
                    }
                    // Coluna direita: Atividade → Metas
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ActivityCard(state, onActivityClick)
                        GoalsCard(state, onOpenGoals, onOpenUserGoal)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RecentActivityCard(state, accent, onRecentDayClick)
                TiersCard(state, accent) { showTierInfo = true }
                ActivityCard(state, onActivityClick)
                GoalsCard(state, onOpenGoals, onOpenUserGoal)
            }
        }
    }

    if (showTierInfo) {
        TierInfoSheet(onDismiss = { showTierInfo = false }, onOpenTheme = onOpenTheme)
    }
}

/** Card "Últimos 7 dias": emoji do tier (ou ponto) + número do dia + inicial do dia da semana. */
@Composable
private fun RecentActivityCard(state: ProgressUiState, accent: Color, onDayClick: (Long) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.progress_section_recent),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.recentDays.forEach { day ->
                    RecentDayCell(day, accent, Modifier.weight(1f)) { onDayClick(day.epochDay) }
                }
            }
        }
    }
}

/** Uma célula da grade dos últimos 7 dias; abre Conquistas no calendário naquele dia ao tocar. */
@Composable
private fun RecentDayCell(day: RecentDayUi, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    val dateText = AppDateFormatters.longDate(LocalDate.ofEpochDay(day.epochDay))
    val desc = if (day.emoji != null) {
        stringResource(R.string.a11y_recent_day_done, dateText)
    } else {
        stringResource(R.string.a11y_recent_day_empty, dateText)
    }
    val hint = stringResource(R.string.a11y_recent_day_hint)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = hint, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = desc; role = Role.Button },
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (day.emoji != null) accent.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                )
                .then(if (day.isToday) Modifier.border(2.dp, accent, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(day.emoji ?: "·", fontSize = if (day.emoji != null) 18.sp else 14.sp)
        }
        Text(
            day.dayNumber.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (day.isToday) accent else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            day.weekday,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Card "Conquistas por Níveis": lista dos 5 tiers com barra proporcional ao maior valor; abre sheet ao tocar. */
@Composable
private fun TiersCard(state: ProgressUiState, accent: Color, onShowTierInfo: () -> Unit) {
    val maxDays = state.tierCounts.maxOfOrNull { it.days } ?: 0
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.progress_section_tiers),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    onClickLabel = stringResource(R.string.a11y_stats_card_tiers_hint),
                    role = Role.Button,
                    onClick = onShowTierInfo,
                ),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                state.tierCounts.forEachIndexed { index, tier ->
                    if (index > 0) GroupRowDivider(startInset = 74.dp)
                    TierRow(tier, maxDays, accent)
                }
            }
        }
    }
}

/** Linha de um tier: mascote em círculo suave, nome, contagem de dias e barra proporcional. */
@Composable
private fun TierRow(tier: TierCountUi, maxDays: Int, accent: Color) {
    val progressFraction = if (maxDays > 0) (tier.days.toFloat() / maxDays.toFloat()).coerceIn(0f, 1f) else 0f
    val tierName = stringResource(tier.nameRes)
    val daysText = stringResource(R.string.progress_days_count, tier.days)
    val desc = "$tierName, $daysText"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = desc },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(tier.emoji, fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics {})
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tierName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    daysText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

/** Card "Atividade nos Treinos": dois blocos lado a lado (treino e cardio). */
@Composable
private fun ActivityCard(state: ProgressUiState, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.progress_section_activity),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityTile(
                emoji = "🏋️",
                label = stringResource(R.string.progress_activity_workout),
                days = state.workoutDays,
                onClick = onClick,
                modifier = Modifier.weight(1f),
            )
            ActivityTile(
                emoji = "🏃",
                label = stringResource(R.string.progress_activity_cardio),
                days = state.cardioDays,
                onClick = onClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Bloco de atividade individual (treino ou cardio) com contagem em negrito arredondada. */
@Composable
private fun ActivityTile(
    emoji: String,
    label: String,
    days: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = stringResource(R.string.a11y_stats_card_activity_hint)
    val daysText = days.toString()
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClickLabel = hint, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "$label, $daysText" },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics {})
            Text(
                daysText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

/** Card "Metas": duas linhas navegáveis (Objetivo fitness e Metas ativas). */
@Composable
private fun GoalsCard(
    state: ProgressUiState,
    onOpenGoals: () -> Unit,
    onOpenUserGoal: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.progress_section_goals),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                ProgressNavRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_settings_flame),
                    iconColor = Color(0xFFFF9500),
                    title = stringResource(R.string.progress_section_goal),
                    value = stringResource(userGoalLabelRes(state.userGoal)),
                    onClick = onOpenUserGoal,
                    onClickHint = stringResource(R.string.a11y_stats_card_usergoal_hint),
                )
                GroupRowDivider(startInset = 64.dp)
                ProgressNavRow(
                    icon = ImageVector.vectorResource(R.drawable.ic_settings_target),
                    iconColor = Color(0xFFFF3B30),
                    title = stringResource(R.string.progress_goals_active_label),
                    value = stringResource(R.string.progress_goals_active, state.activeGoals),
                    onClick = onOpenGoals,
                    onClickHint = stringResource(R.string.a11y_stats_card_goals_hint),
                )
            }
        }
    }
}

/** Linha navegável com IconBadge, título, valor e chevron. */
@Composable
private fun ProgressNavRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    onClickHint: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = onClickHint, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(imageVector = icon, color = iconColor, size = 34.dp)
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f).widthIn(min = 8.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "›",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Rótulo do objetivo fitness. */
@StringRes
private fun userGoalLabelRes(goal: UserGoal): Int = when (goal) {
    UserGoal.Bulking -> R.string.goal_bulking
    UserGoal.Maintenance -> R.string.goal_maintenance
    UserGoal.Cutting -> R.string.goal_cutting
}
