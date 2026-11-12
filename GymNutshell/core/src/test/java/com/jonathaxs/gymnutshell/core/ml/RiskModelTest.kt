package com.jonathaxs.gymnutshell.core.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RiskModelTest {

    private fun iosWeekday(date: LocalDate): Int = (date.dayOfWeek.value % 7) + 1

    /** Histórico sintético: `count` dias seguidos a partir de 2026-01-05 (segunda), percent por regra. */
    private fun historico(count: Int, percent: (Int, LocalDate) -> Int): List<TrainingDay> {
        val inicio = LocalDate.of(2026, 1, 5)
        return (0 until count).map { i ->
            val d = inicio.plusDays(i.toLong())
            TrainingDay(
                date = d,
                water = 2000,
                protein = 100,
                carbs = 200,
                goodFat = 50,
                fiber = 30,
                sleep = 7,
                didWorkout = true,
                didCardio = false,
                workoutRestDay = false,
                cardioRestDay = false,
                percent = percent(i, d),
                points = 0,
            )
        }
    }

    // MARK: - Modelo de dia de risco (ML)

    @Test
    fun sigmoidNoZeroEMeio() {
        assertEquals(0.5, LogisticRegression.sigmoid(0.0), 0.0)
    }

    @Test
    fun regressaoLogisticaSeparaCasoSimples() {
        val x = listOf(listOf(-2.0), listOf(-1.0), listOf(1.0), listOf(2.0))
        val y = listOf(0.0, 0.0, 1.0, 1.0)
        val m = LogisticRegression.train(x = x, y = y, epochs = 500, l2 = 0.0)
        assertTrue(m.weights[0] > 0.0)
        assertTrue(m.probability(listOf(2.0)) > 0.8)
        assertTrue(m.probability(listOf(-2.0)) < 0.2)
    }

    @Test
    fun normalizacaoUsaMediaEDesvio() {
        val s = Standardizer.fit(listOf(listOf(1.0, 5.0), listOf(3.0, 5.0)))
        assertEquals(listOf(2.0, 5.0), s.means)
        assertEquals(listOf(1.0, 1.0), s.stds) // segunda coluna é constante: desvio vira 1
        assertEquals(listOf(1.0, 0.0), s.transform(listOf(3.0, 5.0)))
    }

    @Test
    fun featuresOlhamSoOPassado() {
        val dias = historico(10) { i, _ -> if (i == 8) 30 else 90 }
        val idx = RiskFeatures.index(dias)
        // Dia 9 (quarta 14/01): ontem foi o dia fraco (30%), sequência zerada.
        val v = RiskFeatures.vector(date = dias[9].date, history = idx, weakThreshold = 60)
        assertEquals(RiskFeatures.names.size, v.size)
        assertEquals(1.0, v[3], 0.0) // quarta
        assertEquals(0.3, v[7], 0.0) // ontem_percent
        assertEquals(0.0, v[8], 0.0) // ontem tem registro
        assertEquals(0.0, v[11], 0.0) // sequência
        // Dia 8 olhando pra trás: 7 dias bons seguidos antes dele (o dia 0 fica fora da janela de 7).
        val v8 = RiskFeatures.vector(date = dias[8].date, history = idx, weakThreshold = 60)
        assertEquals(8.0 / 14.0, v8[11], 0.0)
        assertEquals(1.0, v8[10], 0.0)
    }

    @Test
    fun riscoPrecisaDeDiasMinimos() {
        assertNull(RiskModel.evaluate(days = historico(20) { _, _ -> 90 }))
    }

    @Test
    fun riscoComPadraoDeSabadoEmpataComARegra() {
        // Sábado sempre fraco: a regra do dia da semana já acerta tudo, o modelo não pode "ganhar".
        val dias = historico(70) { _, d -> if (iosWeekday(d) == 7) 20 else 90 }
        val r = RiskModel.evaluate(days = dias)!!
        assertEquals(56, r.trainCount)
        assertEquals(14, r.validationCount)
        assertEquals(1.0, r.weekdayAccuracy, 0.0)
        assertEquals(1.0, r.modelAccuracy, 0.0)
        assertFalse(r.beatsBaseline)
        val sabado = dias[0].date.plusDays(75) // 2026-03-21, sábado
        assertTrue(RiskModel.riskProbability(date = sabado, days = dias, report = r) > 0.5)
    }

    @Test
    fun riscoComPadraoDeOntemGanhaDaRegra() {
        // Ciclo de 5 dias (3 bons, 2 fracos): não bate com a semana, mas "ontem" e a sequência explicam.
        val dias = historico(100) { i, _ -> if (i % 5 >= 3) 30 else 90 }
        val r = RiskModel.evaluate(days = dias)!!
        assertTrue(r.modelAccuracy > r.weekdayAccuracy)
        assertTrue(r.beatsBaseline)
    }

    // MARK: - Média por dia da semana

    @Test
    fun mediaPorDiaDaSemanaCalculaCorretamente() {
        // 14 dias (2 semanas completas): em cada dia da semana w, a 1ª semana tem 10*w e a 2ª tem 10*w + 10.
        val dias = historico(14) { i, d ->
            val w = iosWeekday(d)
            if (i < 7) w * 10 else w * 10 + 10
        }
        val medias = WeekdayStats.averages(days = dias, firstDayOfWeek = DayOfWeek.SUNDAY)
        assertEquals(7, medias.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), medias.map { it.weekday })
        for (item in medias) {
            assertEquals(2, item.count)
            assertEquals((item.weekday * 10 + 5).toDouble(), item.average, 0.0)
        }
    }

    @Test
    fun mediaPorDiaDaSemanaSegueFirstWeekdayEOmiteSemRegistro() {
        val semana = historico(7) { i, _ -> (i + 1) * 10 }
        // Só domingo (índice 6, weekday 1), segunda (índice 0, weekday 2) e quarta (índice 2, weekday 4).
        val dias = listOf(semana[6], semana[0], semana[2])
        val medias = WeekdayStats.averages(days = dias, firstDayOfWeek = DayOfWeek.MONDAY)
        assertEquals(listOf(2, 4, 1), medias.map { it.weekday })
        assertEquals(listOf(10.0, 30.0, 70.0), medias.map { it.average })
        assertEquals(listOf(1, 1, 1), medias.map { it.count })
    }

    // MARK: - Frase do dia (Hoje)

    @Test
    fun fraseDoDiaPrecisaDeDiasMinimos() {
        val dias = historico(20) { _, _ -> 90 }
        val amanha = dias[0].date.plusDays(20)
        assertNull(DayInsightBuilder.make(date = amanha, days = dias))
    }

    @Test
    fun fraseDoDiaIgnoraOProprioDia() {
        // 21 dias contando hoje = só 20 no passado, ainda não chega no mínimo.
        val dias = historico(21) { _, _ -> 90 }
        assertNull(DayInsightBuilder.make(date = dias[20].date, days = dias))
    }

    @Test
    fun fraseDoDiaUsaDiaDaSemanaQuandoModeloNaoGanha() {
        // Sábado sempre fraco: o modelo empata com a regra, então a frase usa a média do sábado.
        val dias = historico(70) { _, d -> if (iosWeekday(d) == 7) 20 else 90 }
        val sabado = dias[0].date.plusDays(75) // 2026-03-21
        val insight = DayInsightBuilder.make(date = sabado, days = dias)!!
        assertEquals(DayInsight.Kind.Weekday(average = 20, overall = 80), insight.kind)
        assertEquals(7, insight.weekday)
        assertEquals(70, insight.dayCount)
        assertTrue(insight.needsAttention)
    }

    @Test
    fun fraseDoDiaUsaModeloQuandoEleGanha() {
        // Ciclo de 5 dias: depois de 3 dias bons vem dia fraco. Dia 100 = posição 0 do ciclo (bom),
        // dia 103 = posição 3 (fraco, logo depois de 3 bons).
        val dias = historico(100) { i, _ -> if (i % 5 >= 3) 30 else 90 }
        val inicio = dias[0].date
        val bom = DayInsightBuilder.make(date = inicio.plusDays(100), days = dias)!!
        val kindBom = bom.kind as DayInsight.Kind.Model
        assertTrue(kindBom.risk < 0.5)
        assertTrue(kindBom.accuracy > 0.5)
        assertFalse(bom.needsAttention)

        // Histórico até o dia 102 (3 bons seguidos no fim), prevendo o 103.
        val ate103 = historico(103) { i, _ -> if (i % 5 >= 3 && i < 100) 30 else 90 }
        val fraco = DayInsightBuilder.make(date = inicio.plusDays(103), days = ate103)!!
        val kindFraco = fraco.kind as DayInsight.Kind.Model
        assertTrue(kindFraco.risk >= 0.5)
        assertTrue(fraco.needsAttention)
        assertTrue(kindFraco.factors.isNotEmpty() && kindFraco.factors.size <= 2)
    }

    @Test
    fun fatoresSomamODiaDaSemanaComoUmSo() {
        // Pesos só no one-hot do dia da semana e no sono: o dia da semana vem primeiro por somar mais.
        val w = MutableList(15) { 0.0 }.apply {
            this[0] = 1.0
            this[1] = 1.0
            this[12] = 1.5
        }
        val z = List(15) { 1.0 }
        assertEquals(
            listOf(DayInsightFactor.WEEKDAY, DayInsightFactor.SLEEP),
            DayInsightBuilder.topFactors(weights = w, z = z, towardsWeak = true),
        )
        assertTrue(DayInsightBuilder.topFactors(weights = w, z = z, towardsWeak = false).isEmpty())
    }
}
