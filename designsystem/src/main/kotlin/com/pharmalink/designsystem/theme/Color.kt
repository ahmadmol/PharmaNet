package com.pharmalink.designsystem.theme

import androidx.compose.ui.graphics.Color

/** Stitch “Smart Pharmacy UI Design” primary — teal #008075 */
val PremiumPrimary = Color(0xFF008075)
val PremiumSecondary = Color(0xFF0D9488)
val PremiumAccent = Color(0xFFF97316)
val PremiumUrgent = Color(0xFFEF4444)

// Brand scale (teal-forward; legacy “PharmaBlue*” names kept for call-site stability)
val PharmaBlue900 = Color(0xFF003D38)
val PharmaBlue800 = Color(0xFF005A52)
val PharmaBlue700 = Color(0xFF006B62)
val PharmaBlue600 = Color(0xFF008075)
val PharmaBlue500 = PremiumPrimary
val PharmaBlue400 = Color(0xFF26A69A)
val PharmaBlue300 = Color(0xFF4DB6AC)
val PharmaBlue200 = Color(0xFF80CBC4)
val PharmaBlue100 = Color(0xFFE0F5F3)
val PharmaBlue50 = Color(0xFFF0FAF9)

// Accent
val PharmaOrange = PremiumAccent
val PharmaOrangeLight = Color(0xFFFB923C)

// Neutrals
val PharmaNeutral900 = Color(0xFF1A1A2E)
val PharmaNeutral800 = Color(0xFF2D2D44)
val PharmaNeutral600 = Color(0xFF6B7280)
val PharmaNeutral400 = Color(0xFF9CA3AF)
val PharmaNeutral200 = Color(0xFFE5E7EB)
val PharmaNeutral100 = Color(0xFFF3F4F6)
val PharmaNeutral50 = Color(0xFFF9FAFB)

// Semantic
val PharmaSuccess = PremiumSecondary
val PharmaError = PremiumUrgent
val PharmaWarning = Color(0xFFF59E0B)

// Gradient helpers (use with Brush.linearGradient)
val GradientPrimaryStart = PharmaBlue700
val GradientPrimaryEnd = PremiumPrimary
/** Header: Stitch teal → mint (dashboard / profile hero) */
val GradientHeaderStart = Color(0xFF008075)
val GradientHeaderEnd = Color(0xFF2DD4BF)
val GradientSurfaceStart = PharmaBlue50
val GradientSurfaceEnd = Color.White

// Legacy / convenience (used across app modules)
val PharmaBlue = PharmaBlue500
val Orange = PharmaOrange

/** Medium / light green — legacy brand; prefer [PharmaSuccess] / palette for new UI */
val GreenPrimary = Color(0xFF66BB6A)

val BackgroundWhite = Color(0xFFFFFFFF)
val TextSecondaryGray = PharmaNeutral600
val TextPrimaryDark = PharmaNeutral900
val SurfaceSubtle = PharmaNeutral100

val MessageBubbleOther = Color(0xFFF0F0F0)
val MessageBubbleSelf = Color(0xFFE8F5E9)

val PrimaryLight = GreenPrimary
val SecondaryLight = GreenPrimary
val SurfaceLight = BackgroundWhite

/** Clinical dashboard canvas (Stitch design system background) */
val ClinicalCanvas = Color(0xFFF8F9FA)

val PrimaryDark = Color(0xFF81C784)
val SecondaryDark = Color(0xFF66BB6A)
val SurfaceDark = Color(0xFF121212)
