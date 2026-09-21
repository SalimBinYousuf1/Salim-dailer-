package com.example.ui.components

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Continuous squircle curvature shape modeled on Apple's superellipse corners.
 */
class SquircleShape(val cornerRadius: Dp = 24.dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2f)
        val path = Path().apply {
            val width = size.width
            val height = size.height

            // Smooth continuous curve with Apple-ratio cubic bezier
            val c = radiusPx * 0.55228475f // Smoothing coefficient
            reset()
            moveTo(radiusPx, 0f)
            lineTo(width - radiusPx, 0f)
            cubicTo(width - radiusPx + c, 0f, width, radiusPx - c, width, radiusPx)
            lineTo(width, height - radiusPx)
            cubicTo(width, height - radiusPx + c, width - radiusPx + c, height, width - radiusPx, height)
            lineTo(radiusPx, height)
            cubicTo(radiusPx - c, height, 0f, height - radiusPx + c, 0f, height - radiusPx)
            lineTo(0f, radiusPx)
            cubicTo(0f, radiusPx - c, radiusPx - c, 0f, radiusPx, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
