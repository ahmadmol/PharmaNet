package com.pharmalink.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val PharmaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Facility create form main card (reference: ~32dp corner radius). */
val PharmaFacilityFormShape = RoundedCornerShape(32.dp)

/** Map picker preview card (reference: ~28dp corner radius). */
val PharmaMapPickerShape = RoundedCornerShape(28.dp)
