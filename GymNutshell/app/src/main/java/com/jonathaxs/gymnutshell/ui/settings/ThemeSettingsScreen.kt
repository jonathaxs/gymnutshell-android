package com.jonathaxs.gymnutshell.ui.settings

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jonathaxs.gymnutshell.R
import com.jonathaxs.gymnutshell.core.domain.AppTheme
import com.jonathaxs.gymnutshell.core.domain.DailyAchievement
import com.jonathaxs.gymnutshell.core.domain.ThemeCategory
import com.jonathaxs.gymnutshell.ui.components.GroupCheck
import com.jonathaxs.gymnutshell.ui.components.GroupRow
import com.jonathaxs.gymnutshell.ui.components.GroupRowDivider
import com.jonathaxs.gymnutshell.ui.components.GroupSection
import com.jonathaxs.gymnutshell.ui.theme.color

/** Sub-tela de tema — porte de ThemeSettingsView (iOS): mascotes agrupados por categoria. */
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = viewModel()) {
    val selected by viewModel.theme.collectAsStateWithLifecycle()
    val accent = viewModel.accentColor.collectAsStateWithLifecycle().value.color
    // Sexo do perfil define as variantes femininas de emoji/nome (espelha o `sex` do iOS).
    val sex = viewModel.profile.collectAsStateWithLifecycle().value.sex

    Scaffold(topBar = { SettingsTopBar(stringResource(R.string.settings_theme), onBack) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            ThemeCategory.entries.forEach { category ->
                item(key = "cat_${category.name}") {
                    GroupSection(title = stringResource(categoryNameRes(category, sex)), titleColor = accent) {
                        AppTheme.inCategory(category).forEachIndexed { index, theme ->
                            if (index > 0) GroupRowDivider()
                            ThemeRow(
                                theme = theme,
                                sex = sex,
                                selected = theme == selected,
                                accent = accent,
                                onClick = { viewModel.setTheme(theme) },
                            )
                        }
                    }
                }
            }
            item(key = "bottom_spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }

}

/**
 * Linha de tema, no mesmo layout da Welcome: nome em cima, os 5 emojis embaixo (8 de respiro, 14 de margem
 * vertical); selecionada com um véu claro + check (não mais preenchida com a cor de destaque) e o botão
 * Detalhes, que abre o balão dos níveis, fora da área que seleciona o tema.
 */
@Composable
private fun ThemeRow(
    theme: AppTheme,
    sex: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    val themeName = stringResource(themeNameRes(theme, sex))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (selected) Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, top = 14.dp, bottom = 14.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                themeName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            // Prévia decorativa: ler a lista de emojis do nível não acrescenta nada ao nome do tema.
            Text(
                theme.previewEmojis(sex).joinToString(" "),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            GroupCheck(accent)
        }
        Spacer(Modifier.width(8.dp))
        ThemeDetailsButton(theme = theme, sex = sex)
    }
}

@StringRes
private fun categoryNameRes(category: ThemeCategory): Int = when (category) {
    ThemeCategory.Sport -> R.string.theme_cat_sport
    ThemeCategory.Animals -> R.string.theme_cat_animals
    ThemeCategory.Warrior -> R.string.theme_cat_warrior
    ThemeCategory.Space -> R.string.theme_cat_space
    ThemeCategory.Elements -> R.string.theme_cat_elements
    ThemeCategory.Competition -> R.string.theme_cat_competition
}

/** Nome da categoria com variante por sexo (só Guerreiro→Guerreira) — porte de localizedName(sex:). */
@StringRes
internal fun categoryNameRes(category: ThemeCategory, sex: String): Int =
    if (category == ThemeCategory.Warrior && sex != "male") R.string.theme_cat_warrior_fem
    else categoryNameRes(category)

/** Nome do tema com variante feminina (sex != "male") — porte de AppTheme.displayName(sex:) do iOS. */
@StringRes
internal fun themeNameRes(theme: AppTheme, sex: String): Int {
    if (sex != "male") {
        val fem = when (theme) {
            AppTheme.Cat -> R.string.theme_cat_fem
            AppTheme.Dog -> R.string.theme_dog_fem
            AppTheme.Bear -> R.string.theme_bear_fem
            AppTheme.Dragon -> R.string.theme_dragon_fem
            AppTheme.Horse -> R.string.theme_horse_fem
            AppTheme.Doctor -> R.string.theme_doctor_fem
            else -> 0
        }
        if (fem != 0) return fem
    }
    return themeNameRes(theme)
}

@StringRes
internal fun themeNameRes(theme: AppTheme): Int = when (theme) {
    AppTheme.Gym -> R.string.theme_gym
    AppTheme.Running -> R.string.theme_running
    AppTheme.Cat -> R.string.theme_cat
    AppTheme.Dog -> R.string.theme_dog
    AppTheme.Bear -> R.string.theme_bear
    AppTheme.Dino -> R.string.theme_dino
    AppTheme.Dragon -> R.string.theme_dragon
    AppTheme.Horse -> R.string.theme_horse
    AppTheme.Ocean -> R.string.theme_ocean
    AppTheme.Monkey -> R.string.theme_monkey
    AppTheme.Bird -> R.string.theme_bird
    AppTheme.Doctor -> R.string.theme_doctor
    AppTheme.Ninja -> R.string.theme_ninja
    AppTheme.Fire -> R.string.theme_fire
    AppTheme.Plant -> R.string.theme_plant
    AppTheme.Astronaut -> R.string.theme_astronaut
    AppTheme.Celestial -> R.string.theme_celestial
    AppTheme.Champion -> R.string.theme_champion
    AppTheme.Number -> R.string.theme_number
}

/**
 * Botão "Detalhes" (cinza) que abre, logo abaixo, um balão com os 5 níveis do tema: emoji, nome e faixa.
 * Porte do Visualizar + ThemeLevelsPopover do iOS (Welcome e Ajustes > Emojis).
 */
@Composable
internal fun ThemeDetailsButton(theme: AppTheme, sex: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val themeName = stringResource(themeNameRes(theme, sex))
    val rangeSuffix = stringResource(R.string.tier_info_range_suffix)
    Box(modifier) {
        FilledTonalButton(
            onClick = { expanded = true },
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.welcome_theme_preview),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(300.dp),
        ) {
            Text(
                text = themeName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .semantics { heading() },
            )
            DailyAchievement.entries.forEach { tier ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = theme.emoji(tier, sex),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp).clearAndSetSemantics {},
                    )
                    Column {
                        Text(stringResource(theme.tierNameRes(tier, sex)), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = tierRange(tier) + rangeSuffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
