package com.jonathaxs.gymnutshell.ui.settings

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.NotificationPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.domain.BuiltInGoals
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.core.domain.GoalsProvider
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.NotificationKind
import com.jonathaxs.gymnutshell.core.domain.Profile
import com.jonathaxs.gymnutshell.core.domain.ReminderTime
import com.jonathaxs.gymnutshell.core.domain.UnitConverter
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Estado da tela de detalhe de uma meta fixa (valores já em unidade de exibição). */
data class GoalDetailState(
    val loaded: Boolean = false,
    val emoji: String = "",
    @StringRes val titleRes: Int = R.string.app_name,
    val unit: String = "",
    val value: Int = 0,
    val increment: Int = 1,
    val waterUs: Boolean = false,
    val accentArgb: Long = 0xFF007AFF,
    val reminderTimes: List<ReminderTime> = emptyList(),
)

/**
 * ViewModel da tela de detalhe de meta fixa — porte de TrackingGoalDetailView (iOS).
 * Lê o valor/passo atuais (override ou calculado), exibe na unidade do usuário e grava o override no salvar.
 */
class GoalDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)
    private val notifPrefs = NotificationPreferencesRepository(app.applicationContext)
    private val scheduler = NotificationScheduler(app.applicationContext)

    private val _state = MutableStateFlow(GoalDetailState())
    val state: StateFlow<GoalDetailState> = _state.asStateFlow()

    fun load(key: String) {
        if (_state.value.loaded) return
        viewModelScope.launch {
            val profile = profileRepo.profile.first()
            val effective = if (profile.weightKg <= 0.0) DEMO_PROFILE else profile
            val base = BuiltInGoals.forResult(GoalsProvider.goals(effective)).firstOrNull { it.key == key } ?: return@launch
            val waterUs = key == "tracking.water" && settingsRepo.measurementSystem.first() == MeasurementSystem.Us

            val rawValue = goalConfigRepo.valueOverrides.first()[key] ?: base.target
            val rawIncrement = goalConfigRepo.incrementOverrides.first()[key] ?: base.increment
            val isCheckIn = GoalCategory.defaultCategory(key)?.kind == CategoryKind.CheckIn
            val loadedTimes = if (isCheckIn) {
                notifPrefs.reminderTimes(NotificationPreferencesRepository.CREATINE_REMINDER_ID)
            } else {
                emptyList()
            }

            _state.value = GoalDetailState(
                loaded = true,
                emoji = base.emoji,
                titleRes = GoalsViewModel.goalTitleRes(key),
                unit = if (waterUs) "fl oz" else base.unit,
                value = if (waterUs) UnitConverter.mlToFlOz(rawValue.toDouble()).roundToInt() else rawValue,
                increment = if (waterUs) UnitConverter.mlToFlOz(rawIncrement.toDouble()).roundToInt().coerceAtLeast(1) else rawIncrement,
                waterUs = waterUs,
                accentArgb = settingsRepo.accentColor.first().argb,
                reminderTimes = loadedTimes,
            )
        }
    }

    /** Grava os overrides; a água em US converte de volta pra ml (o armazenamento é sempre métrico). */
    fun save(key: String, value: Int, increment: Int, reminderTimes: List<ReminderTime> = emptyList()) {
        val waterUs = _state.value.waterUs
        val loadedTimes = _state.value.reminderTimes
        viewModelScope.launch {
            goalConfigRepo.setGoalValue(
                key,
                if (waterUs) UnitConverter.flOzToMl(value.toDouble()).roundToInt() else value,
            )
            goalConfigRepo.setGoalIncrement(
                key,
                if (waterUs) UnitConverter.flOzToMl(increment.toDouble()).roundToInt() else increment,
            )
            val isCheckIn = GoalCategory.defaultCategory(key)?.kind == CategoryKind.CheckIn
            if (isCheckIn && reminderTimes.distinct().sorted() != loadedTimes.distinct().sorted()) {
                val id = NotificationPreferencesRepository.CREATINE_REMINDER_ID
                notifPrefs.setReminderTimes(reminderTimes, id)
                notifPrefs.setEnabled(reminderTimes.isNotEmpty(), NotificationKind.Creatine)
                scheduler.reschedule(NotificationKind.Creatine)
            }
        }
    }

    companion object {
        private val DEMO_PROFILE = Profile(
            weightKg = 80.0, heightCm = 180, age = 30, sex = "male", goal = UserGoal.Maintenance,
        )
    }
}
