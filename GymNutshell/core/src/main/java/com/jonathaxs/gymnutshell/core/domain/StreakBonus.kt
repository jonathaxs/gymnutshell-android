package com.jonathaxs.gymnutshell.core.domain

import com.jonathaxs.gymnutshell.core.data.DailyRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * Tipo de bônus de sequência — porte de StreakBonusKind (iOS/GymNutshellCore).
 * Semana fechada (segunda a domingo) ou mês inteiro nos níveis 4 ou 5.
 */
enum class StreakBonusKind(
    val rawValue: String,
    val points: Int,
    val emoji: String,
) {
    WeeklyLevel4("weekly.level4", 8, "🎖️"),
    WeeklyLevel5("weekly.level5", 12, "💀"),
    MonthlyLevel4("monthly.level4", 20, "🌟"),
    MonthlyLevel5("monthly.level5", 33, "☠️");

    /** Bônus mensal tem prioridade sobre o semanal quando caem no mesmo dia. */
    val isMonthly: Boolean
        get() = this == MonthlyLevel4 || this == MonthlyLevel5

    companion object {
        fun fromRaw(raw: String?): StreakBonusKind? =
            entries.firstOrNull { it.rawValue == raw }
    }
}

/**
 * Um bônus conquistado em uma semana ou mês fechado — porte de EarnedStreakBonus (iOS).
 * [anchorDate] é o último dia do período (domingo da semana ou último dia do mês).
 */
data class EarnedStreakBonus(
    val kind: StreakBonusKind,
    val anchorDate: LocalDate,
) {
    val points: Int get() = kind.points

    /** Identificador estável pro histórico e pros avisos (ex.: "weekly.level5.2026-11-08"). */
    val id: String
        get() = String.format(
            Locale.US,
            "%s.%04d-%02d-%02d",
            kind.rawValue,
            anchorDate.year,
            anchorDate.monthValue,
            anchorDate.dayOfMonth,
        )
}

/**
 * Calcula os bônus de sequência e o total de pontos a partir dos registros diários.
 * Regra idêntica ao StreakBonusCalculator (iOS):
 * - Semana fechada de segunda a domingo; âncora no domingo.
 * - Mês fechado do dia 1 ao último dia do mês; âncora no último dia.
 * - Todos os dias no nível 4 ou 5 -> bônus nível 4; todos no nível 5 -> só o bônus nível 5.
 * - Dia sem registro quebra a sequência.
 */
object StreakBonusCalculator {

    /** Nível de um registro salvo pela %, como no resto do app Android (o emoji varia com o tema). */
    fun levelOf(record: DailyRecord): DailyAchievement = DailyAchievement.fromPercent(record.percent)

    /** Bônus conquistados a partir da lista de [DailyRecord], do mais recente pro mais antigo. */
    fun bonuses(records: List<DailyRecord>): List<EarnedStreakBonus> {
        if (records.isEmpty()) return emptyList()
        val byDay = HashMap<LocalDate, DailyAchievement>(records.size)
        for (record in records) {
            byDay[LocalDate.ofEpochDay(record.date)] = levelOf(record)
        }
        return bonuses(byDay)
    }

    /** Versão pura por mapa de dia -> nível (usada também nos testes). */
    fun bonuses(levels: Map<LocalDate, DailyAchievement>): List<EarnedStreakBonus> {
        if (levels.isEmpty()) return emptyList()

        val result = mutableListOf<EarnedStreakBonus>()

        // Semanas: cada domingo presente nos registros fecha a semana segunda..domingo.
        for (sunday in levels.keys) {
            if (sunday.dayOfWeek != DayOfWeek.SUNDAY) continue
            val weekLevels = (0L..6L).mapNotNull { offset ->
                levels[sunday.minusDays(offset)]
            }
            if (weekLevels.size < 7) continue
            if (weekLevels.all { it.number >= 4 }) {
                val kind = if (weekLevels.all { it.number == 5 }) {
                    StreakBonusKind.WeeklyLevel5
                } else {
                    StreakBonusKind.WeeklyLevel4
                }
                result += EarnedStreakBonus(kind = kind, anchorDate = sunday)
            }
        }

        // Meses: cada último dia do mês presente nos registros fecha o mês inteiro.
        for (lastDay in levels.keys) {
            if (lastDay.plusDays(1).monthValue == lastDay.monthValue) continue
            val ym = YearMonth.from(lastDay)
            val totalDays = ym.lengthOfMonth()
            val monthLevels = (1..totalDays).mapNotNull { day ->
                levels[ym.atDay(day)]
            }
            if (monthLevels.size < totalDays) continue
            if (monthLevels.all { it.number >= 4 }) {
                val kind = if (monthLevels.all { it.number == 5 }) {
                    StreakBonusKind.MonthlyLevel5
                } else {
                    StreakBonusKind.MonthlyLevel4
                }
                result += EarnedStreakBonus(kind = kind, anchorDate = lastDay)
            }
        }

        // Mais recente primeiro; no mesmo dia, o mensal vem antes do semanal.
        return result.sortedWith { a, b ->
            val cmp = b.anchorDate.compareTo(a.anchorDate)
            if (cmp != 0) cmp else b.kind.isMonthly.compareTo(a.kind.isMonthly)
        }
    }

    /** Soma os pontos dos dias salvos (1..5 pelo nível) com os pontos de todos os bônus. */
    fun totalPoints(records: List<DailyRecord>): Int {
        val daily = records.sumOf { levelOf(it).points }
        val bonus = bonuses(records).sumOf { it.points }
        return daily + bonus
    }
}
