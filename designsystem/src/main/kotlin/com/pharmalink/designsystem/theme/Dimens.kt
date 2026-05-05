package com.pharmalink.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PharmaDimens(
    val spaceXXS: Dp = 2.dp,
    val spaceXS: Dp = 4.dp,
    val spaceS: Dp = 8.dp,
    val spaceM: Dp = 12.dp,
    val spaceL: Dp = 16.dp,
    val spaceXL: Dp = 20.dp,
    val spaceXXL: Dp = 24.dp,
    val space3XL: Dp = 32.dp,
    val space4XL: Dp = 40.dp,
    val space5XL: Dp = 48.dp,
    val space6XL: Dp = 64.dp,
    val radiusXS: Dp = 4.dp,
    val radiusS: Dp = 8.dp,
    val radiusM: Dp = 12.dp,
    val radiusL: Dp = 16.dp,
    val radiusXL: Dp = 20.dp,
    val radiusXXL: Dp = 24.dp,
    val radiusFull: Dp = 50.dp,
    val navBarHeight: Dp = 72.dp,
    val navIconSize: Dp = 24.dp,
    val navLabelSize: Dp = 11.dp,
    val navBarPadding: Dp = 8.dp,
    val navBarElevation: Dp = 16.dp,
    val navCenterSpacer: Dp = 72.dp,
    val fabSize: Dp = 64.dp,
    val fabIconSize: Dp = 28.dp,
    val fabElevation: Dp = 8.dp,
    val fabOffsetY: Dp = 36.dp,
    val cardElevation: Dp = 2.dp,
    val cardRadius: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val iconXS: Dp = 16.dp,
    val iconS: Dp = 20.dp,
    val iconM: Dp = 24.dp,
    val iconL: Dp = 32.dp,
    val iconXL: Dp = 48.dp,
    val iconXXL: Dp = 64.dp,
    val toolbarAvatarSize: Dp = 44.dp,
    val mapPickerHeight: Dp = 190.dp,
    val cardElevationMedium: Dp = 6.dp,
    val primaryCtaHeightLarge: Dp = 64.dp,
)

val LocalPharmaDimens = staticCompositionLocalOf { PharmaDimens() }

val MaterialTheme.dimens: PharmaDimens
    @Composable
    get() = LocalPharmaDimens.current
