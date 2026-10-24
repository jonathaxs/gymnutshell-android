package com.jonathaxs.gymnutshell.ui.welcome

import android.app.Application
import android.net.Uri
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.core.backup.BackupService
import com.jonathaxs.gymnutshell.core.data.GoalConfigRepository
import com.jonathaxs.gymnutshell.core.data.HealthPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.NotificationPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.GoalsCalculator
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.Profile
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.health.HealthConnectManager
import com.jonathaxs.gymnutshell.notifications.NotificationScheduler
import com.jonathaxs.gymnutshell.ui.theme.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado completo do onboarding Welcome 2.0 — porte de WelcomeView (iOS).
 * Reúne seleção de sexo, exercício favorito, objetivo, medidas, metas calculadas/editadas,
 * controles, tema e permissões.
 */
data class WelcomeUiState(
    val currentStep: WelcomeStep = WelcomeStep.Start,
    val sex: String = "male",
    val favoriteExercise: FavoriteExercise = FavoriteExercise.Unknown,
    val userGoal: UserGoal = UserGoal.Maintenance,
    val measurementSystem: MeasurementSystem = MeasurementSystem.detectDefault(),
    val goals: GoalsCalculator.Result = GoalsCalculator.calculate("male", UserGoal.Maintenance),
    val includeFats: Boolean = false,
    val includeCreatine: Boolean = false,
    val controlStyle: ControlStyle = ControlStyle.Default,
    val theme: AppTheme = AppTheme.Gym,
    val summaryScrolledToEnd: Boolean = false,
    val notificationsPermissionState: WelcomePermissionState = WelcomePermissionState.Idle,
    val healthPermissionState: WelcomePermissionState = WelcomePermissionState.Idle,
    val isHealthAvailable: Boolean = false,
    val restoreFailed: Boolean = false,
) {
    /** Exibe a etapa de unidades só quando o país requer escolha (EUA, GB, etc.). */
    val showsUnitsStep: Boolean = MeasurementSystem.needsChoice()
    val visibleSteps: List<WelcomeStep> = WelcomeStep.visible(showsUnitsStep)
    val currentStepIndex: Int = visibleSteps.indexOf(currentStep).coerceAtLeast(0)

    /** Cor de destaque inicial baseada no sexo. */
    val sexColor: Color = AccentColor.defaultForSex(sex).color

    /**
     * Cor da etapa: nas etapas de objetivo e de metas segue o objetivo escolhido;
     * nas demais segue o sexo. Pinta o fundo, a barra de progresso e os botões do rodapé.
     */
    val stepColor: Color = if (currentStep == WelcomeStep.Goal || currentStep == WelcomeStep.Summary) {
        goalColor(userGoal)
    } else {
        sexColor
    }

    /** Validação da etapa atual: no resumo, exige scroll até o final. */
    val canContinue: Boolean = when (currentStep) {
        WelcomeStep.Start -> true
        WelcomeStep.About -> true
        WelcomeStep.Goal -> true
        WelcomeStep.Units -> true
        WelcomeStep.Summary -> summaryScrolledToEnd
        WelcomeStep.ControlStyle -> true
        WelcomeStep.Theme -> true
        WelcomeStep.Ready -> true
    }

    val isLastStep: Boolean = currentStep == WelcomeStep.Ready
}

/**
 * ViewModel do Welcome 2.0 — porte de WelcomeView (iOS) para MVVM com StateFlow.
 * Gerencia navegação entre etapas, edições das metas, persistência final e restauração de backup.
 */
class WelcomeViewModel(app: Application) : AndroidViewModel(app) {

    private val profileRepo = ProfileRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val goalConfigRepo = GoalConfigRepository(app.applicationContext)
    private val backupService = BackupService(app.applicationContext)
    private val notificationPrefs = NotificationPreferencesRepository(app.applicationContext)
    private val notificationScheduler = NotificationScheduler(app.applicationContext)
    private val healthPrefs = HealthPreferencesRepository(app.applicationContext)
    private val healthManager = HealthConnectManager(app.applicationContext)

    private val _state = MutableStateFlow(
        WelcomeUiState(isHealthAvailable = healthManager.isAvailable)
    )
    val state: StateFlow<WelcomeUiState> = _state.asStateFlow()

    init {
        refreshPermissions()
    }

    val healthPermissions: Set<String>
        get() = healthManager.allPermissions

    // MARK: - Navegação

    /** Avança para a próxima etapa visível ou conclui se estiver na etapa final. */
    fun advance() {
        val s = _state.value
        val nextStep = when (s.currentStep) {
            WelcomeStep.Start -> WelcomeStep.About
            WelcomeStep.About -> WelcomeStep.Goal
            WelcomeStep.Goal -> {
                // Ao sair de objetivo, recalcula as metas padrão pela tabela
                _state.update {
                    it.copy(
                        goals = GoalsCalculator.calculate(it.sex, it.userGoal),
                        summaryScrolledToEnd = false,
                    )
                }
                if (s.showsUnitsStep) WelcomeStep.Units else WelcomeStep.Summary
            }
            WelcomeStep.Units -> WelcomeStep.Summary
            WelcomeStep.Summary -> WelcomeStep.ControlStyle
            WelcomeStep.ControlStyle -> WelcomeStep.Theme
            WelcomeStep.Theme -> WelcomeStep.Ready
            WelcomeStep.Ready -> {
                complete()
                return
            }
        }
        _state.update { it.copy(currentStep = nextStep) }
    }

    /** Volta para a etapa anterior visível. */
    fun goBack() {
        val s = _state.value
        val idx = s.currentStepIndex
        if (idx > 0) {
            val prev = s.visibleSteps[idx - 1]
            _state.update { it.copy(currentStep = prev) }
        }
    }

    // MARK: - Modificadores de Estado

    fun setSex(sex: String) {
        val normalized = Profile.normalizedSex(sex)
        _state.update { current ->
            current.copy(
                sex = normalized,
                goals = GoalsCalculator.calculate(normalized, current.userGoal),
            )
        }
    }

    fun setFavoriteExercise(exercise: FavoriteExercise) {
        _state.update { it.copy(favoriteExercise = exercise) }
    }

    fun setUserGoal(goal: UserGoal) {
        _state.update { current ->
            current.copy(
                userGoal = goal,
                goals = GoalsCalculator.calculate(current.sex, goal),
            )
        }
    }

    fun setMeasurementSystem(system: MeasurementSystem) {
        _state.update { it.copy(measurementSystem = system) }
    }

    fun setControlStyle(style: ControlStyle) {
        _state.update { it.copy(controlStyle = style) }
    }

    fun setTheme(theme: AppTheme) {
        _state.update { it.copy(theme = theme) }
    }

    fun setSummaryScrolledToEnd(scrolled: Boolean) {
        _state.update { it.copy(summaryScrolledToEnd = scrolled) }
    }

    fun setIncludeFats(include: Boolean) {
        _state.update { it.copy(includeFats = include) }
    }

    fun setIncludeCreatine(include: Boolean) {
        _state.update { it.copy(includeCreatine = include) }
    }

    // MARK: - Ajustes de Metas com − / + (passos e limites do iOS)

    fun adjustSleep(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(sleep = (it.goals.sleep + delta).coerceIn(1, 16))) }
    }

    fun adjustWater(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(water = (it.goals.water + delta).coerceIn(250, 10000))) }
    }

    fun adjustProtein(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(protein = (it.goals.protein + delta).coerceIn(10, 500))) }
    }

    fun adjustCarbs(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(carbs = (it.goals.carbs + delta).coerceIn(10, 1000))) }
    }

    fun adjustFiber(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(fiber = (it.goals.fiber + delta).coerceIn(5, 100))) }
    }

    fun adjustGoodFat(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(goodFat = (it.goals.goodFat + delta).coerceIn(5, 300))) }
    }

    fun adjustWorkout(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(workout = (it.goals.workout + delta).coerceIn(5, 300))) }
    }

    fun adjustCardio(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(cardio = (it.goals.cardio + delta).coerceIn(5, 180))) }
    }

    fun adjustCreatine(delta: Int) {
        _state.update { it.copy(goals = it.goals.copy(creatine = (it.goals.creatine + delta).coerceIn(1, 20))) }
    }

    // MARK: - Permissões

    /**
     * Reavalia o estado real das permissões de notificações e Health Connect,
     * atualizando para Allowed se já concedidas no sistema operacional.
     */
    fun refreshPermissions() {
        viewModelScope.launch {
            val notifGranted = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
            val healthGranted = if (healthManager.isAvailable) {
                healthManager.grantedPermissions().containsAll(healthManager.allPermissions)
            } else false

            _state.update { current ->
                current.copy(
                    notificationsPermissionState = if (notifGranted) WelcomePermissionState.Allowed else current.notificationsPermissionState,
                    healthPermissionState = if (healthGranted) WelcomePermissionState.Allowed else current.healthPermissionState,
                )
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        val actuallyGranted = NotificationManagerCompat.from(getApplication()).areNotificationsEnabled() || granted
        _state.update {
            it.copy(notificationsPermissionState = if (actuallyGranted) WelcomePermissionState.Allowed else WelcomePermissionState.Denied)
        }
        if (actuallyGranted) {
            viewModelScope.launch {
                notificationPrefs.applyDefaultEnabledKindsOnce()
                notificationScheduler.rescheduleAllActive()
            }
        }
    }

    fun onHealthPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            val allGranted = healthManager.grantedPermissions().containsAll(healthManager.allPermissions)
            val isAllowed = allGranted || granted
            _state.update {
                it.copy(healthPermissionState = if (isAllowed) WelcomePermissionState.Allowed else WelcomePermissionState.Requested)
            }
            if (isAllowed) {
                healthPrefs.setSyncSleepEnabled(true)
                healthPrefs.setAutoWorkoutCheckin(true)
            }
        }
    }

    // MARK: - Conclusão do Onboarding

    /**
     * Salva o perfil, sistema de medidas, as 9 metas calculadas/editadas, o estilo de controle,
     * o tema, a cor de destaque, marca as opcionais excluídas como removidas e re-agenda as notificações.
     */
    fun complete() {
        // A Hoje mostra as categorias entrando de baixo, uma vez só, logo depois da Welcome.
        com.jonathaxs.gymnutshell.ui.today.GoalsEntrance.pending = true
        // E o anel do hero enche passando pelas cores e volta ao progresso real.
        com.jonathaxs.gymnutshell.ui.today.RingCelebration.pending = true
        val s = _state.value
        viewModelScope.launch {
            // Perfil com sexo normalizado e exercício favorito
            profileRepo.update(
                Profile(
                    name = "",
                    sex = Profile.normalizedSex(s.sex),
                    goal = s.userGoal,
                    favoriteExercise = s.favoriteExercise.rawValue,
                )
            )

            // Sistema de medidas
            settingsRepo.setMeasurementSystem(s.measurementSystem)

            // Salva todas as 9 metas diárias
            goalConfigRepo.setGoalValue("tracking.sleep", s.goals.sleep)
            goalConfigRepo.setGoalValue("tracking.water", s.goals.water)
            goalConfigRepo.setGoalValue("tracking.protein", s.goals.protein)
            goalConfigRepo.setGoalValue("tracking.carbs", s.goals.carbs)
            goalConfigRepo.setGoalValue("tracking.fiber", s.goals.fiber)
            goalConfigRepo.setGoalValue("tracking.goodFat", s.goals.goodFat)
            goalConfigRepo.setGoalValue("tracking.workout", s.goals.workout)
            goalConfigRepo.setGoalValue("tracking.cardio", s.goals.cardio)
            goalConfigRepo.setGoalValue("tracking.creatine", s.goals.creatine)

            // Metas opcionais: se incluídas restaura, se não incluídas remove
            if (s.includeFats) goalConfigRepo.restore("tracking.goodFat") else goalConfigRepo.remove("tracking.goodFat")
            if (s.includeCreatine) goalConfigRepo.restore("tracking.creatine") else goalConfigRepo.remove("tracking.creatine")

            // Estilo de controle e tema
            settingsRepo.setControlStyle(s.controlStyle)
            settingsRepo.setTheme(s.theme)

            // Cor de destaque inicial baseada no sexo
            settingsRepo.setAccentColor(AccentColor.defaultForSex(s.sex))

            // Re-agenda notificações
            notificationScheduler.rescheduleAllActive()

            // Marca o onboarding como concluído
            profileRepo.setOnboardingComplete()
        }
    }

    // MARK: - Restauração de Backup

    fun restore(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().decodeToString()
                    } ?: error("sem stream de entrada")
                }
                backupService.importJson(text)
            }.isSuccess
            if (ok) {
                profileRepo.setOnboardingComplete()
            } else {
                _state.update { it.copy(restoreFailed = true) }
            }
        }
    }

    fun clearRestoreError() {
        _state.update { it.copy(restoreFailed = false) }
    }
}

/** Cor de cada objetivo: ganhar massa laranja, se manter ciano, emagrecer verde (iOS). */
fun goalColor(goal: UserGoal): Color = when (goal) {
    UserGoal.Bulking -> Color(0xFFFF9500)
    UserGoal.Maintenance -> Color(0xFF32ADE6)
    UserGoal.Cutting -> Color(0xFF34C759)
}
