package com.jonathaxs.gymnutshell.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Dobra do aparelho (Pixel Fold e afins) — equivalente Android do `reservedRegions(kind: .division)` do iOS 27.1.
 * `bounds` em px na janela; `halfOpened` = meio aberto (livro ou mesa); `vertical` = dobradiça em pé (livro).
 */
data class FoldInfo(val bounds: Rect, val halfOpened: Boolean, val vertical: Boolean)

/** Lê a dobra atual pelo Jetpack WindowManager; null em aparelho sem dobra (ou fora de uma Activity). */
@Composable
fun rememberFoldInfo(): FoldInfo? {
    val activity = LocalContext.current.findActivity() ?: return null
    var fold by remember { mutableStateOf<FoldInfo?>(null) }
    LaunchedEffect(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
            fold = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let { f ->
                FoldInfo(
                    bounds = Rect(
                        f.bounds.left.toFloat(), f.bounds.top.toFloat(),
                        f.bounds.right.toFloat(), f.bounds.bottom.toFloat(),
                    ),
                    halfOpened = f.state == FoldingFeature.State.HALF_OPENED,
                    vertical = f.orientation == FoldingFeature.Orientation.VERTICAL,
                )
            }
        }
    }
    return fold
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
