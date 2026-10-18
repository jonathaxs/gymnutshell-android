package com.jonathaxs.gymnutshell.ui.welcome

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonathaxs.gymnutshell.R

/** Raio padrão dos cards no estilo iOS 26+ / Material 3. */
val WelcomeCardRadius = 24.dp

/**
 * Cabeçalho centralizado de cada etapa: emoji num círculo tonal ~88dp, título grande e subtítulo opcional.
 * Espelha WelcomeStepHeader do iOS.
 */
@Composable
fun WelcomeStepHeader(
    emoji: String,
    title: String,
    subtitle: String? = null,
    note: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    val animatedAccent by animateColorAsState(accentColor, tween(300), label = "header_accent")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Círculo com emoji tonal (88dp)
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(animatedAccent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emoji,
                fontSize = 44.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        // Título grande em negrito
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        // Subtítulo opcional
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        // Aviso pequeno logo abaixo (ex: "O tema pode ser alterado depois.")
        if (!note.isNullOrEmpty()) {
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

/**
 * Barra de progresso do topo: a etapa ativa tem peso 3f e as demais peso 1f.
 * Espelha WelcomeProgressBar do iOS com altura mínima de toque de 44dp.
 */
@Composable
fun WelcomeProgressBar(
    currentIndex: Int,
    totalSteps: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val animatedAccent by animateColorAsState(accentColor, tween(300), label = "progress_accent")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until totalSteps) {
                val isActive = i == currentIndex
                val isCompleted = i < currentIndex
                val weight = if (isActive) 3f else 1f
                val color = when {
                    isActive -> animatedAccent
                    isCompleted -> animatedAccent.copy(alpha = 0.45f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                }

                Box(
                    modifier = Modifier
                        .weight(weight)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }
    }
}

/**
 * Card reutilizável com cantos de 24dp.
 * Quando selecionado, ganha fundo com 12% de opacidade e borda de 2dp na cor da etapa.
 */
@Composable
fun WelcomeCard(
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(WelcomeCardRadius),
    content: @Composable () -> Unit,
) {
    val animatedAccent by animateColorAsState(accentColor, tween(200), label = "card_accent")
    val surfaceColor = MaterialTheme.colorScheme.surface
    val backgroundColor = if (isSelected) animatedAccent.copy(alpha = 0.12f) else surfaceColor
    // Nao selecionado ganha contorno leve: a sombra some com o clip e o cartao sumia no fundo claro.
    val borderStroke = if (isSelected) BorderStroke(2.dp, animatedAccent)
    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = backgroundColor,
        border = borderStroke,
        shadowElevation = if (isSelected) 0.dp else 1.dp,
    ) {
        content()
    }
}

/**
 * Botão redondo (−, +, lixeira) com área de toque mínima de 48dp.
 * Espelha o RoundStepButton do iOS.
 */
@Composable
fun RoundStepButton(
    symbol: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    isTrash: Boolean = false,
) {
    val tint = if (isTrash) Color(0xFFFF3B30) else if (enabled) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val bg = if (isTrash) Color(0xFFFF3B30).copy(alpha = 0.12f) else color.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .size(48.dp) // Área de toque mínima acessível de 48dp
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(if (enabled) bg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isTrash) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size((size.value * 0.45f).dp),
                )
            } else {
                Text(
                    text = symbol,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * 0.48f).sp,
                    ),
                    color = tint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Reexporta o estado e cartão de permissão compartilhados de ui.components. */
typealias WelcomePermissionState = com.jonathaxs.gymnutshell.ui.components.PermissionCardState
