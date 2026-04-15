package com.pharmalink.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsSheet(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.dimens.spaceL, vertical = MaterialTheme.dimens.spaceM),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceL),
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(PharmaBlue50, RoundedCornerShape(MaterialTheme.dimens.radiusXXL)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = PharmaBlue500, modifier = Modifier.size(34.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("إعدادات اللغة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = PharmaBlue500)
                Text(
                    "اختر اللغة المفضلة لواجهة الصيدلية. الاختيار هنا معاينة آمنة إلى أن يتم ربط آلية تغيير اللغة العامة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            LanguageOption(
                title = "العربية",
                subtitle = "اللغة الحالية لتجربة RTL",
                icon = Icons.Outlined.Translate,
                selected = selectedLanguage == "العربية",
                onClick = { onLanguageSelected("العربية") },
            )
            LanguageOption(
                title = "English",
                subtitle = "System language preview only",
                icon = Icons.Outlined.Language,
                selected = selectedLanguage == "English",
                onClick = { onLanguageSelected("English") },
            )
            StitchButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("تطبيق الإعدادات", fontWeight = FontWeight.Bold)
            }
            Text(
                text = "إلغاء الأمر",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(bottom = MaterialTheme.dimens.spaceL),
            )
        }
    }
}

@Composable
private fun LanguageOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(MaterialTheme.dimens.radiusXL),
        color = if (selected) PharmaBlue50 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(MaterialTheme.dimens.radiusL),
                color = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.surface,
                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.outlineVariant,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                    )
                }
            }
        }
    }
}
