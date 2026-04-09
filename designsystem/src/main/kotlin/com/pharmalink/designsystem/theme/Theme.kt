package com.pharmalink.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val PharmaLightColorScheme = lightColorScheme(
    primary = PremiumPrimary,
    onPrimary = Color.White,
    primaryContainer = PharmaBlue100,
    onPrimaryContainer = PharmaBlue900,
    secondary = PremiumSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = PremiumAccent,
    onTertiary = Color.White,
    background = ClinicalCanvas,
    onBackground = PharmaNeutral900,
    surface = Color.White,
    onSurface = PharmaNeutral900,
    surfaceVariant = PharmaNeutral50,
    onSurfaceVariant = PharmaNeutral600,
    outline = PharmaNeutral200,
    error = PharmaError,
    onError = Color.White,
)

@Composable
fun PharmaTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPharmaDimens provides PharmaDimens()) {
        MaterialTheme(
            colorScheme = PharmaLightColorScheme,
            typography = PharmaTypography,
            shapes = PharmaShapes,
            content = content,
        )
    }
}

/** @deprecated Prefer [PharmaTheme] — alias kept for existing call sites */
@Composable
fun PharmaLinkTheme(content: @Composable () -> Unit) {
    PharmaTheme(content = content)
}
