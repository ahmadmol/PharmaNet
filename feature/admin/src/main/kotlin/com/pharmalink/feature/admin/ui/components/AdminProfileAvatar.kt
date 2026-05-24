package com.pharmalink.feature.admin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun AdminProfileAvatarButton(
    profileImageUrl: String? = null,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = CircleShape,
            )
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        AdminProfileAvatarIcon(
            profileImageUrl = profileImageUrl,
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            fallbackSize = 24.dp,
        )
    }
}

@Composable
fun AdminProfileAvatarIcon(
    profileImageUrl: String? = null,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackSize: Dp = 18.dp,
    fallbackTint: Color = MaterialTheme.colorScheme.onPrimary,
) {
    val model = profileImageUrl?.takeIf { it.isNotBlank() }
    if (model != null) {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
            loading = { AdminProfileAvatarFallback(contentDescription, fallbackSize, fallbackTint) },
            error = { AdminProfileAvatarFallback(contentDescription, fallbackSize, fallbackTint) },
        )
    } else {
        AdminProfileAvatarFallback(contentDescription, fallbackSize, fallbackTint)
    }
}

@Composable
private fun AdminProfileAvatarFallback(
    contentDescription: String?,
    size: Dp,
    tint: Color,
) {
    Icon(
        imageVector = Icons.Outlined.Person,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size),
    )
}
