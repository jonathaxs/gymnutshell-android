package com.jonathaxs.gymnutshell

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.jonathaxs.gymnutshell.core.data.DailyRecordRepository
import com.jonathaxs.gymnutshell.core.data.NotificationPreferencesRepository
import com.jonathaxs.gymnutshell.core.data.ProfileRepository
import com.jonathaxs.gymnutshell.core.data.SettingsRepository
import com.jonathaxs.gymnutshell.notifications.GymNotifier
import com.jonathaxs.gymnutshell.notifications.NotificationScheduler
import com.jonathaxs.gymnutshell.ui.RootScreen
import com.jonathaxs.gymnutshell.ui.theme.GymNutshellTheme
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.util.AppLocales
import com.jonathaxs.gymnutshell.wear.PhoneWearSync
import com.jonathaxs.gymnutshell.widget.GymWidgets
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    // Rota pendente vinda do toque numa notificação (deep-link), observada pelo Compose.
    private var pendingRoute by mutableStateOf<String?>(null)

    // Resultado do pedido de permissão de notificações (Android 13+). Ao conceder, aplica os defaults.
    // Aplica o idioma escolhido no app antes de tudo (Android 12 e abaixo; no 13+ o framework cuida).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocales.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 2.0: migracoes de uma vez so, antes da primeira tela ler os dados.
        runBlocking {
            ProfileRepository(applicationContext).migrateLegacyProfile()
            SettingsRepository(applicationContext).migrateLegacyUkSystem()
            DailyRecordRepository(applicationContext).migrateToFiveLevelsIfNeeded()
        }
        enableEdgeToEdge()
        maybeRequestNotificationsPermission()
        pendingRoute = intent?.getStringExtra(GymNotifier.EXTRA_ROUTE)
        setContent {
            val accentFlow = remember { SettingsRepository(applicationContext).accentColor }
            val accent by accentFlow.collectAsState(initial = null)
            GymNutshellTheme(accent = accent?.color) {
                RootScreen(
                    pendingRoute = pendingRoute,
                    onRouteConsumed = { pendingRoute = null },
                )
            }
        }
    }

    // App já aberto (launchMode singleTop): captura a rota do novo Intent.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.getStringExtra(GymNotifier.EXTRA_ROUTE)
    }

    // Ao sair do app, atualiza os widgets e publica o snapshot pro relógio com o
    // estado mais recente — espelha o "scenePhase background" do iOS.
    override fun onStop() {
        super.onStop()
        GymWidgets.update(applicationContext)
        PhoneWearSync.push(applicationContext)
    }

    /**
     * Com a permissão de notificações já dada, liga os tipos padrão (uma vez só). O pedido em si fica no
     * Tudo pronto da Welcome e em Ajustes > Notificações, como no iOS: não pede mais ao abrir o app.
     */
    private fun maybeRequestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (NotificationManagerCompat.from(this).areNotificationsEnabled()) applyNotificationDefaults()
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) applyNotificationDefaults()
    }

    private fun applyNotificationDefaults() {
        lifecycleScope.launch {
            NotificationPreferencesRepository(applicationContext).applyDefaultEnabledKindsOnce()
            // Re-arma os lembretes por intervalo a cada launch (cobre a virada de dia).
            NotificationScheduler(applicationContext).rescheduleAllActive()
        }
    }
}
