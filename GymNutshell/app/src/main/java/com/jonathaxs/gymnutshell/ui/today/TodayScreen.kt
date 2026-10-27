package com.jonathaxs.gymnutshell.ui.today

import com.jonathaxs.gymnutshell.ui.util.rememberIsTablet
import com.jonathaxs.gymnutshell.ui.util.rememberFoldInfo
import com.jonathaxs.gymnutshell.ui.util.FoldInfo
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.animateBounds
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.BoundsTransform
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.heading
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
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
import androidx.compose.ui.semantics.selected
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
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
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

    // Dobra (Pixel Fold e afins) e onde a Hoje está na janela, pra levar a faixa da dobra pras coordenadas locais.
    val foldInfo = rememberFoldInfo()
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { boxOrigin = it.positionInWindow() }) {
        // Largo a partir de 700 dp, ou já a partir de 560 dp em tablet/dobrável aberto (igual à regra do iOS).
        val isWide = maxWidth >= Breakpoints.WideThreshold || (maxWidth >= 560.dp && rememberIsTablet())
        when {
            !isWide -> TodayPortrait(
                state = state,
                accent = accent,
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
            else -> TodayWide(
                state = state,
                accent = accent,
                width = maxWidth,
                height = maxHeight,
                fold = foldBand(foldInfo, boxOrigin, density),
                onOpenTheme = onOpenTheme,
                onOpenColors = onOpenColors,
                onOpenControls = onOpenControls,
                onOpenGoals = onOpenGoals,
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
        // Topo fixo só com a data: os Ajustes ficam na linha de botões abaixo do anel.
        TodayTopBar(
            dateLabel = state.dateLabel,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )

        // Conteúdo rolável: hero -> (grade em modo grid) -> botões -> (lista em modo list) -> atalhos
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                // Margem maior que a padrão: metas e botões mais estreitos, na mesma largura (iOS).
                .padding(horizontal = 32.dp, vertical = 8.dp)
                .animateContentSize(animationSpec = tween(durationMillis = 300)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Hero: anel de 5 segmentos + conquista do dia + frase de próximo nível
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

            // Botões de página: Ajustes, Registros, Conquistas e troca lista/grade
            TodayPageButtons(
                goalsViewMode = state.goalsViewMode,
                accent = accent,
                onOpenSettings = onOpenSettings,
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

            // Edição rápida no rodapé: um botão só, com o balão de Metas, Controles, Emojis e Cores
            TodayQuickEditButton(
                accent = accent,
                onOpenTheme = onOpenTheme,
                onOpenColors = onOpenColors,
                onOpenControls = onOpenControls,
                onOpenGoals = onOpenGoals,
                modifier = Modifier.padding(
                    top = if (state.goalsViewMode == TodayGoalsViewMode.List) 12.dp else 0.dp,
                    bottom = 24.dp,
                ),
            )
        }
    }
}

/** Topo da Hoje: só a data, centralizada (porte do AppTabHeader sem botões). */
@Composable
private fun TodayTopBar(
    dateLabel: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = dateLabel,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Hero: anel: anel centralizado com a porcentagem do dia e, a partir do 1º registro, o
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

    // Sem card desde a 2.0 (como no iOS): o hero fica direto no fundo da tela.
    Column(
        modifier = modifier.padding(vertical = if (verticalLayout) 8.dp else 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        run {
            if (verticalLayout) {
                // Tablet alto: anel em cima, conquista embaixo
                TodayProgressRing(
                    progress = state.overallProgress,
                    percent = state.overallPercent,
                    onClick = onRingClick,
                    ringSize = ringSize,
                    celebratesAfterOnboarding = true,
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
                        celebratesAfterOnboarding = true,
                    )
                    if (hasStarted) {
                        // Espaço curto: o emoji fica perto do anel (iOS: 8).
                        Spacer(Modifier.width(8.dp))
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

/** Painel de conquista ao lado do anel: emoji grande e nome do nível, da altura do anel. */
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
            // Largura só pro nome caber; mais larga, sobra vão e o emoji se afasta do anel.
            .width(116.dp)
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
            text = tierEmoji,
            fontSize = (height.value * 0.44f).sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tierName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
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
 * Botões de página da Today, todos redondos e só com ícone, na mesma ordem da coluna do iPad:
 * Ajustes, Registros, Conquistas e troca lista/grade. Porte do TodayPageButtons do iOS.
 */
@Composable
private fun TodayPageButtons(
    goalsViewMode: TodayGoalsViewMode,
    accent: Color,
    onOpenSettings: () -> Unit,
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
        TodayRoundButton(accent, onOpenSettings) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.tab_settings), tint = accent, modifier = Modifier.size(22.dp))
        }
        TodayRoundButton(accent, onOpenHistory) {
            Icon(painterResource(R.drawable.ic_records), stringResource(R.string.today_history), tint = accent, modifier = Modifier.size(22.dp))
        }
        TodayRoundButton(accent, onOpenAchievements) {
            Icon(painterResource(R.drawable.ic_settings_trophy), stringResource(R.string.achievements_header_title), tint = accent, modifier = Modifier.size(22.dp))
        }
        // Mostra o ícone do modo pra onde vai: em lista, o de grade; em grade, o de lista.
        TodayRoundButton(accent, onToggleViewMode) {
            Icon(
                painter = painterResource(if (isList) R.drawable.ic_view_grid else R.drawable.ic_view_list),
                contentDescription = viewModeDesc,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Botão redondo com contorno na cor de destaque (linha de botões da Hoje). */
@Composable
private fun TodayRoundButton(accent: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = accent.copy(alpha = 0.08f)),
        modifier = Modifier.size(48.dp),
        content = content,
    )
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
        // Depois do "Vamos lá" da Welcome as categorias entram vindo de baixo, uma por vez (iOS: 140pt, mola,
        // 0,12s entre elas, 0,7s depois de abrir, pra esperar a troca de tela).
        val playEntrance = remember { GoalsEntrance.consume() }
        val density = LocalDensity.current

        // Categorias principais
        state.sections.forEachIndexed { index, section ->
            val offset = remember { Animatable(if (playEntrance) 1f else 0f) }
            if (playEntrance) {
                LaunchedEffect(Unit) {
                    delay(700L + index * 120L)
                    offset.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 120f))
                }
            }
            TodayCategorySectionView(
                modifier = Modifier.graphicsLayer {
                    translationY = offset.value * with(density) { 140.dp.toPx() }
                    alpha = 1f - offset.value
                },
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
    val checkInDone = CategoryKind.isCheckInDone(goal.intake, goal.target)
    val checkInStateLabel = stringResource(if (checkInDone) R.string.today_checkin_done else R.string.today_checkin_pending)

    val valueDesc = when {
        goal.isCheckIn -> "$checkInStateLabel, ${goal.target} ${goal.unit}"
        goal.isRestDay -> stringResource(R.string.a11y_goal_restday)
        else -> stringResource(R.string.a11y_goal_value, shownValue, goal.target, goal.unit)
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
                    text = if (goal.isCheckIn) "${goal.target} ${goal.unit}" else "$shownValue/${goal.target} ${goal.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // No modo Slider, o toggle ON/OFF pequeno fica à direita no cabeçalho
            if (!goal.isCheckIn && goal.supportsRestDay && controlStyle == ControlStyle.Slider) {
                Spacer(Modifier.width(8.dp))
                RestDayTogglePill(
                    isRestDay = goal.isRestDay,
                    accent = accent,
                    onClick = onToggleRest,
                )
            }
        }

        // Linha de controle: seletor Check-in, Slider ou Barra com Steppers -/+
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

/**
 * Seletor Pendente | Feito em cápsula com pílula deslizante — porte do checkInControl (iOS).
 * Usado nas metas de categoria CheckIn tanto na Hoje quanto na edição de registro.
 */
@Composable
internal fun CheckInSelector(
    title: String,
    done: Boolean,
    accent: Color,
    onSelect: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val pendingText = stringResource(R.string.today_checkin_pending)
    val doneText = stringResource(R.string.today_checkin_done)
    val pillColor by animateColorAsState(
        targetValue = if (done) accent else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "checkin_pill_color",
    )
    val trackPadding = 3.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(trackPadding)
            .semantics { contentDescription = title },
    ) {
        val segmentWidth = maxWidth / 2
        val pillOffset by animateDpAsState(
            targetValue = if (done) segmentWidth else 0.dp,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
            label = "checkin_pill_offset",
        )

        // Pílula deslizante do estado selecionado
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(if (done) 2.dp else 1.dp, CircleShape)
                .clip(CircleShape)
                .background(pillColor),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Tab,
                        onClick = {
                            if (done) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            }
                            onSelect(false)
                        },
                    )
                    .semantics { selected = !done },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pendingText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (!done) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (!done) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Tab,
                        onClick = {
                            if (!done) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            }
                            onSelect(true)
                        },
                    )
                    .semantics { selected = done },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = doneText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (done) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    color = if (done) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
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

/**
 * "Edição rápida": um botão só (lápis + texto) que abre um balão com Editar metas, Editar controles,
 * Editar emoji e Editar cores de destaque, nessa ordem. Porte do editShortcuts + TodayQuickEditPopover do iOS.
 * O menu abre acima ou abaixo conforme o espaço.
 */
@Composable
private fun TodayQuickEditButton(
    accent: Color,
    onOpenTheme: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenGoals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var buttonWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val menuWidth = 280.dp
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.onSizeChanged { buttonWidthPx = it.width },
            shape = CircleShape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = accent.copy(alpha = 0.08f),
                contentColor = accent,
            ),
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.today_edit_quick),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            // Centralizado sob o botão (o padrão alinha pela borda esquerda).
            offset = DpOffset(x = with(density) { buttonWidthPx.toDp() } / 2 - menuWidth / 2, y = 0.dp),
            modifier = Modifier.width(menuWidth),
        ) {
            Text(
                text = stringResource(R.string.today_edit_quick),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .semantics { heading() },
            )
            val pick: (() -> Unit) -> () -> Unit = { open -> { expanded = false; open() } }
            QuickEditItem(painterResource(R.drawable.ic_settings_target), R.string.today_edit_goals, accent, pick(onOpenGoals))
            QuickEditItem(painterResource(R.drawable.ic_settings_controls), R.string.today_edit_controls, accent, pick(onOpenControls))
            QuickEditItem(rememberVectorPainter(Icons.Outlined.Face), R.string.today_edit_theme, accent, pick(onOpenTheme))
            QuickEditItem(painterResource(R.drawable.ic_settings_palette), R.string.today_edit_colors, accent, pick(onOpenColors))
        }
    }
}

/** Linha do balão de Edição rápida: ícone na cor de destaque + texto. */
@Composable
private fun QuickEditItem(icon: Painter, @StringRes title: Int, accent: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(title), style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp)) },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    )
}

/** Faixa da dobra em dp, nas coordenadas da Hoje (null sem dobra). */
private data class FoldBand(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp, val halfOpened: Boolean, val vertical: Boolean)

private fun foldBand(info: FoldInfo?, origin: Offset, density: Density): FoldBand? = info?.let {
    with(density) {
        FoldBand(
            left = (it.bounds.left - origin.x).toDp(),
            top = (it.bounds.top - origin.y).toDp(),
            right = (it.bounds.right - origin.x).toDp(),
            bottom = (it.bounds.bottom - origin.y).toDp(),
            halfOpened = it.halfOpened,
            vertical = it.vertical,
        )
    }
}

/** Como o hero e as metas se dividem no layout largo. */
private sealed interface WideArrangement {
    /** Lado a lado: hero com largura fixa à esquerda, metas no resto. */
    data class Side(val heroWidth: Dp, val gap: Dp) : WideArrangement
    /** Empilhado: hero em cima com altura fixa, metas embaixo. */
    data class Stacked(val heroHeight: Dp, val gap: Dp) : WideArrangement
}

/** Margem de cada lado da dobra: o vinco não é lugar pra botão (iOS: faixa de 40 + 20 de cada lado). */
private val FoldMargin = 24.dp

/**
 * Layout largo (tablet, dobrável aberto) — porte do wideLayout do iOS. Coluna do hero (botões, data, anel e Edição
 * rápida) + coluna das metas (grade de categorias, metas em grid quando sobra altura, ou lista de até 330).
 * Com a dobra meio aberta: livro (dobra em pé) = hero na metade esquerda e metas na direita, a faixa vazia; mesa
 * (dobra deitada) = hero colado no topo da metade de cima e metas embaixo. Aberto reto em pé = mesmo empilhado,
 * com o hero centralizado. As views são as mesmas em todos os casos (`animateBounds` num `LookaheadScope`), então
 * dobrar e desdobrar anima as colunas esticando e deslizando em vez de trocar de tela.
 * Tablet deitado sem dobra: os botões vão pra uma coluna de vidro à direita.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun TodayWide(
    state: TodayUiState,
    accent: Color,
    width: Dp,
    height: Dp,
    fold: FoldBand?,
    onOpenTheme: () -> Unit,
    onOpenColors: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    onRingClick: () -> Unit,
    onTierClick: () -> Unit,
    viewModel: TodayViewModel,
) {
    val isTall = height >= Breakpoints.TallThreshold
    // Abaixo de 744 dp o hero encolhe e as margens somem, pra sobrar espaço pras metas.
    val compact = width < 744.dp
    val landscape = width > height
    // Tipo de dobra que vale agora (só faz sentido com a faixa dentro da tela).
    val book = fold != null && fold.halfOpened && fold.vertical
    val tabletop = fold != null && fold.halfOpened && !fold.vertical
    val flatPortrait = fold != null && !fold.halfOpened && !landscape
    val stacked = tabletop || flatPortrait
    // Coluna de vidro: tablet deitado sem dobra meio aberta.
    val sideRail = landscape && !book && !stacked
    val railWidth = if (sideRail) 72.dp else 0.dp
    val padding = 16.dp

    val arrangement: WideArrangement = when {
        book -> WideArrangement.Side(
            heroWidth = (fold!!.left - FoldMargin - padding).coerceAtLeast(0.dp),
            gap = (fold.right - fold.left) + FoldMargin * 2,
        )
        tabletop -> WideArrangement.Stacked(
            heroHeight = (fold!!.top - FoldMargin - padding).coerceAtLeast(0.dp),
            gap = (fold.bottom - fold.top) + FoldMargin * 2,
        )
        flatPortrait -> WideArrangement.Stacked(heroHeight = height / 2 - 16.dp - padding, gap = 16.dp)
        else -> WideArrangement.Side(heroWidth = if (compact) 320.dp else 360.dp, gap = if (compact) 16.dp else 24.dp)
    }
    val isList = state.goalsViewMode == TodayGoalsViewMode.List
    val bounds = BoundsTransform { _, _ -> spring(dampingRatio = 0.85f, stiffness = 130f) }

    Row(Modifier.fillMaxSize().padding(padding), verticalAlignment = Alignment.CenterVertically) {
        LookaheadScope {
            WideArrangedLayout(arrangement, Modifier.weight(1f).fillMaxHeight()) {
                // Coluna do hero
                Column(
                    modifier = Modifier
                        .animateBounds(this@LookaheadScope, boundsTransform = bounds)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // Mesa: colado no topo (longe da dobra); nos outros, centralizado na altura.
                    verticalArrangement = if (tabletop) Arrangement.spacedBy(12.dp, Alignment.Top)
                    else Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    if (!sideRail) {
                        TodayPageButtons(
                            goalsViewMode = state.goalsViewMode,
                            accent = accent,
                            onOpenSettings = onOpenSettings,
                            onToggleViewMode = { viewModel.toggleGoalsViewMode() },
                            onOpenHistory = onOpenHistory,
                            onOpenAchievements = onOpenAchievements,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    TodayTopBar(dateLabel = state.dateLabel)
                    TodayHeroCard(
                        state = state,
                        onRingClick = onRingClick,
                        onTierClick = onTierClick,
                        // Anel maior só com altura e largura sobrando (não empilhado, que tem meia tela).
                        verticalLayout = isTall && !compact && !stacked,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // Na lista a Edição rápida fica abaixo do hero; na grade vai pra baixo das categorias.
                    if (isList) {
                        TodayQuickEditButton(
                            accent = accent,
                            onOpenTheme = onOpenTheme,
                            onOpenColors = onOpenColors,
                            onOpenControls = onOpenControls,
                            onOpenGoals = onOpenGoals,
                        )
                    }
                }
                // Coluna das metas
                Box(
                    modifier = Modifier.animateBounds(this@LookaheadScope, boundsTransform = bounds),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when {
                        !isList -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        ) {
                            TodayCategoryGrid(
                                sections = state.sections,
                                uncategorizedGoals = state.uncategorizedGoals,
                                controlStyle = state.controlStyle,
                                accent = accent,
                                onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
                                onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
                                modifier = Modifier.widthIn(max = 560.dp),
                            )
                            TodayQuickEditButton(
                                accent = accent,
                                onOpenTheme = onOpenTheme,
                                onOpenColors = onOpenColors,
                                onOpenControls = onOpenControls,
                                onOpenGoals = onOpenGoals,
                            )
                        }
                        // Altura sobrando e lado a lado (inclusive o livro): metas em grid de colunas, ocupando a
                        // largura toda. Mesmo componente com e sem dobra, pra a troca animar em vez de recriar.
                        isTall && !stacked -> TodayGoalsGrid(
                            cells = todayGridCells(state, accent, viewModel),
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Lista numa coluna centralizada, perto da largura do hero.
                        else -> TodayGoalsListContent(
                            state = state,
                            accent = accent,
                            onToggleCategory = { viewModel.toggleCategory(it) },
                            onSetIntake = { goal, value -> viewModel.updateIntake(goal, value) },
                            onToggleRest = { goal -> viewModel.toggleRestDay(goal) },
                            modifier = Modifier
                                .widthIn(max = 330.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
        // Coluna de vidro à direita, centralizada na altura (iPad deitado no iOS).
        AnimatedVisibility(
            visible = sideRail,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it },
        ) {
            TodaySideRail(
                goalsViewMode = state.goalsViewMode,
                accent = accent,
                onOpenSettings = onOpenSettings,
                onToggleViewMode = { viewModel.toggleGoalsViewMode() },
                onOpenHistory = onOpenHistory,
                onOpenAchievements = onOpenAchievements,
                modifier = Modifier.padding(start = 16.dp).width(railWidth - 16.dp),
            )
        }
    }
}

/** Mede e posiciona as duas colunas (hero e metas) conforme o arranjo, mantendo as mesmas views. */
@Composable
private fun WideArrangedLayout(
    arrangement: WideArrangement,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        when (arrangement) {
            is WideArrangement.Side -> {
                val heroW = arrangement.heroWidth.roundToPx().coerceIn(0, w)
                val gap = arrangement.gap.roundToPx()
                val goalsW = (w - heroW - gap).coerceAtLeast(0)
                val hero = measurables[0].measure(Constraints.fixed(heroW, h))
                val goals = measurables[1].measure(Constraints.fixed(goalsW, h))
                layout(w, h) {
                    hero.place(0, 0)
                    goals.place(heroW + gap, 0)
                }
            }
            is WideArrangement.Stacked -> {
                val heroH = arrangement.heroHeight.roundToPx().coerceIn(0, h)
                val gap = arrangement.gap.roundToPx()
                val goalsH = (h - heroH - gap).coerceAtLeast(0)
                val hero = measurables[0].measure(Constraints.fixed(w, heroH))
                val goals = measurables[1].measure(Constraints.fixed(w, goalsH))
                layout(w, h) {
                    hero.place(0, 0)
                    goals.place(0, heroH + gap)
                }
            }
        }
    }
}

/**
 * Coluna de vidro com Ajustes, Registros, Conquistas e troca lista/grade, na vertical — porte do TodaySideRail
 * (iOS, iPad deitado), no estilo da barra vertical do iPhone Duo.
 */
@Composable
private fun TodaySideRail(
    goalsViewMode: TodayGoalsViewMode,
    accent: Color,
    onOpenSettings: () -> Unit,
    onToggleViewMode: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isList = goalsViewMode == TodayGoalsViewMode.List
    val nextModeName = stringResource(
        if (isList) R.string.today_goals_view_mode_grid else R.string.today_goals_view_mode_list,
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, stringResource(R.string.tab_settings), tint = accent)
            }
            IconButton(onClick = onOpenHistory) {
                Icon(painterResource(R.drawable.ic_records), stringResource(R.string.today_history), tint = accent)
            }
            IconButton(onClick = onOpenAchievements) {
                Icon(painterResource(R.drawable.ic_settings_trophy), stringResource(R.string.achievements_header_title), tint = accent)
            }
            IconButton(onClick = onToggleViewMode) {
                Icon(
                    painter = painterResource(if (isList) R.drawable.ic_view_grid else R.drawable.ic_view_list),
                    contentDescription = stringResource(R.string.today_goals_view_mode_switch_a11y, nextModeName),
                    tint = accent,
                )
            }
        }
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

/** Pedido único de animar a entrada das categorias da Hoje (marcado ao concluir a Welcome). */
object GoalsEntrance {
    @Volatile var pending: Boolean = false

    fun consume(): Boolean = pending.also { pending = false }
}
