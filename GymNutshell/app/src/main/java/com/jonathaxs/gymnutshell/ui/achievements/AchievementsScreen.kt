package com.jonathaxs.gymnutshell.ui.achievements

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.util.Breakpoints
import java.time.LocalDate

/**
 * Aba Conquistas — porte fiel da AchievementsView 2.0 (iOS).
 * Top bar com sino de notificações e botão alternador de modo (Lista ↔ Calendário).
 * Mantém um único LazyColumn no modo retrato para evitar flickering.
 * Calendário mensal em card com navegação por botões redondos e título com o dia da semana por extenso.
 * Histórico com emoji em círculo suave e botão de lápis redondo pequeno para registros editáveis (janela de 30 dias).
 */
@Composable
fun AchievementsScreen(
    modifier: Modifier = Modifier,
    onOpenHistory: () -> Unit = {},
    onEditRecord: (Long) -> Unit = {},
    onBack: (() -> Unit)? = null,
    viewModel: AchievementsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = Color(state.accentArgb)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= Breakpoints.WideThreshold
        val contentMod = if (isWide) {
            Modifier
                .fillMaxWidth()
                .widthIn(max = 860.dp)
                .fillMaxHeight()
                .align(Alignment.TopCenter)
        } else {
            Modifier.fillMaxSize()
        }

        Column(contentMod) {
            // Barra superior com título, voltar/notificações e botão de alternar Lista ↔ Calendário.
            AchievementsTopBar(
                state = state,
                accent = accent,
                onOpenHistory = onOpenHistory,
                onBack = onBack,
                onToggleFilter = {
                    val next = if (state.filterMode == AchievementsFilterMode.Calendar) {
                        AchievementsFilterMode.List
                    } else {
                        AchievementsFilterMode.Calendar
                    }
                    viewModel.setFilterMode(next)
                },
            )

            if (isWide && state.filterMode == AchievementsFilterMode.Calendar) {
                // Layout wide no modo calendário: 2 colunas (calendário à esquerda, histórico à direita).
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .width(380.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CalendarCard(state, accent, viewModel)
                        Spacer(Modifier.height(24.dp))
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (state.history.isEmpty()) {
                            AchievementsEmptyCard(accent)
                        } else {
                            HistoryCard(state, accent, onEditRecord)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            } else {
                // Layout retrato (ou wide no modo Lista): um único LazyColumn contínuo.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.filterMode == AchievementsFilterMode.Calendar) {
                        item(key = "calendar_card") {
                            CalendarCard(state, accent, viewModel)
                        }
                    }
                    if (state.history.isEmpty()) {
                        item(key = "empty_state") {
                            AchievementsEmptyCard(accent)
                        }
                    } else {
                        item(key = "history_card") {
                            HistoryCard(state, accent, onEditRecord)
                        }
                    }
                }
            }
        }
    }
}

/** Barra de topo com o botão de alternar modo mostrando o destino com ícone + texto (porte do iOS). */
@Composable
private fun AchievementsTopBar(
    state: AchievementsUiState,
    accent: Color,
    onOpenHistory: () -> Unit,
    onBack: (() -> Unit)?,
    onToggleFilter: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = accent,
                    )
                }
            } else {
                // Sem sino aqui (igual ao iOS): o sino fica só na Hoje.
                Spacer(Modifier.width(12.dp))
            }
            Text(
                stringResource(R.string.achievements_header_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        // Botão de alternar exibindo o próximo modo: no Calendário mostra "Lista"; na Lista mostra "Calendário".
        val isCalendar = state.filterMode == AchievementsFilterMode.Calendar
        val targetIcon = if (isCalendar) Icons.AutoMirrored.Filled.List else Icons.Default.DateRange
        val targetTextRes = if (isCalendar) R.string.achievements_filter_list else R.string.achievements_filter_calendar

        TextButton(
            onClick = onToggleFilter,
            colors = ButtonDefaults.textButtonColors(contentColor = accent),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = targetIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(targetTextRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Card do calendário mensal com setas redondas e dia da semana por extenso como título. */
@Composable
private fun CalendarCard(
    state: AchievementsUiState,
    accent: Color,
    viewModel: AchievementsViewModel,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // Cabeçalho: dia da semana selecionado por extenso + setas redondas de mês.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.selectedWeekday,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundMonthButton(
                        symbol = "‹",
                        contentDescription = stringResource(R.string.cd_prev_month),
                        onClick = viewModel::previousMonth,
                    )
                    Text(
                        text = state.monthLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    RoundMonthButton(
                        symbol = "›",
                        contentDescription = stringResource(R.string.cd_next_month),
                        onClick = viewModel::nextMonth,
                    )
                }
            }

            // Grade de dias da semana e células do mês.
            CalendarGrid(state = state, accent = accent, onSelect = viewModel::selectDay)
        }
    }
}

/** Botão redondo de navegação de mês (28dp com touch target de 48dp). */
@Composable
private fun RoundMonthButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/** Grade do calendário: cabeçalho com dias da semana + células dos dias. */
@Composable
private fun CalendarGrid(
    state: AchievementsUiState,
    accent: Color,
    onSelect: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Cabeçalho dos dias da semana (D S T Q Q S S), decorativo.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.weekdays.forEach { symbol ->
                Text(
                    text = symbol,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Células do mês em linhas de 7 dias.
        state.days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                week.forEach { day ->
                    DayCell(day = day, accent = accent, modifier = Modifier.weight(1f)) {
                        onSelect(day.epochDay)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Célula de um dia: número + cápsula no dia de hoje + emoji do tier. */
@Composable
private fun DayCell(
    day: CalendarDayUi,
    accent: Color,
    modifier: Modifier,
    onSelect: () -> Unit,
) {
    val contentColor = when {
        day.isSelected -> Color.White
        day.inMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    val dateText = AppDateFormatters.longDate(LocalDate.ofEpochDay(day.epochDay))
    val p = day.percent
    val base = if (p != null && p > 0) {
        stringResource(R.string.a11y_calendar_day, dateText, p)
    } else {
        stringResource(R.string.a11y_calendar_day_empty, dateText)
    }
    val dayDesc = if (day.isSelected) "$base, ${stringResource(R.string.a11y_selected)}" else base

    Column(
        modifier = modifier
            .height(46.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (day.isSelected) accent else Color.Transparent)
            .clickable(enabled = day.inMonth, onClick = onSelect)
            .semantics { contentDescription = dayDesc; role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Text(
                text = "${day.dayNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier.alpha(if (day.inMonth || day.isSelected) 1f else 0.45f),
            )
            // Traço sutil em cápsula embaixo do número para hoje (branco quando selecionado, accent quando não).
            if (day.isToday) {
                Box(
                    modifier = Modifier
                        .offset(y = 4.dp)
                        .size(width = 16.dp, height = 2.5.dp)
                        .clip(CircleShape)
                        .background(if (day.isSelected) Color.White else accent),
                )
            }
        }
        if (day.emoji != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = day.emoji,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(if (day.inMonth || day.isSelected) 1f else 0.45f),
            )
        }
    }
}

/** Card com as linhas de histórico do dia ou de todos os dias. */
@Composable
private fun HistoryCard(
    state: AchievementsUiState,
    accent: Color,
    onEditRecord: (Long) -> Unit,
) {
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
            state.history.forEachIndexed { index, item ->
                if (index > 0) GroupRowDivider(startInset = 68.dp)
                HistoryRow(
                    item = item,
                    accent = accent,
                    onEdit = if (item.canEdit) ({ onEditRecord(item.epochDay) }) else null,
                )
            }
        }
    }
}

/** Linha do histórico: emoji em círculo suave (40dp), data + percentual e botão redondo de editar (lápis). Sem pontos. */
@Composable
private fun HistoryRow(
    item: HistoryItemUi,
    accent: Color,
    onEdit: (() -> Unit)?,
) {
    val editDesc = stringResource(R.string.cd_edit_record)
    val tierName = stringResource(item.tierNameRes)
    val rowDesc = stringResource(R.string.a11y_history_row, item.dateLabel, item.percent)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Círculo suave com emoji (40dp)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.emoji,
                fontSize = 20.sp,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        Spacer(Modifier.width(14.dp))

        // Data e nível/percentual
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { contentDescription = rowDesc },
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = item.dateLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$tierName · ${item.percent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Botão de editar redondo pequeno com lápis (área de toque acessível de 48dp)
        if (onEdit != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClickLabel = editDesc, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Card de estado vazio: troféu no destaque + título + descrição. */
@Composable
private fun AchievementsEmptyCard(accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 44.dp, horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_settings_trophy),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.achievements_empty),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

