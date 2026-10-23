package com.jonathaxs.gymnutshell.ui.components

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ícone em quadrado arredondado colorido, equivalente ao IconBadge do iOS.
 * Utilizado nas listas de ajustes e permissões.
 */
@Composable
fun IconBadge(
    imageVector: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    contentDescription: String? = null,
) {
    val cornerRadius = size * 0.27f
    val iconSize = size * 0.58f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun IconBadge(
    painter: Painter,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    contentDescription: String? = null,
) {
    val cornerRadius = size * 0.27f
    val iconSize = size * 0.58f

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Ícone de vidro dos Ajustes: o símbolo na cor da página dentro de um círculo translúcido na mesma cor,
 * como os botões redondos da Hoje. Porte do GlassIcon do iOS (substitui o quadradinho colorido na raiz
 * dos Ajustes; o [IconBadge] segue nas permissões e no Perfil).
 */
@Composable
fun GlassIcon(
    painter: Painter,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .border(0.75.dp, color.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

@Composable
fun GlassIcon(
    imageVector: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    contentDescription: String? = null,
) = GlassIcon(rememberVectorPainter(imageVector), color, modifier, size, contentDescription)
