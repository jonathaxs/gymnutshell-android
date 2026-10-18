package com.jonathaxs.gymnutshell.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.jonathaxs.gymnutshell.core.widget.SegmentedRing

/**
 * Desenha o anel de progresso em 5 segmentos como Bitmap — o Glance não tem Canvas arbitrário,
 * então o anel vira uma imagem mostrada via Image(ImageProvider).
 *
 * 5 arcos arredondados (vermelho, amarelo, verde, azul, violeta), com trilho cinza e
 * contorno fino opcional para fundos customizados (espelha o borderColor do iOS).
 */
object WidgetRing {

    fun bitmap(
        context: Context,
        sizeDp: Int,
        progress: Float,
        emoji: String,
        strokeDp: Float = 10f,
        borderArgb: Long? = null,
        ringArgb: Long? = null,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val stroke = strokeDp * density
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Borda de contraste (fundos custom/accent): stroke 1dp mais largo por lado atrás do anel,
        // igual ao `lineWidth + 2` do iOS. Recolhe o rect pra caber os 1dp extras sem clipar.
        val borderExtra = if (borderArgb != null) density else 0f
        val inset = stroke / 2f + borderExtra
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val radius = rect.width() / 2f

        val segments = SegmentedRing.segments(progress.toDouble(), radius, stroke)

        // 1. Contorno de contraste opcional atrás dos segmentos
        if (borderArgb != null) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke + 2f * borderExtra
                strokeCap = Paint.Cap.ROUND
                color = borderArgb.toInt()
            }
            for (seg in segments) {
                canvas.drawArc(rect, seg.startAngle, seg.sweepTotal, false, borderPaint)
            }
        }

        // 2. Trilho de fundo cinza para cada segmento (18% de opacidade, igual ao iOS)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = 0x2E808080.toInt()
        }
        for (seg in segments) {
            canvas.drawArc(rect, seg.startAngle, seg.sweepTotal, false, trackPaint)
        }

        // 3. Parte preenchida de cada segmento na cor correspondente
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        for (seg in segments) {
            if (seg.sweepFilled > 0.001f) {
                fillPaint.color = seg.colorArgb.toInt()
                canvas.drawArc(rect, seg.startAngle, seg.sweepFilled, false, fillPaint)
            }
        }

        // 4. Emoji do tier centralizado
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = sizePx * 0.34f
        }
        val cx = sizePx / 2f
        val cy = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(emoji, cx, cy, textPaint)

        return bmp
    }
}
