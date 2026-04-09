package com.pharmalink.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object PharmaGradients {
    val primaryVertical = Brush.verticalGradient(
        colors = listOf(GradientPrimaryStart, GradientPrimaryEnd),
    )
    val primaryHorizontal = Brush.horizontalGradient(
        colors = listOf(GradientPrimaryStart, GradientPrimaryEnd),
    )
    val primaryDiagonal = Brush.linearGradient(
        colors = listOf(GradientPrimaryStart, GradientPrimaryEnd),
        start = Offset.Zero,
        end = Offset(800f, 800f),
    )
    /** Dashboard header — blue → mint green */
    val headerBlueToGreen = Brush.verticalGradient(
        colors = listOf(GradientHeaderStart, GradientHeaderEnd),
    )
    val headerBlueToGreenHorizontal = Brush.horizontalGradient(
        colors = listOf(GradientHeaderStart, GradientHeaderEnd),
    )
    val fabOrange = Brush.verticalGradient(
        colors = listOf(PremiumAccent, Color(0xFFEA580C)),
    )
    val surfaceSubtle = Brush.verticalGradient(
        colors = listOf(PharmaBlue50, Color.White),
    )
    val cardBlue = Brush.linearGradient(
        colors = listOf(PharmaBlue500, PharmaBlue700),
    )
}
