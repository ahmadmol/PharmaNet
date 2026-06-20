package com.pharmalink.feature.admin.ui.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun EditMedicineScreen(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
    viewModel: EditMedicineViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            viewModel.onImageSelected(uri)
        }
    )

    LaunchedEffect(state.isSuccess, state.isDeleted) {
        if (state.isSuccess || state.isDeleted) {
            onSuccess()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("حذف الدواء؟") },
            text = { Text("لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteMedicine()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    EditMedicineContent(
        state = state,
        onBackClick = onBackClick,
        onDeleteClick = { showDeleteDialog = true },
        onImagePickerClick = { imagePickerLauncher.launch("image/*") },
        onImageDeleteClick = { viewModel.onImageSelected(null) },
        onNameChange = viewModel::onNameChange,
        onBrandChange = viewModel::onBrandChange,
        onStrengthChange = viewModel::onStrengthChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSpecsChange = viewModel::onSpecsChange,
        onPriceChange = viewModel::onPriceChange,
        onStockQuantityChange = viewModel::onStockQuantityChange,
        onVisibilityChange = viewModel::onVisibilityChange,
        onActiveChange = viewModel::onActiveChange,
        onSubmit = viewModel::submitMedicine,
        profileImageUrl = profileImageUrl,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMedicineContent(
    state: EditMedicineUiState,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImagePickerClick: () -> Unit,
    onImageDeleteClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onStrengthChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSpecsChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onStockQuantityChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    profileImageUrl: String? = null,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier,
            containerColor = ClinicalCanvas,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "تعديل دواء",
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
                        TextButton(
                            onClick = onDeleteClick,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("حذف")
                        }
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
                        .clickable { onImagePickerClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imageUri != null || state.existingImageUrl != null) {
                        AsyncImage(
                            model = state.imageUri ?: state.existingImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = onImageDeleteClick,
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
                    onValueChange = onNameChange,
                    label = "اسم الدواء",
                    placeholder = "مثال: بانادول 500 مجم"
                )

                PharmaTextField(
                    value = state.brand,
                    onValueChange = onBrandChange,
                    label = "الشركة المصنعة / العلامة التجارية",
                    placeholder = "مثال: GSK"
                )

                PharmaTextField(
                    value = state.strength,
                    onValueChange = onStrengthChange,
                    label = "العيار / القوة",
                    placeholder = "مثال: 500 ملغ"
                )

                PharmaTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = "التفاصيل / الوصف",
                    placeholder = "تفاصيل مختصرة عن المنتج",
                )

                PharmaTextField(
                    value = state.specs,
                    onValueChange = onSpecsChange,
                    label = "المواصفات",
                    placeholder = "مثال: 20 قرص، يحفظ بدرجة حرارة الغرفة",
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(d.spaceM)
                ) {
                    PharmaTextField(
                        value = state.price,
                        onValueChange = onPriceChange,
                        label = "سعر البيع (اختياري)",
                        placeholder = "اتركه فارغاً",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    PharmaTextField(
                        value = state.stockQuantity,
                        onValueChange = onStockQuantityChange,
                        label = "الكمية المتوفرة",
                        placeholder = "0",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                ProductVisibilityCard(
                    isVisible = state.isVisible,
                    onVisibilityChange = onVisibilityChange,
                )

                ProductActiveCard(
                    isActive = state.isActive,
                    onActiveChange = onActiveChange,
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
                    text = if (state.isUploading) "جاري الحفظ..." else "حفظ التعديلات",
                    onClick = onSubmit,
                    enabled = !state.isUploading && !state.isDeleting && state.name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = d.spaceL)
                )

                if (state.isUploading || state.isDeleting) {
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

@Composable
private fun ProductActiveCard(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
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
                    text = "المنتج نشط",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (isActive) "متاح للطلب من الصيدليات" else "موقوف مؤقتاً ولا يستقبل طلبات",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = onActiveChange,
            )
        }
    }
}
