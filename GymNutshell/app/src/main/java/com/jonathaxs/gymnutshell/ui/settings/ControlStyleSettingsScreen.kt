package com.jonathaxs.gymnutshell.ui.settings

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.ControlStyle
import com.jonathaxs.gymnutshell.ui.components.GroupInset
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.welcome.RoundStepButton
import com.jonathaxs.gymnutshell.ui.welcome.WelcomeCard

/**
 * Tela de preferências do estilo dos controles (porte de ControlStyleSettingsView do iOS).
 * Permite alternar entre Deslizar (Slider) e Botões (Stepper); gravação imediata ao tocar.
 */
@Composable
fun ControlStyleSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val currentStyle by viewModel.controlStyle.collectAsStateWithLifecycle()
    val accent by viewModel.accentColor.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_preference_control_style),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = GroupInset, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Card 1: Deslizar (Slider)
                ControlStyleOptionCard(
                    title = stringResource(R.string.welcome_control_slider),
                    description = stringResource(R.string.welcome_control_slider_desc),
                    isSelected = currentStyle == ControlStyle.Slider,
                    accentColor = accent.color,
                    onClick = { viewModel.setControlStyle(ControlStyle.Slider) },
                ) {
                    Slider(
                        value = 0.45f,
                        onValueChange = {},
                        enabled = false,
                        colors = SliderDefaults.colors(
                            disabledThumbColor = accent.color,
                            disabledActiveTrackColor = accent.color,
                            disabledInactiveTrackColor = accent.color.copy(alpha = 0.25f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }

                // Card 2: Botões (Stepper)
                ControlStyleOptionCard(
                    title = stringResource(R.string.welcome_control_stepper),
                    description = stringResource(R.string.welcome_control_stepper_desc),
                    isSelected = currentStyle == ControlStyle.Stepper,
                    accentColor = accent.color,
                    onClick = { viewModel.setControlStyle(ControlStyle.Stepper) },
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
                            color = accent.color,
                            trackColor = accent.color.copy(alpha = 0.25f),
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
                                color = accent.color,
                                size = 38.dp,
                            )
                            RoundStepButton(
                                symbol = "+",
                                contentDescription = "",
                                onClick = {},
                                enabled = false,
                                color = accent.color,
                                size = 38.dp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Rodapé informativo
                Text(
                    text = stringResource(R.string.settings_control_style_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ControlStyleOptionCard(
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    modifier = Modifier.size(24.dp),
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
