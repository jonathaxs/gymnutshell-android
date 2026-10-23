package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.foundation.text.TextAutoSize
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.widget.SegmentedRingPainter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.foundation.isSystemInDarkTheme
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.widget.SegmentedRing
import com.jonathaxs.gymnutshell.core.widget.WidgetBackground
import com.jonathaxs.gymnutshell.core.widget.WidgetBackgroundMode
import com.jonathaxs.gymnutshell.ui.welcome.WelcomeCard

/** Paleta de cores personalizadas pro fundo do widget (ARGB). */
private val CUSTOM_PALETTE = listOf(
    0xFFFF3B30L, 0xFFFF9500L, 0xFFFFCC00L, 0xFF34C759L,
    0xFF32ADE6L, 0xFF007AFFL, 0xFF5856D6L, 0xFFAF52DEL,
    0xFFFF2D55L, 0xFFA2845EL, 0xFF8E8E93L, 0xFF1C1C1EL,
)

/**
 * Sub-tela "Fundo do widget" — porte de WidgetBackgroundSettingsView (iOS).
 * Prévias pequena e média com o anel real no topo, 3 cards de opção (Padrão, Destaque,
 * Personalizada) e a paleta de cores dentro do card de Personalizada.
 */
@Composable
fun WidgetBackgroundScreen(
    onBack: () -> Unit,
    viewModel: WidgetBackgroundViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    // Prévia com o emoji do tema e o sexo do usuário em 62% (nível 4), como no iOS.
    val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
    val sex = settingsViewModel.profile.collectAsStateWithLifecycle().value.sex
    val previewEmoji = theme.emoji(DailyAchievement.fromPercent(62), sex)
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val customArgb by viewModel.customArgb.collectAsStateWithLifecycle()
    val accentArgb by viewModel.accentArgb.collectAsStateWithLifecycle()
    val accentColor = Color(accentArgb)

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.settings_widget_background), onBack) }) { padding ->
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Rótulo da prévia
                Text(
                    text = stringResource(R.string.widget_bg_preview),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                // Prévias pequena e média com anel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WidgetPreviewTile(
                        mode = mode,
                        accentArgb = accentArgb,
                        customArgb = customArgb,
                        isWide = false,
                        emoji = previewEmoji,
                    )
                    WidgetPreviewTile(
                        mode = mode,
                        accentArgb = accentArgb,
                        customArgb = customArgb,
                        isWide = true,
                        emoji = previewEmoji,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Card 1: Padrão
                WidgetModeOptionCard(
                    title = stringResource(R.string.widget_bg_mode_system),
                    description = stringResource(R.string.widget_bg_footer_system),
                    isSelected = mode == WidgetBackgroundMode.System,
                    accentColor = accentColor,
                    onClick = { viewModel.setMode(WidgetBackgroundMode.System) },
                )

                // Card 2: Destaque
                WidgetModeOptionCard(
                    title = stringResource(R.string.widget_bg_mode_accent),
                    description = stringResource(R.string.widget_bg_footer_accent),
                    isSelected = mode == WidgetBackgroundMode.Accent,
                    accentColor = accentColor,
                    onClick = { viewModel.setMode(WidgetBackgroundMode.Accent) },
                )

                // Card 3: Personalizada
                WidgetModeOptionCard(
                    title = stringResource(R.string.widget_bg_mode_custom),
                    description = stringResource(R.string.widget_bg_footer_custom),
                    isSelected = mode == WidgetBackgroundMode.Custom,
                    accentColor = accentColor,
                    onClick = { viewModel.setMode(WidgetBackgroundMode.Custom) },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(
                            text = stringResource(R.string.widget_bg_pick_color),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ColorPalette(selected = customArgb, onSelect = viewModel::setCustomColor)
                    }
                }
            }
        }
    }
}

/** Card de opção de fundo com cantos arredondados, borda no selecionado e extra opcional. */
@Composable
private fun WidgetModeOptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null,
) {
    WelcomeCard(
        isSelected = isSelected,
        accentColor = accentColor,
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (content != null && isSelected) {
                content()
            }
        }
    }
}

/**
 * Tile de prévia com os mesmos layouts do widget: pequeno (anel com o emoji + % embaixo) e médio (anel com o
 * emoji + % e a frase do próximo nível). Em fundo colorido o anel ganha o contorno do widget de verdade.
 */
@Composable
private fun WidgetPreviewTile(
    mode: WidgetBackgroundMode,
    accentArgb: Long,
    customArgb: Long,
    isWide: Boolean,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    val height = 130.dp
    val width = if (isWide) 230.dp else 120.dp
    val isSystem = mode == WidgetBackgroundMode.System
    val sourceArgb = if (mode == WidgetBackgroundMode.Accent) accentArgb else customArgb
    val textColor = if (isSystem) MaterialTheme.colorScheme.onSurface else Color(WidgetBackground.contrastingTextArgb(sourceArgb))
    val borderArgb = if (isSystem) null else WidgetBackground.contrastingTextArgb(sourceArgb)
    val bgModifier = if (isSystem) {
        Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    } else {
        Modifier.background(gradientBrush(sourceArgb))
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(24.dp))
            .then(bgModifier)
            .border(
                BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)),
                RoundedCornerShape(24.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isWide) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PreviewRing(emoji = emoji, borderArgb = borderArgb, size = 74.dp)
                Text("62%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 14.dp),
            ) {
                PreviewRing(emoji = emoji, borderArgb = borderArgb, size = 86.dp)
                Column {
                    Text("62%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
                    Text(
                        text = stringResource(R.string.today_hero_caption_next_level, 19),
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        softWrap = false,
                        // Numa linha só: encolhe a fonte em vez de quebrar (iOS: até 50%).
                        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 12.sp),
                    )
                }
            }
        }
    }
}

/** Anel da prévia do widget: o mesmo desenho do widget ([SegmentedRingPainter]) com o emoji no centro. */
@Composable
private fun PreviewRing(
    emoji: String,
    borderArgb: Long?,
    size: Dp,
    progress: Float = 0.62f,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * SegmentedRing.ICON_LINE_WIDTH_RATIO
            val extra = if (borderArgb != null) 2.dp.toPx() else 0f
            val inset = stroke / 2f + extra
            drawIntoCanvas { canvas ->
                SegmentedRingPainter.draw(
                    canvas = canvas.nativeCanvas,
                    rect = RectF(inset, inset, this.size.width - inset, this.size.height - inset),
                    progress = progress.toDouble(),
                    strokePx = stroke,
                    isDark = isDark,
                    borderArgb = borderArgb,
                    borderExtraPx = extra,
                )
            }
        }
        Text(text = emoji, fontSize = (size.value * 0.30f).sp, textAlign = TextAlign.Center)
    }
}

/** Grade 4 colunas com as cores da paleta; círculo + check na selecionada. */
@Composable
private fun ColorPalette(selected: Long, onSelect: (Long) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        CUSTOM_PALETTE.chunked(4).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowColors.forEach { argb ->
                    val color = Color(argb)
                    val isSelected = argb == selected
                    // Célula ocupa a coluna; o círculo tem tamanho fixo no centro (sem virar pílula).
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .then(if (isSelected) Modifier.border(2.5.dp, color, CircleShape) else Modifier)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onSelect(argb) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Text(
                                text = "✓",
                                color = Color(WidgetBackground.contrastingTextArgb(argb)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                        }
                    }
                    }
                }
                repeat(4 - rowColors.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun gradientBrush(argb: Long): Brush = Brush.linearGradient(
    listOf(Color(WidgetBackground.lighterArgb(argb)), Color(WidgetBackground.darkerArgb(argb))),
)
