package com.jonathaxs.gymnutshell.ui.settings

import com.jonathaxs.gymnutshell.ui.util.rememberIsTablet
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.ui.MainRoutes

/** Rotas da Settings. GRAPH casa com o route da aba (MainTab.Settings). */
object SettingsRoutes {
    const val GRAPH = "settings"
    const val LIST = "settings_list"
    const val COLOR = "settings_color"
    const val PROFILE = "settings_profile"
    const val GOAL = "settings_goal"
    const val THEME = "settings_theme"
    const val CONTROLS = "settings_controls"
    const val CUSTOM_GOALS = "settings_custom_goals" // hub de Goals (porte de TrackingGoalsSettingsView)
    const val ADD_GOAL = "settings_add_goal" // + "?goalId={goalId}" pra editar
    const val GOAL_DETAIL = "settings_goal_detail" // + "/{key}"
    const val CATEGORIES = "settings_categories"
    const val UNITS = "settings_units"
    const val NOTIFICATIONS = "settings_notifications"
    const val NOTIF_EDIT = "settings_notif_edit" // + "/{target}"
    const val HEALTH = "settings_health"
    const val BACKUP = "settings_backup"
    const val LANGUAGE = "settings_language"
    const val WIDGET_BG = "settings_widget_bg"
    const val ABOUT = "settings_about"
    const val WEAR = "settings_wear"
    const val RING_INFO = "settings_ring_info"
    const val TIER_INFO = "settings_tier_info"
}

/** Grafo aninhado da Settings: lista → sub-telas. */
fun NavGraphBuilder.settingsGraph(navController: NavController) {
    navigation(startDestination = SettingsRoutes.LIST, route = SettingsRoutes.GRAPH) {
        composable(SettingsRoutes.LIST) {
            // Tela larga (mesma regra da Hoje): lista à esquerda e a página escolhida à direita, como os Ajustes do
            // iPad. Subpáginas (ex.: Metas -> detalhe) empilham em tela cheia.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val wide = maxWidth >= 700.dp || (maxWidth >= 560.dp && rememberIsTablet())
                val listWidth = if (maxWidth < 744.dp) 300.dp else 340.dp
                var selected by rememberSaveable { mutableStateOf(SettingsRoutes.CUSTOM_GOALS) }
                val open: (String) -> Unit = { route -> if (wide) selected = route else navController.navigate(route) }
                val list = @Composable {
                    SettingsListScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCustomGoals = { open(SettingsRoutes.CUSTOM_GOALS) },
                        onOpenGoal = { open(SettingsRoutes.GOAL) },
                        onOpenProfile = { open(SettingsRoutes.PROFILE) },
                        onOpenTheme = { open(SettingsRoutes.THEME) },
                        onOpenColor = { open(SettingsRoutes.COLOR) },
                        onOpenWidgetBackground = { open(SettingsRoutes.WIDGET_BG) },
                        onOpenControls = { open(SettingsRoutes.CONTROLS) },
                        onOpenHealth = { open(SettingsRoutes.HEALTH) },
                        onOpenNotifications = { open(SettingsRoutes.NOTIFICATIONS) },
                        onOpenNotificationHistory = { navController.navigate(MainRoutes.NOTIFICATIONS) },
                        onOpenUnits = { open(SettingsRoutes.UNITS) },
                        onOpenBackup = { open(SettingsRoutes.BACKUP) },
                        onOpenLanguage = { open(SettingsRoutes.LANGUAGE) },
                        onOpenAbout = { open(SettingsRoutes.ABOUT) },
                        onOpenWear = { open(SettingsRoutes.WEAR) },
                        onOpenRingInfo = { open(SettingsRoutes.RING_INFO) },
                        onOpenTierInfo = { open(SettingsRoutes.TIER_INFO) },
                        selectedRoute = if (wide) selected else null,
                    )
                }
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.width(listWidth).fillMaxHeight()) { list() }
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            CompositionLocalProvider(LocalSettingsInPane provides true) {
                                // key: cada página com o próprio estado (e ViewModel) ao trocar.
                                key(selected) { SettingsPage(selected, navController) }
                            }
                        }
                    }
                } else {
                    list()
                }
            }
        }
        composable(SettingsRoutes.COLOR) { SettingsPage(SettingsRoutes.COLOR, navController) }
        composable(SettingsRoutes.PROFILE) { SettingsPage(SettingsRoutes.PROFILE, navController) }
        composable(SettingsRoutes.GOAL) { SettingsPage(SettingsRoutes.GOAL, navController) }
        composable(SettingsRoutes.THEME) { SettingsPage(SettingsRoutes.THEME, navController) }
        composable(SettingsRoutes.CONTROLS) { SettingsPage(SettingsRoutes.CONTROLS, navController) }
        composable(SettingsRoutes.CUSTOM_GOALS) { SettingsPage(SettingsRoutes.CUSTOM_GOALS, navController) }
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
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = "${SettingsRoutes.GOAL_DETAIL}/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
        ) { entry ->
            GoalDetailScreen(
                goalKey = entry.arguments?.getString("key").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
        composable(SettingsRoutes.CATEGORIES) { SettingsPage(SettingsRoutes.CATEGORIES, navController) }
        composable(SettingsRoutes.UNITS) { SettingsPage(SettingsRoutes.UNITS, navController) }
        composable(SettingsRoutes.NOTIFICATIONS) { SettingsPage(SettingsRoutes.NOTIFICATIONS, navController) }
        composable(SettingsRoutes.HEALTH) { SettingsPage(SettingsRoutes.HEALTH, navController) }
        composable(SettingsRoutes.BACKUP) { SettingsPage(SettingsRoutes.BACKUP, navController) }
        composable(SettingsRoutes.LANGUAGE) { SettingsPage(SettingsRoutes.LANGUAGE, navController) }
        composable(SettingsRoutes.WIDGET_BG) { SettingsPage(SettingsRoutes.WIDGET_BG, navController) }
        composable(SettingsRoutes.ABOUT) { SettingsPage(SettingsRoutes.ABOUT, navController) }
        composable(SettingsRoutes.WEAR) { SettingsPage(SettingsRoutes.WEAR, navController) }
        composable(SettingsRoutes.RING_INFO) { SettingsPage(SettingsRoutes.RING_INFO, navController) }
        composable(SettingsRoutes.TIER_INFO) { SettingsPage(SettingsRoutes.TIER_INFO, navController) }
        composable(
            route = "${SettingsRoutes.NOTIF_EDIT}/{target}",
            arguments = listOf(navArgument("target") { type = NavType.StringType }),
        ) { entry ->
            NotificationIntervalEditScreen(
                target = entry.arguments?.getString("target").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/** Página de Ajustes por rota — a mesma na pilha (celular) e na direita da lista (tela larga). */
@Composable
fun SettingsPage(route: String, navController: NavController) {
    // No painel da tela larga não há pra onde voltar (a seta some); páginas que voltam sozinhas depois de salvar
    // ficam onde estão.
    val inPane = LocalSettingsInPane.current
    val back: () -> Unit = { if (!inPane) navController.popBackStack() }
    when (route) {
        SettingsRoutes.COLOR -> {
        ColorSettingsScreen(onBack = back)
        }
        SettingsRoutes.PROFILE -> {
        ProfileSettingsScreen(onBack = back)
        }
        SettingsRoutes.GOAL -> {
        UserGoalScreen(onBack = back)
        }
        SettingsRoutes.THEME -> {
        ThemeSettingsScreen(onBack = back)
        }
        SettingsRoutes.CONTROLS -> {
        ControlStyleSettingsScreen(onBack = back)
        }
        SettingsRoutes.CUSTOM_GOALS -> {
        GoalsScreen(
            onBack = back,
            onAddGoal = { navController.navigate(SettingsRoutes.ADD_GOAL) },
            onEditGoal = { id -> navController.navigate("${SettingsRoutes.ADD_GOAL}?goalId=$id") },
            onOpenGoalDetail = { key -> navController.navigate("${SettingsRoutes.GOAL_DETAIL}/$key") },
            onOpenCategories = { navController.navigate(SettingsRoutes.CATEGORIES) },
        )
        }
        SettingsRoutes.CATEGORIES -> {
        CategoriesScreen(onBack = back)
        }
        SettingsRoutes.UNITS -> {
        MeasurementScreen(onBack = back)
        }
        SettingsRoutes.NOTIFICATIONS -> {
        NotificationsSettingsScreen(
            onBack = back,
            onEditTarget = { target -> navController.navigate("${SettingsRoutes.NOTIF_EDIT}/$target") },
        )
        }
        SettingsRoutes.HEALTH -> {
        HealthSettingsScreen(onBack = back)
        }
        SettingsRoutes.BACKUP -> {
        BackupScreen(onBack = back)
        }
        SettingsRoutes.LANGUAGE -> {
        LanguageScreen(onBack = back)
        }
        SettingsRoutes.WIDGET_BG -> {
        WidgetBackgroundScreen(onBack = back)
        }
        SettingsRoutes.ABOUT -> {
        AboutScreen(onBack = back)
        }
        SettingsRoutes.WEAR -> {
        WearInstructionsScreen(onBack = back)
        }
        SettingsRoutes.RING_INFO -> {
        ProgressRingInfoScreen(onBack = back)
        }
        SettingsRoutes.TIER_INFO -> {
        TierInfoScreen(
            onBack = back,
            onOpenTheme = { navController.navigate(SettingsRoutes.THEME) },
        )
        }
    }
}

/** true dentro do painel da direita da tela larga: as páginas não mostram a seta de voltar. */
val LocalSettingsInPane = staticCompositionLocalOf { false }

/** Top bar das telas de Settings; mostra o botão de voltar quando `onBack` é fornecido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title) },
        actions = actions,
        navigationIcon = {
            if (onBack != null && !LocalSettingsInPane.current) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            }
        },
    )
}
