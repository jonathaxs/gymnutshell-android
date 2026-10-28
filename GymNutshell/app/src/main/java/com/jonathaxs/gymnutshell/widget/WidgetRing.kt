package com.jonathaxs.gymnutshell.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import com.jonathaxs.gymnutshell.core.widget.SegmentedRing
import com.jonathaxs.gymnutshell.core.widget.SegmentedRingPainter

/**
 * Desenha o anel de progresso como Bitmap — o Glance não tem Canvas arbitrário, então o anel vira uma imagem
 * mostrada via Image(ImageProvider). Porte do PercentProgressRing (iOS): mesmo anel da Hoje (espessura 18%,
 * trilho lavado), com o emoji no centro quando passado, senão a %.
 *
 * Em fundo colorido (`borderArgb`), o trilho vira a cor do texto a 22% e só a parte preenchida ganha o contorno.
 */
object WidgetRing {

    fun bitmap(
        context: Context,
        sizeDp: Int,
        progress: Float,
        emoji: String? = null,
        percent: Int = 0,
        textArgb: Long = 0xFF000000,
        borderArgb: Long? = null,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt().coerceAtLeast(1)
        val bmp = createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        val stroke = sizePx * SegmentedRing.ICON_LINE_WIDTH_RATIO
        // Contorno de contraste: 2 dp de cada lado; recolhe o quadro pra caber sem cortar.
        val borderExtra = if (borderArgb != null) 2f * density else 0f
        val inset = stroke / 2f + borderExtra
        SegmentedRingPainter.draw(
            canvas = canvas,
            rect = RectF(inset, inset, sizePx - inset, sizePx - inset),
            progress = progress.toDouble(),
            strokePx = stroke,
            isDark = isDark,
            borderArgb = borderArgb,
            borderExtraPx = borderExtra,
        )

        // Centro: emoji da conquista ou a % (apagada em 0%)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        val text: String
        if (emoji != null) {
            text = emoji
            textPaint.textSize = sizePx * 0.36f
        } else {
            text = "$percent%"
            textPaint.textSize = sizePx * 0.22f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val alpha = if (percent > 0) 0xFF else 0x80
            textPaint.color = ((alpha.toLong() shl 24) or (textArgb and 0xFFFFFFL)).toInt()
        }
        // Cabe dentro do furo do anel
        val maxWidth = sizePx - 2f * (stroke + borderExtra) - 4f * density
        val measured = textPaint.measureText(text)
        if (measured > maxWidth) textPaint.textSize *= maxWidth / measured
        val cx = sizePx / 2f
        val cy = sizePx / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text, cx, cy, textPaint)

        return bmp
    }
}
