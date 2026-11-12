package com.jonathaxs.gymnutshell.core.ml

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Resultado do treino, pra decidir o que mostrar e pra tela de debug. */
data class RiskModelReport(
    val model: LogisticRegression,
    val standardizer: Standardizer,
    val trainCount: Int,
    val validationCount: Int,
    /** Fração de dias fracos no histórico todo. */
    val weakRate: Double,
    /** Acertos na validação (0 a 1). */
    val modelAccuracy: Double,
    /** Regra "dia da semana": prevê fraco se aquele dia da semana foi fraco em metade ou mais do treino. */
    val weekdayAccuracy: Double,
    /** Regra "maioria": chuta sempre a resposta mais comum no treino. */
    val majorityAccuracy: Double,
) {
    /** O modelo só vale se ganhar das duas regras (empate não conta). */
    val beatsBaseline: Boolean
        get() = modelAccuracy > max(weekdayAccuracy, majorityAccuracy)
}

/**
 * Junta tudo do "dia de risco": monta o dataset, separa treino e validação em
 * ordem de data, treina a regressão logística e compara com duas regras simples.
 * O app só deve mostrar a previsão do modelo se ele ganhar das regras na validação.
 */
object RiskModel {

    /** Dia fraco = percent até 60 (nível 3 ou menos), o dia que quebra o bônus de sequência. */
    const val DEFAULT_WEAK_THRESHOLD = 60

    /** Mínimo de dias registrados pra treinar. */
    const val MINIMUM_DAYS = 21

    /** Fração final do histórico (os dias mais recentes) guardada pra validação. */
    const val VALIDATION_FRACTION = 0.2

    /** Treina e avalia. Devolve null com menos de [MINIMUM_DAYS] dias. */
    fun evaluate(
        days: List<TrainingDay>,
        weakThreshold: Int = DEFAULT_WEAK_THRESHOLD,
    ): RiskModelReport? {
        if (days.size < MINIMUM_DAYS) return null

        // 1. Dataset: uma linha por dia registrado, features + rótulo (1 = fraco).
        val sorted = days.sortedBy { it.date }
        val byDay = RiskFeatures.index(sorted)
        val rawX = sorted.map {
            RiskFeatures.vector(date = it.date, history = byDay, weakThreshold = weakThreshold)
        }
        val y = sorted.map { if (it.percent <= weakThreshold) 1.0 else 0.0 }
        val weekdays = sorted.map { (it.date.dayOfWeek.value % 7) + 1 }

        // 2. Separação cronológica: treina no passado, valida no futuro (nunca embaralhar,
        //    senão o modelo "vê" dias vizinhos dos que vai prever).
        val validationCount = max(1, (sorted.size.toDouble() * VALIDATION_FRACTION).roundToInt())
        val trainCount = sorted.size - validationCount
        val trainRange = 0 until trainCount
        val validationRange = trainCount until sorted.size

        // 3. Normalização com estatística só do treino.
        val standardizer = Standardizer.fit(rawX.subList(0, trainCount))
        val x = rawX.map(standardizer::transform)

        // 4. Treino.
        val model = LogisticRegression.train(
            x = x.subList(0, trainCount),
            y = y.subList(0, trainCount),
        )

        // 5. Avaliação na validação.
        fun accuracy(predict: (Int) -> Boolean): Double {
            val hits = validationRange.count { i -> predict(i) == (y[i] == 1.0) }
            return hits.toDouble() / validationCount.toDouble()
        }
        val modelAccuracy = accuracy { i -> model.probability(x[i]) >= 0.5 }

        val trainWeakRate = trainRange.fold(0.0) { acc, i -> acc + y[i] } / trainCount.toDouble()
        val majorityAccuracy = accuracy { trainWeakRate >= 0.5 }

        val weekdayRate = HashMap<Int, Pair<Double, Double>>()
        for (i in trainRange) {
            val w = weekdays[i]
            val current = weekdayRate[w] ?: (0.0 to 0.0)
            weekdayRate[w] = (current.first + y[i]) to (current.second + 1.0)
        }
        val weekdayAccuracy = accuracy { i ->
            // Dia da semana que não apareceu no treino cai na regra da maioria.
            val r = weekdayRate[weekdays[i]]
            if (r == null) trainWeakRate >= 0.5 else (r.first / r.second) >= 0.5
        }

        return RiskModelReport(
            model = model,
            standardizer = standardizer,
            trainCount = trainCount,
            validationCount = validationCount,
            weakRate = y.fold(0.0) { acc, v -> acc + v } / y.size.toDouble(),
            modelAccuracy = modelAccuracy,
            weekdayAccuracy = weekdayAccuracy,
            majorityAccuracy = majorityAccuracy,
        )
    }

    /** Chance de [date] ser um dia fraco, usando o modelo treinado e o histórico até ontem. */
    fun riskProbability(
        date: LocalDate,
        days: List<TrainingDay>,
        report: RiskModelReport,
        weakThreshold: Int = DEFAULT_WEAK_THRESHOLD,
    ): Double {
        val byDay = RiskFeatures.index(days)
        val raw = RiskFeatures.vector(date = date, history = byDay, weakThreshold = weakThreshold)
        return report.model.probability(report.standardizer.transform(raw))
    }
}
