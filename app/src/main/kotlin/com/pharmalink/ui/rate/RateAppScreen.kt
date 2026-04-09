package com.pharmalink.ui.rate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaSectionHeader

@Composable
fun RateAppScreen(
    onLoveIt: () -> Unit,
    onFeedback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO(phase-2): Decide whether rating remains app-local or becomes its own feedback feature.
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.rate_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.rate_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        PharmaSectionHeader(title = stringResource(R.string.rate_kpi_section_title))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PharmaOutlinedTile(
                title = stringResource(R.string.rate_kpi_compliance),
                value = "96%",
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircle,
            )
            PharmaOutlinedTile(
                title = stringResource(R.string.rate_kpi_response),
                value = stringResource(R.string.rate_kpi_value_minutes, "22"),
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        PharmaOutlinedTile(
            title = stringResource(R.string.rate_kpi_supply_quality),
            value = "A+",
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Outlined.Assessment,
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onLoveIt,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.rate_love_it))
        }

        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onFeedback) {
            Text(
                text = stringResource(R.string.rate_feedback_cta),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
