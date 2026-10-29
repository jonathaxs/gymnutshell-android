package com.jonathaxs.gymnutshell.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.core.domain.pluralQuantity
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.components.categoryIcon

/** Cor verde padrão para itens e categorias concluídas (paridade com iOS). */
private val DoneGreenColor = Color(0xFF34C759)

/**
 * Retorna a cor própria de cada categoria na grade:
 * Essencial vermelho, Nutrição verde, Treino laranja, Suplemento violeta;
 * Categorias do usuário e "Outras" usam a cor de destaque configurada.
 */
fun categoryTintColor(categoryId: String, accent: Color): Color {
    val raw = categoryId.removePrefix("builtin:")
    return when (GoalCategory.fromRaw(raw)) {
        GoalCategory.Essencial -> Color(AccentColor.Red.argb)
        GoalCategory.Nutricao -> Color(AccentColor.Green.argb)
        GoalCategory.Treino -> Color(AccentColor.Orange.argb)
        GoalCategory.Suplemento -> Color(AccentColor.Purple.argb)
        null -> accent
    }
}

/** Dados de um bloco de categoria da grade na TodayView. */
data class TodayCategoryTileUi(
    val id: String,
    val title: String,
    val symbolName: String?,
    val tintColor: Color,
    val progress: Double,
    val completedCount: Int,
    val totalCount: Int,
    val goals: List<TodayGoalUi>,
)

/**
 * Grade de categorias da tela Hoje (modo "Grade" no retrato) — porte de TodayCategoryGrid (iOS).
 * Botões circulares de 4 por linha com ícone, mini anel de progresso, nome e contagem;
 * tocar abre um balão (popover) abaixo/acima com os controles das metas.
 * A última linha incompleta fica centralizada mantendo a mesma largura dos botões.
 */
@Composable
fun TodayCategoryGrid(
    sections: List<TodayCategoryUi>,
    uncategorizedGoals: List<TodayGoalUi>,
    controlStyle: ControlStyle,
    accent: Color,
    onSetIntake: (TodayGoalUi, Int) -> Unit,
    onToggleRest: (TodayGoalUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val otherTitle = stringResource(R.string.today_category_other)

    // Monta os blocos a partir das seções com metas visíveis
    val tiles = remember(sections, uncategorizedGoals, accent, otherTitle, resources) {
        val list = mutableListOf<TodayCategoryTileUi>()
        sections.forEach { section ->
            if (section.goals.isNotEmpty()) {
                val title = section.titleRes?.let { resources.getString(it) } ?: section.title.orEmpty()
                val completed = section.goals.count { it.progress >= 1.0 }
                val total = section.goals.size
                val avg = section.goals.map { it.progress }.average()
                list.add(
                    TodayCategoryTileUi(
                        id = section.id,
                        title = title,
                        symbolName = section.symbolName,
                        tintColor = categoryTintColor(section.id, accent),
                        progress = avg,
                        completedCount = completed,
                        totalCount = total,
                        goals = section.goals,
                    ),
                )
            }
        }
        if (uncategorizedGoals.isNotEmpty()) {
            val completed = uncategorizedGoals.count { it.progress >= 1.0 }
            val total = uncategorizedGoals.size
            val avg = uncategorizedGoals.map { it.progress }.average()
            list.add(
                TodayCategoryTileUi(
                    id = "__uncategorized__",
                    title = otherTitle,
                    symbolName = "square.grid.2x2.fill",
                    tintColor = accent,
                    progress = avg,
                    completedCount = completed,
                    totalCount = total,
                    goals = uncategorizedGoals,
                ),
            )
        }
        list
    }

    if (tiles.isEmpty()) return

    // Bloco com o balão aberto (id da categoria ou null)
    var openTileId by remember { mutableStateOf<String?>(null) }

    // Fecha o balão se a grade sair da tela ou mudar de modo
    DisposableEffect(Unit) {
        onDispose { openTileId = null }
    }

    val columns = 4
    val spacing = 8.dp
    val maxCircleSize = 88.dp

    BoxWithConstraints(modifier = modifier) {
        val totalSpacing = spacing * (columns - 1)
        val tileWidth = ((maxWidth - totalSpacing) / columns).coerceAtLeast(0.dp)
        val circleSize = minOf(maxCircleSize, (tileWidth - 2.dp).coerceAtLeast(0.dp))

        val rows = remember(tiles) { tiles.chunked(columns) }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            rows.forEach { row ->
                val isLastRowIncomplete = row.size < columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isLastRowIncomplete) {
                        Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
                    } else {
                        Arrangement.spacedBy(spacing)
                    },
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { tile ->
                        CategoryTileItem(
                            tile = tile,
                            tileWidth = tileWidth,
                            circleSize = circleSize,
                            isOpen = openTileId == tile.id,
                            onOpen = { openTileId = tile.id },
                            onDismiss = { openTileId = null },
                            controlStyle = controlStyle,
                            onSetIntake = onSetIntake,
                            onToggleRest = onToggleRest,
                        )
                    }
                }
            }
        }
    }
}

/** Bloco redondo de uma categoria com ícone, mini anel, título e balão ancorado. */
@Composable
private fun CategoryTileItem(
    tile: TodayCategoryTileUi,
    tileWidth: Dp,
    circleSize: Dp,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    controlStyle: ControlStyle,
    onSetIntake: (TodayGoalUi, Int) -> Unit,
    onToggleRest: (TodayGoalUi) -> Unit,
) {
    val isDone = tile.totalCount > 0 && tile.completedCount == tile.totalCount
    val ringColor = if (isDone) DoneGreenColor else tile.tintColor
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    val animatedProgress by animateFloatAsState(
        targetValue = tile.progress.toFloat(),
        animationSpec = tween(400),
        label = "category_tile_progress",
    )

    val a11yValue = pluralStringResource(
        R.plurals.today_category_tile_a11y_value,
        pluralQuantity(tile.totalCount),
        tile.completedCount,
        tile.totalCount,
    )
    val a11yHint = stringResource(R.string.today_category_tile_a11y_hint)

    val density = LocalDensity.current

    Box(
        modifier = Modifier.width(tileWidth),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .width(tileWidth)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = 4.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = "${tile.title}, $a11yValue. $a11yHint"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Botão redondo: fundo sutil + mini anel + ícone central
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Mini anel com o progresso da categoria
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                ) {
                    val strokeWidth = 5.dp.toPx()
                    // Trilho cinza
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    // Segmento preenchido
                    val sweep = 360f * animatedProgress.coerceIn(0f, 1f)
                    if (sweep > 0f) {
                        drawArc(
                            color = ringColor,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )
                    }
                }

                // Ícone: check verde quando concluído; ícone da categoria caso contrário
                val iconSize = (circleSize * 0.32f).coerceAtLeast(18.dp)
                Icon(
                    imageVector = if (isDone) Icons.Filled.Check else categoryIcon(tile.symbolName),
                    contentDescription = null,
                    tint = ringColor,
                    modifier = Modifier.size(iconSize),
                )
            }

            // Nome da categoria
            Text(
                text = tile.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            // Contagem "x/y" metas completas
            Text(
                text = "${tile.completedCount}/${tile.totalCount}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }

        // Balão (popover) ancorado no botão
        if (isOpen) {
            val positionProvider = remember(density) {
                CategoryPopoverPositionProvider(marginDp = 8.dp, density = density)
            }

            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onDismiss,
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                ),
            ) {
                CategoryPopover(
                    tile = tile,
                    controlStyle = controlStyle,
                    onSetIntake = onSetIntake,
                    onToggleRest = onToggleRest,
                )
            }
        }
    }
}

/**
 * Balão de controles da categoria (popover): lista as metas com os mesmos controles da lista
 * (Slider ou botões -/+, ON/OFF para descanso/booleanas), título com ícone na cor da categoria.
 */
@Composable
private fun CategoryPopover(
    tile: TodayCategoryTileUi,
    controlStyle: ControlStyle,
    onSetIntake: (TodayGoalUi, Int) -> Unit,
    onToggleRest: (TodayGoalUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val maxAvailableWidth = (windowWidthDp - 32.dp).coerceAtLeast(260.dp)
    val popoverWidth = minOf(340.dp, maxAvailableWidth)

    Surface(
        modifier = modifier
            .width(popoverWidth)
            .heightIn(max = 520.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // Cabeçalho do balão: ícone + título na cor da categoria
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = categoryIcon(tile.symbolName),
                    contentDescription = null,
                    tint = tile.tintColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Metas da categoria em card agrupado com divisores
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top,
                    ) {
                        tile.goals.forEachIndexed { index, goal ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(start = 52.dp),
                                )
                            }
                            GoalRow(
                                goal = goal,
                                controlStyle = controlStyle,
                                accent = tile.tintColor,
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

/**
 * Posicionador do balão: abre abaixo do botão da categoria se houver espaço;
 * se faltar espaço na parte inferior da tela, abre acima. Centraliza no eixo X travando na tela.
 */
class CategoryPopoverPositionProvider(
    private val marginDp: Dp = 8.dp,
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val marginPx = with(density) { marginDp.roundToPx() }
        val screenPaddingPx = with(density) { 16.dp.roundToPx() }

        // Centralização horizontal com o botão da categoria, travado nas bordas da tela
        val idealX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val maxX = (windowSize.width - popupContentSize.width - screenPaddingPx).coerceAtLeast(screenPaddingPx)
        val x = idealX.coerceIn(screenPaddingPx, maxX)

        // Eixo vertical: prefere abaixo; se não couber abaixo e houver mais espaço acima, abre acima
        val spaceBelow = windowSize.height - anchorBounds.bottom - marginPx
        val spaceAbove = anchorBounds.top - marginPx

        val y = if (spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove) {
            val idealY = anchorBounds.bottom + marginPx
            idealY.coerceAtMost(windowSize.height - popupContentSize.height - screenPaddingPx)
        } else {
            val idealY = anchorBounds.top - popupContentSize.height - marginPx
            idealY.coerceAtLeast(screenPaddingPx)
        }

        return IntOffset(x, y)
    }
}
