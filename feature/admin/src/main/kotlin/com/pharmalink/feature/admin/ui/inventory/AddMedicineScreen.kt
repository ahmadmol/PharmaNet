package com.pharmalink.feature.admin.ui.inventory

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.admin.ui.components.AdminProfileAvatarIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: AddMedicineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val d = MaterialTheme.dimens

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewModel.onImageSelected(uri)
        }
    )

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "إضافة دواء جديد",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    actions = {
                        AdminProfileAvatarIcon(
                            profileImageUrl = profileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            fallbackSize = 24.dp,
                            fallbackTint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(d.spaceM))
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = d.spaceL)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(d.spaceM)
            ) {
                Spacer(modifier = Modifier.height(d.spaceS))

                // Image Picker Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(d.radiusL))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(d.radiusL)
                        )
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imageUri != null) {
                        AsyncImage(
                            model = state.imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.onImageSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(d.spaceS)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "حذف الصورة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(d.spaceS)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "إضافة صورة الدواء",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                PharmaTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "اسم الدواء",
                    placeholder = "مثال: بانادول 500 مجم"
                )

                PharmaTextField(
                    value = state.brand,
                    onValueChange = viewModel::onBrandChange,
                    label = "الشركة المصنعة / العلامة التجارية",
                    placeholder = "مثال: GSK"
                )

                PharmaTextField(
                    value = state.strength,
                    onValueChange = viewModel::onStrengthChange,
                    label = "العيار / القوة",
                    placeholder = "مثال: 500 ملغ"
                )

                PharmaTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = "التفاصيل / الوصف",
                    placeholder = "تفاصيل مختصرة عن المنتج",
                )

                PharmaTextField(
                    value = state.specs,
                    onValueChange = viewModel::onSpecsChange,
                    label = "المواصفات",
                    placeholder = "مثال: 20 قرص، يحفظ بدرجة حرارة الغرفة",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM)
                ) {
                    PharmaTextField(
                        value = state.price,
                        onValueChange = viewModel::onPriceChange,
                        label = "سعر البيع (اختياري)",
                        placeholder = "اتركه فارغاً",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    PharmaTextField(
                        value = state.stockQuantity,
                        onValueChange = viewModel::onStockQuantityChange,
                        label = "الكمية المتوفرة",
                        placeholder = "0",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ProductVisibilityCard(
                    isVisible = state.isVisible,
                    onVisibilityChange = viewModel::onVisibilityChange,
                )

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = d.spaceS)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PharmaButton(
                    text = if (state.isUploading) "جاري الحفظ..." else "إضافة الدواء للمخزون",
                    onClick = viewModel::submitMedicine,
                    enabled = !state.isUploading && state.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceL)
                )

                if (state.isUploading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductVisibilityCard(
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(d.spaceM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(d.spaceXS),
            ) {
                Text(
                    text = "إظهار المنتج للصيدليات",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isVisible) "سيظهر في معرض المستودع لاحقاً" else "سيبقى مخفياً عن التصفح",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isVisible,
                onCheckedChange = onVisibilityChange,
            )
        }
    }
}
