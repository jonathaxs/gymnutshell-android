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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
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

/** Mapeamento SF Symbol -> ImageVector do Material Icons (set core). */
private val SYMBOL_ICON_MAP: Map<String, ImageVector> = mapOf(
    "square.grid.2x2.fill" to Icons.Default.Menu,
    "star.fill" to Icons.Default.Star,
    "heart.fill" to Icons.Default.Favorite,
    "bolt.fill" to Icons.Default.PlayArrow,
    "flame.fill" to Icons.Default.ThumbUp,
    "leaf.fill" to Icons.Default.Place,
    "figure.run" to Icons.Default.Person,
    "figure.walk" to Icons.Default.AccountCircle,
    "figure.strengthtraining.traditional" to Icons.Default.Build,
    "figure.yoga" to Icons.Default.Face,
    "figure.pool.swim" to Icons.Default.Refresh,
    "figure.outdoor.cycle" to Icons.Default.Share,
    "dumbbell.fill" to Icons.Default.Build,
    "sportscourt.fill" to Icons.Default.Home,
    "trophy.fill" to Icons.Default.Star,
    "target" to Icons.Default.CheckCircle,
    "timer" to Icons.Default.Info,
    "stopwatch.fill" to Icons.Default.Refresh,
    "fork.knife" to Icons.Default.ShoppingCart,
    "cup.and.saucer.fill" to Icons.Default.ShoppingCart,
    "carrot.fill" to Icons.Default.ShoppingCart,
    "drop.fill" to Icons.Default.ThumbUp,
    "pills.fill" to Icons.Default.AddCircle,
    "cross.case.fill" to Icons.Default.Add,
    "bed.double.fill" to Icons.Default.Home,
    "moon.fill" to Icons.Default.AccountBox,
    "sun.max.fill" to Icons.Default.Star,
    "brain.head.profile" to Icons.Default.Face,
    "lungs.fill" to Icons.Default.FavoriteBorder,
    "figure.mind.and.body" to Icons.Default.Person,
    "book.fill" to Icons.Default.List,
    "pencil" to Icons.Default.Edit,
    "checkmark.seal.fill" to Icons.Default.CheckCircle,
    "bell.fill" to Icons.Default.Notifications,
    "calendar" to Icons.Default.DateRange,
    "sparkles" to Icons.Default.Star,
)

/**
 * Retorna o ImageVector correspondente ao SF Symbol do iOS, com fallback para o ícone padrão de categoria.
 * Usado pelo seletor e pela Hoje para renderizar categorias personalizadas.
 */
fun categoryIcon(symbolName: String?): ImageVector {
    return (if (symbolName != null) SYMBOL_ICON_MAP[symbolName] else null)
        ?: Icons.Default.Menu
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
