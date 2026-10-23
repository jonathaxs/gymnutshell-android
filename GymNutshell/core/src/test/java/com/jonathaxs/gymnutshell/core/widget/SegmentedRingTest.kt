package com.jonathaxs.gymnutshell.core.widget

import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários da lógica pura do anel de 5 segmentos (SegmentedRing).
 * Valida frações por porcentagem (0%, 20%, 21%, 50%, 100%), cores e geometria.
 */
class SegmentedRingTest {

    @Test
    fun `fracoes de preenchimento para 0 por cento`() {
        val fracs = SegmentedRing.fractions(0)
        assertEquals(5, fracs.size)
        assertEquals(0f, fracs[0], 0.0001f)
        assertEquals(0f, fracs[1], 0.0001f)
        assertEquals(0f, fracs[2], 0.0001f)
        assertEquals(0f, fracs[3], 0.0001f)
        assertEquals(0f, fracs[4], 0.0001f)
    }

    @Test
    fun `fracoes de preenchimento para 20 por cento`() {
        val fracs = SegmentedRing.fractions(20)
        assertEquals(1f, fracs[0], 0.0001f)
        assertEquals(0f, fracs[1], 0.0001f)
        assertEquals(0f, fracs[2], 0.0001f)
        assertEquals(0f, fracs[3], 0.0001f)
        assertEquals(0f, fracs[4], 0.0001f)
    }

    @Test
    fun `fracoes de preenchimento para 21 por cento`() {
        val fracs = SegmentedRing.fractions(21)
        assertEquals(1f, fracs[0], 0.0001f)
        // 0.21 * 5 - 1 = 1.05 - 1 = 0.05
        assertEquals(0.05f, fracs[1], 0.001f)
        assertEquals(0f, fracs[2], 0.0001f)
        assertEquals(0f, fracs[3], 0.0001f)
        assertEquals(0f, fracs[4], 0.0001f)
    }

    @Test
    fun `fracoes de preenchimento para 50 por cento`() {
        val fracs = SegmentedRing.fractions(50)
        assertEquals(1f, fracs[0], 0.0001f)
        assertEquals(1f, fracs[1], 0.0001f)
        // 0.50 * 5 - 2 = 2.5 - 2 = 0.5
        assertEquals(0.5f, fracs[2], 0.0001f)
        assertEquals(0f, fracs[3], 0.0001f)
        assertEquals(0f, fracs[4], 0.0001f)
    }

    @Test
    fun `fracoes de preenchimento para 100 por cento`() {
        val fracs = SegmentedRing.fractions(100)
        assertEquals(1f, fracs[0], 0.0001f)
        assertEquals(1f, fracs[1], 0.0001f)
        assertEquals(1f, fracs[2], 0.0001f)
        assertEquals(1f, fracs[3], 0.0001f)
        assertEquals(1f, fracs[4], 0.0001f)
    }

    @Test
    fun `cores dos 5 segmentos sao identicas ao iOS`() {
        assertEquals(5, SegmentedRing.SEGMENT_COLORS.size)
        assertEquals(0xFFFE6E55L, SegmentedRing.SEGMENT_COLORS[0]) // Vermelho
        assertEquals(0xFFFEC239L, SegmentedRing.SEGMENT_COLORS[1]) // Amarelo
        assertEquals(0xFF46DA7FL, SegmentedRing.SEGMENT_COLORS[2]) // Verde
        assertEquals(0xFF3CA6FCL, SegmentedRing.SEGMENT_COLORS[3]) // Azul
        assertEquals(0xFF47468FL, SegmentedRing.SEGMENT_COLORS[4]) // Violeta/Roxo
    }

    @Test
    fun `cor por tier mapeia cada nivel para a cor correspondente`() {
        assertEquals(0xFFFE6E55L, SegmentedRing.colorForTier(DailyAchievement.Level1))
        assertEquals(0xFFFEC239L, SegmentedRing.colorForTier(DailyAchievement.Level2))
        assertEquals(0xFF46DA7FL, SegmentedRing.colorForTier(DailyAchievement.Level3))
        assertEquals(0xFF3CA6FCL, SegmentedRing.colorForTier(DailyAchievement.Level4))
        assertEquals(0xFF47468FL, SegmentedRing.colorForTier(DailyAchievement.Level5))
    }

    @Test
    fun `cor por progresso segue a cor do segmento atual`() {
        assertEquals(0xFFFE6E55L, SegmentedRing.colorForProgress(0.0))
        assertEquals(0xFFFEC239L, SegmentedRing.colorForProgress(0.20))
        assertEquals(0xFFFEC239L, SegmentedRing.colorForProgress(0.21))
        assertEquals(0xFF46DA7FL, SegmentedRing.colorForProgress(0.50))
        assertEquals(0xFF47468FL, SegmentedRing.colorForProgress(1.0))
    }

    @Test
    fun `geometria dos 5 segmentos calcula arcos e lacunas corretamente`() {
        val radius = 40f
        val stroke = 10f
        val segments = SegmentedRing.segments(progress = 0.50, radius = radius, strokeWidth = stroke)

        assertEquals(5, segments.size)

        val gap = SegmentedRing.gapDegrees(radius, stroke)
        assertTrue("Lacuna deve ser positiva", gap > 0f)
        assertTrue("Lacuna deve ser menor que o vão do segmento (72 graus)", gap < 72f)

        val expectedTotalSweep = 72f - gap
        for (seg in segments) {
            assertEquals(expectedTotalSweep, seg.sweepTotal, 0.001f)
        }

        // 50% de progresso: segmentos 0 e 1 cheios, 2 pela metade, 3 e 4 vazios
        assertEquals(expectedTotalSweep, segments[0].sweepFilled, 0.001f)
        assertEquals(expectedTotalSweep, segments[1].sweepFilled, 0.001f)
        assertEquals(expectedTotalSweep * 0.5f, segments[2].sweepFilled, 0.001f)
        assertEquals(0f, segments[3].sweepFilled, 0.0001f)
        assertEquals(0f, segments[4].sweepFilled, 0.0001f)
    }
}
