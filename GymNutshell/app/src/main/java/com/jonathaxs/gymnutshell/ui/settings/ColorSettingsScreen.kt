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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.jonathaxs.gymnutshell.core.domain.AppearanceMode
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.components.GroupCard
import com.jonathaxs.gymnutshell.ui.components.GroupInset
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton

/**
 * Tela de Aparência — porte de ColorSettingsView (iOS):
 * card "Modo" (Dispositivo, Claro, Escuro), grade com as 8 cores de destaque e, embaixo, a prévia ao vivo.
 */
@Composable
fun ColorSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val selected by viewModel.accentColor.collectAsStateWithLifecycle()
    val appearanceMode by viewModel.appearanceMode.collectAsStateWithLifecycle()
    val animatedColor by animateColorAsState(selected.color, tween(250), label = "color_preview")

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_appearance_title),
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
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 1. Modo (Dispositivo / Claro / Escuro)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_appearance_mode_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = GroupInset + 20.dp, end = GroupInset),
                    )

                    GroupCard {
                        AppearanceModePicker(
                            selected = appearanceMode,
                            accent = animatedColor,
                            onSelect = viewModel::setAppearanceMode,
                        )
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

                // 2. Prévia ao vivo da cor (embaixo da grade, como no iOS)
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

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Mini-versão da Today com a cor selecionada: %, 2 botões de categoria e barra de progresso com − e +. */
@Composable
private fun ColorLivePreview(color: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        // Linha superior: 62% + Água e 2 botões de categoria (sem anel: as cores do anel são fixas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
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
                    color = color,
                    size = 44.dp,
                )
                RoundStepButton(
                    symbol = "+",
                    contentDescription = "",
                    onClick = {},
                    color = color,
                    size = 44.dp,
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

/** Ordem de exibição das cores (ordem dos cases de AppAccentColor no iOS). O enum não muda: é salvo pelo nome. */
private val AccentColorOrder = listOf(
    AccentColor.Purple, AccentColor.Blue, AccentColor.Green, AccentColor.Red,
    AccentColor.Pink, AccentColor.Cyan, AccentColor.Yellow, AccentColor.Orange,
)

/** Grade 4 colunas com as 8 cores de destaque. */
@Composable
private fun ColorGrid(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AccentColorOrder.chunked(4).forEach { rowColors ->
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

/** 3 opções lado a lado (Dispositivo, Claro, Escuro) — porte de appearanceModePicker (iOS). */
@Composable
private fun AppearanceModePicker(
    selected: AppearanceMode,
    accent: Color,
    onSelect: (AppearanceMode) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppearanceMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val label = stringResource(appearanceModeTitleRes(mode))
            val stateDesc = stringResource(if (isSelected) R.string.a11y_selected else R.string.a11y_not_selected)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(
                        if (isSelected) accent.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    )
                    .then(
                        if (isSelected) Modifier.border(2.dp, accent, shape)
                        else Modifier,
                    )
                    .clickable { onSelect(mode) }
                    .padding(vertical = 14.dp, horizontal = 8.dp)
                    .semantics {
                        contentDescription = label
                        stateDescription = stateDesc
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(appearanceModeIconRes(mode)),
                    contentDescription = null,
                    tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

@StringRes
private fun appearanceModeTitleRes(mode: AppearanceMode): Int = when (mode) {
    AppearanceMode.SYSTEM -> R.string.settings_appearance_mode_system
    AppearanceMode.LIGHT -> R.string.settings_appearance_mode_light
    AppearanceMode.DARK -> R.string.settings_appearance_mode_dark
}

private fun appearanceModeIconRes(mode: AppearanceMode): Int = when (mode) {
    AppearanceMode.SYSTEM -> R.drawable.ic_mode_system
    AppearanceMode.LIGHT -> R.drawable.ic_mode_light
    AppearanceMode.DARK -> R.drawable.ic_mode_dark
}
