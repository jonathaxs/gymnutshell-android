package com.jonathaxs.gymnutshell.ui.today

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Anel de progresso da TodayView em 5 segmentos arredondados — porte de SegmentedProgressRing e
 * TodayProgressRingView (iOS).
 *
 * Utiliza [SegmentedRing] do :core para cálculo de geometria (ângulos, lacunas e cores dos 5 arcos).
 * Cada segmento possui trilha lavada na própria cor (tintedTrack).
 * No centro, exibe a porcentagem do dia (apagada enquanto estiver em 0%).
 * Ao completar um segmento, realiza um pop suave de escala.
 */
@Composable
fun TodayProgressRing(
    progress: Float,
    percent: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    ringSize: Dp = 150.dp,
    strokeWidth: Dp = 16.dp,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val hasStarted = percent > 0

    // Animação suave do progresso
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 450),
        label = "ringProgress",
    )

    // Detecção de segmento completado para animação de pop
    val completedCount = (clampedProgress * SegmentedRing.SEGMENT_COUNT + 0.0001f).toInt()
    val popScale = remember { Animatable(1f) }
    var prevCompleted by remember { mutableIntStateOf(completedCount) }

    LaunchedEffect(completedCount) {
        if (completedCount > prevCompleted && prevCompleted >= 0) {
            popScale.animateTo(1.08f, tween(160))
            popScale.animateTo(1.0f, spring(dampingRatio = 0.6f))
        }
        prevCompleted = completedCount
    }

    val a11yLabel = stringResource(R.string.today_ring_a11y_label)
    val a11yValue = stringResource(R.string.today_ring_a11y_value, percent)
    val a11yHint = stringResource(R.string.today_ring_a11y_hint)

    Box(
        modifier = modifier
            .size(ringSize)
            .scale(popScale.value)
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
            val strokePx = strokeWidth.toPx()
            val radius = (size.minDimension) / 2f
            val segments = SegmentedRing.segments(
                progress = animatedProgress.toDouble(),
                radius = radius,
                strokeWidth = strokePx,
            )

            val arcSize = Size(size.width, size.height)
            val topLeft = Offset.Zero

            for (segment in segments) {
                val color = Color(segment.colorArgb)
                val trackColor = color.copy(alpha = 0.20f)

                // Trilha na cor do segmento lavada (tintedTrack)
                drawArc(
                    color = trackColor,
                    startAngle = segment.startAngle,
                    sweepAngle = segment.sweepTotal,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )

                // Segmento preenchido na cor cheia
                if (segment.sweepFilled > 0.1f) {
                    drawArc(
                        color = color,
                        startAngle = segment.startAngle,
                        sweepAngle = segment.sweepFilled,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )
                }
            }
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
