package com.jonathaxs.gymnutshell.ui.achievements

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jonathaxs.gymnutshell.core.data.DailyRecordRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.domain.EarnedStreakBonus
import com.jonathaxs.gymnutshell.core.domain.StreakBonusCalculator
import com.jonathaxs.gymnutshell.core.domain.StreakBonusKind
import com.jonathaxs.gymnutshell.notifications.NotificationStrings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Uma célula do calendário. */
data class CalendarDayUi(
    val epochDay: Long,
    val dayNumber: Int,
    val inMonth: Boolean,
    val emoji: String?,
    /** Percentual do dia (null = sem registro), usado na descrição de acessibilidade. */
    val percent: Int?,
    val isToday: Boolean,
    val isSelected: Boolean,
)

/** Uma linha do histórico: conquista diária ou bônus de sequência (quando [bonusKind] != null). */
data class HistoryItemUi(
    val epochDay: Long,
    val dateLabel: String,
    val emoji: String,
    @param:StringRes val tierNameRes: Int,
    val percent: Int,
    /** Editável dentro da janela de 30 dias, igual ao editWindow do iOS. Bônus nunca são editáveis. */
    val canEdit: Boolean = false,
    val bonusKind: StreakBonusKind? = null,
    val bonusId: String? = null,
    val bonusPoints: Int = 0,
    /** Pontos do dia (número do nível, 1 a 5). Zero nas linhas de bônus. */
    val points: Int = 0,
)

/** Modo de visualização: calendário (com o dia selecionado) ou lista completa. Espelha o FilterMode (iOS). */
enum class AchievementsFilterMode { Calendar, List }

/** Estado da AchievementsView. */
data class AchievementsUiState(
    val monthLabel: String = "",
    val selectedWeekday: String = "",
    val weekdays: List<String> = emptyList(),
    val days: List<CalendarDayUi> = emptyList(),
    val history: List<HistoryItemUi> = emptyList(),
    val filterMode: AchievementsFilterMode = AchievementsFilterMode.Calendar,
    val accentArgb: Long = 0xFF007AFF,
)

/**
 * ViewModel da AchievementsView — porte da AchievementsView (iOS).
 * Lê os DailyRecord (Room) e monta o calendário do mês + o histórico, na cor de destaque do usuário.
 */
class AchievementsViewModel(app: Application) : AndroidViewModel(app) {

    private val recordRepo = DailyRecordRepository(app.applicationContext)
    private val settingsRepo = SettingsRepository(app.applicationContext)
    private val profileRepo = ProfileRepository(app.applicationContext)

    private val month = MutableStateFlow(YearMonth.now())

    // Modo local da sheet (igual ao iOS): abre no Calendário, a menos que alguém (ex.: Historico) tenha pedido
    // a Lista antes de abrir; o pedido é consumido e volta pra "calendar".
    private val filterMode = MutableStateFlow(AchievementsFilterMode.Calendar)

    init {
        viewModelScope.launch {
            if (settingsRepo.achievementsFilterMode.first() == "list") filterMode.value = AchievementsFilterMode.List
            settingsRepo.setAchievementsFilterMode("calendar")
        }
        // O mês visível acompanha o dia selecionado (inclusive quando a tela de Histórico troca o dia),
        // espelhando o onChange(selectedDate) → visibleMonthDate do iOS.
        viewModelScope.launch {
            settingsRepo.achievementsSelectedDay.collect { day ->
                month.value = YearMonth.from(LocalDate.ofEpochDay(day))
            }
        }
    }

    private val themeAndProfile = combine(
        settingsRepo.theme,
        profileRepo.profile,
        settingsRepo.accentColor,
    ) { theme, profile, accent -> Triple(theme, profile, accent) }

    val uiState: StateFlow<AchievementsUiState> = combine(
        recordRepo.records,
        month,
        settingsRepo.achievementsSelectedDay,
        filterMode,
        themeAndProfile,
    ) { records, ym, selected, mode, extra ->
        val (theme, profile, accent) = extra
        val todayEpoch = LocalDate.now().toEpochDay()
        val locale = Locale.getDefault()
        // A semana começa sempre na segunda-feira pra bater com a regra do bônus semanal (segunda a domingo).
        val firstDow = DayOfWeek.MONDAY
        val bonuses = StreakBonusCalculator.bonuses(records)

        // Mapeamento de emojis pelo tema ativo; o bônus sobrescreve o dia-âncora (mensal vence semanal).
        val emojiByDay = records.associate { record ->
            val tier = StreakBonusCalculator.levelOf(record)
            record.date to theme.emoji(tier, profile.sex)
        }.toMutableMap()
        for (bonus in bonuses.reversed().sortedBy { it.kind.isMonthly }) {
            emojiByDay[bonus.anchorDate.toEpochDay()] = bonus.kind.emoji
        }
        val percentByDay = records.associate { it.date to it.percent }

        // Grade: começa na segunda-feira da semana que contém o dia 1, vai até o fim do mês.
        val firstOfMonth = ym.atDay(1)
        val shift = ((firstOfMonth.dayOfWeek.value - firstDow.value) + 7) % 7
        val gridStart = firstOfMonth.minusDays(shift.toLong())
        val lastOfMonth = ym.atEndOfMonth()

        val days = generateSequence(gridStart) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastOfMonth) }
            .map { date ->
                val epochDay = date.toEpochDay()
                CalendarDayUi(
                    epochDay = epochDay,
                    dayNumber = date.dayOfMonth,
                    inMonth = date.monthValue == ym.monthValue && date.year == ym.year,
                    emoji = emojiByDay[epochDay],
                    percent = percentByDay[epochDay],
                    isToday = epochDay == todayEpoch,
                    isSelected = epochDay == selected,
                )
            }.toList()

        val weekdays = (0L..6L).map {
            firstDow.plus(it).getDisplayName(TextStyle.NARROW, locale).uppercase()
        }

        // Dia da semana do dia selecionado por extenso (ex: "Quinta-feira"), porte do AppDateFormatters.weekday do iOS.
        val selectedDate = LocalDate.ofEpochDay(selected)
        val selectedWeekday = selectedDate.dayOfWeek
            .getDisplayName(TextStyle.FULL, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        // No modo Calendário o histórico mostra só o dia selecionado; no modo Lista, todos os registros e bônus.
        val historyRecords = records
            .let { all -> if (mode == AchievementsFilterMode.Calendar) all.filter { it.date == selected } else all }
            .sortedByDescending { it.date }
        val visibleBonuses = if (mode == AchievementsFilterMode.Calendar) {
            bonuses.filter { it.anchorDate.toEpochDay() == selected }
        } else {
            bonuses
        }

        fun bonusToUi(bonus: EarnedStreakBonus) = HistoryItemUi(
            epochDay = bonus.anchorDate.toEpochDay(),
            dateLabel = AppDateFormatters.mediumDate(bonus.anchorDate),
            emoji = bonus.kind.emoji,
            tierNameRes = NotificationStrings.streakBonusTitleRes(bonus.kind),
            percent = 0,
            canEdit = false,
            bonusKind = bonus.kind,
            bonusId = bonus.id,
            bonusPoints = bonus.points,
        )

        // Intercala dias e bônus do mais recente pro mais antigo; o bônus entra logo acima do dia que fechou o período.
        val history = mutableListOf<HistoryItemUi>()
        val pendingBonuses = visibleBonuses.toMutableList()
        for (record in historyRecords) {
            while (pendingBonuses.isNotEmpty() && pendingBonuses.first().anchorDate.toEpochDay() >= record.date) {
                history += bonusToUi(pendingBonuses.removeAt(0))
            }
            val tier = StreakBonusCalculator.levelOf(record)
            history += HistoryItemUi(
                epochDay = record.date,
                dateLabel = AppDateFormatters.mediumDate(LocalDate.ofEpochDay(record.date)),
                emoji = theme.emoji(tier, profile.sex),
                tierNameRes = theme.tierNameRes(tier, profile.sex),
                percent = record.percent,
                canEdit = (todayEpoch - record.date) in 0..EDIT_WINDOW_DAYS,
                points = tier.points,
            )
        }
        for (bonus in pendingBonuses) {
            history += bonusToUi(bonus)
        }

        AchievementsUiState(
            monthLabel = AppDateFormatters.monthYear(ym.atDay(1)),
            selectedWeekday = selectedWeekday,
            weekdays = weekdays,
            days = days,
            history = history,
            filterMode = mode,
            accentArgb = accent.argb,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }

    // Dia é persistido no DataStore (compartilhados com a tela de Histórico, que os pré-define).
    fun selectDay(epochDay: Long) {
        viewModelScope.launch { settingsRepo.setAchievementsSelectedDay(epochDay) }
    }

    fun setFilterMode(mode: AchievementsFilterMode) {
        filterMode.value = mode
    }

    private companion object {
        // Janela de edição: registros dos últimos 30 dias (1 mês no iOS) são editáveis.
        const val EDIT_WINDOW_DAYS = 30L
    }
}
