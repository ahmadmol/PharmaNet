package com.pharmalink.feature.cart.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.pharmalink.R
import com.pharmalink.designsystem.theme.dimens

/**
 * Cart Bottom Actions Component
 * Primary and secondary CTAs for cart submission
 */
@Composable
fun CartBottomActions(
    onSubmitRequest: () -> Unit,
    onSaveDraft: () -> Unit = {},
    onContinueBrowsing: () -> Unit = {},
    modifier: Modifier = Modifier,
    isSubmitEnabled: Boolean = true,
) {
    val d = MaterialTheme.dimens
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(d.spaceL),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(d.spaceL))
            
            // Primary CTA - Submit Request
            Button(
                onClick = onSubmitRequest,
                enabled = isSubmitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.cart_action_submit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            
            Spacer(Modifier.height(d.spaceM))
            
            // Secondary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                // Save Draft
                AssistChip(
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            text = stringResource(R.string.cart_action_save_draft),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                )
                
                // Continue Browsing
                AssistChip(
                    onClick = onContinueBrowsing,
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            text = stringResource(R.string.cart_action_browse),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                )
            }
            
            Spacer(Modifier.height(d.spaceXL))
        }
    }
}
