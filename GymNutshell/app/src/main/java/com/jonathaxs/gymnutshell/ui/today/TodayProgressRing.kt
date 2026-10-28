package com.jonathaxs.gymnutshell.ui.today

import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
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
import com.jonathaxs.gymnutshell.core.widget.SegmentedRingPainter
import android.graphics.RectF
import kotlinx.coroutines.delay

/**
 * Anel de progresso da TodayView em 5 segmentos arredondados — porte de SegmentedProgressRing e
 * TodayProgressRingView (iOS), desenhado igual ao ícone do app.
 *
 * Usa [SegmentedRing] do :core pra geometria e paleta. Espessura = 18% do lado. No centro, a % do dia
 * (apagada em 0%). Quando o progresso entra numa cor nova, o anel inteiro pulsa uma vez.
 *
 * `celebratesAfterOnboarding` (só no hero): logo depois da Welcome, o anel enche, pulsa e volta ao progresso real.
 */
@Composable
fun TodayProgressRing(
    progress: Float,
    percent: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    ringSize: Dp = 150.dp,
    celebratesAfterOnboarding: Boolean = false,
) {
    val strokeWidth = ringSize * SegmentedRing.ICON_LINE_WIDTH_RATIO
    val context = LocalContext.current

    // Progresso forçado durante a comemoração pós-Welcome (null = mostra o progresso real).
    var celebrationProgress by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        if (!celebratesAfterOnboarding || !RingCelebration.consume()) return@LaunchedEffect
        // Animações desligadas no sistema (equivale ao Reduzir Movimento do iOS): sem comemoração.
        val animationsOff = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        if (animationsOff) return@LaunchedEffect
        delay(800)
        celebrationProgress = 1f
        delay(1800)
        celebrationProgress = null
    }

    val clampedProgress = (celebrationProgress ?: progress).coerceIn(0f, 1f)
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
            val haptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            }
            view.performHapticFeedback(haptic)
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
 * metade da espessura). O desenho em si é o [SegmentedRingPainter] do :core, o mesmo dos widgets e do Wear.
 */
internal fun DrawScope.drawSegmentedRing(progress: Double, strokePx: Float, isDark: Boolean) {
    drawIntoCanvas { canvas ->
        SegmentedRingPainter.draw(
            canvas = canvas.nativeCanvas,
            rect = RectF(0f, 0f, size.width, size.height),
            progress = progress,
            strokePx = strokePx,
            isDark = isDark,
        )
    }
}

/** Pedido único da comemoração do anel (marcado ao concluir a Welcome) — porte do `pendingRingCelebration` do iOS. */
object RingCelebration {
    @Volatile var pending: Boolean = false

    fun consume(): Boolean = pending.also { pending = false }
}
