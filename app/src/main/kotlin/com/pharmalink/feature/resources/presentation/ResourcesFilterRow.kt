package com.pharmalink.feature.resources.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.dimens

/**
 * Filter options for resources
 */
enum class ResourceFilter {
    ALL,
    NEARBY,
    SUPPLY_CHAIN,
    AVAILABLE_NOW,
    FAST_DELIVERY
}

/**
 * Resources Filter Row Component
 * Horizontal scrollable filter pills with smooth animations
 */
@Composable
fun ResourcesFilterRow(
    selectedFilter: ResourceFilter,
    onFilterSelected: (ResourceFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = d.spaceL),
    ) {
        items(ResourceFilter.values()) { filter ->
            FilterPill(
                filter = filter,
                isSelected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}

@Composable
private fun FilterPill(
    filter: ResourceFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween<Color>(300),
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween<Color>(300),
        label = "textColor"
    )
    
    val filterText = when (filter) {
        ResourceFilter.ALL -> "الكل"
        ResourceFilter.NEARBY -> "قريب"
        ResourceFilter.SUPPLY_CHAIN -> "سلسلة توريد"
        ResourceFilter.AVAILABLE_NOW -> "متوفر الآن"
        ResourceFilter.FAST_DELIVERY -> "توصيل سريع"
    }
    
    Row(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(50.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = filterText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
        )
    }
}
