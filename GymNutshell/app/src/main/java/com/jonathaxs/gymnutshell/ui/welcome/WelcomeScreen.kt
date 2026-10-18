package com.jonathaxs.gymnutshell.ui.welcome

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.core.domain.MeasurementSystem
import com.jonathaxs.gymnutshell.core.domain.ThemeCategory
import com.jonathaxs.gymnutshell.core.domain.UnitConverter
import com.jonathaxs.gymnutshell.core.domain.UserGoal
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.settings.ThemeInfoSheet
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
                            StartStep(
                                accentColor = state.sexColor,
                                onStart = viewModel::advance,
                                onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                            )
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

                // Rodapé com botões Continuar e Voltar (exceto na tela de início)
                if (state.currentStep != WelcomeStep.Start) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = viewModel::advance,
                            enabled = state.canContinue,
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = state.stepColor,
                                disabledContainerColor = state.stepColor.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (state.isLastStep) R.string.welcome_button_start
                                    else R.string.welcome_button_continue
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                        }

                        TextButton(
                            onClick = viewModel::goBack,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_button_back),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

@Composable
private fun StartStep(
    accentColor: Color,
    onStart: () -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "🐿️",
            title = stringResource(R.string.welcome_start_title),
            accentColor = accentColor,
        )

        Spacer(Modifier.height(16.dp))

        // Destaques com ícone na cor de destaque
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            StartFeatureRow(
                icon = Icons.Default.CheckCircle,
                text = stringResource(R.string.welcome_start_feature_track),
                color = accentColor,
            )
            StartFeatureRow(
                icon = Icons.Default.DateRange,
                text = stringResource(R.string.welcome_start_feature_progress),
                color = accentColor,
            )
            StartFeatureRow(
                icon = Icons.Default.Star,
                text = stringResource(R.string.welcome_start_feature_theme),
                color = accentColor,
            )
        }

        Spacer(Modifier.weight(1f, fill = true).heightIn(min = 32.dp))

        // Botões de início: Começar e Restaurar backup
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onStart,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.welcome_start_new),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }

            OutlinedButton(
                onClick = onRestore,
                shape = CircleShape,
                border = BorderStroke(1.5.dp, accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.welcome_start_restore),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor,
                )
            }
        }
    }
}

@Composable
private fun StartFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
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
    val goalColor = when (selectedGoal) {
        UserGoal.Bulking -> Color(0xFFFF9500)
        UserGoal.Maintenance -> Color(0xFF007AFF)
        UserGoal.Cutting -> Color(0xFF34C759)
    }

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
            accentColor = goalColor,
        )

        // Bulking
        GoalOptionCard(
            goal = UserGoal.Bulking,
            title = stringResource(R.string.goal_bulking),
            description = stringResource(R.string.welcome_goal_bulking_desc),
            symbol = "↗",
            cardColor = Color(0xFFFF9500),
            isSelected = selectedGoal == UserGoal.Bulking,
            onClick = { onSelectGoal(UserGoal.Bulking) },
        )

        // Maintenance
        GoalOptionCard(
            goal = UserGoal.Maintenance,
            title = stringResource(R.string.goal_maintenance),
            description = stringResource(R.string.welcome_goal_maintenance_desc),
            symbol = "=",
            cardColor = Color(0xFF007AFF),
            isSelected = selectedGoal == UserGoal.Maintenance,
            onClick = { onSelectGoal(UserGoal.Maintenance) },
        )

        // Cutting
        GoalOptionCard(
            goal = UserGoal.Cutting,
            title = stringResource(R.string.goal_cutting),
            description = stringResource(R.string.welcome_goal_cutting_desc),
            symbol = "↘",
            cardColor = Color(0xFF34C759),
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
    viewModel: WelcomeViewModel,
) {
    val scrollState = rememberScrollState()

    // Detecta quando a lista foi rolada até o final para destravar o botão Continuar
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue == 0 || scrollState.value >= scrollState.maxValue - 40) {
            viewModel.setSummaryScrolledToEnd(true)
        }
    }

    val goals = state.goals
    val accent = state.sexColor

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

        // 2. Nutrição: Calorias, Proteína, Carbo, Fibra, Gordura boa
        SummaryCategorySection(title = stringResource(R.string.category_nutricao)) {
            // Calorias (opcional, começa desligada, passo 50, 500..10000 kcal)
            SummaryOptionalGoalRow(
                emoji = "🔥",
                title = stringResource(R.string.today_calories),
                valueText = "${goals.calories} kcal",
                isIncluded = state.includeCalories,
                accentColor = accent,
                canDecrease = goals.calories - 50 >= 500,
                canIncrease = goals.calories + 50 <= 10000,
                onAdd = { viewModel.setIncludeCalories(true) },
                onRemove = { viewModel.setIncludeCalories(false) },
                onDecrease = { viewModel.adjustCalories(-50) },
                onIncrease = { viewModel.adjustCalories(50) },
            )
            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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

        if (isIncluded) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    valueText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 4.dp),
                )
                // Lixeira vermelha redonda
                RoundStepButton(
                    symbol = "",
                    contentDescription = stringResource(R.string.a11y_welcome_optional_remove, title),
                    onClick = onRemove,
                    isTrash = true,
                )
                // Botão −
                RoundStepButton(
                    symbol = "−",
                    contentDescription = stringResource(R.string.a11y_welcome_goal_decrease, title),
                    onClick = onDecrease,
                    enabled = canDecrease,
                    color = accentColor,
                )
                // Botão +
                RoundStepButton(
                    symbol = "+",
                    contentDescription = stringResource(R.string.a11y_welcome_goal_increase, title),
                    onClick = onIncrease,
                    enabled = canIncrease,
                    color = accentColor,
                )
            }
        } else {
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
            Slider(
                value = 0.45f,
                onValueChange = {},
                enabled = false,
                colors = SliderDefaults.colors(
                    disabledThumbColor = accentColor,
                    disabledActiveTrackColor = accentColor,
                    disabledInactiveTrackColor = accentColor.copy(alpha = 0.25f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )
        }

        // Opção Botões (Stepper)
        ControlStyleCard(
            title = stringResource(R.string.welcome_control_stepper),
            description = stringResource(R.string.welcome_control_stepper_desc),
            isSelected = selection == ControlStyle.Stepper,
            accentColor = accentColor,
            onClick = { onSelectStyle(ControlStyle.Stepper) },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LinearProgressIndicator(
                    progress = { 0.45f },
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.25f),
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(CircleShape),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoundStepButton(
                        symbol = "−",
                        contentDescription = "",
                        onClick = {},
                        enabled = false,
                        color = accentColor,
                        size = 38.dp,
                    )
                    RoundStepButton(
                        symbol = "+",
                        contentDescription = "",
                        onClick = {},
                        enabled = false,
                        color = accentColor,
                        size = 38.dp,
                    )
                }
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
            preview()
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
    var infoTheme by remember { mutableStateOf<AppTheme?>(null) }

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
                                onInfo = { infoTheme = theme },
                            )
                        }
                    }
                }
            }
        }
    }

    // Sheet de informações dos níveis do tema (reutiliza ThemeInfoSheet do app)
    infoTheme?.let { theme ->
        ThemeInfoSheet(theme = theme, sex = sex, onDismiss = { infoTheme = null })
    }
}

@Composable
private fun ThemeOptionRow(
    theme: AppTheme,
    sex: String,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onInfo: () -> Unit,
) {
    val themeName = stringResource(themeNameRes(theme, sex))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (isSelected) accentColor.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = themeName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = theme.previewEmojis(sex).joinToString("  "),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 4.dp),
            )
        }

        // Botão (i) com área de toque mínima de 48dp
        IconButton(
            onClick = onInfo,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = stringResource(R.string.a11y_theme_info, themeName),
                tint = accentColor,
                modifier = Modifier.size(24.dp),
            )
        }
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WelcomeStepHeader(
            emoji = "🚀",
            title = stringResource(R.string.welcome_step_ready_title),
            subtitle = stringResource(R.string.welcome_step_ready_subtitle),
            accentColor = accentColor,
        )

        // Permissão de Notificações
        PermissionCard(
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
                icon = Icons.Default.Favorite,
                iconBackgroundColor = Color(0xFFFF2D55),
                title = stringResource(R.string.welcome_ready_health_title),
                description = stringResource(R.string.welcome_ready_health_desc),
                state = state.healthPermissionState,
                accentColor = accentColor,
                onAllow = onRequestHealth,
            )
        }
    }
}
