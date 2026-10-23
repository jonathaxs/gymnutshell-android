package com.jonathaxs.gymnutshell.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.jonathaxs.gymnutshell.core.domain.AppDateFormatters
import com.jonathaxs.gymnutshell.core.domain.NotificationRoute
import com.jonathaxs.gymnutshell.core.widget.WidgetSnapshot
import com.jonathaxs.gymnutshell.core.widget.WidgetSnapshotBuilder
import java.time.Instant
import java.time.ZoneId

/**
 * Widget "Progresso" (Glance) — porte do GymNutshellWidget (iOS): mostra o progresso do dia e o tier.
 * Pequeno (≈2x2): anel com o emoji + % embaixo. Médio (≈4x2): anel com o emoji + % e a frase do próximo nível.
 * Tocar abre o app na tela Hoje (reusa o deep link de EXTRA_ROUTE).
 */
class ProgressWidget : GlanceAppWidget() {

    // Exact: o conteúdo recebe o tamanho real (LocalSize) e escolhe o layout pequeno/médio.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Monta o snapshot lendo os repositórios direto (mesmo processo do app).
        val snapshot = WidgetSnapshotBuilder.build(context)
        provideContent {
            GlanceTheme {
                Content(context, snapshot)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Content(context: Context, snapshot: WidgetSnapshot) {
        val size = LocalSize.current
        val textColor = widgetTextColor(snapshot)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .then(widgetBackgroundModifier(snapshot))
                .cornerRadius(16.dp)
                .clickable(actionStartActivity(appIntent(context, NotificationRoute.Today)))
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (size.width < 200.dp) {
                SmallLayout(context, snapshot, textColor)
            } else {
                MediumLayout(context, snapshot, textColor)
            }
        }
    }

    /** Pequeno: anel com o emoji dentro, no alto e centralizado; a % centralizada embaixo (systemSmall do iOS). */
    @androidx.compose.runtime.Composable
    private fun SmallLayout(context: Context, snapshot: WidgetSnapshot, textColor: ColorProvider) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(ringBitmap(context, snapshot, sizeDp = 90)),
                contentDescription = null,
                modifier = GlanceModifier.size(90.dp),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                "${snapshot.progressPercent}%",
                maxLines = 1,
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, color = textColor),
            )
        }
    }

    /**
     * Médio: anel com o emoji dentro à esquerda; ao lado a % e, embaixo dela, quanto falta pro próximo nível
     * (a mesma frase do hero da Hoje), numa linha só (systemMedium do iOS).
     */
    @androidx.compose.runtime.Composable
    private fun MediumLayout(context: Context, snapshot: WidgetSnapshot, textColor: ColorProvider) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(ringBitmap(context, snapshot, sizeDp = 104)),
                contentDescription = null,
                modifier = GlanceModifier.size(104.dp),
            )
            Spacer(GlanceModifier.width(14.dp))
            Column(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "${snapshot.progressPercent}%",
                    maxLines = 1,
                    style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Bold, color = textColor),
                )
                Text(
                    nextLevelText(context, snapshot.progressPercent),
                    maxLines = 1,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = textColor),
                )
            }
        }
    }

    private fun ringBitmap(context: Context, snapshot: WidgetSnapshot, sizeDp: Int) = WidgetRing.bitmap(
        context = context,
        sizeDp = sizeDp,
        progress = snapshot.progressNormalized.toFloat(),
        emoji = snapshot.tierEmoji,
        borderArgb = widgetBorderArgb(snapshot),
    )

    /** Data do snapshot formatada (padrão médio do app). */
    private fun dateLabel(epochMillis: Long): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        return AppDateFormatters.mediumDate(date)
    }
}
