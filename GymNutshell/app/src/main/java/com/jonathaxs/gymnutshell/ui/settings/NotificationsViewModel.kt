package com.jonathaxs.gymnutshell.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.core.data.CustomGoal
import com.jonathaxs.gymnutshell.core.data.CustomGoalCategoryRepository
import com.jonathaxs.gymnutshell.core.data.CustomGoalRepository
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.NotificationPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.SupplementCheckInRepository
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.NotificationKind
import com.jonathaxs.gymnutshell.core.domain.ReminderTime
import com.jonathaxs.gymnutshell.notifications.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da tela de Notificações: quais kinds estão ligados + horários + metas personalizadas. */
data class NotificationsUiState(
    /** Chave = rawValue do kind (ex.: "water") ou "custom.<id>". */
    val enabled: Map<String, Boolean> = emptyMap(),
    val reminderTimes: Map<String, List<ReminderTime>> = emptyMap(),
    val groupSupplements: Boolean = false,
    val customGoals: List<CustomGoal> = emptyList(),
    val customCheckInIds: Set<Long> = emptySet(),
    val hasCheckInGoals: Boolean = true,
) {
    fun isEnabled(kind: NotificationKind): Boolean = enabled[kind.rawValue] ?: false
    fun isCustomEnabled(id: Long): Boolean = enabled["custom.$id"] ?: false
    fun isCustomCheckIn(id: Long): Boolean = id in customCheckInIds
    fun timesFor(kind: NotificationKind): List<ReminderTime> = reminderTimes[kind.rawValue].orEmpty()
    fun timesForCustom(id: Long): List<ReminderTime> = reminderTimes["custom.$id"].orEmpty()
}

/**
 * ViewModel da tela de Notificações — porte de NotificationsSettingsView / GoalsNotificationsSettingsView (iOS).
 * Liga/desliga cada kind, gerencia o agrupamento de suplementos e re-agenda via [NotificationScheduler].
 */
class NotificationsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = NotificationPreferencesRepository(app.applicationContext)
    private val customRepo = CustomGoalRepository(app.applicationContext)
    private val customCategoryRepo = CustomGoalCategoryRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)
    private val scheduler = NotificationScheduler(app.applicationContext)

    val state: StateFlow<NotificationsUiState> =
        combine(
            combine(prefs.enabledStates, prefs.reminderTimes, prefs.groupSupplements) { enabled, times, grouped ->
                Triple(enabled, times, grouped)
            },
            customRepo.goals,
            customCategoryRepo.categories,
            goalConfigRepo.removedKeys,
        ) { (enabled, times, grouped), goals, categories, removedKeys ->
            val checkInIds = goals
                .filter { it.kind(categories) == CategoryKind.CheckIn }
                .map { it.id }
                .toSet()
            val hasCheckIn =
                SupplementCheckInRepository.CREATINE_STORAGE_KEY !in removedKeys || checkInIds.isNotEmpty()
            NotificationsUiState(
                enabled = enabled,
                reminderTimes = times,
                groupSupplements = grouped,
                customGoals = goals,
                customCheckInIds = checkInIds,
                hasCheckInGoals = hasCheckIn,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationsUiState())

    /** Liga/desliga um kind fixo e re-agenda (se for baseado em intervalo ou horário). */
    fun setEnabled(kind: NotificationKind, value: Boolean) {
        viewModelScope.launch {
            prefs.setEnabled(value, kind)
            if (kind.isIntervalBased) {
                if (kind == NotificationKind.Creatine || value) {
                    scheduler.reschedule(kind)
                } else {
                    scheduler.cancel(kind)
                }
            }
        }
    }

    /** Liga/desliga o lembrete de uma meta personalizada e re-agenda. */
    fun setCustomEnabled(id: Long, value: Boolean) {
        viewModelScope.launch {
            prefs.setCustomEnabled(value, id)
            if (id in state.value.customCheckInIds || value) {
                scheduler.rescheduleCustom(id)
            } else {
                scheduler.cancelCustom(id)
            }
        }
    }

    /** Liga/desliga o agrupamento de suplementos do mesmo horário e re-agenda. */
    fun setGroupSupplements(value: Boolean) {
        viewModelScope.launch {
            prefs.setGroupSupplements(value)
            scheduler.rescheduleSupplementReminders()
        }
    }

    /** Chamado quando o usuário concede a permissão pela tela: aplica defaults e re-arma tudo. */
    fun onPermissionGranted() {
        viewModelScope.launch {
            prefs.applyDefaultEnabledKindsOnce()
            scheduler.rescheduleAllActive()
        }
    }
}
