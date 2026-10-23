package com.jonathaxs.gymnutshell.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.ui.achievements.AchievementsScreen
import com.jonathaxs.gymnutshell.ui.achievements.EditRecordScreen
import com.jonathaxs.gymnutshell.ui.history.NotificationHistoryScreen
import com.jonathaxs.gymnutshell.ui.progress.ProgressScreen
import com.jonathaxs.gymnutshell.ui.settings.AddGoalScreen
import com.jonathaxs.gymnutshell.ui.settings.CategoriesScreen
import com.jonathaxs.gymnutshell.ui.settings.GoalDetailScreen
import com.jonathaxs.gymnutshell.ui.settings.GoalsScreen
import com.jonathaxs.gymnutshell.ui.settings.ProfileSettingsScreen
import com.jonathaxs.gymnutshell.ui.settings.SettingsRoutes
import com.jonathaxs.gymnutshell.ui.settings.ThemeSettingsScreen
import com.jonathaxs.gymnutshell.ui.settings.UserGoalScreen
import com.jonathaxs.gymnutshell.ui.settings.settingsGraph
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.today.TodayScreen

/**
 * Tela principal do app — 2.0 (porte da MainView do iOS).
 * Um NavHost raiz único com a tela Hoje de destino inicial.
 * Histórico e Conquistas abrem em ModalBottomSheet com NavHost próprio e botão Fechar no topo esquerdo.
 * Ajustes, Histórico de notificações e Edição de registro ficam como páginas na pilha raiz.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    pendingRoute: String? = null,
    onRouteConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val router = rememberAppRouter(navController)
    val accent = viewModel.accentColor.collectAsStateWithLifecycle().value.color

    // Deep-link de notificação: consome sem flags pendentes nem delays.
    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            router.navigateForRoute(pendingRoute)
            onRouteConsumed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Pilha de navegação raiz: Hoje é a tela inicial; Ajustes, notificações e edição empilham como páginas.
        NavHost(
            navController = navController,
            startDestination = MainRoutes.TODAY,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(MainRoutes.TODAY) {
                TodayScreen(
                    accent = accent,
                    onOpenSettings = { router.openSettings() },
                    onOpenHistory = { router.openSheet(AppSheet.History) },
                    onOpenAchievements = { router.openSheet(AppSheet.Achievements) },
                    onOpenTheme = { navController.navigate(SettingsRoutes.THEME) },
                    onOpenColors = { navController.navigate(SettingsRoutes.COLOR) },
                    onOpenControls = { navController.navigate(SettingsRoutes.CONTROLS) },
                    onOpenGoals = { navController.navigate(SettingsRoutes.CUSTOM_GOALS) },
                )
            }
            settingsGraph(navController)
            composable(MainRoutes.NOTIFICATIONS) {
                NotificationHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoute = { route, _ -> router.navigateForRoute(route) },
                )
            }
            // Alias para rotas legadas que apontavam para "history"
            composable("history") {
                NotificationHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRoute = { route, _ -> router.navigateForRoute(route) },
                )
            }
            composable(
                route = "${MainRoutes.EDIT_RECORD}/{epochDay}",
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
            ) { entry ->
                EditRecordScreen(
                    epochDay = entry.arguments?.getLong("epochDay") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Sheet de Histórico ou Conquistas: ModalBottomSheet com altura máxima e NavHost próprio.
        val currentSheet = router.sheet
        if (currentSheet != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { router.closeSheet() },
                sheetState = sheetState,
                modifier = Modifier.fillMaxHeight(),
                dragHandle = null,
                // Fundo agrupado (como os Ajustes) pra os cards aparecerem, igual ao iOS.
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                val sheetNavController = rememberNavController()
                val sheetStart = when (currentSheet) {
                    AppSheet.History -> SheetRoutes.HISTORY
                    AppSheet.Achievements -> SheetRoutes.ACHIEVEMENTS
                }

                // Botão "Fechar" no topo esquerdo da sheet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { router.closeSheet() }) {
                        Text(stringResource(R.string.common_close))
                    }
                }

                NavHost(
                    navController = sheetNavController,
                    startDestination = sheetStart,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    composable(SheetRoutes.HISTORY) {
                        ProgressScreen(
                            onOpenTheme = { sheetNavController.navigate(SettingsRoutes.THEME) },
                            onOpenGoals = { sheetNavController.navigate(SettingsRoutes.CUSTOM_GOALS) },
                            onOpenUserGoal = { sheetNavController.navigate(SettingsRoutes.GOAL) },
                            onOpenAchievements = { sheetNavController.navigate(SheetRoutes.ACHIEVEMENTS) },
                        )
                    }
                    composable(SheetRoutes.ACHIEVEMENTS) {
                        AchievementsScreen(
                            onOpenHistory = {
                                router.closeSheet()
                                router.openNotifications()
                            },
                            onEditRecord = { epochDay ->
                                sheetNavController.navigate("${MainRoutes.EDIT_RECORD}/$epochDay")
                            },
                            onBack = if (currentSheet == AppSheet.History) {
                                { sheetNavController.popBackStack() }
                            } else null,
                        )
                    }
                    composable(
                        route = "${MainRoutes.EDIT_RECORD}/{epochDay}",
                        arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
                    ) { entry ->
                        EditRecordScreen(
                            epochDay = entry.arguments?.getLong("epochDay") ?: 0L,
                            onBack = { sheetNavController.popBackStack() },
                        )
                    }
                    composable(SettingsRoutes.THEME) {
                        ThemeSettingsScreen(onBack = { sheetNavController.popBackStack() })
                    }
                    composable(SettingsRoutes.GOAL) {
                        UserGoalScreen(onBack = { sheetNavController.popBackStack() })
                    }
                    composable(SettingsRoutes.PROFILE) {
                        ProfileSettingsScreen(onBack = { sheetNavController.popBackStack() })
                    }
                    composable(SettingsRoutes.CUSTOM_GOALS) {
                        GoalsScreen(
                            onBack = { sheetNavController.popBackStack() },
                            onAddGoal = { sheetNavController.navigate(SettingsRoutes.ADD_GOAL) },
                            onEditGoal = { id -> sheetNavController.navigate("${SettingsRoutes.ADD_GOAL}?goalId=$id") },
                            onOpenGoalDetail = { key -> sheetNavController.navigate("${SettingsRoutes.GOAL_DETAIL}/$key") },
                            onOpenCategories = { sheetNavController.navigate(SettingsRoutes.CATEGORIES) },
                        )
                    }
                    composable(
                        route = "${SettingsRoutes.ADD_GOAL}?goalId={goalId}",
                        arguments = listOf(
                            navArgument("goalId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                    ) { entry ->
                        AddGoalScreen(
                            goalId = entry.arguments?.getString("goalId")?.toLongOrNull(),
                            onBack = { sheetNavController.popBackStack() },
                        )
                    }
                    composable(
                        route = "${SettingsRoutes.GOAL_DETAIL}/{key}",
                        arguments = listOf(navArgument("key") { type = NavType.StringType }),
                    ) { entry ->
                        GoalDetailScreen(
                            goalKey = entry.arguments?.getString("key").orEmpty(),
                            onBack = { sheetNavController.popBackStack() },
                        )
                    }
                    composable(SettingsRoutes.CATEGORIES) {
                        CategoriesScreen(onBack = { sheetNavController.popBackStack() })
                    }
                }
            }
        }
    }
}
