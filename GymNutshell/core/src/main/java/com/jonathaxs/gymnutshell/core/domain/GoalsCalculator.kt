package com.jonathaxs.gymnutshell.core.domain

import kotlin.math.roundToInt

/**
 * Metas diárias padrão por objetivo × sexo (tabela fixa) — porte de GoalsCalculator (iOS).
 * Desde a 2.0 o app não coleta peso, altura nem idade: os valores partem de um corpo de
 * referência e o usuário ajusta tudo depois com − / +.
 */
object GoalsCalculator {

    /** Todas as metas diárias calculadas pra um perfil. */
    data class Result(
        val calories: Int,  // kcal
        val water: Int,     // ml
        val protein: Int,   // g
        val carbs: Int,     // g
        val goodFat: Int,   // g
        val fiber: Int,     // g
        val sleep: Int,     // h (fixo)
        val creatine: Int,  // g (fixo)
        val workout: Int,   // min (fixo)
        val cardio: Int,    // min (fixo)
    )

    private data class Row(
        val calories: Int,
        val water: Int,
        val protein: Int,
        val carbs: Int,
        val goodFat: Int,
        val fiber: Int,
    )

    // Homem: cutting / manutenção / bulking.
    private val male: Map<UserGoal, Row> = mapOf(
        UserGoal.Cutting to Row(calories = 2100, water = 2750, protein = 165, carbs = 190, goodFat = 55, fiber = 29),
        UserGoal.Maintenance to Row(calories = 2650, water = 2750, protein = 150, carbs = 260, goodFat = 70, fiber = 37),
        UserGoal.Bulking to Row(calories = 3050, water = 2750, protein = 150, carbs = 340, goodFat = 85, fiber = 43),
    )

    // Mulher: cutting / manutenção / bulking.
    private val female: Map<UserGoal, Row> = mapOf(
        UserGoal.Cutting to Row(calories = 1650, water = 2250, protein = 135, carbs = 160, goodFat = 45, fiber = 23),
        UserGoal.Maintenance to Row(calories = 2050, water = 2250, protein = 125, carbs = 220, goodFat = 55, fiber = 29),
        UserGoal.Bulking to Row(calories = 2350, water = 2250, protein = 125, carbs = 280, goodFat = 70, fiber = 33),
    )

    /**
     * Metas padrão pro objetivo e sexo informados. Qualquer sexo diferente de "female"
     * (inclusive o legado "other") usa a coluna masculina.
     */
    fun calculate(sex: String, goal: UserGoal): Result {
        val table = if (Profile.normalizedSex(sex) == "female") female else male
        val row = table[goal] ?: table[UserGoal.Maintenance]!!
        return Result(
            calories = row.calories,
            water = row.water,
            protein = row.protein,
            carbs = row.carbs,
            goodFat = row.goodFat,
            fiber = row.fiber,
            sleep = DefaultGoals.SLEEP,
            creatine = DefaultGoals.CREATINE,
            workout = DefaultGoals.WORKOUT,
            cardio = DefaultGoals.CARDIO,
        )
    }

    /** Sobrecarga de compatibilidade da v1.0 — ignora dados físicos e usa a tabela fixa. */
    @Suppress("UNUSED_PARAMETER")
    fun calculate(weightKg: Double, heightCm: Int, age: Int, sex: String, goal: UserGoal): Result =
        calculate(sex, goal)

    /**
     * Reescala as metas atuais do usuário ao trocar de objetivo. Cada meta (calorias, proteína, carbo,
     * gordura boa e fibra) é multiplicada por `tabela(sexo, novo) / tabela(sexo, antigo)`, então o que o
     * usuário personalizou continua proporcional. Arredonda ao passo da Welcome e respeita os mesmos limites.
     * Água, sono, treino, cardio e creatina não mudam.
     */
    fun rescale(current: Result, sex: String, from: UserGoal, to: UserGoal): Result {
        val table = if (Profile.normalizedSex(sex) == "female") female else male
        val old = table[from] ?: table[UserGoal.Maintenance]!!
        val new = table[to] ?: table[UserGoal.Maintenance]!!

        fun scaled(value: Int, fromVal: Int, toVal: Int, step: Int, range: IntRange): Int {
            if (fromVal <= 0) return value
            val raw = value.toDouble() * toVal.toDouble() / fromVal.toDouble()
            val rounded = (raw / step).roundToInt() * step
            return rounded.coerceIn(range.first, range.last)
        }

        return current.copy(
            calories = scaled(current.calories, old.calories, new.calories, 50, 500..10000),
            protein = scaled(current.protein, old.protein, new.protein, 5, 10..500),
            carbs = scaled(current.carbs, old.carbs, new.carbs, 10, 10..1000),
            goodFat = scaled(current.goodFat, old.goodFat, new.goodFat, 5, 5..300),
            fiber = scaled(current.fiber, old.fiber, new.fiber, 1, 5..100),
        )
    }
}
