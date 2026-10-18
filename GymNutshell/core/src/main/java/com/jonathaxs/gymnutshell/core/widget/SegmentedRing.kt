package com.jonathaxs.gymnutshell.core.widget

import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import kotlin.math.PI

/**
 * Lógica pura do anel de progresso em 5 segmentos arredondados — porte de SegmentedProgressRing (iOS).
 * Livre de dependência de UI: define cores (ARGB cru), ângulos, lacunas e preenchimento de cada segmento.
 *
 * Sentido horário a partir do topo: vermelho → amarelo → verde → azul → roxo/violeta (20% cada).
 */
object SegmentedRing {

    const val SEGMENT_COUNT = 5

    /** Cores fixas dos 5 segmentos, na ordem do sentido horário (iguais ao iOS). */
    val SEGMENT_COLORS: List<Long> = listOf(
        0xFFFF3B30L, // 1º Vermelho (Red)
        0xFFFFCC00L, // 2º Amarelo (Yellow)
        0xFF34C759L, // 3º Verde (Green)
        0xFF007AFFL, // 4º Azul (Blue)
        0xFFAF52DEL, // 5º Violeta / Roxo (Purple)
    )

    /** Retorna a cor ARGB de um segmento pelo índice (0..4). */
    fun colorForIndex(index: Int): Long =
        SEGMENT_COLORS[index.coerceIn(0, SEGMENT_COUNT - 1)]

    /** Cor do segmento em que o progresso está (0.0..1.0), igual ao ringColor do WatchHeroView (iOS). */
    fun colorForProgress(progress: Double): Long {
        val clamped = progress.coerceIn(0.0, 1.0)
        val index = (clamped * SEGMENT_COUNT).toInt().coerceIn(0, SEGMENT_COUNT - 1)
        return SEGMENT_COLORS[index]
    }

    /** Retorna a cor ARGB do segmento correspondente a uma porcentagem (0..100). */
    fun colorForPercent(percent: Int): Long = colorForProgress(percent / 100.0)

    /** Retorna a cor ARGB do nível de conquista (Level 1..5). */
    fun colorForTier(tier: DailyAchievement): Long = SEGMENT_COLORS[tier.number - 1]

    /**
     * Fração de preenchimento (0.0f..1.0f) de um segmento específico para um dado progresso.
     */
    fun fraction(index: Int, progress: Double): Float {
        val clamped = progress.coerceIn(0.0, 1.0)
        return (clamped * SEGMENT_COUNT - index).coerceIn(0.0, 1.0).toFloat()
    }

    /** Fração de preenchimento a partir de porcentagem inteira (0..100). */
    fun fraction(index: Int, percent: Int): Float = fraction(index, percent / 100.0)

    /**
     * Lista com a fração de preenchimento (0.0f..1.0f) de cada um dos 5 segmentos.
     */
    fun fractions(progress: Double): List<Float> {
        val clamped = progress.coerceIn(0.0, 1.0)
        return List(SEGMENT_COUNT) { index ->
            (clamped * SEGMENT_COUNT - index).coerceIn(0.0, 1.0).toFloat()
        }
    }

    /** Frações a partir de porcentagem inteira. */
    fun fractions(percent: Int): List<Float> = fractions(percent / 100.0)

    /**
     * Espaço entre segmentos em graus — compensa as pontas arredondadas + um respiro visível de 4pt.
     * Fórmula idêntica ao iOS: `((lineWidth + 4) / max(radius, 1)) * 180 / .pi`.
     */
    fun gapDegrees(radius: Float, strokeWidth: Float): Float {
        val r = maxOf(radius, 1f)
        return ((strokeWidth + 4f) / r) * (180f / PI.toFloat())
    }

    /**
     * Calcula a geometria completa de todos os 5 segmentos para desenho em Canvas.
     *
     * Ângulos são formatados para o Canvas do Android (0° em 3 horas, topo em -90°).
     */
    fun segments(progress: Double, radius: Float, strokeWidth: Float): List<SegmentGeometry> {
        val span = 360f / SEGMENT_COUNT
        val gap = gapDegrees(radius, strokeWidth)
        val totalSweep = maxOf(0f, span - gap)
        val fracs = fractions(progress)

        return List(SEGMENT_COUNT) { index ->
            val startFromTop = index * span + gap / 2f
            val startAngle = -90f + startFromTop
            val frac = fracs[index]
            val filledSweep = totalSweep * frac
            SegmentGeometry(
                index = index,
                startAngle = startAngle,
                sweepTotal = totalSweep,
                sweepFilled = filledSweep,
                fraction = frac,
                colorArgb = SEGMENT_COLORS[index],
            )
        }
    }
}

/**
 * Geometria calculada de um segmento do anel para desenho em Canvas.
 */
data class SegmentGeometry(
    val index: Int,
    val startAngle: Float,
    val sweepTotal: Float,
    val sweepFilled: Float,
    val fraction: Float,
    val colorArgb: Long,
)
