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

    /** Espessura do anel no ícone do app: 18% do lado (igual ao iconLineWidthRatio do iOS). */
    const val ICON_LINE_WIDTH_RATIO = 0.18f

    /** Contorno da parte preenchida: 6% da espessura, no mínimo 1 (igual ao iOS). */
    fun rimWidth(strokeWidth: Float): Float = maxOf(strokeWidth * 0.06f, 1f)

    /** Paleta de cada segmento, medida no ícone do app — porte de RingSegmentPalette (iOS). */
    val PALETTES: List<RingSegmentPalette> = listOf(
        RingSegmentPalette(light = 0xFFFD866CL, base = 0xFFFE6E55L, dark = 0xFFF25C48L, rimOnLight = 0xFF7F382BL, rimOnDark = 0xFFFFB9ADL),
        RingSegmentPalette(light = 0xFFFEDB56L, base = 0xFFFEC239L, dark = 0xFFF6AD2AL, rimOnLight = 0xFF83631BL, rimOnDark = 0xFFFBE29CL),
        RingSegmentPalette(light = 0xFF6CF4A0L, base = 0xFF46DA7FL, dark = 0xFF38C86EL, rimOnLight = 0xFF267143L, rimOnDark = 0xFFA4EFC0L),
        RingSegmentPalette(light = 0xFF67CEFDL, base = 0xFF3CA6FCL, dark = 0xFF2D92EAL, rimOnLight = 0xFF1E547FL, rimOnDark = 0xFF9FD4FFL),
        RingSegmentPalette(light = 0xFF5C57A8L, base = 0xFF47468FL, dark = 0xFF3B3A7EL, rimOnLight = 0xFF272650L, rimOnDark = 0xFFA5A3C8L),
    )

    /** Cor base dos 5 segmentos (também cor dos níveis), no sentido horário: vermelho, amarelo, verde, azul, roxo. */
    val SEGMENT_COLORS: List<Long> = PALETTES.map { it.base }

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
     * Espaço entre segmentos em graus — compensa as pontas arredondadas + um respiro de 22% da espessura.
     * Fórmula idêntica ao iOS: `(lineWidth * 1.22 / max(radius, 1)) * 180 / .pi`.
     */
    fun gapDegrees(radius: Float, strokeWidth: Float): Float {
        val r = maxOf(radius, 1f)
        return (strokeWidth * 1.22f / r) * (180f / PI.toFloat())
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

/** Cores de um segmento (ARGB cru): gradiente claro -> escuro, base e contorno no claro/escuro. */
data class RingSegmentPalette(
    val light: Long,
    val base: Long,
    val dark: Long,
    val rimOnLight: Long,
    val rimOnDark: Long,
)

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
