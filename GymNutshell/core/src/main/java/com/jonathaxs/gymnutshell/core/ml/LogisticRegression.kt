package com.jonathaxs.gymnutshell.core.ml

import kotlin.math.exp

/**
 * Regressão logística escrita do zero, treinada no próprio aparelho.
 * Modelo: `p = sigmoid(w · x + b)`, a chance de o dia ser fraco.
 * Treino: descida do gradiente na log-loss, com regularização L2 pra segurar
 * overfitting (poucos dados de uma pessoa só).
 * Sem aleatoriedade: pesos começam em zero, então o resultado bate com o iOS.
 */
data class LogisticRegression(
    val weights: List<Double>,
    val bias: Double,
) {
    fun probability(x: List<Double>): Double {
        var z = bias
        for (j in weights.indices) {
            z += weights[j] * x[j]
        }
        return sigmoid(z)
    }

    companion object {
        /** Converte qualquer número numa probabilidade entre 0 e 1. */
        fun sigmoid(z: Double): Double = 1.0 / (1.0 + exp(-z))

        /**
         * Treina com descida do gradiente em lote (todas as linhas a cada passo).
         * [learningRate]: tamanho do passo. [epochs]: quantos passos. [l2]: força da regularização.
         */
        fun train(
            x: List<List<Double>>,
            y: List<Double>,
            learningRate: Double = 0.1,
            epochs: Int = 1000,
            l2: Double = 0.01,
        ): LogisticRegression {
            val width = x.firstOrNull()?.size ?: 0
            val n = x.size.toDouble()
            val weights = DoubleArray(width)
            var bias = 0.0
            if (x.isEmpty()) return LogisticRegression(weights = weights.toList(), bias = bias)

            repeat(epochs) {
                val gradW = DoubleArray(width)
                var gradB = 0.0
                // O gradiente da log-loss é (previsto - real) vezes a entrada.
                for (i in x.indices) {
                    val row = x[i]
                    var z = bias
                    for (j in 0 until width) {
                        z += weights[j] * row[j]
                    }
                    val error = sigmoid(z) - y[i]
                    for (j in 0 until width) {
                        gradW[j] += error * row[j]
                    }
                    gradB += error
                }
                for (j in 0 until width) {
                    // L2 puxa os pesos pra zero (o bias fica de fora).
                    weights[j] -= learningRate * (gradW[j] / n + l2 * weights[j])
                }
                bias -= learningRate * gradB / n
            }
            return LogisticRegression(weights = weights.toList(), bias = bias)
        }
    }
}
