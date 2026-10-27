package com.jonathaxs.gymnutshell.ui.today

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.R
// R do :core (onde vivem os nomes de tier). Os módulos têm R separado — `nonTransitiveRClass` é o
// default do AGP 8+, então o R do :app NÃO inclui os recursos da lib.
import com.jonathaxs.gymnutshell.core.R as CoreR
import com.jonathaxs.gymnutshell.health.HealthConnectManager
import com.jonathaxs.gymnutshell.notifications.GymNotifier
import com.jonathaxs.gymnutshell.notifications.NotificationScheduler
import com.jonathaxs.gymnutshell.core.data.CustomGoalCategoryRepository
import com.jonathaxs.gymnutshell.core.data.CustomGoalRepository
import com.jonathaxs.gymnutshell.core.data.DailyRecordRepository
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.HealthPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.IntakeRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.data.TodayPreferencesRepository
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.BuiltInGoals
import com.jonathaxs.gymnutshell.core.domain.CategoryItem
import com.jonathaxs.gymnutshell.core.domain.CategoryKind
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.domain.DailyRecordFactory
import com.jonathaxs.gymnutshell.core.domain.GoalCategory
import com.jonathaxs.gymnutshell.core.domain.GoalsProvider
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.NotificationKind
import com.jonathaxs.gymnutshell.core.domain.Profile
import com.jonathaxs.gymnutshell.core.domain.ProgressHelpers
import com.jonathaxs.gymnutshell.core.domain.TodayGoalsViewMode
import com.jonathaxs.gymnutshell.core.domain.UnifiedCategoryOrder
import com.jonathaxs.gymnutshell.core.domain.UnitConverter
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.roundToInt

/** Uma meta da TodayView pronta pra exibição. */
data class TodayGoalUi(
    val key: String,
    val emoji: String,
    val unit: String,
    val increment: Int,
    val intake: Int,
    val target: Int,
    val isRestDay: Boolean = false,
    val supportsRestDay: Boolean = false,
    val isCheckIn: Boolean = false,
    val category: GoalCategory? = null,
    /** Categoria personalizada à qual a meta pertence (null quando é fixa do app ou builtin). */
    val customCategoryId: String? = null,
    /** Título já resolvido (metas custom); null = built-in (UI resolve via string). */
    val title: String? = null,
    /** Água no sistema US: exibida em fl oz, mas armazenada em ml. */
    val unitIsFlOz: Boolean = false,
) {
    // Em dia de descanso a meta conta como 100%, sem precisar de intake.
    val progress: Double get() = if (isRestDay) 1.0 else ProgressHelpers.normalizedProgress(intake, target)
}

private data class PreferencesGroup(
    val measurement: MeasurementSystem,
    val theme: AppTheme,
    val controlStyle: ControlStyle,
    val goalsViewMode: TodayGoalsViewMode,
)

/**
 * Uma seção da Today: identidade da categoria ("builtin:<raw>"/"custom:<id>"), título (res p/ fixa
 * ou texto p/ personalizada), símbolo, metas e estado de colapso.
 */
data class TodayCategoryUi(
    val id: String,
    @StringRes val titleRes: Int?,
    val title: String?,
    val symbolName: String? = null,
    val goals: List<TodayGoalUi>,
    val collapsed: Boolean,
)

/** Estado completo da TodayView. */
data class TodayUiState(
    val dateLabel: String = "",
    val overallPercent: Int = 0,
    val tierEmoji: String = "🐓",
    /** Nível do tier (1–5) — controla o emoji e conquista no hero. */
    val tierLevel: Int = 1,
    /** Nome do nível no tema atual (ex.: "Monstro"), exibido sob o emoji do hero. */
    @param:StringRes val tierNameRes: Int = CoreR.string.tier_gym_level1,
    /** Percentual que falta para alcançar o próximo nível (null se no nível máximo). */
    val nextLevelPercent: Int? = null,
    /** Verdadeiro quando o usuário atingiu o nível máximo (nível 5). */
    val isAtMaxLevel: Boolean = false,
    /** Estilo global de controle das metas (Slider ou Stepper). */
    val controlStyle: ControlStyle = ControlStyle.Default,
    /** Modo de visualização no celular em retrato (Lista ou Grade com balão). */
    val goalsViewMode: TodayGoalsViewMode = TodayGoalsViewMode.Default,
    val overallProgress: Float = 0f,
    val sections: List<TodayCategoryUi> = emptyList(),
    /** Metas personalizadas sem categoria, exibidas no fim da lista. */
    val uncategorizedGoals: List<TodayGoalUi> = emptyList(),
)

/**
 * ViewModel da TodayView — porte (MVP) do estado da TodayView (iOS).
 * Junta perfil → metas (GoalsProvider) + intakes (persistidos) + colapso de categorias,
 * e calcula % geral e tier. As metas são agrupadas por categoria (Essencial primeiro).
 */
class TodayViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app.applicationContext)
    private val intakeRepo = IntakeRepository(app.applicationContext)
    private val todayPrefs = TodayPreferencesRepository(app.applicationContext)
    private val recordRepo = DailyRecordRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val customGoalRepo = CustomGoalRepository(app.applicationContext)
    private val customCategoryRepo = CustomGoalCategoryRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)
    private val notifier = GymNotifier(app.applicationContext)
    private val scheduler = NotificationScheduler(app.applicationContext)
    private val healthPrefs = HealthPreferencesRepository(app.applicationContext)
    private val healthConnect = HealthConnectManager(app.applicationContext)

    init {
        viewModelScope.launch {
            rolloverIfNeeded()
            // Após a virada (intakes zerados), tenta preencher Treino/Cardio com o que veio do Health Connect.
            checkWorkoutsFromHealth()
        }
    }

    /**
     * Vira o dia (porte de checkIfNewDay/finishSpecificDay do iOS): grava o DailyRecord do último
     * dia ativo com os intakes atuais, zera os intakes e preenche dias perdidos com Level1.
     * No primeiro uso, só marca hoje como dia ativo.
     */
    private suspend fun rolloverIfNeeded() {
        val today = LocalDate.now().toEpochDay()
        val last = intakeRepo.lastActiveDay()
        if (last == null) {
            intakeRepo.setLastActiveDay(today)
            return
        }
        if (last >= today) return

        val profile = profileRepo.profile.first()
        val effective = if (profile.weightKg <= 0.0) DEMO_PROFILE else profile
        val result = GoalsProvider.goals(effective)
        val theme = settingsRepo.theme.first()
        val restDays = intakeRepo.restDays.first()
        val customGoals = customGoalRepo.all()
        val config = goalConfigRepo.goalConfig.first()

        // 1) grava o último dia com os intakes que ficaram (tema + dias de descanso + metas custom + config)
        val intakes = intakeRepo.intakes.first()
        val finalized = DailyRecordFactory.build(last, intakes, result, theme, restDays, customGoals, config)
        recordRepo.upsert(finalized)
        // Grava o sono do dia que virou no Health Connect, se o usuário habilitou o sync (porte do writeSleepIfNeeded).
        val sleepHours = intakes["tracking.sleep"] ?: 0
        if (sleepHours > 0 && healthPrefs.syncSleepEnabled()) {
            if (healthConnect.writeSleep(LocalDate.ofEpochDay(last), sleepHours)) {
                notifier.fireHealthLogged(GymNotifier.HealthLogKind.Sleep, sleepHours)
            }
        }
        // Notifica a conquista do dia que virou (porte da notificação de meia-noite do iOS).
        val tier = DailyAchievement.from(finalized.percent / 100.0)
        val tierName = getApplication<Application>().getString(theme.tierNameRes(tier, profile.sex))
        notifier.fireAchievementUnlocked(tierName, finalized.achievementEmoji, last)
        // 2) zera os intakes pro novo dia
        intakeRepo.resetAllIntakes()
        // 3) preenche dias perdidos (last+1 .. today-1) com Level1
        var day = last + 1
        while (day < today) {
            if (recordRepo.findByDate(day) == null) recordRepo.upsert(DailyRecordFactory.missed(day, theme))
            day++
        }
        // 4) marca hoje como dia ativo
        intakeRepo.setLastActiveDay(today)
    }

    val uiState: StateFlow<TodayUiState> =
        combine(
            combine(profileRepo.profile, customGoalRepo.goals, customCategoryRepo.categories) { p, g, c ->
                Triple(p, g, c)
            },
            combine(goalConfigRepo.goalConfig, goalConfigRepo.categoryOrderIds) { config, ids -> config to ids },
            combine(intakeRepo.intakes, intakeRepo.restDays, todayPrefs.collapsedCategories) { i, r, c ->
                Triple(i, r, c)
            },
            combine(
                settingsRepo.measurementSystem,
                settingsRepo.theme,
                settingsRepo.controlStyle,
                settingsRepo.goalsViewMode,
            ) { m, t, c, v ->
                PreferencesGroup(m, t, c, v)
            },
        ) { (profile, customGoals, customCategories), (config, orderIds), (intakeMap, restDays, collapsed), prefsGroup ->
            // Enquanto não houver onboarding, usa um perfil-demo se nada foi salvo.
            val effective = if (profile.weightKg <= 0.0) DEMO_PROFILE else profile

            // Resolve se uma meta suporta dia de descanso pelo tipo da categoria (RestDay).
            fun supportsRest(category: GoalCategory?, customCategoryId: String?): Boolean =
                if (customCategoryId != null) {
                    customCategories.firstOrNull { it.id == customCategoryId }?.kind == CategoryKind.RestDay
                } else {
                    category?.kind == CategoryKind.RestDay
                }

            // Metas fixas ativas (ordem/removidas/overrides). Água em US é exibida em fl oz (armazenada em ml).
            val builtinGoals = BuiltInGoals.active(GoalsProvider.goals(effective), config).map { g ->
                val category = GoalCategory.defaultCategory(g.key)
                val waterUs = g.key == "tracking.water" && prefsGroup.measurement == MeasurementSystem.Us
                val storedMl = intakeMap[g.key] ?: 0
                TodayGoalUi(
                    key = g.key,
                    emoji = g.emoji,
                    unit = if (waterUs) "fl oz" else g.unit,
                    increment = if (waterUs) 8 else g.increment,
                    intake = if (waterUs) UnitConverter.mlToFlOz(storedMl.toDouble()).roundToInt() else storedMl,
                    target = if (waterUs) UnitConverter.mlToFlOz(g.target.toDouble()).roundToInt() else g.target,
                    isRestDay = g.key in restDays,
                    supportsRestDay = category?.kind == CategoryKind.RestDay,
                    isCheckIn = category?.kind == CategoryKind.CheckIn,
                    category = category,
                    unitIsFlOz = waterUs,
                )
            }
            // Metas personalizadas (key "custom:<id>", título = nome).
            val customGoalsUi = customGoals.map { c ->
                val category = GoalCategory.fromRaw(c.categoryRaw)
                TodayGoalUi(
                    key = c.intakeKey, emoji = c.emoji, unit = c.unit, increment = c.increment,
                    intake = intakeMap[c.intakeKey] ?: 0, target = c.target,
                    isRestDay = c.intakeKey in restDays,
                    supportsRestDay = supportsRest(category, c.customCategoryId),
                    isCheckIn = c.kind(customCategories) == CategoryKind.CheckIn,
                    category = category,
                    customCategoryId = c.customCategoryId,
                    title = c.name,
                )
            }

            // Agrupa pela ordem unificada de categorias (fixas + personalizadas); sem categoria vão no fim.
            val orderedCategories = UnifiedCategoryOrder.resolve(orderIds, customCategories)
            val sections = orderedCategories.mapNotNull { item ->
                when (item) {
                    is CategoryItem.Builtin -> {
                        val goals = builtinGoals.filter { it.category == item.category } +
                            customGoalsUi.filter { it.category == item.category && it.customCategoryId == null }
                        if (goals.isEmpty()) {
                            null
                        } else {
                            val symbol = when (item.category) {
                                GoalCategory.Essencial -> "heart.fill"
                                GoalCategory.Nutricao -> "fork.knife"
                                GoalCategory.Treino -> "dumbbell.fill"
                                GoalCategory.Suplemento -> "pills.fill"
                            }
                            TodayCategoryUi(
                                id = item.id,
                                titleRes = categoryTitleRes(item.category),
                                title = null,
                                symbolName = symbol,
                                goals = goals,
                                collapsed = item.id in collapsed,
                            )
                        }
                    }
                    is CategoryItem.Custom -> {
                        val goals = customGoalsUi.filter { it.customCategoryId == item.category.id }
                        if (goals.isEmpty()) {
                            null
                        } else {
                            TodayCategoryUi(
                                id = item.id,
                                titleRes = null,
                                title = item.category.name,
                                symbolName = item.category.symbolName,
                                goals = goals,
                                collapsed = item.id in collapsed,
                            )
                        }
                    }
                }
            }
            val uncategorized = customGoalsUi.filter { it.category == null && it.customCategoryId == null }

            val allGoals = builtinGoals + customGoalsUi
            val avg = if (allGoals.isEmpty()) 0.0 else allGoals.sumOf { it.progress } / allGoals.size
            val percent = floor(avg * 100).toInt()
            val tier = DailyAchievement.fromPercent(percent)
            val nextTier = tier.next
            val nextLevelPercent = nextTier?.let { it.minPercent - percent }
            val isAtMaxLevel = nextTier == null

            TodayUiState(
                dateLabel = AppDateFormatters.weekdayAndDay(
                    LocalDate.now(),
                    format = getApplication<Application>().getString(com.jonathaxs.gymnutshell.core.R.string.date_weekday_day_format),
                ),
                overallPercent = percent,
                tierEmoji = prefsGroup.theme.emoji(tier, profile.sex),
                tierLevel = tier.number,
                tierNameRes = prefsGroup.theme.tierNameRes(tier, profile.sex),
                nextLevelPercent = nextLevelPercent,
                isAtMaxLevel = isAtMaxLevel,
                controlStyle = prefsGroup.controlStyle,
                goalsViewMode = prefsGroup.goalsViewMode,
                overallProgress = avg.toFloat(),
                sections = sections,
                uncategorizedGoals = uncategorized,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** Alterna o modo de visualização das metas entre lista e grade. */
    fun toggleGoalsViewMode() {
        viewModelScope.launch {
            val current = uiState.value.goalsViewMode
            val next = if (current == TodayGoalsViewMode.List) {
                TodayGoalsViewMode.Grid
            } else {
                TodayGoalsViewMode.List
            }
            settingsRepo.setGoalsViewMode(next)
        }
    }

    // Define o valor da meta (vindo do slider), limitado entre 0 e o alvo.
    // Água em fl oz é convertida pra ml ao salvar (o armazenamento é sempre métrico).
    fun updateIntake(goal: TodayGoalUi, displayValue: Int) {
        val clamped = displayValue.coerceIn(0, goal.target)
        val toStore = if (goal.unitIsFlOz) UnitConverter.flOzToMl(clamped.toDouble()).roundToInt() else clamped
        viewModelScope.launch {
            intakeRepo.setIntake(goal.key, toStore)
            rescheduleReminder(goal.key)
        }
    }

    fun toggleCategory(id: String) {
        viewModelScope.launch { todayPrefs.toggleCategory(id) }
    }

    fun toggleRestDay(goal: TodayGoalUi) {
        viewModelScope.launch {
            intakeRepo.toggleRestDay(goal.key)
            rescheduleReminder(goal.key)
        }
    }

    /**
     * Re-checa os treinos do Health Connect ao voltar pra tela (porte do refresh no scenePhase do iOS).
     * Chamado no ON_RESUME da Today: pega o treino que o usuário registrou em outro app enquanto o
     * Hoje ficava em background. Idempotente — reusa [checkWorkoutsFromHealth], que só preenche zeradas.
     */
    fun refreshWorkoutsFromHealth() {
        viewModelScope.launch { checkWorkoutsFromHealth() }
    }

    /**
     * Auto check-in dos treinos do Health Connect — porte de checkWorkoutsFromHealth (iOS).
     * Lê os treinos de hoje e preenche as metas de Treino/Cardio só quando ainda estão zeradas
     * (nunca sobrescreve o que o usuário já registrou). Gateado pelo toggle `autoWorkoutCheckin`.
     */
    private suspend fun checkWorkoutsFromHealth() {
        if (!healthPrefs.autoWorkoutCheckin()) return
        val summary = healthConnect.readTodayWorkouts()
        if (summary.workoutMinutes == 0 && summary.cardioMinutes == 0) return

        val intakes = intakeRepo.intakes.first()
        val profile = profileRepo.profile.first()
        val effective = if (profile.weightKg <= 0.0) DEMO_PROFILE else profile
        val config = goalConfigRepo.goalConfig.first()
        val targets = BuiltInGoals.active(GoalsProvider.goals(effective), config).associate { it.key to it.target }

        // Treino de musculação (força): só preenche se a meta está zerada.
        if ((intakes["tracking.workout"] ?: 0) == 0) {
            val value = summary.workoutMinutes.coerceAtMost(targets["tracking.workout"] ?: 0)
            if (value > 0) {
                intakeRepo.setIntake("tracking.workout", value)
                val name = summary.workoutNameRes?.let { getApplication<Application>().getString(it) }
                notifier.fireHealthLogged(GymNotifier.HealthLogKind.Workout, value, name)
            }
        }
        // Cardio: idem.
        if ((intakes["tracking.cardio"] ?: 0) == 0) {
            val value = summary.cardioMinutes.coerceAtMost(targets["tracking.cardio"] ?: 0)
            if (value > 0) {
                intakeRepo.setIntake("tracking.cardio", value)
                val name = summary.cardioNameRes?.let { getApplication<Application>().getString(it) }
                notifier.fireHealthLogged(GymNotifier.HealthLogKind.Cardio, value, name)
            }
        }
    }

    /** Re-arma (ou cancela) o lembrete da meta quando o intake/descanso muda. */
    private suspend fun rescheduleReminder(goalKey: String) {
        val kind = NotificationKind.fromTrackingKey(goalKey)
        if (kind != null) {
            scheduler.reschedule(kind)
        } else if (goalKey.startsWith("custom:")) {
            goalKey.removePrefix("custom:").toLongOrNull()?.let { scheduler.rescheduleCustom(it) }
        }
    }

    companion object {
        // Perfil-demo temporário até a tela de Welcome existir (Fase 4).
        private val DEMO_PROFILE = Profile(
            weightKg = 80.0, heightCm = 180, age = 30, sex = "male", goal = UserGoal.Maintenance,
        )
    }
}
