package com.pharmalink.designsystem.stitch

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pharmalink.designsystem.R
import com.pharmalink.designsystem.stitch.theme.LocalStitchDimens
import com.pharmalink.designsystem.stitch.theme.StitchDimens

// Pharmacy Sanctuary Color Palette
val Primary = Color(0xFF0D9488) // Teal - للأزرار والـ CTAs
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF00655C) // From DESIGN.md
val OnPrimaryContainer = Color(0xFFB5FFD8) // Derived from previous theme

val Secondary = Color(0xFF14B8A6) // للعناصر الثانوية
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFCCE8E3) // Derived from previous theme
val OnSecondaryContainer = Color(0xFF06201D) // Derived from previous theme

val Tertiary = Color(0xFFF43F5E) // للتنبيهات الحرجة
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFCFE5FF) // Derived from previous theme
val OnTertiaryContainer = Color(0xFF021D32) // Derived from previous theme

val Error = Color(0xFFBA1A1A) // Default Material3 Error
val OnError = Color(0xFFFFFFFF) // Default Material3 OnError
val ErrorContainer = Color(0xFFFFDAD6) // Default Material3 ErrorContainer
val OnErrorContainer = Color(0xFF410002) // Default Material3 OnErrorContainer

val Background = Color(0xFFF8FAFC) // الخلفية الأساسية
val OnBackground = Color(0xFF191C1B) // Derived from previous theme

val Surface = Color(0xFFF8FAFC) // الخلفية الأساسية
val OnSurface = Color(0xFF191C1B) // Derived from previous theme

val SurfaceVariant = Color(0xFFDBE5E2) // Derived from previous theme
val OnSurfaceVariant = Color(0xFF3F4947) // Derived from previous theme

val Outline = Color(0xFF6F7977) // Derived from previous theme
val InverseOnSurface = Color(0xFFEEF1F0) // Derived from previous theme
val InverseSurface = Color(0xFF2E3130) // Derived from previous theme
val InversePrimary = Color(0xFF66DBCF) // Derived from previous theme

val Scrim = Color(0xFF000000) // Default Scrim

val SurfaceDim = Color(0xFFD1D9D7) // Derived from previous theme
val SurfaceBright = Color(0xFFF0F1F1) // Derived from previous theme
val SurfaceContainerLowest = Color(0xFFFFFFFF) // البطاقات
val SurfaceContainerLow = Color(0xFFF3F4F5)
val SurfaceContainer = Color(0xFFEDEEEF)
val SurfaceContainerHigh = Color(0xFFDFDFDF) // Derived from previous theme
val SurfaceContainerHighest = Color(0xFFD9D9D9) // Derived from previous theme

private val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Light), // Fallback to regular for light
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_medium, FontWeight.Medium),
    Font(R.font.tajawal_bold, FontWeight.Bold),
)

// Set of Material typography styles to start with
private val StitchTypography = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = TajawalFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

private val StitchShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    inverseOnSurface = InverseOnSurface,
    inverseSurface = InverseSurface,
    inversePrimary = InversePrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    inverseOnSurface = InverseOnSurface,
    inverseSurface = InverseSurface,
    inversePrimary = InversePrimary,
)

@Composable
fun StitchTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalStitchDimens provides StitchDimens()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StitchTypography,
            shapes = StitchShapes,
            content = content
        )
    }
}
