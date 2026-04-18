package com.pharmalink.feature.help.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pharmalink.R
import com.pharmalink.designsystem.components.PharmaScreenScaffold
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.dimens

data class ContactChannel(
    val icon: ImageVector,
    val title: String,
    val value: String,
    val helper: String,
    val link: String,
)

@Composable
fun ContactUsScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        PharmaScreenScaffold(
            title = "التواصل معنا",
            onBack = onBack,
            navigationContentDescription = stringResource(R.string.common_back),
            modifier = modifier,
        ) {
            ContactUsContent()
        }
    }
}

@Composable
private fun ContactUsContent() {
    val d = MaterialTheme.dimens
    val uriHandler = LocalUriHandler.current
    var name by rememberSaveable { mutableStateOf("") }
    var replyChannel by rememberSaveable { mutableStateOf("") }
    var topic by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val channels = listOf(
        ContactChannel(
            icon = Icons.Outlined.Phone,
            title = "اتصال مباشر",
            value = "+966 800 100 4422",
            helper = "للطلبات الحرجة والمشكلات التشغيلية العاجلة خلال ساعات العمل.",
            link = "tel:+9668001004422",
        ),
        ContactChannel(
            icon = Icons.Outlined.Email,
            title = "البريد الإلكتروني",
            value = "support@pharmalink.sa",
            helper = "لمشاركة التفاصيل المكتوبة أو المستندات أو متابعة الحالات غير العاجلة.",
            link = "mailto:support@pharmalink.sa",
        ),
        ContactChannel(
            icon = Icons.Outlined.Phone,
            title = "واتساب",
            value = "+966 800 100 4422",
            helper = "مناسب للتحديثات السريعة وإرسال لقطات الشاشة والاستفسارات القصيرة.",
            link = "https://wa.me/9668001004422",
        ),
    )

    LazyColumn(
        contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, top = d.spaceS, bottom = d.spaceXXL),
        verticalArrangement = Arrangement.spacedBy(d.spaceL),
    ) {
        item {
            ContactHeroCard()
        }
        item {
            PharmaSectionHeader(
                title = "قنوات مباشرة",
                subtitle = "اختر وسيلة التواصل الأنسب حسب سرعة الاستجابة وطبيعة الحالة.",
            )
        }
        items(channels) { channel ->
            ContactChannelCard(
                channel = channel,
                onClick = { uriHandler.openUri(channel.link) },
            )
        }
        item {
            LocationCard(
                onOpenMap = {
                    uriHandler.openUri("https://maps.google.com/?q=PharmaLink+Riyadh+Saudi+Arabia")
                },
            )
        }
        item {
            PharmaSectionHeader(
                title = "نموذج التواصل",
                subtitle = "النموذج جاهز بصريًا الآن، لكن الإرسال الفعلي لم يُربط بخدمة خلفية بعد عمدًا.",
            )
        }
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier.padding(d.spaceL),
                    verticalArrangement = Arrangement.spacedBy(d.spaceM),
                ) {
                    PharmaTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "الاسم",
                    )
                    PharmaTextField(
                        value = replyChannel,
                        onValueChange = { replyChannel = it },
                        label = "رقم الهاتف أو البريد الإلكتروني",
                    )
                    PharmaTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = "موضوع الرسالة",
                    )
                    PharmaTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = "تفاصيل الحالة",
                        singleLine = false,
                        supportingText = "شارك رقم الطلب أو المورد أو أي تفاصيل تساعد فريق الدعم.",
                    )
                    Surface(
                        shape = RoundedCornerShape(d.radiusL),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ) {
                        Text(
                            text = "سيتم تفعيل إرسال النموذج بعد ربط واجهة الدعم. حتى ذلك الحين استخدم القنوات المباشرة أعلاه.",
                            modifier = Modifier.padding(d.spaceM),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(),
                    ) {
                        Text("إرسال النموذج قريبًا")
                    }
                }
            }
        }
        item {
            TrustCard()
        }
    }
}

@Composable
private fun ContactHeroCard() {
    val d = MaterialTheme.dimens

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreenHorizontal)
                .padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Text(
                text = "تواصل مع فريق PharmaLink",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = "هاتف مباشر، بريد، واتساب، وموقع رئيسي واضح — مع نموذج جاهز بصريًا بانتظار ربط الإرسال الحقيقي.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("نموذج آمن")
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(d.radiusXL),
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.size(d.spaceXS))
                        Text(
                            text = "الرياض",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactChannelCard(
    channel: ContactChannel,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = channel.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(d.spaceS)
                        .size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = channel.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = channel.value,
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = channel.helper,
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocationCard(
    onOpenMap: () -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenMap),
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
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "الموقع الرئيسي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "المكتب الإقليمي - الرياض، المملكة العربية السعودية",
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "أيام العمل: الأحد إلى الخميس • 9:00 ص - 6:00 م",
                    modifier = Modifier.padding(top = d.spaceXS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrustCard() {
    val d = MaterialTheme.dimens
    val bullets = listOf(
        "فريق دعم حقيقي يتعامل مع الحالات التشغيلية والتنظيمية بوضوح وسرعة.",
        "بياناتك لا تُرسل عبر النموذج حاليًا لأن التكامل الخلفي غير مفعل بعد.",
        "يمكنك دائمًا الاعتماد على الهاتف أو البريد أو واتساب للوصول المباشر الآن.",
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            verticalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Text(
                text = "طمأنة وثقة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            bullets.forEach { bullet ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = bullet,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
