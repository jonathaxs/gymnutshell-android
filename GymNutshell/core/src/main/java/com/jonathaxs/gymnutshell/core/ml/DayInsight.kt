package com.jonathaxs.gymnutshell.core.ml

import java.time.LocalDate
import kotlin.math.roundToInt

/** Grupos de features, na linguagem do usuário. O dia da semana junta as 7 posições do one-hot. */
enum class DayInsightFactor(val featureIndices: List<Int>) {
    WEEKDAY((0..6).toList()),
    YESTERDAY(listOf(7, 8)),
    LAST_WEEK(listOf(9, 10)),
    STREAK(listOf(11)),
    SLEEP(listOf(12)),
    WORKOUT(listOf(13)),
    CARDIO(listOf(14)),
}

/**
 * Decide o que a Hoje mostra abaixo da data: a previsão do modelo de "dia de risco"
 * (só se ele ganhar das regras simples na validação) ou, senão, a média daquele dia
 * da semana. Também diz o que mais pesou na previsão, pro balão da data.
 */
data class DayInsight(
    val kind: Kind,
    /** Dia da semana de hoje no padrão do Calendar (1 = domingo ... 7 = sábado). */
    val weekday: Int,
    /** Quantos dias (antes de hoje) entraram na conta. */
    val dayCount: Int,
) {
    sealed interface Kind {
        /** Previsão do modelo: chance de dia fraco (0 a 1), acerto na validação e o que mais pesou. */
        data class Model(
            val risk: Double,
            val accuracy: Double,
            val factors: List<DayInsightFactor>,
        ) : Kind

        /** Sem modelo confiável: média desse dia da semana e média geral (0 a 100). */
        data class Weekday(
            val average: Int,
            val overall: Int,
        ) : Kind
    }

    /** Hoje tende a ser fraco (risco >= 50% ou média do dia da semana até o limite de dia fraco). */
    val needsAttention: Boolean
        get() = when (kind) {
            is Kind.Model -> kind.risk >= 0.5
            is Kind.Weekday -> kind.average <= RiskModel.DEFAULT_WEAK_THRESHOLD
        }
}

object DayInsightBuilder {

    /** Máximo de fatores citados no balão. */
    private const val MAX_FACTORS = 2

    /** Mínimo de registros naquele dia da semana pra média valer. */
    private const val MINIMUM_WEEKDAY_COUNT = 2

    /**
     * Insight de [date]. Usa só os dias antes dela (o dia de hoje ainda está em andamento).
     * Devolve null com menos de [RiskModel.MINIMUM_DAYS] dias ou sem registros suficientes nesse dia da semana.
     */
    fun make(date: LocalDate, days: List<TrainingDay>): DayInsight? {
        val past = days.filter { it.date < date }
        if (past.size < RiskModel.MINIMUM_DAYS) return null
        val weekday = (date.dayOfWeek.value % 7) + 1

        // 1. Modelo, se ele ganhou das regras na validação.
        val report = RiskModel.evaluate(days = past)
        if (report != null && report.beatsBaseline) {
            val byDay = RiskFeatures.index(past)
            val raw = RiskFeatures.vector(
                date = date,
                history = byDay,
                weakThreshold = RiskModel.DEFAULT_WEAK_THRESHOLD,
            )
            val z = report.standardizer.transform(raw)
            val risk = report.model.probability(z)
            val factors = topFactors(weights = report.model.weights, z = z, towardsWeak = risk >= 0.5)
            return DayInsight(
                kind = DayInsight.Kind.Model(
                    risk = risk,
                    accuracy = report.modelAccuracy,
                    factors = factors,
                ),
                weekday = weekday,
                dayCount = past.size,
            )
        }

        // 2. Senão, a estatística do dia da semana (não é ML, só média).
        val stat = WeekdayStats.averages(days = past).firstOrNull { it.weekday == weekday }
        if (stat == null || stat.count < MINIMUM_WEEKDAY_COUNT) return null
        val overall = past.fold(0) { acc, d -> acc + d.percent }.toDouble() / past.size.toDouble()
        return DayInsight(
            kind = DayInsight.Kind.Weekday(
                average = stat.average.roundToInt(),
                overall = overall.roundToInt(),
            ),
            weekday = weekday,
            dayCount = past.size,
        )
    }

    /**
     * Contribuição de cada grupo = soma de peso × valor normalizado. Positivo puxa pra dia fraco.
     * Com risco alto, cita os que mais puxaram pra fraco; com risco baixo, os que mais ajudaram.
     */
    internal fun topFactors(
        weights: List<Double>,
        z: List<Double>,
        towardsWeak: Boolean,
    ): List<DayInsightFactor> {
        val sign = if (towardsWeak) 1.0 else -1.0
        return DayInsightFactor.entries
            .map { factor ->
                val score = factor.featureIndices.fold(0.0) { acc, idx -> acc + weights[idx] * z[idx] }
                factor to score
            }
            .filter { (_, score) -> score * sign > 1e-6 }
            .sortedByDescending { (_, score) -> score * sign }
            .take(MAX_FACTORS)
            .map { (factor, _) -> factor }
    }
}
