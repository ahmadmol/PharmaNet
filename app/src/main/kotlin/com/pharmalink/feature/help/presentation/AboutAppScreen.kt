package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmalink.BuildConfig
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaOutlinedTile
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.theme.PharmaBlue700
import com.pharmalink.designsystem.theme.PharmaBlue900
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

data class AboutFeature(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

data class AboutValue(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

@Composable
fun AboutAppScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PharmaScreenScaffold(
            title = "حول التطبيق",
            onBack = onBack,
            navigationContentDescription = stringResource(R.string.common_back),
            modifier = modifier,
        ) {
            AboutAppContent()
        }
    }
}

@Composable
private fun AboutAppContent() {
    val d = MaterialTheme.dimens
    val features = listOf(
        AboutFeature(
            icon = Icons.Outlined.Store,
            title = "تشغيل الصيدلية من شاشة واحدة",
            description = "نجمع الطلبات والمستودعات والملف الشخصي والدعم ضمن رحلة عربية واضحة وسريعة للفريق.",
        ),
        AboutFeature(
            icon = Icons.Outlined.Lock,
            title = "ثقة وخصوصية افتراضيًا",
            description = "بنية نظيفة وصلاحيات واضحة وتجهيزات جاهزة لربط الحماية والامتثال دون بعثرة التجربة.",
        ),
        AboutFeature(
            icon = Icons.Outlined.Language,
            title = "تصميم عربي RTL أصيل",
            description = "كل هذه الشاشات مبنية مباشرة بـ Compose وتلتزم بهوية التطبيق الحالية من دون أي WebView أو تحويل HTML.",
        ),
    )
    val values = listOf(
        AboutValue(
            icon = Icons.Outlined.CheckCircleOutline,
            title = "الوضوح أولًا",
            description = "الواجهة تركز على المعلومة المهمة والقرار التالي بدل ازدحام العناصر والزينة غير المفيدة.",
        ),
        AboutValue(
            icon = Icons.Outlined.Info,
            title = "الموثوقية التشغيلية",
            description = "نترك الوظائف الخلفية غير المربوطة واضحة بصياغة صادقة بدل ادعاء نجاحات غير حقيقية.",
        ),
        AboutValue(
            icon = Icons.Outlined.Language,
            title = "خدمة قابلة للنمو",
            description = "تمهيد جاهز لإضافة مزيد من التكاملات والخدمات مع الحفاظ على بساطة التجربة الحالية.",
        ),
    )

    LazyColumn(
        contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            AboutHeroCard()
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                PharmaOutlinedTile(
                    title = "الإصدار",
                    value = BuildConfig.VERSION_NAME,
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = "الواجهة",
                    value = "RTL",
                    modifier = Modifier.weight(1f),
                )
                PharmaOutlinedTile(
                    title = "الدعم",
                    value = "مباشر",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            PharmaSectionHeader(
                title = "خدماتنا المميزة",
                subtitle = "ترجمة مرجعية الواجهة إلى بطاقات وخطوات عملية داخل تصميم PharmaLink الحالي.",
            )
        }
        items(features) { feature ->
            AboutFeatureCard(feature)
        }
        item {
            PharmaSectionHeader(
                title = "قيمنا الجوهرية",
                subtitle = "المهمة ليست مجرد شاشة جميلة، بل رحلة تشغيلية مستقرة وواضحة وقابلة للتوسع.",
            )
        }
        items(values) { value ->
            AboutValueCard(value)
        }
    }
}

@Composable
private fun AboutHeroCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = PharmaGradients.primaryHorizontal,
                    shape = RoundedCornerShape(d.radiusXXL),
                )
                .padding(d.spaceL),
        ) {
            androidx.compose.foundation.layout.Column(
                verticalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                ) {
                    Text(
                        text = "VERSION ${BuildConfig.VERSION_NAME}",
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "الرعاية الصحية باللمسة الرقمية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    text = "PharmaLink يقدّم تجربة عربية أنيقة لإدارة الطلبات والمستودعات والامتثال والدعم من داخل تطبيق واحد.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.94f),
                )
                Surface(
                    shape = RoundedCornerShape(d.radiusXL),
                    color = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Store,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "هوية واضحة • بطاقات راقية • معلومات قابلة للفهم فورًا",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureCard(feature: AboutFeature) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = RoundedCornerShape(d.radiusL),
                color = PharmaBlue900.copy(alpha = 0.08f),
                contentColor = PharmaBlue700,
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(20.dp),
                )
            }
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = feature.description,
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutValueCard(value: AboutValue) {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceL),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = value.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    text = value.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = value.description,
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
