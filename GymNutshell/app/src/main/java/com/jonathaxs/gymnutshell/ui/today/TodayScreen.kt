package com.jonathaxs.gymnutshell.ui.today

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.core.domain.ProgressColors
import com.jonathaxs.gymnutshell.core.domain.TodayGoalsViewMode
import com.jonathaxs.gymnutshell.ui.components.categoryIcon
import com.jonathaxs.gymnutshell.ui.settings.ProgressRingInfoSheet
import com.jonathaxs.gymnutshell.ui.settings.TierInfoSheet
import com.jonathaxs.gymnutshell.ui.util.Breakpoints
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton
import kotlin.math.roundToInt

/** Largura máxima do conteúdo da Today no celular em retrato (parity com os 560pt do iOS). */
private val TodayPortraitMaxWidth = 560.dp

/** Cor verde padrão para itens concluídos (iOS parity). */
private val DoneGreenColor = Color(0xFF34C759)

/**
 * Tela principal "Hoje" — versão 2.0 (porte da TodayView do iOS).
 * Cabeçalho fixo no topo (sino, data centralizada, engrenagem) e conteúdo rolável com:
 * hero em card (anel de 5 segmentos + conquista do dia + frase de próximo nível),
 * botões de página (lista/grade, Histórico, troféu de Conquistas), lista de metas por
 * categoria em cards agrupados com divisores, e atalhos de Ajustes no rodapé.
 */
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onOpenTheme: () -> Unit = {},
    onOpenColors: () -> Unit = {},
    onOpenControls: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    viewModel: TodayViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-checa os treinos do Health Connect ao voltar pra tela (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var isFirstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isFirstResume) isFirstResume = false else viewModel.refreshWorkoutsFromHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Sheets de informação do anel e dos níveis (abertas pelo toque no hero)
    var showRingInfo by remember { mutableStateOf(false) }
    var showTierInfo by remember { mutableStateOf(false) }

    val onRingClick = { showRingInfo = true }
    val onTierClick = { showTierInfo = true }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val isWide = maxWidth >= Breakpoints.WideThreshold
        val isTall = maxHeight >= Breakpoints.TallThreshold
        when {
            !isWide -> TodayPortrait(
                state = state,
                accent = accent,
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = onOpenSettings,
                onOpenHistory = onOpenHistory,
                onOpenAchievements = onOpenAchievements,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                onOpenTheme = onOpenTheme,
                onOpenColors = onOpenColors,
                onOpenControls = onOpenControls,
                onOpenGoals = onOpenGoals,
                viewModel = viewModel,
            )
            isTall -> TodayWideGrid(
                state = state,
                accent = accent,
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = onOpenSettings,
                onOpenHistory = onOpenHistory,
                onOpenAchievements = onOpenAchievements,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                viewModel = viewModel,
            )
            else -> TodayWideColumn(
                state = state,
                accent = accent,
                onOpenNotifications = onOpenNotifications,
                onOpenSettings = onOpenSettings,
                onOpenHistory = onOpenHistory,
                onOpenAchievements = onOpenAchievements,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                viewModel = viewModel,
            )
        }
    }

    // Sheets informativas
    if (showRingInfo) {
        ProgressRingInfoSheet(onDismiss = { showRingInfo = false }, currentPercent = state.overallPercent)
    }
    if (showTierInfo) {
        TierInfoSheet(
            onDismiss = { showTierInfo = false },
            onOpenTheme = onOpenTheme,
            currentPercent = state.overallPercent,
        )
    }
}

/** Layout em retrato (celular): topo com sino/data/engrenagem + corpo rolável. */
@Composable
private fun TodayPortrait(
    state: TodayUiState,
    accent: Color,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    onRingClick: () -> Unit,
    onTierClick: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenGoals: () -> Unit,
    viewModel: TodayViewModel,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // Barra do topo: sino à esquerda, data no meio e engrenagem à direita
        TodayTopBar(
            dateLabel = state.dateLabel,
            accent = accent,
            onOpenNotifications = onOpenNotifications,
            onOpenSettings = onOpenSettings,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )

        // Conteúdo rolável: hero -> (grade em modo grid) -> botões -> (lista em modo list) -> atalhos
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .animateContentSize(animationSpec = tween(durationMillis = 300)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero em card: anel de 5 segmentos + conquista do dia + frase de próximo nível
            TodayHeroCard(
                state = state,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                modifier = Modifier
                    .widthIn(max = TodayPortraitMaxWidth)
                    .fillMaxWidth(),
            )

            // Modo grade: anel -> grade -> botões -> atalhos
            AnimatedVisibility(
                visible = state.goalsViewMode == TodayGoalsViewMode.Grid,
                enter = fadeIn(tween(250)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
                modifier = Modifier
                    .widthIn(max = TodayPortraitMaxWidth)
                    .fillMaxWidth(),
            ) {
                TodayCategoryGrid(
                    sections = state.sections,
                    uncategorizedGoals = state.uncategorizedGoals,
                    controlStyle = state.controlStyle,
                    accent = accent,
                    onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
                    onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Botões de página: [alternar lista/grade] [Histórico] [troféu Conquistas]
            TodayPageButtons(
                goalsViewMode = state.goalsViewMode,
                accent = accent,
                onToggleViewMode = { viewModel.toggleGoalsViewMode() },
                onOpenHistory = onOpenHistory,
                onOpenAchievements = onOpenAchievements,
                modifier = Modifier
                    .widthIn(max = TodayPortraitMaxWidth)
                    .fillMaxWidth(),
            )

            // Modo lista: anel -> botões -> lista -> atalhos
            AnimatedVisibility(
                visible = state.goalsViewMode == TodayGoalsViewMode.List,
                enter = fadeIn(tween(250)) + expandVertically(tween(300)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
                modifier = Modifier
                    .widthIn(max = TodayPortraitMaxWidth)
                    .fillMaxWidth(),
            ) {
                TodayGoalsListContent(
                    state = state,
                    accent = accent,
                    onToggleCategory = { viewModel.toggleCategory(it) },
                    onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
                    onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Atalhos rápidos no rodapé: Tema, Cores, Controles e Editar metas
            TodayEditShortcuts(
                accent = accent,
                onOpenTheme = onOpenTheme,
                onOpenColors = onOpenColors,
                onOpenControls = onOpenControls,
                onOpenGoals = onOpenGoals,
                modifier = Modifier
                    .widthIn(max = TodayPortraitMaxWidth)
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

/** Barra superior: sino à esquerda, data no centro e engrenagem à direita (porte do AppTabHeader). */
@Composable
private fun TodayTopBar(
    dateLabel: String,
    accent: Color,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.today_hero_bell_a11y_label),
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }

        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.tab_settings),
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Hero em Card: anel centralizado com a porcentagem do dia e, a partir do 1º registro, o
 * painel de Conquista ao lado; abaixo, a frase de próximo nível (porte de TodayHeroView).
 */
@Composable
private fun TodayHeroCard(
    state: TodayUiState,
    onRingClick: () -> Unit,
    onTierClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalLayout: Boolean = false,
) {
    val hasStarted = state.overallPercent > 0
    val ringSize = if (verticalLayout) 170.dp else 150.dp

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (verticalLayout) {
                // Tablet alto: anel em cima, conquista embaixo
                TodayProgressRing(
                    progress = state.overallProgress,
                    percent = state.overallPercent,
                    onClick = onRingClick,
                    ringSize = ringSize,
                )
                if (hasStarted) {
                    TodayAchievementPanel(
                        tierEmoji = state.tierEmoji,
                        tierNameRes = state.tierNameRes,
                        onClick = onTierClick,
                        height = ringSize,
                    )
                }
            } else {
                // Celular retrato/landscape: anel à esquerda, conquista ao lado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TodayProgressRing(
                        progress = state.overallProgress,
                        percent = state.overallPercent,
                        onClick = onRingClick,
                        ringSize = ringSize,
                    )
                    if (hasStarted) {
                        Spacer(Modifier.width(28.dp))
                        TodayAchievementPanel(
                            tierEmoji = state.tierEmoji,
                            tierNameRes = state.tierNameRes,
                            onClick = onTierClick,
                            height = ringSize,
                        )
                    }
                }
            }

            // Frase abaixo: antes de começar ou quanto falta pro próximo nível
            TodayHeroCaption(
                hasStarted = hasStarted,
                nextLevelPercent = state.nextLevelPercent,
                isAtMaxLevel = state.isAtMaxLevel,
                onClick = onTierClick,
            )
        }
    }
}

/** Painel de conquista ao lado do anel: rótulo "Conquista do dia", emoji grande e nome do nível. */
@Composable
private fun TodayAchievementPanel(
    tierEmoji: String,
    @StringRes tierNameRes: Int,
    onClick: () -> Unit,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val tierName = stringResource(tierNameRes)
    val a11yDesc = stringResource(R.string.cd_daily_tier, tierName)

    Column(
        modifier = modifier
            .width(130.dp)
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = a11yDesc
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.today_hero_achievement_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tierEmoji,
            fontSize = 62.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tierName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Frase abaixo do anel e da conquista. Toque abre os detalhes dos níveis. */
@Composable
private fun TodayHeroCaption(
    hasStarted: Boolean,
    nextLevelPercent: Int?,
    isAtMaxLevel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val captionText = when {
        !hasStarted -> stringResource(R.string.today_hero_caption_start)
        nextLevelPercent != null -> stringResource(R.string.today_hero_caption_next_level, nextLevelPercent)
        isAtMaxLevel -> stringResource(R.string.today_hero_caption_max)
        else -> stringResource(R.string.today_hero_caption_max)
    }

    Text(
        text = captionText,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

/**
 * Botões de página da Today: alternar lista/grade (só ícone), Histórico (ícone + texto) e troféu
 * de Conquistas (só ícone). Porte do TodayPageButtons do iOS.
 */
@Composable
private fun TodayPageButtons(
    goalsViewMode: TodayGoalsViewMode,
    accent: Color,
    onToggleViewMode: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isList = goalsViewMode == TodayGoalsViewMode.List
    val nextModeName = stringResource(
        if (isList) R.string.today_goals_view_mode_grid else R.string.today_goals_view_mode_list,
    )
    val viewModeDesc = stringResource(R.string.today_goals_view_mode_switch_a11y, nextModeName)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Botão só ícone: lista / grade (mostra o ícone do destino)
        OutlinedIconButton(
            onClick = onToggleViewMode,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                painter = painterResource(if (isList) R.drawable.ic_view_grid else R.drawable.ic_view_list),
                contentDescription = viewModeDesc,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        // Botão Histórico: ícone + texto
        OutlinedButton(
            onClick = onOpenHistory,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.today_history),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Botão Conquistas: só o troféu
        OutlinedIconButton(
            onClick = onOpenAchievements,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_trophy),
                contentDescription = stringResource(R.string.achievements_header_title),
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Conteúdo das seções de metas em modo lista: cada categoria com cabeçalho + card agrupado de metas. */
@Composable
private fun TodayGoalsListContent(
    state: TodayUiState,
    accent: Color,
    onToggleCategory: (String) -> Unit,
    onSetIntake: (TodayGoalUi, Int) -> Unit,
    onToggleRest: (TodayGoalUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Categorias principais
        state.sections.forEach { section ->
            TodayCategorySectionView(
                section = section,
                controlStyle = state.controlStyle,
                accent = accent,
                onToggle = { onToggleCategory(section.id) },
                onSetIntake = onSetIntake,
                onToggleRest = onToggleRest,
            )
        }

        // Metas personalizadas sem categoria ("Outras")
        if (state.uncategorizedGoals.isNotEmpty()) {
            val otherTitle = stringResource(R.string.today_category_other)
            val completedOther = state.uncategorizedGoals.count { it.progress >= 1.0 }
            val totalOther = state.uncategorizedGoals.size

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TodayCategoryHeaderView(
                    title = otherTitle,
                    symbolName = "square.grid.2x2.fill",
                    completedCount = completedOther,
                    totalCount = totalOther,
                    collapsed = false,
                    accentColor = accent,
                    onToggle = {},
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        state.uncategorizedGoals.forEachIndexed { index, goal ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(start = 52.dp),
                                )
                            }
                            GoalRow(
                                goal = goal,
                                controlStyle = state.controlStyle,
                                accent = accent,
                                onSet = { onSetIntake(goal, it) },
                                onToggleRest = { onToggleRest(goal) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Seção de uma categoria: cabeçalho + card agrupado com as metas e divisores. */
@Composable
private fun TodayCategorySectionView(
    section: TodayCategoryUi,
    controlStyle: ControlStyle,
    accent: Color,
    onToggle: () -> Unit,
    onSetIntake: (TodayGoalUi, Int) -> Unit,
    onToggleRest: (TodayGoalUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = section.titleRes?.let { stringResource(it) } ?: section.title.orEmpty()
    val completedCount = section.goals.count { it.progress >= 1.0 }
    val totalCount = section.goals.size

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Cabeçalho da categoria: ícone, título, contagem e chevron
        TodayCategoryHeaderView(
            title = title,
            symbolName = section.symbolName,
            completedCount = completedCount,
            totalCount = totalCount,
            collapsed = section.collapsed,
            accentColor = accent,
            onToggle = onToggle,
        )

        // Card agrupado contendo as linhas de meta da categoria
        AnimatedVisibility(
            visible = !section.collapsed,
            enter = fadeIn(tween(220)) +
                expandVertically(tween(220), expandFrom = Alignment.Top) +
                scaleIn(tween(220), initialScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 0f)),
            exit = fadeOut(tween(220)) +
                shrinkVertically(tween(220), shrinkTowards = Alignment.Top) +
                scaleOut(tween(220), targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 0f)),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    section.goals.forEachIndexed { index, goal ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(start = 52.dp),
                            )
                        }
                        GoalRow(
                            goal = goal,
                            controlStyle = controlStyle,
                            accent = accent,
                            onSet = { onSetIntake(goal, it) },
                            onToggleRest = { onToggleRest(goal) },
                        )
                    }
                }
            }
        }
    }
}

/** Cabeçalho de categoria estilo iOS: ícone na cor de destaque + nome + contagem x/y + chevron. */
@Composable
private fun TodayCategoryHeaderView(
    title: String,
    symbolName: String?,
    completedCount: Int,
    totalCount: Int,
    collapsed: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDone = totalCount > 0 && completedCount == totalCount
    val categoryLabel = stringResource(R.string.a11y_category_label, title)
    val a11yValue = stringResource(R.string.today_category_a11y_value, completedCount, totalCount)
    val stateDesc = stringResource(if (collapsed) R.string.state_collapsed else R.string.state_expanded)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$categoryLabel, $a11yValue"
                stateDescription = stateDesc
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = categoryIcon(symbolName),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$completedCount/$totalCount",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isDone) DoneGreenColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (collapsed) "▸" else "▾",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

/**
 * Linha de meta (porte de TrackingGoalRowView do iOS):
 * - Cabeçalho: emoji + título + valor/meta. No modo Slider, o ON/OFF pequeno fica aqui.
 * - Controle:
 *   - No modo Stepper: barra de progresso à esquerda + botões -/+ redondos juntos à direita (+ ON/OFF redondo de 48dp).
 *   - No modo Slider: Slider padrão preenchido com a cor de progresso.
 *   - Em dia de descanso: o texto centralizado "Dia de descanso" substitui o controle sem mudar a altura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GoalRow(
    goal: TodayGoalUi,
    controlStyle: ControlStyle = ControlStyle.Default,
    accent: Color = MaterialTheme.colorScheme.primary,
    onSet: (Int) -> Unit = {},
    onToggleRest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = goal.title ?: stringResource(titleRes(goal.key))
    val restLabel = stringResource(R.string.rest_day)

    var sliderValue by remember(goal.key) { mutableFloatStateOf(goal.intake.toFloat()) }
    LaunchedEffect(goal.intake) { sliderValue = goal.intake.toFloat() }
    val shownValue = snapToIncrement(sliderValue, goal.increment, goal.target)

    val progressFraction = if (goal.target > 0) (shownValue.toFloat() / goal.target).coerceIn(0f, 1f) else 0f
    val progressColor = Color(ProgressColors.ringArgb(progressFraction.toDouble()))

    val valueDesc = if (goal.isRestDay) {
        stringResource(R.string.a11y_goal_restday)
    } else {
        stringResource(R.string.a11y_goal_value, shownValue, goal.target, goal.unit)
    }
    val headerDesc = "${stringResource(R.string.a11y_goal_label, title)}, $valueDesc"
    val sliderLabel = stringResource(R.string.a11y_slider_label, title)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Linha superior: emoji + título + valor numérico + ON/OFF (se no modo slider)
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
                Text(
                    text = goal.emoji,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clearAndSetSemantics {},
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$shownValue/${goal.target} ${goal.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // No modo Slider, o toggle ON/OFF pequeno fica à direita no cabeçalho
            if (goal.supportsRestDay && controlStyle == ControlStyle.Slider) {
                Spacer(Modifier.width(8.dp))
                RestDayTogglePill(
                    isRestDay = goal.isRestDay,
                    accent = accent,
                    onClick = onToggleRest,
                )
            }
        }

        // Linha de controle: Slider ou Barra com Steppers -/+
        if (goal.isRestDay) {
            // Em descanso: exibe texto centralizado e, se no modo stepper, o botão ON/OFF redondo à direita
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = restLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clearAndSetSemantics {},
                )
                if (goal.supportsRestDay && controlStyle == ControlStyle.Stepper) {
                    RoundRestDayButton(
                        isRestDay = true,
                        accent = accent,
                        onClick = onToggleRest,
                    )
                }
            }
        } else {
            when (controlStyle) {
                ControlStyle.Slider -> {
                    val maxTarget = goal.target.toFloat().coerceAtLeast(1f)
                    Slider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .systemGestureExclusion()
                            .semantics {
                                contentDescription = sliderLabel
                                stateDescription = valueDesc
                            },
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            onSet(snapToIncrement(sliderValue, goal.increment, goal.target))
                        },
                        valueRange = 0f..maxTarget,
                        colors = SliderDefaults.colors(
                            thumbColor = progressColor,
                            activeTrackColor = progressColor,
                            inactiveTrackColor = progressColor.copy(alpha = 0.24f),
                        ),
                        track = {
                            val fraction = (sliderValue / maxTarget).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(progressColor.copy(alpha = 0.24f)),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(fraction)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(progressColor),
                                )
                            }
                        },
                        thumb = {
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(progressColor, CircleShape),
                            )
                        },
                    )
                }
                ControlStyle.Stepper -> {
                    // Modo Stepper: barra de progresso à esquerda + botões -/+ e toggle ON/OFF redondo à direita
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = progressColor,
                            trackColor = progressColor.copy(alpha = 0.22f),
                            strokeCap = StrokeCap.Round,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RoundStepButton(
                                symbol = "−",
                                contentDescription = stringResource(R.string.cd_decrease, title),
                                onClick = {
                                    val next = (shownValue - goal.increment).coerceAtLeast(0)
                                    sliderValue = next.toFloat()
                                    onSet(next)
                                },
                                size = 44.dp,
                                color = accent,
                                enabled = shownValue > 0,
                            )
                            RoundStepButton(
                                symbol = "+",
                                contentDescription = stringResource(R.string.cd_increase, title),
                                onClick = {
                                    val next = (shownValue + goal.increment).coerceAtMost(goal.target)
                                    sliderValue = next.toFloat()
                                    onSet(next)
                                },
                                size = 44.dp,
                                color = accent,
                                enabled = shownValue < goal.target,
                            )
                            if (goal.supportsRestDay) {
                                RoundRestDayButton(
                                    isRestDay = false,
                                    accent = accent,
                                    onClick = onToggleRest,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Botão pílula ON/OFF de descanso para a linha no modo Slider (porte do restDayToggle do iOS). */
@Composable
private fun RestDayTogglePill(
    isRestDay: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isRestDay) stringResource(R.string.rest_day_off) else stringResource(R.string.rest_day_on)
    val textColor = if (isRestDay) MaterialTheme.colorScheme.onSurfaceVariant else accent
    val bgColor = if (isRestDay) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val toggleDesc = if (isRestDay) stringResource(R.string.a11y_rest_day_on) else stringResource(R.string.a11y_rest_day_off)

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = textColor,
        textAlign = TextAlign.Center,
        modifier = modifier
            .width(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(vertical = 5.dp)
            .semantics {
                role = Role.Button
                contentDescription = toggleDesc
            },
    )
}

/** Botão redondo ON/OFF de descanso para a linha no modo Stepper (porte do roundRestDayToggle do iOS). */
@Composable
private fun RoundRestDayButton(
    isRestDay: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isRestDay) stringResource(R.string.rest_day_off) else stringResource(R.string.rest_day_on)
    val bgColor = if (isRestDay) accent else accent.copy(alpha = 0.12f)
    val textColor = if (isRestDay) Color.White else accent
    val toggleDesc = if (isRestDay) stringResource(R.string.a11y_rest_day_on) else stringResource(R.string.a11y_rest_day_off)

    Box(
        modifier = modifier
            .size(48.dp)
            .semantics {
                this.role = Role.Button
                this.contentDescription = toggleDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Atalhos no rodapé da Today: botões circulares Tema, Cores, Controles e pílula Editar metas. */
@Composable
private fun TodayEditShortcuts(
    accent: Color,
    onOpenTheme: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Tema
        OutlinedIconButton(
            onClick = onOpenTheme,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = stringResource(R.string.today_edit_theme),
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        // Cores
        OutlinedIconButton(
            onClick = onOpenColors,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_palette),
                contentDescription = stringResource(R.string.today_edit_colors),
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        // Controles
        OutlinedIconButton(
            onClick = onOpenControls,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_controls),
                contentDescription = stringResource(R.string.today_edit_controls),
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }

        // Editar metas
        OutlinedButton(
            onClick = onOpenGoals,
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        ) {
            Text(
                text = stringResource(R.string.today_edit_goals),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

/** Wide + alto (tablet): hero empilhado numa coluna fixa à esquerda + metas em grid à direita. */
@Composable
private fun TodayWideGrid(
    state: TodayUiState,
    accent: Color,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    onRingClick: () -> Unit,
    onTierClick: () -> Unit,
    viewModel: TodayViewModel,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            TodayHeroCard(
                state = state,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                verticalLayout = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(24.dp))
        TodayGoalsGrid(
            cells = todayGridCells(state, accent, viewModel),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

/** Wide + baixo (celular landscape): hero horizontal à esquerda + metas em coluna única à direita. */
@Composable
private fun TodayWideColumn(
    state: TodayUiState,
    accent: Color,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    onRingClick: () -> Unit,
    onTierClick: () -> Unit,
    viewModel: TodayViewModel,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            TodayHeroCard(
                state = state,
                onRingClick = onRingClick,
                onTierClick = onTierClick,
                verticalLayout = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        TodayGoalsListContent(
            state = state,
            accent = accent,
            onToggleCategory = { viewModel.toggleCategory(it) },
            onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
            onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Monta as células do grid wide: uma por categoria + uma final pras metas sem categoria. */
private fun todayGridCells(
    state: TodayUiState,
    accent: Color,
    viewModel: TodayViewModel,
): List<TodayGridCell> = buildList {
    state.sections.forEach { section ->
        add(
            TodayGridCell(section.id) {
                TodayCategorySectionView(
                    section = section,
                    controlStyle = state.controlStyle,
                    accent = accent,
                    onToggle = { viewModel.toggleCategory(section.id) },
                    onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
                    onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
                )
            },
        )
    }
    if (state.uncategorizedGoals.isNotEmpty()) {
        add(
            TodayGridCell("__uncategorized__") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        state.uncategorizedGoals.forEachIndexed { index, goal ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(start = 52.dp),
                                )
                            }
                            GoalRow(
                                goal = goal,
                                controlStyle = state.controlStyle,
                                accent = accent,
                                onSet = { viewModel.updateIntake(goal, it) },
                                onToggleRest = { viewModel.toggleRestDay(goal) },
                            )
                        }
                    }
                }
            },
        )
    }
}

/** Arredonda o valor do slider pro múltiplo de increment mais próximo, dentro de [0, target]. */
private fun snapToIncrement(value: Float, increment: Int, target: Int): Int {
    val step = increment.coerceAtLeast(1)
    val snapped = (value / step).roundToInt() * step
    return snapped.coerceIn(0, target)
}

/** Mapeia a categoria pro título localizado. `internal` pra reúso na EditRecordScreen e ViewModel. */
@StringRes
internal fun categoryTitleRes(category: GoalCategory): Int = when (category) {
    GoalCategory.Essencial -> R.string.category_essencial
    GoalCategory.Nutricao -> R.string.category_nutricao
    GoalCategory.Treino -> R.string.category_treino
    GoalCategory.Suplemento -> R.string.category_suplemento
}

/** Mapeia a chave da meta pro título localizado (strings.xml). */
@StringRes
private fun titleRes(key: String): Int = when (key) {
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
