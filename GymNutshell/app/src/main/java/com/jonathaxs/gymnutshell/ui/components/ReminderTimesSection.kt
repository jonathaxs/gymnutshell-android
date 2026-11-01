package com.jonathaxs.gymnutshell.ui.components

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.ReminderTime
import com.jonathaxs.gymnutshell.notifications.NotificationStrings
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Seção reutilizável de horários dos lembretes de uma meta check-in (suplemento) — porte de
 * ReminderTimesSection (iOS). Lista os horários (tocar abre o TimePicker do Material 3),
 * exibe os 5 atalhos em FilterChips e permite adicionar/remover horários.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimesSection(
    times: List<ReminderTime>,
    onTimesChange: (List<ReminderTime>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    fun togglePreset(time: ReminderTime) {
        if (time in times) {
            onTimesChange(times.filterNot { it == time })
        } else {
            onTimesChange((times + time).distinct().sorted())
        }
    }

    fun addNextFreeTime() {
        var candidate = times.maxOrNull()?.let {
            ReminderTime(hour = (it.hour + 1).coerceAtMost(23), minute = it.minute)
        } ?: ReminderTime(hour = 8, minute = 0)
        while (candidate in times && candidate.hour < 23) {
            candidate = ReminderTime(hour = candidate.hour + 1, minute = candidate.minute)
        }
        if (candidate !in times) {
            onTimesChange((times + candidate).distinct().sorted())
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.settings_notifications_times_header),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column {
                times.forEachIndexed { index, time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button) { editingIndex = index }
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_notifications_times_row),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                text = time.formatted(context),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                onTimesChange(times.filterIndexed { i, _ -> i != index })
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }

                // Atalhos: um toque adiciona ou remove o horário; o que já está na lista fica selecionado.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReminderTime.presets.forEach { preset ->
                        val selected = preset.time in times
                        val presetTitle = stringResource(NotificationStrings.presetTitleRes(preset))
                        FilterChip(
                            selected = selected,
                            onClick = { togglePreset(preset.time) },
                            label = { Text("$presetTitle ${preset.time.formatted(context)}") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selected) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { addNextFreeTime() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.settings_notifications_times_add),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.settings_notifications_times_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
        )
    }

    val currentIndex = editingIndex
    if (currentIndex != null && currentIndex in times.indices) {
        val current = times[currentIndex]
        val pickerState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text(stringResource(R.string.settings_notifications_times_row)) },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = times.toMutableList()
                        if (currentIndex in updated.indices) {
                            updated[currentIndex] = ReminderTime(
                                hour = pickerState.hour,
                                minute = pickerState.minute,
                            )
                            onTimesChange(updated.distinct().sorted())
                        }
                        editingIndex = null
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text(stringResource(R.string.backup_cancel))
                }
            },
        )
    }
}

/** Formata o horário respeitando a preferência 12h/24h do aparelho. */
fun ReminderTime.formatted(context: Context): String =
    if (DateFormat.is24HourFormat(context)) {
        storageString
    } else {
        toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

/** Resumo dos horários ("08:00 · 22:30") ou o texto de quando não há nenhum horário. */
fun reminderTimesSummary(context: Context, times: List<ReminderTime>): String {
    if (times.isEmpty()) return context.getString(R.string.settings_notifications_times_empty)
    return times.sorted().joinToString(" · ") { it.formatted(context) }
}
