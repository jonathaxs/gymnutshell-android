package com.jonathaxs.gymnutshell.ui.welcome

import androidx.compose.animation.scaleOut
import kotlin.math.cos
import kotlin.math.PI
import kotlinx.coroutines.delay
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.ui.components.PermissionCard
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.ThemeCategory
import com.jonathaxs.gymnutshell.core.domain.UnitConverter
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.settings.ThemeDetailsButton
import com.jonathaxs.gymnutshell.ui.settings.categoryNameRes
import com.jonathaxs.gymnutshell.ui.settings.themeNameRes
import com.jonathaxs.gymnutshell.ui.theme.color
import kotlin.math.roundToInt

/**
 * Tela de onboarding Welcome 2.0 — porte de WelcomeView (iOS).
 * Fluxo: início → sobre você → objetivo → medidas (opcional) → metas → controles → tema → tudo pronto.
 */
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val animatedStepColor by animateColorAsState(state.stepColor, tween(400), label = "step_color")

    // Reavalia permissões reais ao voltar para a tela (espelha ON_RESUME do iOS)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Trata o botão voltar do sistema Android (espelha swipe-back e botão de rodapé)
    BackHandler(enabled = state.currentStep != WelcomeStep.Start) {
        viewModel.goBack()
    }

    // Seletor de backup JSON local
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restore(it) }
    }

    // Pedido de permissão de notificações (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    // Pedido de permissões do Health Connect
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedSet ->
        viewModel.onHealthPermissionResult(grantedSet.isNotEmpty())
    }

    // Botões do rodapé que viajam: cada etapa com espaço reservado (Início e Tudo pronto) avisa onde ele fica,
    // e os botões sobem do rodapé até lá. Só vale o aviso da etapa atual (a que sai não puxa os botões).
    val currentStep by rememberUpdatedState(state.currentStep)
    val slotTops = remember { mutableStateMapOf<WelcomeStep, Float>() }
    var footerTop by remember { mutableStateOf<Float?>(null) }
    var buttonsHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val slotReporter = remember {
        WelcomeSlotReporter { step, top -> if (step == currentStep) slotTops[step] = top }
    }
    val targetOffset = run {
        val slot = slotTops[state.currentStep]
        val rest = footerTop
        if (slot != null && rest != null) slot - rest else 0f
    }
    val buttonsOffset = remember { Animatable(0f) }
    var didPlaceButtons by remember { mutableStateOf(false) }
    LaunchedEffect(targetOffset, footerTop) {
        if (footerTop == null) return@LaunchedEffect
        if (!didPlaceButtons) {
            // Até saber onde fica o espaço do Início, os botões não aparecem (evita o pulo ao abrir).
            buttonsOffset.snapTo(targetOffset)
            didPlaceButtons = slotTops[state.currentStep] != null || state.currentStep != WelcomeStep.Start
        } else {
            buttonsOffset.animateTo(targetOffset, spring(dampingRatio = 0.85f, stiffness = 130f))
        }
    }
    // No Início os botões são mais estreitos (proporção do "Olá!"); ao descer pro rodapé se alargam.
    val buttonsMaxWidth by animateDpAsState(
        targetValue = if (state.currentStep == WelcomeStep.Start) 260.dp else 600.dp,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 130f),
        label = "welcome_buttons_width",
    )
    var showRestoreOptions by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Degradê vertical suave no topo tingido pela cor da etapa (espelha welcomeBackground do iOS)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(animatedStepColor.copy(alpha = 0.22f), Color.Transparent),
                            startY = 0f,
                            endY = 800f,
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Barra de progresso fixa no topo (etapa atual peso 3f, outras peso 1f)
                WelcomeProgressBar(
                    currentIndex = state.currentStepIndex,
                    totalSteps = state.visibleSteps.size,
                    accentColor = state.stepColor,
                )

                // Conteúdo da etapa atual com transição suave
                CompositionLocalProvider(
                    LocalWelcomeSlotReporter provides slotReporter,
                    LocalWelcomeButtonsHeight provides with(density) { buttonsHeightPx.toDp() },
                ) {
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    label = "welcome_step_content",
                ) { targetStep ->
                    when (targetStep) {
                        WelcomeStep.Start -> {
                            StartStep(accentColor = state.sexColor)
                        }
                        WelcomeStep.About -> {
                            AboutStep(
                                sex = state.sex,
                                favoriteExercise = state.favoriteExercise,
                                accentColor = state.sexColor,
                                onSelectSex = viewModel::setSex,
                                onSelectExercise = viewModel::setFavoriteExercise,
                            )
                        }
                        WelcomeStep.Goal -> {
                            GoalStep(
                                selectedGoal = state.userGoal,
                                onSelectGoal = viewModel::setUserGoal,
                            )
                        }
                        WelcomeStep.Units -> {
                            UnitsStep(
                                selection = state.measurementSystem,
                                accentColor = state.sexColor,
                                onSelectSystem = viewModel::setMeasurementSystem,
                            )
                        }
                        WelcomeStep.Summary -> {
                            SummaryStep(
                                state = state,
                                accent = state.stepColor,
                                viewModel = viewModel,
                            )
                        }
                        WelcomeStep.ControlStyle -> {
                            ControlStyleStep(
                                selection = state.controlStyle,
                                accentColor = state.sexColor,
                                onSelectStyle = viewModel::setControlStyle,
                            )
                        }
                        WelcomeStep.Theme -> {
                            ThemeStep(
                                selectedTheme = state.theme,
                                sex = state.sex,
                                accentColor = state.sexColor,
                                onSelectTheme = viewModel::setTheme,
                            )
                        }
                        WelcomeStep.Ready -> {
                            ReadyStep(
                                state = state,
                                accentColor = state.sexColor,
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.onNotificationPermissionResult(true)
                                    }
                                },
                                onRequestHealth = {
                                    healthPermissionLauncher.launch(viewModel.healthPermissions)
                                },
                            )
                        }
                    }
                }

                }

                // Um só par de botões pra todas as etapas: Começar / Restaurar backup no Início, Continuar / Voltar
                // nas demais e Vamos lá / Voltar no Tudo pronto (porte do footerButtons do iOS).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
                        // Medido sem o deslocamento: é a posição de repouso no rodapé.
                        .onGloballyPositioned { footerTop = it.positionInRoot().y },
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = buttonsMaxWidth)
                            .fillMaxWidth()
                            .onSizeChanged { buttonsHeightPx = it.height }
                            .offset { IntOffset(0, buttonsOffset.value.roundToInt()) }
                            .alpha(if (didPlaceButtons) 1f else 0f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val isStart = state.currentStep == WelcomeStep.Start
                        Button(
                            onClick = viewModel::advance,
                            enabled = state.canContinue,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = animatedStepColor,
                                disabledContainerColor = animatedStepColor.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    when {
                                        isStart -> R.string.welcome_start_new
                                        state.isLastStep -> R.string.welcome_button_start
                                        else -> R.string.welcome_button_continue
                                    }
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                            )
                        }

                        Box {
                            Button(
                                onClick = { if (isStart) showRestoreOptions = true else viewModel.goBack() },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = animatedStepColor.copy(alpha = 0.15f),
                                    contentColor = animatedStepColor,
                                ),
                                elevation = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isStart) R.string.welcome_start_restore else R.string.welcome_button_back
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                )
                            }
                            // Restaurar backup abre um balão logo abaixo, como o Detalhes dos emojis.
                            WelcomeRestoreMenu(
                                expanded = showRestoreOptions,
                                accentColor = animatedStepColor,
                                onDismiss = { showRestoreOptions = false },
                                onImportJson = {
                                    showRestoreOptions = false
                                    restoreLauncher.launch(arrayOf("application/json"))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // Alerta de falha na restauração do backup
    if (state.restoreFailed) {
        AlertDialog(
            onDismissRequest = viewModel::clearRestoreError,
            title = { Text(stringResource(R.string.welcome_restore_error_title)) },
            confirmButton = {
                TextButton(onClick = viewModel::clearRestoreError) {
                    Text(stringResource(R.string.action_ok))
                }
            },
        )
    }
}

// ============================================================================
// 1. Start Step
// ============================================================================

/** Início: esquilo + "Olá!" e, logo abaixo, o espaço dos botões Começar / Restaurar backup, centralizados. */
@Composable
private fun StartStep(accentColor: Color) {
    CenteredStepColumn {
        // O esquilo que corre atrás da noz (nutshell), como o da Era do Gelo.
        WelcomeStepHeader(
            emoji = "🐿️",
            title = stringResource(R.string.welcome_start_title),
            accentColor = accentColor,
        )
        Spacer(Modifier.height(28.dp))
        WelcomeButtonsSlot(WelcomeStep.Start)
    }
}

/** Coluna rolável que centraliza o conteúdo na altura quando ele cabe na tela. */
@Composable
private fun CenteredStepColumn(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content,
        )
    }
}

/** Balão de Restaurar backup (porte do WelcomeRestorePopover; no Android só há o arquivo JSON). */
@Composable
private fun WelcomeRestoreMenu(
    expanded: Boolean,
    accentColor: Color,
    onDismiss: () -> Unit,
    onImportJson: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.width(280.dp),
    ) {
        Text(
            text = stringResource(R.string.welcome_start_restore),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .semantics { heading() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.welcome_restore_import_json), style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_backup),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            },
            onClick = onImportJson,
        )
    }
}

// ============================================================================
// 2. About Step ("Sobre você")
// ============================================================================

@Composable
private fun AboutStep(
    sex: String,
    favoriteExercise: FavoriteExercise,
    accentColor: Color,
    onSelectSex: (String) -> Unit,
    onSelectExercise: (FavoriteExercise) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "👤",
            title = stringResource(R.string.welcome_step_about_title),
            accentColor = accentColor,
        )

        // Seletor de sexo com 2 cards grandes lado a lado (Feminino / Masculino)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.welcome_field_sex),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Feminino (cor lilás/purple do app)
                val femaleColor = AccentColor.Purple.color
                WelcomeCard(
                    isSelected = sex == "female",
                    accentColor = femaleColor,
                    onClick = { onSelectSex("female") },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "👩",
                            fontSize = 32.sp,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        Text(
                            text = stringResource(R.string.welcome_field_sex_female),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (sex == "female") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Masculino (cor azul do app)
                val maleColor = AccentColor.Blue.color
                WelcomeCard(
                    isSelected = sex == "male",
                    accentColor = maleColor,
                    onClick = { onSelectSex("male") },
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "👨",
                            fontSize = 32.sp,
                            modifier = Modifier.clearAndSetSemantics {},
                        )
                        Text(
                            text = stringResource(R.string.welcome_field_sex_male),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (sex == "male") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Menu suspenso de Exercício Favorito
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var expanded by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(WelcomeCardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(WelcomeCardRadius))
                        .clickable { expanded = true },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.settings_profile_favorite_exercise),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(favoriteExercise.labelRes),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = accentColor,
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    FavoriteExercise.entries.forEach { exercise ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(exercise.labelRes),
                                    fontWeight = if (exercise == favoriteExercise) FontWeight.Bold else FontWeight.Normal,
                                    color = if (exercise == favoriteExercise) accentColor else MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                onSelectExercise(exercise)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. Goal Step ("Seu objetivo")
// ============================================================================

@Composable
private fun GoalStep(
    selectedGoal: UserGoal,
    onSelectGoal: (UserGoal) -> Unit,
) {
    val selectedColor = goalColor(selectedGoal)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "🎯",
            title = stringResource(R.string.welcome_goal_title),
            accentColor = selectedColor,
        )

        // Bulking
        GoalOptionCard(
            goal = UserGoal.Bulking,
            title = stringResource(R.string.goal_bulking),
            description = stringResource(R.string.welcome_goal_bulking_desc),
            symbol = "↗",
            cardColor = goalColor(UserGoal.Bulking),
            isSelected = selectedGoal == UserGoal.Bulking,
            onClick = { onSelectGoal(UserGoal.Bulking) },
        )

        // Maintenance
        GoalOptionCard(
            goal = UserGoal.Maintenance,
            title = stringResource(R.string.goal_maintenance),
            description = stringResource(R.string.welcome_goal_maintenance_desc),
            symbol = "=",
            cardColor = goalColor(UserGoal.Maintenance),
            isSelected = selectedGoal == UserGoal.Maintenance,
            onClick = { onSelectGoal(UserGoal.Maintenance) },
        )

        // Cutting
        GoalOptionCard(
            goal = UserGoal.Cutting,
            title = stringResource(R.string.goal_cutting),
            description = stringResource(R.string.welcome_goal_cutting_desc),
            symbol = "↘",
            cardColor = goalColor(UserGoal.Cutting),
            isSelected = selectedGoal == UserGoal.Cutting,
            onClick = { onSelectGoal(UserGoal.Cutting) },
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.welcome_goal_info_editable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GoalOptionCard(
    goal: UserGoal,
    title: String,
    description: String,
    symbol: String,
    cardColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    WelcomeCard(
        isSelected = isSelected,
        accentColor = cardColor,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Quadrado colorido 36dp com ícone
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cardColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    ),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Checkmark circle
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                contentDescription = null,
                tint = if (isSelected) cardColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ============================================================================
// 4. Units Step ("Suas medidas")
// ============================================================================

@Composable
private fun UnitsStep(
    selection: MeasurementSystem,
    accentColor: Color,
    onSelectSystem: (MeasurementSystem) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "📏",
            title = stringResource(R.string.welcome_step_units_title),
            subtitle = stringResource(R.string.welcome_step_units_subtitle),
            note = stringResource(R.string.welcome_step_units_note),
            accentColor = accentColor,
        )

        // Imperial (EUA)
        val imperialDesc = stringResource(
            R.string.welcome_units_imperial_desc,
            UnitConverter.flOzWaterStep(),
        )
        UnitsCard(
            title = stringResource(R.string.unit_us),
            description = imperialDesc,
            isSelected = selection == MeasurementSystem.Us,
            accentColor = accentColor,
            onClick = { onSelectSystem(MeasurementSystem.Us) },
        )

        // Métrico
        UnitsCard(
            title = stringResource(R.string.unit_metric),
            description = stringResource(R.string.welcome_units_metric_desc),
            isSelected = selection == MeasurementSystem.Metric,
            accentColor = accentColor,
            onClick = { onSelectSystem(MeasurementSystem.Metric) },
        )
    }
}

@Composable
private fun UnitsCard(
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    WelcomeCard(
        isSelected = isSelected,
        accentColor = accentColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ============================================================================
// 5. Summary Step ("Suas metas", emoji 💪)
// ============================================================================

@Composable
private fun SummaryStep(
    state: WelcomeUiState,
    accent: Color,
    viewModel: WelcomeViewModel,
) {
    val scrollState = rememberScrollState()

    // Detecta quando a lista foi rolada até o final para destravar o botão Continuar
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 40) {
            viewModel.setSummaryScrolledToEnd(true)
        }
    }

    // Metas na cor do objetivo (fundo, barra, botões, Adicionar e os − / +), como no iOS.
    val goals = state.goals

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "💪",
            title = stringResource(R.string.welcome_summary_title),
            accentColor = accent,
        )

        // 1. Essencial
        SummaryCategorySection(title = stringResource(R.string.category_essencial)) {
            // Sono (passo 1, 1..16 h)
            SummaryGoalRow(
                emoji = "💤",
                title = stringResource(R.string.today_sleep),
                valueText = "${goals.sleep}h",
                accentColor = accent,
                canDecrease = goals.sleep > 1,
                canIncrease = goals.sleep < 16,
                onDecrease = { viewModel.adjustSleep(-1) },
                onIncrease = { viewModel.adjustSleep(1) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Água (passo 250, 250..10000 ml; em US exibe fl oz)
            val waterFormatted = if (state.measurementSystem == MeasurementSystem.Us) {
                "${UnitConverter.mlToFlOz(goals.water.toDouble()).roundToInt()} fl oz"
            } else {
                "${goals.water} ml"
            }
            SummaryGoalRow(
                emoji = "💧",
                title = stringResource(R.string.today_water),
                valueText = waterFormatted,
                accentColor = accent,
                canDecrease = goals.water > 250,
                canIncrease = goals.water < 10000,
                onDecrease = { viewModel.adjustWater(-250) },
                onIncrease = { viewModel.adjustWater(250) },
            )
        }

        // 2. Nutrição: Proteína, Carbo, Fibra, Gordura boa
        SummaryCategorySection(title = stringResource(R.string.category_nutricao)) {
            // Proteína (passo 5, 10..500 g)
            SummaryGoalRow(
                emoji = "🍗",
                title = stringResource(R.string.today_protein),
                valueText = "${goals.protein}g",
                accentColor = accent,
                canDecrease = goals.protein > 10,
                canIncrease = goals.protein < 500,
                onDecrease = { viewModel.adjustProtein(-5) },
                onIncrease = { viewModel.adjustProtein(5) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Carboidratos (passo 10, 10..1000 g)
            SummaryGoalRow(
                emoji = "🍞",
                title = stringResource(R.string.today_carbs),
                valueText = "${goals.carbs}g",
                accentColor = accent,
                canDecrease = goals.carbs > 10,
                canIncrease = goals.carbs < 1000,
                onDecrease = { viewModel.adjustCarbs(-10) },
                onIncrease = { viewModel.adjustCarbs(10) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Fibras (passo 1, 5..100 g)
            SummaryGoalRow(
                emoji = "🌾",
                title = stringResource(R.string.today_fiber),
                valueText = "${goals.fiber}g",
                accentColor = accent,
                canDecrease = goals.fiber > 5,
                canIncrease = goals.fiber < 100,
                onDecrease = { viewModel.adjustFiber(-1) },
                onIncrease = { viewModel.adjustFiber(1) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Gordura boa (opcional, começa desligada, passo 5, 5..300 g)
            SummaryOptionalGoalRow(
                emoji = "🧈",
                title = stringResource(R.string.today_good_fat),
                valueText = "${goals.goodFat}g",
                isIncluded = state.includeFats,
                accentColor = accent,
                canDecrease = goals.goodFat - 5 >= 5,
                canIncrease = goals.goodFat + 5 <= 300,
                onAdd = { viewModel.setIncludeFats(true) },
                onRemove = { viewModel.setIncludeFats(false) },
                onDecrease = { viewModel.adjustGoodFat(-5) },
                onIncrease = { viewModel.adjustGoodFat(5) },
            )
        }

        // 3. Treino: Treino de Força, Cardio
        SummaryCategorySection(title = stringResource(R.string.category_treino)) {
            // Treino (passo 15, 5..300 min)
            SummaryGoalRow(
                emoji = "🏋️",
                title = stringResource(R.string.today_workout),
                valueText = "${goals.workout} min",
                accentColor = accent,
                canDecrease = goals.workout > 5,
                canIncrease = goals.workout < 300,
                onDecrease = { viewModel.adjustWorkout(-15) },
                onIncrease = { viewModel.adjustWorkout(15) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Cardio (passo 5, 5..180 min)
            SummaryGoalRow(
                emoji = "🏃",
                title = stringResource(R.string.today_cardio),
                valueText = "${goals.cardio} min",
                accentColor = accent,
                canDecrease = goals.cardio > 5,
                canIncrease = goals.cardio < 180,
                onDecrease = { viewModel.adjustCardio(-5) },
                onIncrease = { viewModel.adjustCardio(5) },
            )
        }

        // 4. Suplemento: Creatina
        SummaryCategorySection(title = stringResource(R.string.category_suplemento)) {
            // Creatina (opcional, começa desligada, passo 1, 1..20 g)
            SummaryOptionalGoalRow(
                emoji = "🧪",
                title = stringResource(R.string.today_creatine),
                valueText = "${goals.creatine}g",
                isIncluded = state.includeCreatine,
                accentColor = accent,
                canDecrease = goals.creatine - 1 >= 1,
                canIncrease = goals.creatine + 1 <= 20,
                onAdd = { viewModel.setIncludeCreatine(true) },
                onRemove = { viewModel.setIncludeCreatine(false) },
                onDecrease = { viewModel.adjustCreatine(-1) },
                onIncrease = { viewModel.adjustCreatine(1) },
            )
        }

        // Texto informativo final
        Text(
            text = stringResource(R.string.welcome_summary_subtitle_customize),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun SummaryCategorySection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(WelcomeCardRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SummaryGoalRow(
    emoji: String,
    title: String,
    valueText: String,
    accentColor: Color,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(emoji, fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics {})
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                valueText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 6.dp),
            )
            RoundStepButton(
                symbol = "−",
                contentDescription = stringResource(R.string.a11y_welcome_goal_decrease, title),
                onClick = onDecrease,
                enabled = canDecrease,
                color = accentColor,
            )
            RoundStepButton(
                symbol = "+",
                contentDescription = stringResource(R.string.a11y_welcome_goal_increase, title),
                onClick = onIncrease,
                enabled = canIncrease,
                color = accentColor,
            )
        }
    }
}

@Composable
private fun SummaryOptionalGoalRow(
    emoji: String,
    title: String,
    valueText: String,
    isIncluded: Boolean,
    accentColor: Color,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Text(emoji, fontSize = 22.sp, modifier = Modifier.clearAndSetSemantics {})
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Adicionar encolhe e valor, lixeira, − e + surgem um a um com mola (0,06 s entre eles); a lixeira faz o
        // inverso (somem da direita pra esquerda e o Adicionar volta crescendo). Porte do iOS.
        var shownCount by remember { mutableIntStateOf(if (isIncluded) 4 else 0) }
        LaunchedEffect(isIncluded) {
            if (isIncluded) {
                while (shownCount < 4) { delay(60); shownCount++ }
            } else {
                while (shownCount > 0) { shownCount--; delay(60) }
            }
        }
        val pop = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
        Box(contentAlignment = Alignment.CenterEnd) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimatedVisibility(shownCount > 0, enter = scaleIn(pop) + fadeIn(), exit = scaleOut(pop) + fadeOut()) {
                    Text(
                        valueText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                // Lixeira vermelha redonda
                AnimatedVisibility(shownCount > 1, enter = scaleIn(pop) + fadeIn(), exit = scaleOut(pop) + fadeOut()) {
                    RoundStepButton(
                        symbol = "",
                        contentDescription = stringResource(R.string.a11y_welcome_optional_remove, title),
                        onClick = onRemove,
                        isTrash = true,
                    )
                }
                AnimatedVisibility(shownCount > 2, enter = scaleIn(pop) + fadeIn(), exit = scaleOut(pop) + fadeOut()) {
                    RoundStepButton(
                        symbol = "−",
                        contentDescription = stringResource(R.string.a11y_welcome_goal_decrease, title),
                        onClick = onDecrease,
                        enabled = canDecrease,
                        color = accentColor,
                    )
                }
                AnimatedVisibility(shownCount > 3, enter = scaleIn(pop) + fadeIn(), exit = scaleOut(pop) + fadeOut()) {
                    RoundStepButton(
                        symbol = "+",
                        contentDescription = stringResource(R.string.a11y_welcome_goal_increase, title),
                        onClick = onIncrease,
                        enabled = canIncrease,
                        color = accentColor,
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = !isIncluded && shownCount == 0,
                enter = scaleIn(pop) + fadeIn(),
                exit = scaleOut(pop) + fadeOut(),
            ) {
                Button(
                    onClick = onAdd,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(R.string.welcome_option_add),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. Control Style Step ("Controles")
// ============================================================================

@Composable
private fun ControlStyleStep(
    selection: ControlStyle,
    accentColor: Color,
    onSelectStyle: (ControlStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "🎛️",
            title = stringResource(R.string.welcome_step_control_title),
            subtitle = stringResource(R.string.welcome_step_control_subtitle),
            accentColor = accentColor,
        )

        // Opção Deslizar (Slider)
        ControlStyleCard(
            title = stringResource(R.string.welcome_control_slider),
            description = stringResource(R.string.welcome_control_slider_desc),
            isSelected = selection == ControlStyle.Slider,
            accentColor = accentColor,
            onClick = { onSelectStyle(ControlStyle.Slider) },
        ) {
            SliderPreview(accentColor)
        }

        // Opção Botões (Stepper)
        ControlStyleCard(
            title = stringResource(R.string.welcome_control_stepper),
            description = stringResource(R.string.welcome_control_stepper_desc),
            isSelected = selection == ControlStyle.Stepper,
            accentColor = accentColor,
            onClick = { onSelectStyle(ControlStyle.Stepper) },
        ) {
            StepperPreview(accentColor)
        }
    }
}

/** Slider que vai e volta sozinho (onda de 4 s entre 15% e 85%), sem toque. */
@Composable
private fun SliderPreview(color: Color) {
    val phase by rememberInfiniteTransition(label = "slider_preview").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "slider_phase",
    )
    Slider(
        value = 0.5f - 0.35f * cos(phase * 2f * PI.toFloat()),
        onValueChange = {},
        enabled = false,
        colors = SliderDefaults.colors(
            disabledThumbColor = color,
            disabledActiveTrackColor = color,
            disabledInactiveTrackColor = color.copy(alpha = 0.25f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clearAndSetSemantics {},
    )
}

/** Barra com − / +: o + é "tocado" 3 vezes enchendo a barra, depois o − 3 vezes, em loop. */
@Composable
private fun StepperPreview(color: Color) {
    var pressedPlus by remember { mutableStateOf<Boolean?>(null) }
    val value = remember { Animatable(0.25f) }
    LaunchedEffect(Unit) {
        var tap = 0
        while (true) {
            delay(700)
            val plus = (tap / 3) % 2 == 0
            pressedPlus = plus
            delay(150)
            pressedPlus = null
            value.animateTo(value.value + if (plus) 0.2f else -0.2f, tween(300))
            tap++
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinearProgressIndicator(
            progress = { value.value.coerceIn(0f, 1f) },
            color = color,
            trackColor = color.copy(alpha = 0.25f),
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(false to "−", true to "+").forEach { (plus, symbol) ->
                val scale by animateFloatAsState(
                    targetValue = if (pressedPlus == plus) 0.85f else 1f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
                    label = "stepper_press",
                )
                RoundStepButton(
                    symbol = symbol,
                    contentDescription = "",
                    // Ativo só pra ficar na cor (o toque não faz nada; a prévia é decorativa).
                    onClick = {},
                    color = color,
                    size = 40.dp,
                    modifier = Modifier.scale(scale),
                )
            }
        }
    }
}

@Composable
private fun ControlStyleCard(
    title: String,
    description: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    WelcomeCard(
        isSelected = isSelected,
        accentColor = accentColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Só o cartão escolhido mostra a prévia, abrindo com animação.
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(250)) + expandVertically(tween(300)) + scaleIn(initialScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 0f)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
            ) {
                preview()
            }
        }
    }
}

// ============================================================================
// 7. Theme Step ("Tema")
// ============================================================================

@Composable
private fun ThemeStep(
    selectedTheme: AppTheme,
    sex: String,
    accentColor: Color,
    onSelectTheme: (AppTheme) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "🎭",
            title = stringResource(R.string.welcome_step_theme_title),
            subtitle = stringResource(R.string.welcome_step_theme_subtitle),
            note = stringResource(R.string.welcome_step_theme_footer),
            accentColor = accentColor,
        )

        // Temas agrupados por categoria
        ThemeCategory.entries.forEach { category ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(categoryNameRes(category, sex)),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Surface(
                    shape = RoundedCornerShape(WelcomeCardRadius),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        val themes = AppTheme.inCategory(category)
                        themes.forEachIndexed { index, theme ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 20.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            ThemeOptionRow(
                                theme = theme,
                                sex = sex,
                                isSelected = theme == selectedTheme,
                                accentColor = accentColor,
                                onClick = { onSelectTheme(theme) },
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun ThemeOptionRow(
    theme: AppTheme,
    sex: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    val themeName = stringResource(themeNameRes(theme, sex))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Nome em cima e os 5 emojis numa linha só embaixo
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = themeName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = theme.previewEmojis(sex).joinToString(" "),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp),
            )
        }

        ThemeDetailsButton(theme = theme, sex = sex)
    }
}

// ============================================================================
// 8. Ready Step ("Tudo pronto!")
// ============================================================================

@Composable
private fun ReadyStep(
    state: WelcomeUiState,
    accentColor: Color,
    onRequestNotifications: () -> Unit,
    onRequestHealth: () -> Unit,
) {
    // Conteúdo centralizado na tela; Vamos lá / Voltar sobem pra logo abaixo das permissões.
    CenteredStepColumn {
        WelcomeStepHeader(
            emoji = "🚶",
            title = stringResource(R.string.welcome_step_ready_title),
            subtitle = stringResource(R.string.welcome_step_ready_subtitle),
            accentColor = accentColor,
        )

        // Permissão de Notificações
        PermissionCard(
            modifier = Modifier.padding(top = 16.dp),
            icon = Icons.Default.Notifications,
            iconBackgroundColor = Color(0xFFFF3B30),
            title = stringResource(R.string.welcome_ready_notifications_title),
            description = stringResource(R.string.welcome_ready_notifications_desc),
            state = state.notificationsPermissionState,
            accentColor = accentColor,
            onAllow = onRequestNotifications,
        )

        // Permissão de Saúde (Health Connect, se disponível no aparelho)
        if (state.isHealthAvailable) {
            PermissionCard(
                modifier = Modifier.padding(top = 16.dp),
                icon = Icons.Default.Favorite,
                iconBackgroundColor = Color(0xFFFF2D55),
                title = stringResource(R.string.welcome_ready_health_title),
                description = stringResource(R.string.welcome_ready_health_desc),
                state = state.healthPermissionState,
                accentColor = accentColor,
                onAllow = onRequestHealth,
            )
        }
        Spacer(Modifier.height(16.dp))
        WelcomeButtonsSlot(WelcomeStep.Ready)
    }
}
