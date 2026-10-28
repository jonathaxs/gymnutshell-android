package com.jonathaxs.gymnutshell.ui.today

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.widget.SegmentedRing
import com.jonathaxs.gymnutshell.ui.settings.SettingsViewModel
import com.jonathaxs.gymnutshell.ui.settings.tierRange
import com.jonathaxs.gymnutshell.ui.theme.color
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Balão informativo ancorado da tela Hoje — porte dos popovers de TodayInfoPopovers (iOS).
 * Reutiliza [CategoryPopoverPositionProvider] para abrir abaixo/acima do elemento clicado.
 */
@Composable
internal fun TodayInfoPopover(
    onDismiss: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        CategoryPopoverPositionProvider(marginDp = 8.dp, density = density)
    }
    val windowWidthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp() }
    val maxAvailableWidth = (windowWidthDp - 32.dp).coerceAtLeast(240.dp)
    val popoverWidth = minOf(320.dp, maxAvailableWidth)

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .width(popoverWidth)
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() },
                )
                content()
            }
        }
    }
}

/**
 * Balão com os 5 níveis de conquista do tema atual e atalho para trocar os emojis.
 */
@Composable
internal fun TierInfoPopover(
    onDismiss: () -> Unit,
    onChangeTheme: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val sex = viewModel.profile.collectAsStateWithLifecycle().value.sex
    val accent = viewModel.accentColor.collectAsStateWithLifecycle().value.color
    val rangeSuffix = stringResource(R.string.tier_info_range_suffix)

    TodayInfoPopover(
        onDismiss = onDismiss,
        title = stringResource(R.string.settings_about_achievement),
    ) {
        Text(
            text = stringResource(R.string.tier_info_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DailyAchievement.entries.forEach { tier ->
            val tierName = stringResource(theme.tierNameRes(tier, sex))
            val rangeText = tierRange(tier) + rangeSuffix
            val levelNumber = tier.number
            val levelLabel = stringResource(R.string.today_info_level_format, levelNumber)
            val rowA11y = stringResource(R.string.today_info_tier_row_a11y, tierName, rangeText, levelNumber)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = rowA11y },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = theme.emoji(tier, sex),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(36.dp)
                        .clearAndSetSemantics {},
                )
                Column {
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = rangeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = levelLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accent,
                )
            }
        }

        // Atalho para trocar o tema de emojis
        FilledTonalButton(
            onClick = onChangeTheme,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = accent.copy(alpha = 0.15f),
                contentColor = accent,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_palette),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.tier_info_change_theme),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/**
 * Balão explicativo dos 5 segmentos coloridos do anel de progresso.
 */
@Composable
internal fun RingInfoPopover(onDismiss: () -> Unit) {
    val rangeSuffix = stringResource(R.string.tier_info_range_suffix)
    val labels = listOf(
        R.string.ring_info_color_red,
        R.string.ring_info_color_orange,
        R.string.ring_info_color_green,
        R.string.ring_info_color_cyan,
        R.string.ring_info_color_purple,
    )
    val ranges = listOf("0 – 20%", "21 – 40%", "41 – 60%", "61 – 80%", "81 – 100%")

    TodayInfoPopover(
        onDismiss = onDismiss,
        title = stringResource(R.string.settings_about_progress_ring),
    ) {
        Text(
            text = stringResource(R.string.ring_info_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        labels.forEachIndexed { index, labelRes ->
            val swatch = Color(SegmentedRing.SEGMENT_COLORS[index])
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(swatch),
                    )
                }
                Column {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = ranges[index] + rangeSuffix,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Balão da data no topo da Hoje: exibe a data completa e quanto tempo falta até a meia-noite.
 */
@Composable
internal fun DayInfoPopover(onDismiss: () -> Unit) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Atualiza na virada de cada minuto cheio enquanto o balão estiver aberto
    LaunchedEffect(Unit) {
        while (true) {
            val current = LocalDateTime.now()
            val delayMillis = ((60 - current.second) * 1_000L - current.nano / 1_000_000L).coerceAtLeast(500L)
            delay(delayMillis)
            now = LocalDateTime.now()
        }
    }

    // Idioma lido da configuração: se o usuário trocar de idioma, o balão se atualiza.
    val locale = LocalConfiguration.current.locales[0]
    val fullDate = remember(now.toLocalDate(), locale) {
        now.toLocalDate()
            .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
            .replaceFirstChar { it.titlecase(locale) }
    }
    val visibleRemaining = remember(now, locale) {
        formatRemainingUntilMidnight(now, locale, MeasureFormat.FormatWidth.NARROW)
    }
    val spokenRemaining = remember(now, locale) {
        formatRemainingUntilMidnight(now, locale, MeasureFormat.FormatWidth.WIDE)
    }
    val visibleText = stringResource(R.string.today_day_remaining, visibleRemaining)
    val spokenText = stringResource(R.string.today_day_remaining, spokenRemaining)

    TodayInfoPopover(
        onDismiss = onDismiss,
        title = fullDate,
    ) {
        Text(
            text = visibleText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = spokenText },
        )
    }
}

/**
 * Calcula e formata o tempo restante até a meia-noite:
 * se faltar >= 1 hora, usa horas inteiras arredondadas para baixo;
 * caso contrário, minutos arredondados para cima (mínimo 1).
 */
private fun formatRemainingUntilMidnight(
    now: LocalDateTime,
    locale: Locale,
    width: MeasureFormat.FormatWidth,
): String {
    val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
    val secondsLeft = Duration.between(now, midnight).seconds.coerceAtLeast(1L)
    val measure = if (secondsLeft >= 3_600L) {
        Measure(secondsLeft / 3_600L, MeasureUnit.HOUR)
    } else {
        val minutes = ((secondsLeft + 59L) / 60L).coerceAtLeast(1L)
        Measure(minutes, MeasureUnit.MINUTE)
    }
    return MeasureFormat.getInstance(locale, width).format(measure)
}
