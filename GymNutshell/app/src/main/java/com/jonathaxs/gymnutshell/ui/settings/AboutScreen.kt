package com.jonathaxs.gymnutshell.ui.settings

import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.ImageView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.ui.theme.color

/**
 * Sobre o app — porte da AboutView (iOS): ícone com easter egg, versão, descrição
 * e botão centralizado para enviar feedback por e-mail.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val accent = viewModel.accentColor.collectAsStateWithLifecycle().value.color
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val view = LocalView.current

    // Versão lida do PackageManager (mesma fonte do CFBundleShortVersionString no iOS)
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "-"
    }

    // Easter egg: 5 toques no ícone do app dentro de 2 s reabrem o Welcome
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val onIconTap: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 2_000L) {
            tapCount = 0
        }
        lastTapTime = now
        tapCount += 1

        if (tapCount >= 5) {
            tapCount = 0
            lastTapTime = 0L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            viewModel.resetOnboarding()
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.settings_about_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            // Ícone do app com suporte a toques do easter egg
            AppIcon(onTap = onIconTap)

            Spacer(Modifier.height(12.dp))
            Text(
                text = "${stringResource(R.string.settings_about_version)} $versionName",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = accent,
            )

            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(32.dp))

            // Botão centralizado de feedback por e-mail
            OutlinedButton(
                onClick = {
                    uriHandler.openUri("mailto:jonathaxs@icloud.com")
                },
                shape = CircleShape,
                border = BorderStroke(1.5.dp, accent),
                modifier = Modifier
                    .height(48.dp)
                    .padding(horizontal = 24.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings_envelope),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.settings_about_feedback),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = accent,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AppIcon(onTap: () -> Unit) {
    val context = LocalContext.current
    val appIcon = remember {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
        }.getOrNull()
    }
    val iconDescription = stringResource(R.string.a11y_app_icon)

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            )
            .semantics { contentDescription = iconDescription },
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    setImageDrawable(appIcon)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
