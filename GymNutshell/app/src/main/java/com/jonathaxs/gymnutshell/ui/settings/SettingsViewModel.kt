package com.jonathaxs.gymnutshell.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.GoalsCalculator
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.Profile
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel da SettingsView — gerencia estado e persistência das preferências,
 * perfil do usuário, objetivo e metas recalculadas.
 */
class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val profileRepo = ProfileRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)

    val accentColor: StateFlow<AccentColor> = settingsRepo.accentColor.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccentColor.Default,
    )

    val profile: StateFlow<Profile> = profileRepo.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Profile(),
    )

    val theme: StateFlow<AppTheme> = settingsRepo.theme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppTheme.Default,
    )

    val controlStyle: StateFlow<ControlStyle> = settingsRepo.controlStyle.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ControlStyle.Default,
    )

    val measurementSystem: StateFlow<MeasurementSystem> = settingsRepo.measurementSystem.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeasurementSystem.Metric,
    )

    val goalValueOverrides: StateFlow<Map<String, Int>> = goalConfigRepo.valueOverrides.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap(),
    )

    fun setAccent(color: AccentColor) {
        viewModelScope.launch { settingsRepo.setAccentColor(color) }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepo.setTheme(theme) }
    }

    fun setControlStyle(style: ControlStyle) {
        viewModelScope.launch { settingsRepo.setControlStyle(style) }
    }

    fun setMeasurement(system: MeasurementSystem) {
        viewModelScope.launch { settingsRepo.setMeasurementSystem(system) }
    }

    fun saveProfileBasics(name: String, sex: String, favoriteExercise: String) {
        viewModelScope.launch {
            // Lê do repositório: o StateFlow pode estar no valor inicial vazio.
            val current = profileRepo.profile.first()
            profileRepo.update(
                current.copy(
                    name = name.trim(),
                    sex = Profile.normalizedSex(sex),
                    favoriteExercise = favoriteExercise,
                ),
            )
        }
    }

    fun saveGoalWithRecalculate(newGoal: UserGoal, rescaledGoals: GoalsCalculator.Result) {
        viewModelScope.launch {
            // Lê do repositório: o StateFlow pode estar no valor inicial vazio.
            val current = profileRepo.profile.first()
            profileRepo.update(current.copy(goal = newGoal))
            goalConfigRepo.setGoalValue("tracking.protein", rescaledGoals.protein)
            goalConfigRepo.setGoalValue("tracking.carbs", rescaledGoals.carbs)
            goalConfigRepo.setGoalValue("tracking.goodFat", rescaledGoals.goodFat)
            goalConfigRepo.setGoalValue("tracking.fiber", rescaledGoals.fiber)
        }
    }

    fun saveGoalKeep(newGoal: UserGoal) {
        viewModelScope.launch {
            // Lê do repositório: o StateFlow pode estar no valor inicial vazio.
            val current = profileRepo.profile.first()
            profileRepo.update(current.copy(goal = newGoal))
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            profileRepo.resetOnboarding()
        }
    }
}
