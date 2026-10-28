package com.jonathaxs.gymnutshell.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import com.jonathaxs.gymnutshell.ui.icons.Air
import com.jonathaxs.gymnutshell.ui.icons.AutoAwesome
import com.jonathaxs.gymnutshell.ui.icons.AvTimer
import com.jonathaxs.gymnutshell.ui.icons.Bed
import com.jonathaxs.gymnutshell.ui.icons.Bolt
import com.jonathaxs.gymnutshell.ui.icons.CalendarMonth
import com.jonathaxs.gymnutshell.ui.icons.DarkMode
import com.jonathaxs.gymnutshell.ui.icons.DirectionsBike
import com.jonathaxs.gymnutshell.ui.icons.DirectionsRun
import com.jonathaxs.gymnutshell.ui.icons.DirectionsWalk
import com.jonathaxs.gymnutshell.ui.icons.Eco
import com.jonathaxs.gymnutshell.ui.icons.EmojiEvents
import com.jonathaxs.gymnutshell.ui.icons.FitnessCenter
import com.jonathaxs.gymnutshell.ui.icons.GridView
import com.jonathaxs.gymnutshell.ui.icons.LocalCafe
import com.jonathaxs.gymnutshell.ui.icons.LocalFireDepartment
import com.jonathaxs.gymnutshell.ui.icons.LunchDining
import com.jonathaxs.gymnutshell.ui.icons.MedicalServices
import com.jonathaxs.gymnutshell.ui.icons.Medication
import com.jonathaxs.gymnutshell.ui.icons.MenuBook
import com.jonathaxs.gymnutshell.ui.icons.Pool
import com.jonathaxs.gymnutshell.ui.icons.Psychology
import com.jonathaxs.gymnutshell.ui.icons.Restaurant
import com.jonathaxs.gymnutshell.ui.icons.SelfImprovement
import com.jonathaxs.gymnutshell.ui.icons.Spa
import com.jonathaxs.gymnutshell.ui.icons.SportsBasketball
import com.jonathaxs.gymnutshell.ui.icons.SportsGymnastics
import com.jonathaxs.gymnutshell.ui.icons.Timer
import com.jonathaxs.gymnutshell.ui.icons.TrackChanges
import com.jonathaxs.gymnutshell.ui.icons.Verified
import com.jonathaxs.gymnutshell.ui.icons.WaterDrop
import com.jonathaxs.gymnutshell.ui.icons.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * 36 símbolos de categorias selecionáveis — exatamente os mesmos SF Symbols do iOS
 * (CategorySymbolPicker.swift).
 */
val CATEGORY_SYMBOLS: List<String> = listOf(
    "square.grid.2x2.fill", "star.fill", "heart.fill", "bolt.fill", "flame.fill", "leaf.fill",
    "figure.run", "figure.walk", "figure.strengthtraining.traditional", "figure.yoga", "figure.pool.swim", "figure.outdoor.cycle",
    "dumbbell.fill", "sportscourt.fill", "trophy.fill", "target", "timer", "stopwatch.fill",
    "fork.knife", "cup.and.saucer.fill", "carrot.fill", "drop.fill", "pills.fill", "cross.case.fill",
    "bed.double.fill", "moon.fill", "sun.max.fill", "brain.head.profile", "lungs.fill", "figure.mind.and.body",
    "book.fill", "pencil", "checkmark.seal.fill", "bell.fill", "calendar", "sparkles",
)

/**
 * Mapeamento SF Symbol -> ImageVector. Os que não existem no set core vem de `ui/icons/`
 * (copiados do material-icons-extended 1.7.8, Apache 2.0) pra não pesar o APK com o pacote inteiro.
 */
private val SYMBOL_ICON_MAP: Map<String, ImageVector> = mapOf(
    "square.grid.2x2.fill" to Icons.Filled.GridView,
    "star.fill" to Icons.Filled.Star,
    "heart.fill" to Icons.Filled.Favorite,
    "bolt.fill" to Icons.Filled.Bolt,
    "flame.fill" to Icons.Filled.LocalFireDepartment,
    "leaf.fill" to Icons.Filled.Eco,
    "figure.run" to Icons.AutoMirrored.Filled.DirectionsRun,
    "figure.walk" to Icons.AutoMirrored.Filled.DirectionsWalk,
    "figure.strengthtraining.traditional" to Icons.Filled.SportsGymnastics,
    "figure.yoga" to Icons.Filled.SelfImprovement,
    "figure.pool.swim" to Icons.Filled.Pool,
    "figure.outdoor.cycle" to Icons.AutoMirrored.Filled.DirectionsBike,
    "dumbbell.fill" to Icons.Filled.FitnessCenter,
    "sportscourt.fill" to Icons.Filled.SportsBasketball,
    "trophy.fill" to Icons.Filled.EmojiEvents,
    "target" to Icons.Filled.TrackChanges,
    "timer" to Icons.Filled.Timer,
    "stopwatch.fill" to Icons.Filled.AvTimer,
    "fork.knife" to Icons.Filled.Restaurant,
    "cup.and.saucer.fill" to Icons.Filled.LocalCafe,
    "carrot.fill" to Icons.Filled.LunchDining,
    "drop.fill" to Icons.Filled.WaterDrop,
    "pills.fill" to Icons.Filled.Medication,
    "cross.case.fill" to Icons.Filled.MedicalServices,
    "bed.double.fill" to Icons.Filled.Bed,
    "moon.fill" to Icons.Filled.DarkMode,
    "sun.max.fill" to Icons.Filled.WbSunny,
    "brain.head.profile" to Icons.Filled.Psychology,
    "lungs.fill" to Icons.Filled.Air,
    "figure.mind.and.body" to Icons.Filled.Spa,
    "book.fill" to Icons.AutoMirrored.Filled.MenuBook,
    "pencil" to Icons.Filled.Edit,
    "checkmark.seal.fill" to Icons.Filled.Verified,
    "bell.fill" to Icons.Filled.Notifications,
    "calendar" to Icons.Filled.CalendarMonth,
    "sparkles" to Icons.Filled.AutoAwesome,
)

/**
 * Retorna o ImageVector correspondente ao SF Symbol do iOS, com fallback para o ícone padrão de categoria.
 * Usado pelo seletor e pela Hoje para renderizar categorias personalizadas.
 */
fun categoryIcon(symbolName: String?): ImageVector {
    return (if (symbolName != null) SYMBOL_ICON_MAP[symbolName] else null)
        ?: Icons.Filled.GridView
}

/**
 * Seletor de ícone (SF Symbol) das categorias personalizadas — porte de CategorySymbolPicker (iOS).
 * Grade de 6 colunas com os mesmos 36 símbolos do iOS.
 */
@Composable
fun CategorySymbolPicker(
    selection: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val itemShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        val rows = CATEGORY_SYMBOLS.chunked(6)
        for (row in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (symbol in row) {
                    val isSelected = symbol == selection
                    val bg = if (isSelected) accentColor.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    val border = if (isSelected) BorderStroke(2.dp, accentColor) else null

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(itemShape)
                            .background(bg)
                            .then(if (border != null) Modifier.border(border, itemShape) else Modifier)
                            .clickable(
                                role = Role.RadioButton,
                                onClick = { onSelect(symbol) },
                            )
                            .semantics { this.selected = isSelected },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = categoryIcon(symbol),
                            contentDescription = symbol,
                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}
