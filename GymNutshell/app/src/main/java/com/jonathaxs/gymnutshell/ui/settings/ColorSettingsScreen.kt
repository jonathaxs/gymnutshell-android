package com.jonathaxs.gymnutshell.ui.settings

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.components.GroupCard
import com.jonathaxs.gymnutshell.ui.components.GroupInset
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton

/**
 * Tela de escolha de cor de destaque — porte de ColorSettingsView (iOS):
 * Prévia ao vivo no topo (mini Hoje com anel, % na cor, 2 botões de categoria e barra de progresso)
 * e grade com as 8 cores num card agrupado.
 */
@Composable
fun ColorSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val selected by viewModel.accentColor.collectAsStateWithLifecycle()
    val animatedColor by animateColorAsState(selected.color, tween(250), label = "color_preview")

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_accent_color),
                onBack = onBack,
            )
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
                // 1. Prévia ao vivo da cor
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_color_preview),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = GroupInset + 20.dp, end = GroupInset),
                    )

                    GroupCard {
                        ColorLivePreview(color = animatedColor)
                    }
                }

                // 2. Grade de cores de destaque
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_color_subtitle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = GroupInset + 20.dp, end = GroupInset),
                    )

                    GroupCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp, horizontal = 12.dp),
                        ) {
                            ColorGrid(
                                selected = selected,
                                onSelect = viewModel::setAccent,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Mini-versão da Today com a cor selecionada: anel + %, 2 botões de categoria e barra de progresso com − e +. */
@Composable
private fun ColorLivePreview(color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Linha superior: anel, texto 62% + Água, e 2 botões de categoria
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Anel de progresso 84x84dp com "💪" no centro
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    // Trilho de fundo
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                    // Segmento preenchido 62%
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * 0.62f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
                Text(
                    text = "💪",
                    fontSize = 28.sp,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }

            Spacer(Modifier.width(16.dp))

            // Rótulo 62% + Água
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "62%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                    ),
                    color = color,
                )
                Text(
                    text = stringResource(R.string.today_water),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Dois botões de categoria (Treino/Coração e Comida)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryPreviewCircle(color = color) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                }
                CategoryPreviewCircle(color = color) {
                    Icon(
                        painter = painterResource(R.drawable.ic_category_food),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // Linha inferior: barra de progresso + botões − e +
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            LinearProgressIndicator(
                progress = { 0.62f },
                color = color,
                trackColor = color.copy(alpha = 0.25f),
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoundStepButton(
                    symbol = "−",
                    contentDescription = "",
                    onClick = {},
                    enabled = false,
                    color = color,
                    size = 40.dp,
                )
                RoundStepButton(
                    symbol = "+",
                    contentDescription = "",
                    onClick = {},
                    enabled = false,
                    color = color,
                    size = 40.dp,
                )
            }
        }
    }
}

@Composable
private fun CategoryPreviewCircle(color: Color, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(2.dp, color.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        icon()
    }
}

/** Grade 4 colunas com as 8 cores de destaque. */
@Composable
private fun ColorGrid(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AccentColor.entries.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowColors.forEach { accent ->
                    ColorSwatch(
                        accent = accent,
                        isSelected = accent == selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(accent) },
                    )
                }
                repeat(4 - rowColors.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Círculo de cor com nome; anel + checkmark quando selecionado. */
@Composable
private fun ColorSwatch(
    accent: AccentColor,
    isSelected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val name = stringResource(colorNameRes(accent))
    val stateDesc = stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected)

    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = name
                stateDescription = stateDesc
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .then(
                    if (isSelected) Modifier.border(2.5.dp, accent.color, CircleShape)
                    else Modifier,
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(accent.color),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@StringRes
private fun colorNameRes(accent: AccentColor): Int = when (accent) {
    AccentColor.Red -> R.string.color_red
    AccentColor.Blue -> R.string.color_blue
    AccentColor.Purple -> R.string.color_purple
    AccentColor.Green -> R.string.color_green
    AccentColor.Yellow -> R.string.color_yellow
    AccentColor.Orange -> R.string.color_orange
    AccentColor.Cyan -> R.string.color_cyan
    AccentColor.Pink -> R.string.color_pink
}
