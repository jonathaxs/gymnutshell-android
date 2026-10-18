package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.ui.components.GroupRow
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.components.GroupSection
import com.jonathaxs.gymnutshell.ui.components.IconBadge
import com.jonathaxs.gymnutshell.ui.theme.color

/**
 * Lista raiz da Settings — porte da SettingsView (iOS): mesmas seções, na mesma ordem
 * (Seus dados → Aparência → Sistema → Sobre), com IconBadge colorido em cada linha.
 * Top bar com seta de voltar para a tela Hoje.
 */
@Composable
fun SettingsListScreen(
    onBack: () -> Unit,
    onOpenCustomGoals: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenColor: () -> Unit,
    onOpenWidgetBackground: () -> Unit,
    onOpenControls: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenUnits: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenWear: () -> Unit,
    onOpenRingInfo: () -> Unit,
    onOpenTierInfo: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val accent by viewModel.accentColor.collectAsStateWithLifecycle()
    val dividerInset = 66.dp

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.tab_settings), onBack = onBack) }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // 1. Seção "Seus dados": Metas, Objetivo e Perfil
                GroupSection(
                    title = stringResource(R.string.settings_section_data),
                    titleColor = accent.color,
                ) {
                    GroupRow(
                        title = stringResource(R.string.settings_daily_goals),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_target),
                                color = Color(0xFFFF3B30),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenCustomGoals,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_fitness_goal),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_flame),
                                color = Color(0xFFFF9500),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenGoal,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_profile_title),
                        leading = {
                            IconBadge(
                                imageVector = Icons.Default.AccountCircle,
                                color = Color(0xFF007AFF),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenProfile,
                    )
                }

                // 2. Seção "Aparência": Tema, Cores de destaque, Widgets e Controles
                GroupSection(
                    title = stringResource(R.string.settings_appearance),
                    titleColor = accent.color,
                ) {
                    GroupRow(
                        title = stringResource(R.string.settings_theme),
                        leading = {
                            IconBadge(
                                imageVector = Icons.Default.Face,
                                color = Color(0xFFAF52DE),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenTheme,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_accent_color),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_palette),
                                color = Color(0xFFFF2D55),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenColor,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_widget_background),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_widgets),
                                color = Color(0xFF5856D6),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenWidgetBackground,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_preference_control_style),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_controls),
                                color = Color(0xFF8E8E93),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenControls,
                    )
                }

                // 3. Seção "Sistema": Health Connect, Notificações, Medidas, Backup e Idioma
                GroupSection(
                    title = stringResource(R.string.settings_section_system),
                    titleColor = accent.color,
                ) {
                    GroupRow(
                        title = stringResource(R.string.settings_health),
                        leading = {
                            IconBadge(
                                imageVector = Icons.Default.Favorite,
                                color = Color(0xFFFF2D55),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenHealth,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_notifications),
                        leading = {
                            IconBadge(
                                imageVector = Icons.Default.Notifications,
                                color = Color(0xFFFF3B30),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenNotifications,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_units),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_ruler),
                                color = Color(0xFF30B0C7),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenUnits,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_backup),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_backup),
                                color = Color(0xFF8E8E93),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenBackup,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_language),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_globe),
                                color = Color(0xFF007AFF),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenLanguage,
                    )
                }

                // 4. Seção "Sobre": Sobre o App, Wear OS, Anel de progresso e Conquistas
                GroupSection(
                    title = stringResource(R.string.settings_section_about),
                    titleColor = accent.color,
                ) {
                    GroupRow(
                        title = stringResource(R.string.settings_about_link),
                        leading = {
                            IconBadge(
                                imageVector = Icons.Default.Info,
                                color = Color(0xFF8E8E93),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenAbout,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_wear_os),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_watch),
                                color = Color(0xFF34C759),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenWear,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_about_progress_ring),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_ring),
                                color = Color(0xFFFF9500),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenRingInfo,
                    )
                    GroupRowDivider(startInset = dividerInset)
                    GroupRow(
                        title = stringResource(R.string.settings_about_achievement),
                        leading = {
                            IconBadge(
                                painter = painterResource(R.drawable.ic_settings_trophy),
                                color = Color(0xFFFFCC00),
                            )
                        },
                        showChevron = true,
                        onClick = onOpenTierInfo,
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
