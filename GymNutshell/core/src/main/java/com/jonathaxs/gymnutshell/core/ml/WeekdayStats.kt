package com.jonathaxs.gymnutshell.core.ml

import java.time.DayOfWeek

/** Média de percentual acumulado em um dia da semana. */
data class WeekdayAverage(
    /** Dia da semana no padrão do Calendar (1 = domingo ... 7 = sábado). */
    val weekday: Int,
    /** Média do percentual (0 a 100). */
    val average: Double,
    /** Quantidade de dias registrados nesse dia da semana. */
    val count: Int,
)

/** Calcula a média do percentual diário por dia da semana a partir do histórico. */
object WeekdayStats {

    /**
     * Devolve a média de `percent` por dia da semana (só os dias com pelo menos 1 registro),
     * ordenados a partir de [firstDayOfWeek].
     */
    fun averages(
        days: List<TrainingDay>,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<WeekdayAverage> {
        val totals = HashMap<Int, Pair<Int, Int>>()
        for (day in days) {
            val weekday = (day.date.dayOfWeek.value % 7) + 1
            val current = totals[weekday] ?: (0 to 0)
            totals[weekday] = (current.first + day.percent) to (current.second + 1)
        }

        val first = firstDayOfWeek.value % 7
        val orderedWeekdays = (0 until 7).map { ((first + it) % 7) + 1 }

        return orderedWeekdays.mapNotNull { weekday ->
            val bucket = totals[weekday] ?: return@mapNotNull null
            if (bucket.second <= 0) return@mapNotNull null
            WeekdayAverage(
                weekday = weekday,
                average = bucket.first.toDouble() / bucket.second.toDouble(),
                count = bucket.second,
            )
        }
    }
}
