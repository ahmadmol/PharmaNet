package com.pharmalink.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.R

/**
 * A reusable app banner component that displays the brand image.
 * Used at the top of main screens to enhance visual appeal.
 */
@Composable
fun AppBanner(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    Image(
        painter = painterResource(id = R.drawable.picture_app),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentScale = ContentScale.FillWidth
    )
}
