package com.pharmalink.ui.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.feature.common.component.CenterTitleTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO(phase-2): Convert this static conversation screen into a real messaging feature module.
    var draft by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        CenterTitleTopBar(
            title = stringResource(R.string.messages_title),
            navigationIcon = {
                TextButton(onClick = onBack) {
                    Text(
                        text = stringResource(R.string.action_back),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
            actions = {
                TextButton(onClick = onFilter) {
                    Text(
                        text = stringResource(R.string.action_filter),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )

        PharmaSectionHeader(
            title = "المحادثة الحالية",
            subtitle = "استفسارات الطلبات والدعم",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MessageBubble(
                    text = "مرحباً، تم استلام طلبك #REQ-1284 وسيتم مراجعته قريباً.",
                    alignEnd = false,
                )
            }
            item {
                MessageBubble(
                    text = "شكراً، أحتاج تحديثاً حول موعد وصول الشحنة.",
                    alignEnd = true,
                )
            }
            item {
                MessageBubble(
                    text = "تم التحديث. من المتوقع وصول الشحنة قبل نهاية اليوم.",
                    alignEnd = false,
                )
            }
        }

        Surface(
            shadowElevation = 4.dp,
            tonalElevation = 1.dp,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.message_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = false,
                minLines = 1,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun MessageBubble(text: String, alignEnd: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (alignEnd) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
