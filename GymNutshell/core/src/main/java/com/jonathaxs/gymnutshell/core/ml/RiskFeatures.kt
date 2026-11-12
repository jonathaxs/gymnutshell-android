package com.jonathaxs.gymnutshell.core.ml

import java.time.LocalDate
import kotlin.math.sqrt

/**
 * Transforma o histórico em números (features) pro modelo de "dia de risco".
 * Cada dia vira um vetor montado só com o que se sabe NO COMEÇO daquele dia
 * (dia da semana e dias anteriores), senão o modelo "cola" vendo o próprio resultado.
 */
object RiskFeatures {

    /** Nome de cada posição do vetor, na mesma ordem de [vector]. Ajuda a ler os pesos na tela de debug. */
    val names: List<String> = listOf(
        "dom", "seg", "ter", "qua", "qui", "sex", "sab",
        "ontem_percent", "ontem_sem_registro", "media_7d", "dias_registrados_7d",
        "sequencia", "ontem_sono", "ontem_treino", "ontem_cardio",
    )

    /** Índice dos dias pela data, pra achar "ontem" e "últimos 7 dias" rápido. */
    fun index(days: List<TrainingDay>): Map<LocalDate, TrainingDay> {
        val byDay = HashMap<LocalDate, TrainingDay>(days.size)
        for (day in days) {
            byDay[day.date] = day
        }
        return byDay
    }

    /** Vetor de features do dia [date], olhando só os dias antes dele. */
    fun vector(
        date: LocalDate,
        history: Map<LocalDate, TrainingDay>,
        weakThreshold: Int,
    ): List<Double> {
        fun daysAgo(n: Int): TrainingDay? = history[date.minusDays(n.toLong())]

        // Dia da semana em "one-hot": 7 posições (1 = domingo ... 7 = sábado), só a do dia vale 1.
        val weekday = MutableList(7) { 0.0 }
        val iosWeekday = (date.dayOfWeek.value % 7) + 1
        weekday[iosWeekday - 1] = 1.0

        // Ontem. Sem registro conta como 0 e liga a flag "sem registro".
        val yesterday = daysAgo(1)
        val yesterdayPercent = (yesterday?.percent ?: 0).toDouble() / 100.0
        val yesterdayMissing = if (yesterday == null) 1.0 else 0.0

        // Últimos 7 dias: média do percent dos dias registrados e quantos dias tiveram registro.
        val last7 = (1..7).mapNotNull { daysAgo(it) }
        val mean7 = if (last7.isEmpty()) {
            0.0
        } else {
            last7.fold(0) { acc, d -> acc + d.percent }.toDouble() / last7.size.toDouble() / 100.0
        }
        val recorded7 = last7.size.toDouble() / 7.0

        // Sequência: dias seguidos antes de hoje acima do limite de dia fraco (até 14).
        var streak = 0
        while (streak < 14) {
            val d = daysAgo(streak + 1)
            if (d != null && d.percent > weakThreshold) {
                streak += 1
            } else {
                break
            }
        }

        return weekday + listOf(
            yesterdayPercent,
            yesterdayMissing,
            mean7,
            recorded7,
            streak.toDouble() / 14.0,
            (yesterday?.sleep ?: 0).toDouble(),
            if (yesterday?.didWorkout == true) 1.0 else 0.0,
            if (yesterday?.didCardio == true) 1.0 else 0.0,
        )
    }
}

/**
 * Deixa cada feature com média 0 e desvio 1. Sem isso, uma feature grande (sono = 8) pesa
 * mais no gradiente que uma pequena (percent = 0.8) só por causa da escala.
 * A média e o desvio vêm SÓ do treino; a validação usa os mesmos valores.
 */
data class Standardizer(
    val means: List<Double>,
    val stds: List<Double>,
) {
    fun transform(row: List<Double>): List<Double> =
        row.indices.map { i -> (row[i] - means[i]) / stds[i] }

    companion object {
        fun fit(rows: List<List<Double>>): Standardizer {
            val n = rows.size.toDouble()
            val width = rows.firstOrNull()?.size ?: 0
            val means = MutableList(width) { 0.0 }
            val stds = MutableList(width) { 1.0 }
            for (j in 0 until width) {
                val mean = rows.fold(0.0) { acc, r -> acc + r[j] } / n
                val variance = rows.fold(0.0) { acc, r ->
                    val diff = r[j] - mean
                    acc + diff * diff
                } / n
                means[j] = mean
                // Feature constante (desvio 0) fica com desvio 1 pra não dividir por zero.
                stds[j] = if (variance > 1e-12) sqrt(variance) else 1.0
            }
            return Standardizer(means = means, stds = stds)
        }
    }
}
