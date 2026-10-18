package com.jonathaxs.gymnutshell.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.Profile
import com.jonathaxs.gymnutshell.core.theme.AccentColor
import com.jonathaxs.gymnutshell.ui.components.GroupCard
import com.jonathaxs.gymnutshell.ui.components.GroupInset
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.theme.color
import com.jonathaxs.gymnutshell.ui.welcome.FavoriteExercise
import com.jonathaxs.gymnutshell.ui.welcome.WelcomeCard

/**
 * Tela de configurações para editar o perfil básico do usuário (porte de ProfileSettingsView, iOS).
 * Sexo em blocos (feminino/masculino) no topo e seção opcional com nome e exercício favorito.
 * Sem dados físicos (peso, altura, idade, data de nascimento).
 */
@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val accent by viewModel.accentColor.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var favoriteExercise by remember { mutableStateOf(FavoriteExercise.Unknown) }

    // Carrega os dados persistidos ao inicializar
    LaunchedEffect(profile) {
        name = profile.name
        sex = Profile.normalizedSex(profile.sex)
        favoriteExercise = FavoriteExercise.fromRaw(profile.favoriteExercise)
    }

    val handleSave = {
        viewModel.saveProfileBasics(name, sex, favoriteExercise.rawValue)
    }

    // Garante que salve ao sair da tela
    BackHandler {
        handleSave()
        onBack()
    }
    DisposableEffect(name, sex, favoriteExercise) {
        onDispose {
            handleSave()
        }
    }

    Scaffold(
        topBar = {
            SettingsTopBar(
                title = stringResource(R.string.settings_profile_title),
                onBack = {
                    handleSave()
                    onBack()
                },
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
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 1. Seletor de sexo em blocos (Feminino / Masculino)
                Column(
                    modifier = Modifier.padding(horizontal = GroupInset),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_field_sex),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Feminino
                        val femaleColor = AccentColor.Purple.color
                        WelcomeCard(
                            isSelected = sex == "female",
                            accentColor = femaleColor,
                            onClick = {
                                sex = "female"
                                viewModel.saveProfileBasics(name, "female", favoriteExercise.rawValue)
                            },
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

                        // Masculino
                        val maleColor = AccentColor.Blue.color
                        WelcomeCard(
                            isSelected = sex == "male",
                            accentColor = maleColor,
                            onClick = {
                                sex = "male"
                                viewModel.saveProfileBasics(name, "male", favoriteExercise.rawValue)
                            },
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

                // 2. Seção Opcional: Nome e Exercício favorito
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.settings_profile_section_optional),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = GroupInset + 20.dp, end = GroupInset),
                    )

                    GroupCard {
                        // Linha do Nome
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_profile_name),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(16.dp))
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                ),
                                cursorBrush = SolidColor(accent.color),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (name.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.settings_profile_name_placeholder),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.End,
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                            )
                        }

                        GroupRowDivider(startInset = 16.dp)

                        // Linha do Exercício favorito
                        var exerciseMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .clickable { exerciseMenuExpanded = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_profile_favorite_exercise),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = stringResource(favoriteExercise.labelRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "›",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.clearAndSetSemantics {},
                                )
                            }

                            DropdownMenu(
                                expanded = exerciseMenuExpanded,
                                onDismissRequest = { exerciseMenuExpanded = false },
                            ) {
                                FavoriteExercise.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.labelRes)) },
                                        onClick = {
                                            favoriteExercise = option
                                            exerciseMenuExpanded = false
                                            viewModel.saveProfileBasics(name, sex, option.rawValue)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
