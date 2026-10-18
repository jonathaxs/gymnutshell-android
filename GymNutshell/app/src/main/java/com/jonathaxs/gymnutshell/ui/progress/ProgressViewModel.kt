package com.jonathaxs.gymnutshell.ui.progress

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.core.data.CustomGoalRepository
import com.jonathaxs.gymnutshell.core.data.DailyRecord
import com.jonathaxs.gymnutshell.core.data.DailyRecordRepository
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.domain.GoalOrder
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Contagem de dias num tier. */
/** `nameRes` = nome do nível no tema atual (ex.: "Gym Rat"); vem do `AppTheme.tierNameRes`. */
data class TierCountUi(val emoji: String, val level: Int, val days: Int, @param:StringRes val nameRes: Int)

/** Uma célula da grade "Últimos 7 dias". `emoji` nulo = dia sem registro (mostra um ponto). */
data class RecentDayUi(
    val epochDay: Long,
    val dayNumber: Int,
    val weekday: String,
    val emoji: String?,
    val isToday: Boolean,
)

/** Estado da ProgressView — porte da ProgressOverView 2.0 (sem pontos, sem bônus, sem dados físicos). */
data class ProgressUiState(
    val tierCounts: List<TierCountUi> = emptyList(),
    val workoutDays: Int = 0,
    val cardioDays: Int = 0,
    val activeGoals: Int = 0,
    val userGoal: UserGoal = UserGoal.Maintenance,
    val recentDays: List<RecentDayUi> = emptyList(),
    val accentArgb: Long = 0xFF007AFF,
)

/**
 * ViewModel da ProgressView — porte da ProgressOverView (iOS).
 * Deriva estatísticas dos DailyRecord (Room) e do perfil/configurações:
 * últimos 7 dias, distribuição por tier (5 níveis), atividade e metas ativas.
 */
class ProgressViewModel(app: Application) : AndroidViewModel(app) {

    private val recordRepo = DailyRecordRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val profileRepo = ProfileRepository(app.applicationContext)
    private val customGoalRepo = CustomGoalRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)

    // Perfil + metas customizadas + metas removidas agrupados num fluxo só.
    private val extras = combine(
        profileRepo.profile,
        customGoalRepo.goals,
        goalConfigRepo.removedKeys,
    ) { profile, customGoals, removedKeys -> Triple(profile, customGoals, removedKeys) }

    val uiState: StateFlow<ProgressUiState> =
        combine(
            recordRepo.records,
            settingsRepo.theme,
            settingsRepo.accentColor,
            extras,
        ) { records, theme, accent, extra ->
            val (profile, customGoals, removedKeys) = extra
            val countByTier = records.groupingBy {
                if (it.achievementEmoji.isNotEmpty()) DailyAchievement.from(it.achievementEmoji)
                else DailyAchievement.fromPercent(it.percent)
            }.eachCount()

            // Metas fixas ativas (GoalOrder.DEFAULT menos as removidas) + metas personalizadas (porte do iOS).
            val activeBuiltinCount = GoalOrder.DEFAULT.count { it !in removedKeys }
            val activeGoals = activeBuiltinCount + customGoals.size

            ProgressUiState(
                tierCounts = DailyAchievement.entries.map { tier ->
                    TierCountUi(
                        emoji = theme.emoji(tier, profile.sex),
                        level = tier.number,
                        days = countByTier[tier] ?: 0,
                        nameRes = theme.tierNameRes(tier, profile.sex),
                    )
                },
                workoutDays = records.count { it.didWorkout },
                cardioDays = records.count { it.didCardio },
                activeGoals = activeGoals,
                userGoal = profile.goal,
                recentDays = recentDaysOf(records, theme, profile.sex),
                accentArgb = accent.argb,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    /** Pré-define o modo Lista antes de abrir Conquistas — porte do openAchievementsList() do iOS (toque em Atividade). */
    fun prepareAchievementsList() {
        viewModelScope.launch { settingsRepo.setAchievementsFilterMode("list") }
    }

    /** Pré-define o modo Calendário no dia tocado antes de abrir Conquistas — porte do jumpToAchievements(date:) do iOS. */
    fun prepareAchievementsDay(epochDay: Long) {
        viewModelScope.launch {
            settingsRepo.setAchievementsFilterMode("calendar")
            settingsRepo.setAchievementsSelectedDay(epochDay)
        }
    }

    /** Últimos 7 dias do calendário (mais antigo → hoje), cada um com o emoji do tier daquele dia. */
    private fun recentDaysOf(
        records: List<DailyRecord>,
        theme: AppTheme,
        sex: String,
    ): List<RecentDayUi> {
        val todayEpoch = LocalDate.now().toEpochDay()
        val byDate = records.associateBy { it.date }
        return (6 downTo 0).map { offset ->
            val epoch = todayEpoch - offset
            val date = LocalDate.ofEpochDay(epoch)
            val record = byDate[epoch]
            val hasRecord = record != null && record.percent > 0
            val tier = record?.let {
                if (it.achievementEmoji.isNotEmpty()) DailyAchievement.from(it.achievementEmoji)
                else DailyAchievement.fromPercent(it.percent)
            }
            RecentDayUi(
                epochDay = epoch,
                dayNumber = date.dayOfMonth,
                weekday = AppDateFormatters.weekdayInitial(date),
                emoji = if (hasRecord && tier != null) theme.emoji(tier, sex) else null,
                isToday = epoch == todayEpoch,
            )
        }
    }
}
