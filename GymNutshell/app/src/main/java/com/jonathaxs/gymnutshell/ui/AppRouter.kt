package com.jonathaxs.gymnutshell.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.jonathaxs.gymnutshell.core.domain.NotificationRoute
import com.jonathaxs.gymnutshell.ui.settings.SettingsRoutes

/**
 * Telas que abrem em sheet por cima da Hoje (espelha AppSheet do iOS).
 */
enum class AppSheet {
    History,
    Achievements,
}

/**
 * Rotas de navegação da pilha raiz (Today, Notificações e Edição de registro).
 */
object MainRoutes {
    const val TODAY = "today"
    const val NOTIFICATIONS = "notifications"
    const val EDIT_RECORD = "edit_record"
}

/**
 * Rotas de navegação internas da sheet.
 */
object SheetRoutes {
    const val HISTORY = "sheet_history"
    const val ACHIEVEMENTS = "sheet_achievements"
}

/**
 * Roteador de navegação do app (porte do AppRouter do iOS).
 * Controla a sheet aberta por cima da Hoje e roteia a navegação de deep links e telas de sistema.
 */
class AppRouter(
    val rootNavController: NavHostController,
) {
    /** Sheet aberta por cima da Hoje (null = nenhuma). */
    var sheet by mutableStateOf<AppSheet?>(null)
        private set

    /** Abre a sheet especificada (Histórico ou Conquistas). */
    fun openSheet(newSheet: AppSheet) {
        sheet = newSheet
    }

    /** Fecha a sheet ativa. */
    fun closeSheet() {
        sheet = null
    }

    /**
     * Abre a raiz de Ajustes: fecha a sheet (se aberta) e navega na pilha raiz.
     */
    fun openSettings() {
        closeSheet()
        rootNavController.navigate(SettingsRoutes.GRAPH)
    }

    /**
     * Abre a tela de histórico de notificações na pilha raiz.
     */
    fun openNotifications() {
        closeSheet()
        rootNavController.navigate(MainRoutes.NOTIFICATIONS)
    }

    /**
     * Processa rotas de deep links vindas de notificações:
     * - Today: fecha a sheet e desempilha a raiz até Hoje.
     * - AchievementsToday: abre a sheet de Conquistas.
     * - Backup: fecha a sheet e abre Ajustes -> Backup na pilha raiz.
     * Sem flags pendentes nem delays.
     */
    fun navigateForRoute(routeRaw: String) {
        when (NotificationRoute.fromRaw(routeRaw)) {
            NotificationRoute.Today -> {
                closeSheet()
                rootNavController.popBackStack(MainRoutes.TODAY, inclusive = false)
            }
            NotificationRoute.AchievementsToday -> {
                openSheet(AppSheet.Achievements)
            }
            NotificationRoute.Backup -> {
                closeSheet()
                rootNavController.navigate(SettingsRoutes.BACKUP) {
                    launchSingleTop = true
                }
            }
            null -> Unit
        }
    }
}

/**
 * Lembra a instância de [AppRouter] vinculada ao [rootNavController].
 */
@Composable
fun rememberAppRouter(rootNavController: NavHostController): AppRouter {
    return remember(rootNavController) { AppRouter(rootNavController) }
}
