package com.jonathaxs.gymnutshell.core.widget

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Desenho do anel de 5 segmentos igual ao ícone do app, em `android.graphics.Canvas` — porte do corpo do
 * SegmentedProgressRing (iOS). Um só desenho pra Hoje (via nativeCanvas), widgets (Bitmap), prévias e Wear.
 *
 * `rect` é a caixa da linha central do traço (quem chama recua metade da espessura).
 * - Sem `borderArgb`: trilho na cor do segmento lavada (20%) com o contorno a 25%.
 * - Com `borderArgb` (widget com fundo colorido): trilho na cor do texto a 22% e contorno de 2 dp de cada lado só
 *   na parte preenchida.
 * A parte preenchida tem o contorno (cor escurecida no claro, clareada no escuro; sombra suave só no claro) e o
 * gradiente do segmento por cima, mais fino.
 */
object SegmentedRingPainter {

    fun draw(
        canvas: Canvas,
        rect: RectF,
        progress: Double,
        strokePx: Float,
        isDark: Boolean,
        borderArgb: Long? = null,
        borderExtraPx: Float = 0f,
    ) {
        val radius = rect.width() / 2f
        val rimPx = SegmentedRing.rimWidth(strokePx)
        val segments = SegmentedRing.segments(progress, radius, strokePx)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        for (segment in segments) {
            val palette = SegmentedRing.PALETTES[segment.index]
            val rim = if (isDark) palette.rimOnDark else palette.rimOnLight

            if (borderArgb != null) {
                // Fundo colorido: o trilho vira um vidro na cor do texto e só a parte preenchida ganha o contorno.
                stroke.reset(strokePx, withAlpha(borderArgb, 0.22f))
                canvas.drawArc(rect, segment.startAngle, segment.sweepTotal, false, stroke)
                if (segment.sweepFilled > 0.1f) {
                    stroke.reset(strokePx + 2f * borderExtraPx, borderArgb)
                    canvas.drawArc(rect, segment.startAngle, segment.sweepFilled, false, stroke)
                }
            } else {
                // Trilho do segmento na própria cor, lavada, com o contorno bem fraco
                stroke.reset(strokePx, withAlpha(palette.base, 0.20f))
                canvas.drawArc(rect, segment.startAngle, segment.sweepTotal, false, stroke)
                drawArcOutline(canvas, rect, segment.startAngle, segment.sweepTotal, strokePx - rimPx, rimPx, withAlpha(rim, 0.25f))
            }

            if (segment.sweepFilled > 0.1f) {
                // Contorno embaixo, com sombra suave só no claro
                stroke.reset(strokePx, rim)
                if (!isDark && borderArgb == null) {
                    stroke.setShadowLayer(strokePx * 0.12f, 0f, strokePx * 0.1f, 0x24000000)
                }
                canvas.drawArc(rect, segment.startAngle, segment.sweepFilled, false, stroke)

                // Gradiente do canto de cima/esquerda (claro) ao de baixo/direita (escuro) do segmento cheio
                val fromTop = segment.startAngle + 90f
                val (from, to) = gradientCorners(fromTop, fromTop + segment.sweepTotal, strokePx / 2f / (radius * 2f))
                stroke.reset(strokePx - rimPx * 2f, palette.light)
                stroke.shader = LinearGradient(
                    rect.left + from.first * rect.width(), rect.top + from.second * rect.height(),
                    rect.left + to.first * rect.width(), rect.top + to.second * rect.height(),
                    palette.light.toInt(), palette.dark.toInt(), Shader.TileMode.CLAMP,
                )
                canvas.drawArc(rect, segment.startAngle, segment.sweepFilled, false, stroke)
            }
        }
    }

    /** Contorno (linha fina) em volta de um arco de pontas redondas com a espessura dada. */
    private fun drawArcOutline(
        canvas: Canvas,
        rect: RectF,
        startAngle: Float,
        sweep: Float,
        bandWidth: Float,
        lineWidth: Float,
        argb: Long,
    ) {
        val arc = Path().apply { addArc(rect, startAngle, sweep) }
        val band = Path()
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = bandWidth
            strokeCap = Paint.Cap.ROUND
        }.getFillPath(arc, band)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = lineWidth
            color = argb.toInt()
        }
        canvas.drawPath(band, paint)
    }

    /**
     * Cantos da caixa do segmento inteiro (traço incluído), relativos ao quadro do arco (0..1). Ângulos em graus
     * a partir do topo, no sentido horário. Porte de gradientCorners (iOS).
     */
    internal fun gradientCorners(startDegrees: Float, endDegrees: Float, halfWidth: Float): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        // Pontas do arco + topo/direita/base/esquerda do círculo que caírem dentro dele.
        val angles = mutableListOf(startDegrees, endDegrees)
        for (extreme in listOf(0f, 90f, 180f, 270f, 360f)) {
            if (extreme > startDegrees && extreme < endDegrees) angles.add(extreme)
        }
        val xs = angles.map { 0.5f + 0.5f * sin(it * PI.toFloat() / 180f) }
        val ys = angles.map { 0.5f - 0.5f * cos(it * PI.toFloat() / 180f) }
        return (xs.min() - halfWidth to ys.min() - halfWidth) to (xs.max() + halfWidth to ys.max() + halfWidth)
    }

    private fun Paint.reset(width: Float, argb: Long) {
        shader = null
        clearShadowLayer()
        strokeWidth = width
        color = argb.toInt()
    }

    private fun withAlpha(argb: Long, alpha: Float): Long =
        ((alpha * 255).toLong().coerceIn(0, 255) shl 24) or (argb and 0xFFFFFFL)
}
