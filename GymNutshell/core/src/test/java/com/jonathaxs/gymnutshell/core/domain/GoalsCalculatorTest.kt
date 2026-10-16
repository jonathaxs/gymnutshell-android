package com.jonathaxs.gymnutshell.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsCalculatorTest {

    // MARK: - Metas padrão (tabela objetivo × sexo)

    @Test
    fun `perfil masculino em manutencao bate o resultado completo da tabela`() {
        // Tabela 2.0: corpo de referência masculino em manutenção
        val result = GoalsCalculator.calculate(
            weightKg = 80.0, heightCm = 180, age = 30, sex = "male", goal = UserGoal.Maintenance,
        )
        val expected = GoalsCalculator.Result(
            calories = 2650,
            water = 2750,
            protein = 150,
            carbs = 260,
            goodFat = 70,
            fiber = 37,
            sleep = 8,
            creatine = 5,
            workout = 50,
            cardio = 15,
        )
        assertEquals(expected, result)
        assertEquals(expected, GoalsCalculator.calculate("male", UserGoal.Maintenance))
    }

    @Test
    fun `cutting masculino reduz calorias e aumenta proteina`() {
        val r = GoalsCalculator.calculate(80.0, 180, 30, "male", UserGoal.Cutting)
        assertEquals(2100, r.calories)
        assertEquals(165, r.protein)
        assertEquals(2750, r.water)
        assertEquals(190, r.carbs)
        assertEquals(55, r.goodFat)
        assertEquals(29, r.fiber)
    }

    @Test
    fun `perfil feminino em cutting bate valores da tabela`() {
        val r = GoalsCalculator.calculate("female", UserGoal.Cutting)
        assertEquals(1650, r.calories)
        assertEquals(2250, r.water)
        assertEquals(135, r.protein)
        assertEquals(160, r.carbs)
        assertEquals(45, r.goodFat)
        assertEquals(23, r.fiber)
    }

    @Test
    fun `perfil feminino e sobrecarga de compatibilidade ignoram dados fisicos`() {
        val r = GoalsCalculator.calculate(60.0, heightCm = 0, age = 0, sex = "female", goal = UserGoal.Maintenance)
        val expected = GoalsCalculator.calculate("female", UserGoal.Maintenance)
        assertEquals(2050, r.calories)
        assertEquals(2250, r.water)
        assertEquals(125, r.protein)
        assertEquals(220, r.carbs)
        assertEquals(55, r.goodFat)
        assertEquals(29, r.fiber)
        assertEquals(expected, r)
    }

    @Test
    fun `tabela completa para homem e mulher em cutting manutencao e bulking`() {
        // Homem
        val hCut = GoalsCalculator.calculate("male", UserGoal.Cutting)
        assertEquals(2100, hCut.calories)
        assertEquals(2750, hCut.water)
        assertEquals(165, hCut.protein)
        assertEquals(190, hCut.carbs)
        assertEquals(55, hCut.goodFat)
        assertEquals(29, hCut.fiber)

        val hManut = GoalsCalculator.calculate("male", UserGoal.Maintenance)
        assertEquals(2650, hManut.calories)
        assertEquals(2750, hManut.water)
        assertEquals(150, hManut.protein)
        assertEquals(260, hManut.carbs)
        assertEquals(70, hManut.goodFat)
        assertEquals(37, hManut.fiber)

        val hBulk = GoalsCalculator.calculate("male", UserGoal.Bulking)
        assertEquals(3050, hBulk.calories)
        assertEquals(2750, hBulk.water)
        assertEquals(150, hBulk.protein)
        assertEquals(340, hBulk.carbs)
        assertEquals(85, hBulk.goodFat)
        assertEquals(43, hBulk.fiber)

        // Mulher
        val mCut = GoalsCalculator.calculate("female", UserGoal.Cutting)
        assertEquals(1650, mCut.calories)
        assertEquals(2250, mCut.water)
        assertEquals(135, mCut.protein)
        assertEquals(160, mCut.carbs)
        assertEquals(45, mCut.goodFat)
        assertEquals(23, mCut.fiber)

        val mManut = GoalsCalculator.calculate("female", UserGoal.Maintenance)
        assertEquals(2050, mManut.calories)
        assertEquals(2250, mManut.water)
        assertEquals(125, mManut.protein)
        assertEquals(220, mManut.carbs)
        assertEquals(55, mManut.goodFat)
        assertEquals(29, mManut.fiber)

        val mBulk = GoalsCalculator.calculate("female", UserGoal.Bulking)
        assertEquals(2350, mBulk.calories)
        assertEquals(2250, mBulk.water)
        assertEquals(125, mBulk.protein)
        assertEquals(280, mBulk.carbs)
        assertEquals(70, mBulk.goodFat)
        assertEquals(33, mBulk.fiber)
    }

    @Test
    fun `metas fixas sao iguais para todos os sexos e objetivos`() {
        for (sex in listOf("male", "female")) {
            for (goal in UserGoal.entries) {
                val r = GoalsCalculator.calculate(sex, goal)
                assertEquals(8, r.sleep)
                assertEquals(50, r.workout)
                assertEquals(15, r.cardio)
                assertEquals(5, r.creatine)
            }
        }
    }

    @Test
    fun `sexo legado other ou vazio usa tabela masculina`() {
        for (goal in UserGoal.entries) {
            val maleResult = GoalsCalculator.calculate("male", goal)
            assertEquals(maleResult, GoalsCalculator.calculate("other", goal))
            assertEquals(maleResult, GoalsCalculator.calculate("", goal))
        }
    }

    @Test
    fun `bulking tem mais calorias que cutting`() {
        for (sex in listOf("male", "female")) {
            val cut = GoalsCalculator.calculate(sex, UserGoal.Cutting)
            val bulk = GoalsCalculator.calculate(sex, UserGoal.Bulking)
            assertTrue(bulk.calories > cut.calories)
        }
    }

    // MARK: - Recálculo proporcional ao trocar de objetivo (rescale)

    @Test
    fun `recalculo da tabela casa com a tabela`() {
        for (sex in listOf("male", "female")) {
            val base = GoalsCalculator.calculate(sex, UserGoal.Maintenance)
            for (goal in listOf(UserGoal.Bulking, UserGoal.Cutting)) {
                val r = GoalsCalculator.rescale(base, sex, UserGoal.Maintenance, goal)
                val t = GoalsCalculator.calculate(sex, goal)
                assertEquals(t.calories, r.calories)
                assertEquals(t.protein, r.protein)
                assertEquals(t.carbs, r.carbs)
                assertEquals(t.goodFat, r.goodFat)
                assertEquals(t.fiber, r.fiber)
            }
        }
    }

    @Test
    fun `recalculo respeita valor personalizado`() {
        val mine = GoalsCalculator.calculate("male", UserGoal.Maintenance).copy(calories = 3000)
        val r = GoalsCalculator.rescale(mine, "male", UserGoal.Maintenance, UserGoal.Bulking)
        // 3000 * 3050 / 2650 = 3452 -> 3450 (passo de 50)
        assertEquals(3450, r.calories)
    }

    @Test
    fun `recalculo nao mexe em agua sono treino cardio creatina`() {
        val mine = GoalsCalculator.calculate("female", UserGoal.Maintenance).copy(
            water = 3500, sleep = 9, creatine = 5, workout = 90, cardio = 30,
        )
        val r = GoalsCalculator.rescale(mine, "female", UserGoal.Maintenance, UserGoal.Cutting)
        assertEquals(3500, r.water)
        assertEquals(9, r.sleep)
        assertEquals(5, r.creatine)
        assertEquals(90, r.workout)
        assertEquals(30, r.cardio)
    }

    @Test
    fun `recalculo ida e volta devolve o valor`() {
        for (sex in listOf("male", "female")) {
            val base = GoalsCalculator.calculate(sex, UserGoal.Maintenance)
            for (goal in listOf(UserGoal.Bulking, UserGoal.Cutting)) {
                val there = GoalsCalculator.rescale(base, sex, UserGoal.Maintenance, goal)
                val back = GoalsCalculator.rescale(there, sex, goal, UserGoal.Maintenance)
                assertEquals(base, back)
            }
        }
    }

    @Test
    fun `recalculo mesmo objetivo nao muda`() {
        val base = GoalsCalculator.calculate("male", UserGoal.Cutting)
        assertEquals(base, GoalsCalculator.rescale(base, "male", UserGoal.Cutting, UserGoal.Cutting))
    }

    @Test
    fun `recalculo limita aos ranges`() {
        val mine = GoalsCalculator.calculate("male", UserGoal.Maintenance).copy(
            calories = 10000,
            protein = 10,
        )
        val r = GoalsCalculator.rescale(mine, "male", UserGoal.Maintenance, UserGoal.Bulking)
        assertEquals(10000, r.calories)
        assertEquals(10, r.protein)
    }
}
