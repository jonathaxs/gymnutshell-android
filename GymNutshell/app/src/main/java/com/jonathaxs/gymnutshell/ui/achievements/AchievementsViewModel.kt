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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
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

/** Uma linha do histórico: mascote, data, nível e percentual. */
data class HistoryItemUi(
    val epochDay: Long,
    val dateLabel: String,
    val emoji: String,
    @param:StringRes val tierNameRes: Int,
    val percent: Int,
    /** Editável dentro da janela de 30 dias, igual ao editWindow do iOS. */
    val canEdit: Boolean = false,
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
        val firstDow = WeekFields.of(locale).firstDayOfWeek

        // Mapeamento de emojis pelo tema ativo, espelhando o iOS (não usa o emoji antigo salvo).
        val emojiByDay = records.associate { record ->
            val tier = if (record.achievementEmoji.isNotEmpty()) DailyAchievement.from(record.achievementEmoji)
            else DailyAchievement.fromPercent(record.percent)
            record.date to theme.emoji(tier, profile.sex)
        }
        val percentByDay = records.associate { it.date to it.percent }

        // Grade: começa no primeiro dia da semana que contém o dia 1, vai até o fim do mês.
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

        // No modo Calendário o histórico mostra só o dia selecionado; no modo Lista, todos os registros.
        val historyRecords = records
            .let { all -> if (mode == AchievementsFilterMode.Calendar) all.filter { it.date == selected } else all }
            .sortedByDescending { it.date }
        val history = historyRecords.map { record ->
            val tier = if (record.achievementEmoji.isNotEmpty()) DailyAchievement.from(record.achievementEmoji)
            else DailyAchievement.fromPercent(record.percent)
            HistoryItemUi(
                epochDay = record.date,
                dateLabel = AppDateFormatters.mediumDate(LocalDate.ofEpochDay(record.date)),
                emoji = theme.emoji(tier, profile.sex),
                tierNameRes = theme.tierNameRes(tier, profile.sex),
                percent = record.percent,
                canEdit = (todayEpoch - record.date) in 0..EDIT_WINDOW_DAYS,
            )
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

    fun deleteRecord(epochDay: Long) {
        viewModelScope.launch {
            recordRepo.deleteByDate(epochDay)
        }
    }

    private companion object {
        // Janela de edição: registros dos últimos 30 dias (1 mês no iOS) são editáveis.
        const val EDIT_WINDOW_DAYS = 30L
    }
}
