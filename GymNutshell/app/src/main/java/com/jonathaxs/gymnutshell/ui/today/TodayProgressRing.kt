package com.jonathaxs.gymnutshell.ui.today

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.widget.SegmentedRing
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anel de progresso da TodayView em 5 segmentos arredondados — porte de SegmentedProgressRing e
 * TodayProgressRingView (iOS), desenhado igual ao ícone do app.
 *
 * Usa [SegmentedRing] do :core pra geometria e paleta. Espessura = 18% do lado. No centro, a % do dia
 * (apagada em 0%). Quando o progresso entra numa cor nova, o anel inteiro pulsa uma vez.
 */
@Composable
fun TodayProgressRing(
    progress: Float,
    percent: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    ringSize: Dp = 150.dp,
) {
    val strokeWidth = ringSize * SegmentedRing.ICON_LINE_WIDTH_RATIO
    val clampedProgress = progress.coerceIn(0f, 1f)
    val hasStarted = percent > 0
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    // Animação suave do progresso
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 450),
        label = "ringProgress",
    )

    // Entrou numa cor nova (ex.: vermelho -> amarelo): o anel inteiro pulsa uma vez, com vibração.
    val completedCount = (clampedProgress * SegmentedRing.SEGMENT_COUNT + 0.0001f).toInt()
    val pulseScale = remember { Animatable(1f) }
    var prevCompleted by remember { mutableIntStateOf(completedCount) }

    LaunchedEffect(completedCount) {
        if (completedCount > prevCompleted) {
            delay(350)
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            pulseScale.animateTo(1.08f, spring(dampingRatio = 0.5f, stiffness = 1000f))
            pulseScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 250f))
        }
        prevCompleted = completedCount
    }

    val a11yLabel = stringResource(R.string.today_ring_a11y_label)
    val a11yValue = stringResource(R.string.today_ring_a11y_value, percent)
    val a11yHint = stringResource(R.string.today_ring_a11y_hint)

    Box(
        modifier = modifier
            .size(ringSize)
            .scale(pulseScale.value)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .semantics {
                            role = Role.Button
                            contentDescription = a11yLabel
                            stateDescription = "$a11yValue. $a11yHint"
                        }
                } else {
                    Modifier.semantics {
                        contentDescription = a11yLabel
                        stateDescription = a11yValue
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(strokeWidth / 2f)) {
            drawSegmentedRing(animatedProgress.toDouble(), strokeWidth.toPx(), isDark)
        }

        // Porcentagem no centro (apagada em 0%)
        Text(
            text = "$percent%",
            fontSize = (ringSize.value * 0.22f).sp,
            fontWeight = FontWeight.Bold,
            color = if (hasStarted) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )
    }
}

/**
 * Desenha os 5 segmentos no quadro atual (o quadro é a linha central do traço, então quem chama recua
 * metade da espessura). Trilho lavado com contorno fraco; parte preenchida com contorno (sombra suave no
 * claro) e o gradiente do segmento por cima, mais fino.
 */
internal fun DrawScope.drawSegmentedRing(progress: Double, strokePx: Float, isDark: Boolean) {
    val radius = size.minDimension / 2f
    val rimPx = SegmentedRing.rimWidth(strokePx)
    val segments = SegmentedRing.segments(progress = progress, radius = radius, strokeWidth = strokePx)

    for (segment in segments) {
        val palette = SegmentedRing.PALETTES[segment.index]
        val rim = Color(if (isDark) palette.rimOnDark else palette.rimOnLight)

        // Trilho na cor do segmento, lavada, com o contorno a 25%
        drawArc(
            color = Color(palette.base).copy(alpha = 0.20f),
            startAngle = segment.startAngle,
            sweepAngle = segment.sweepTotal,
            useCenter = false,
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        drawArcOutline(segment.startAngle, segment.sweepTotal, strokePx - rimPx, rimPx, rim.copy(alpha = 0.25f))

        if (segment.sweepFilled > 0.1f) {
            // Contorno embaixo, com sombra suave só no claro
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = strokePx
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    color = rim.toArgb()
                    if (!isDark) {
                        setShadowLayer(strokePx * 0.12f, 0f, strokePx * 0.1f, Color.Black.copy(alpha = 0.14f).toArgb())
                    }
                }
                canvas.nativeCanvas.drawArc(
                    0f, 0f, size.width, size.height,
                    segment.startAngle, segment.sweepFilled, false, paint,
                )
            }
            // Gradiente do canto de cima/esquerda (claro) ao de baixo/direita (escuro) do segmento cheio
            val (from, to) = gradientCorners(segment.startAngle + 90f, segment.startAngle + 90f + segment.sweepTotal, strokePx / 2f / (radius * 2f))
            drawArc(
                brush = Brush.linearGradient(
                    colors = listOf(Color(palette.light), Color(palette.dark)),
                    start = Offset(from.x * size.width, from.y * size.height),
                    end = Offset(to.x * size.width, to.y * size.height),
                ),
                startAngle = segment.startAngle,
                sweepAngle = segment.sweepFilled,
                useCenter = false,
                style = Stroke(width = strokePx - rimPx * 2f, cap = StrokeCap.Round),
            )
        }
    }
}

/** Contorno (linha fina) em volta de um arco de pontas redondas com a espessura dada. */
private fun DrawScope.drawArcOutline(startAngle: Float, sweep: Float, bandWidth: Float, lineWidth: Float, color: Color) {
    drawIntoCanvas { canvas ->
        val arc = android.graphics.Path().apply {
            addArc(0f, 0f, size.width, size.height, startAngle, sweep)
        }
        val band = android.graphics.Path()
        android.graphics.Paint().apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = bandWidth
            strokeCap = android.graphics.Paint.Cap.ROUND
        }.getFillPath(arc, band)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = lineWidth
            this.color = color.toArgb()
        }
        canvas.nativeCanvas.drawPath(band, paint)
    }
}

/**
 * Cantos da caixa do segmento inteiro (traço incluído), relativos ao quadro do arco (0..1). Ângulos em graus
 * a partir do topo, no sentido horário. Porte de gradientCorners (iOS).
 */
private fun gradientCorners(startDegrees: Float, endDegrees: Float, halfWidth: Float): Pair<Offset, Offset> {
    // Pontas do arco + topo/direita/base/esquerda do círculo que caírem dentro dele.
    val angles = mutableListOf(startDegrees, endDegrees)
    for (extreme in listOf(0f, 90f, 180f, 270f, 360f)) {
        if (extreme > startDegrees && extreme < endDegrees) angles.add(extreme)
    }
    val xs = angles.map { 0.5f + 0.5f * sin(it * PI.toFloat() / 180f) }
    val ys = angles.map { 0.5f - 0.5f * cos(it * PI.toFloat() / 180f) }
    return Offset(xs.min() - halfWidth, ys.min() - halfWidth) to Offset(xs.max() + halfWidth, ys.max() + halfWidth)
}
