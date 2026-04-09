package com.pharmalink.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

@Composable
fun PharmaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: PharmaButtonStyle = PharmaButtonStyle.Filled,
) {
    val d = MaterialTheme.dimens
    when (style) {
        PharmaButtonStyle.Filled -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(d.radiusL),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 2.dp,
                ),
                contentPadding = PaddingValues(horizontal = d.spaceXL),
            ) {
                Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
        PharmaButtonStyle.GradientAccent -> {
            val shape = RoundedCornerShape(d.radiusL)
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(PharmaGradients.fabOrange)
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        PharmaButtonStyle.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.fillMaxWidth().height(52.dp),
                enabled = enabled,
                shape = RoundedCornerShape(d.radiusL),
            ) {
                Text(text = text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

enum class PharmaButtonStyle { Filled, GradientAccent, Outlined }
