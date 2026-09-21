package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass Material Surface with specular highlights and physical thickness.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(24.dp),
    opacity: Float = 0.75f,
    contentColor: Color = Color.White,
    darkBorder: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color.Black ||
            MaterialTheme.colorScheme.surface == Color.Black ||
            MaterialTheme.colorScheme.surface.red < 0.2f

    val baseFill = if (isDark) {
        Color(0xFF14171F).copy(alpha = opacity.coerceIn(0.3f, 0.98f))
    } else {
        Color(0xFFFFFFFF).copy(alpha = opacity.coerceIn(0.4f, 0.98f))
    }

    // Specular light gradient: subtle light sheen from top-left to dark edge at bottom-right
    val specularBorder = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.25f * opacity),
                Color.White.copy(alpha = 0.05f * opacity),
                Color.Black.copy(alpha = 0.40f * opacity)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.9f * opacity),
                Color.White.copy(alpha = 0.4f * opacity),
                Color.Black.copy(alpha = 0.08f * opacity)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(baseFill)
            .border(
                width = 1.dp,
                brush = specularBorder,
                shape = shape
            ),
        content = content
    )
}

/**
 * Liquid Glass interactive button with real-time spring scale physics on press.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = SquircleShape(20.dp),
    opacity: Float = 0.75f,
    activeColor: Color? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Apple spring physics: instant response on touch down, spring rebound on release
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_button_scale"
    )

    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    val baseFill = when {
        activeColor != null -> activeColor.copy(alpha = opacity)
        isDark -> Color(0xFF1C202B).copy(alpha = opacity)
        else -> Color(0xFFF2F4F8).copy(alpha = opacity)
    }

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.28f else 0.7f),
            Color.White.copy(alpha = 0.04f),
            Color.Black.copy(alpha = 0.35f)
        )
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(baseFill)
            .border(1.dp, borderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom physical spring scale serves as feedback
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
