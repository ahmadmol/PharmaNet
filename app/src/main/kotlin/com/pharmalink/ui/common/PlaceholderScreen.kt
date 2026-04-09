package com.pharmalink.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

@Composable
fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier,
    showOpenDetailAction: Boolean = false,
    onOpenDetail: () -> Unit = {},
    showLogout: Boolean = false,
    onLogout: () -> Unit = {},
) {
    val d = MaterialTheme.dimens
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(d.spaceXXL),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = PharmaGradients.primaryDiagonal,
                        shape = RoundedCornerShape(d.radiusXL),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(d.iconL),
                )
            }
            Spacer(modifier = Modifier.height(d.spaceL))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(d.spaceS))
            Text(
                text = "لا توجد بيانات كافية لعرض هذه الصفحة الآن.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (showOpenDetailAction) {
                Spacer(modifier = Modifier.height(d.spaceL))
                Button(onClick = onOpenDetail) {
                    Text(text = stringResource(R.string.warehouse_detail_placeholder))
                }
            }
            if (showLogout) {
                Spacer(modifier = Modifier.height(d.spaceL))
                Button(onClick = onLogout) {
                    Text(text = stringResource(R.string.action_logout))
                }
            }
        }
    }
}
