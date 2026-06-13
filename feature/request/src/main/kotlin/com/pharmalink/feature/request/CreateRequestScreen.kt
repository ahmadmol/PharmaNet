package com.pharmalink.feature.request

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.stitch.components.StitchButton
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaBlue50
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaNeutral100
import com.pharmalink.designsystem.theme.PharmaNeutral600
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.feature.request.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    viewModel: CreateRequestViewModel = hiltViewModel(),
    onNavigateToRequestList: () -> Unit = {},
    onNavigateToRequestDetails: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var medicineMenuExpanded by remember { mutableStateOf(false) }
    var isSelectorVisible by remember { mutableStateOf(false) }
    val d = MaterialTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ClinicalCanvas),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = d.spaceL,
                top = d.spaceL,
                end = d.spaceL,
                bottom = d.spaceXL,
            ),
            verticalArrangement = Arrangement.spacedBy(d.spaceL),
        ) {
            item {
                CreateRequestHeader()
            }
            item {
                RequestSummaryCard(uiState = uiState)
            }
            item {
                RequestFormCard(
                    uiState = uiState,
                    medicineMenuExpanded = medicineMenuExpanded,
                    onMedicineMenuExpandedChange = { medicineMenuExpanded = it },
                    onMedicineSelected = { medicine ->
                        viewModel.onMedicineSelected(medicine)
                        medicineMenuExpanded = false
                    },
                    onOpenSelector = { isSelectorVisible = true },
                    onQuantityChange = { next ->
                        viewModel.onQuantityChange(next.coerceAtLeast(1).toString())
                    },
                    onPendingUnitChange = viewModel::onPendingUnitChange,
                    onAddToBasket = viewModel::addSelectedItemToBasket,
                    onEditBasketItem = viewModel::editBasketItem,
                    onRemoveBasketItem = viewModel::removeBasketItem,
                    onClearBasket = viewModel::clearBasket,
                    onNotesChange = viewModel::onNotesChange,
                )
            }
            item {
                RequestStatusCard(
                    uiState = uiState,
                    onViewDetails = {
                        if (uiState.createdRequestId.isNotBlank()) {
                            onNavigateToRequestDetails(uiState.createdRequestId)
                        } else {
                            onNavigateToRequestList()
                        }
                    },
                    onCreateAnother = viewModel::resetState,
                    onDismissError = viewModel::clearError,
                )
            }
        }

        CreateRequestBottomActions(
            isLoading = uiState.isLoading,
            canSubmit = uiState.selectedWarehouseId.isNotBlank() &&
                (uiState.items.isNotEmpty() || uiState.selectedMedicine != null),
            itemCount = uiState.items.size,
            totalQuantity = uiState.items.sumOf { it.quantity },
            onSubmit = viewModel::sendRequest,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = d.spaceL, vertical = d.spaceM),
        )
    }

    if (isSelectorVisible) {
        MedicineWarehouseSelectorSheet(
            uiState = uiState,
            onMedicineSelected = viewModel::onMedicineSelected,
            onWarehouseSelected = viewModel::onWarehouseSelected,
            onDismiss = { isSelectorVisible = false },
        )
    }

    if (uiState.isSuccess) {
        AlertDialog(
            onDismissRequest = { /* Dialog is not dismissible by external click */ },
            title = {
                Text(text = stringResource(R.string.request_sent_title))
            },
            text = {
                Text(text = stringResource(R.string.request_sent_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNavigateToRequestList()
                        viewModel.clearRequestSent()
                    }
                ) {
                    Text(stringResource(R.string.request_view_my_requests))
                }
            }
        )
    }
}

@Composable
private fun CreateRequestHeader() {
    val d = MaterialTheme.dimens

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PharmaGradients.headerBlueToGreenHorizontal)
                .padding(d.spaceL),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(d.radiusL),
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.LocalPharmacy,
                            contentDescription = null,
                            modifier = Modifier.size(d.iconL),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = stringResource(R.string.create_request_screen_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(Modifier.height(d.spaceXS))
                    Text(
                        text = "جهز طلب النواقص بسرعة وأرسله للمستودع المناسب.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestSummaryCard(uiState: CreateRequestUiState) {
    val d = MaterialTheme.dimens
    val medicine = uiState.selectedMedicine
    val statusText = when {
        uiState.isSuccess -> stringResource(R.string.request_status_submitted)
        uiState.isLoading -> stringResource(R.string.sending_request_message)
        medicine == null -> "بانتظار اختيار الدواء"
        else -> "جاهز للمراجعة"
    }
    val statusColor = when {
        uiState.isSuccess -> PharmaSuccess
        uiState.errorMessage != null -> PremiumUrgent
        else -> PharmaBlue500
    }

    DashboardCard(
        containerColor = MaterialTheme.colorScheme.surface,
        elevationDp = 4f,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(d.radiusXL),
                color = PharmaBlue50,
                contentColor = PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.size(d.iconL),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = medicine?.name ?: "لم يتم اختيار دواء بعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                )
                Spacer(Modifier.height(d.spaceXS))
                Text(
                    text = medicine?.let { "${it.brand} - ${it.strength}" }
                        ?: "اختر الدواء والكمية لإكمال الطلب.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }

        Spacer(Modifier.height(d.spaceL))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            SummaryMetric(
                label = "الكمية",
                value = uiState.quantity.ifBlank { "1" },
                modifier = Modifier.weight(1f),
            )
            SummaryMetric(
                label = "الحالة",
                value = statusText,
                valueColor = statusColor,
                modifier = Modifier.weight(1.7f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestFormCard(
    uiState: CreateRequestUiState,
    medicineMenuExpanded: Boolean,
    onMedicineMenuExpandedChange: (Boolean) -> Unit,
    onMedicineSelected: (MedicineItem) -> Unit,
    onOpenSelector: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onPendingUnitChange: (String) -> Unit,
    onAddToBasket: () -> Unit,
    onEditBasketItem: (String, Int, String) -> Unit,
    onRemoveBasketItem: (String) -> Unit,
    onClearBasket: () -> Unit,
    onNotesChange: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    val quantity = uiState.quantity.toIntOrNull() ?: 1
    val selectedWarehouse = uiState.selectedWarehouseOption()
    val resolvedUnit = uiState.pendingUnit.ifBlank { uiState.selectedMedicine?.strength.orEmpty() }
    val unitOptions = listOf(
        uiState.selectedMedicine?.strength.orEmpty(),
        "علبة",
        "شريط",
        "حبة",
        "زجاجة",
    ).filter { it.isNotBlank() }.distinct()

    DashboardCard {
        SectionTitle(
            title = "تفاصيل الطلب",
            subtitle = "املأ المعلومات الأساسية لإرسال طلب واضح للمستودع.",
        )

        Spacer(Modifier.height(d.spaceL))

        Text(
            text = stringResource(R.string.select_medicine_placeholder),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(d.spaceS))
        ExposedDropdownMenuBox(
            expanded = medicineMenuExpanded,
            onExpandedChange = {
                if (uiState.medicines.isNotEmpty()) {
                    onMedicineMenuExpandedChange(it)
                }
            },
        ) {
            OutlinedTextField(
                value = uiState.selectedMedicine?.name.orEmpty(),
                onValueChange = { },
                readOnly = true,
                placeholder = { Text("اختر دواء من القائمة") },
                leadingIcon = {
                    Icon(Icons.Outlined.Medication, contentDescription = null)
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = medicineMenuExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(d.radiusL),
                colors = polishedFieldColors(),
                supportingText = {
                    if (uiState.medicines.isEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("جاري تحميل قائمة الأدوية")
                        }
                    }
                },
            )
            ExposedDropdownMenu(
                expanded = medicineMenuExpanded,
                onDismissRequest = { onMedicineMenuExpandedChange(false) },
            ) {
                if (uiState.medicines.isEmpty()) {
                    Text(
                        text = "جاري تحميل الأدوية...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
                    )
                } else {
                    uiState.medicines.forEach { medicine ->
                        DropdownMenuItem(
                            text = {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = medicine.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${medicine.brand} - ${medicine.strength}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = { onMedicineSelected(medicine) },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(d.spaceL))

        Text(
            text = "المستودع / المورد",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(d.spaceS))
        SelectedWarehouseCard(
            selectedWarehouse = selectedWarehouse,
            onOpenSelector = onOpenSelector,
        )

        Spacer(Modifier.height(d.spaceL))

        QuantityStepper(
            quantity = quantity,
            onQuantityChange = onQuantityChange,
        )

        Spacer(Modifier.height(d.spaceL))

        UnitSelector(
            value = resolvedUnit,
            options = unitOptions,
            onUnitSelected = onPendingUnitChange,
        )

        Spacer(Modifier.height(d.spaceL))

        StitchButton(
            onClick = onAddToBasket,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.selectedMedicine != null,
            contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceM),
        ) {
            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(d.iconS))
            Spacer(Modifier.width(d.spaceS))
            Text("إضافة إلى السلة", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(d.spaceL))

        BasketCard(
            items = uiState.items,
            onEditBasketItem = onEditBasketItem,
            onRemoveBasketItem = onRemoveBasketItem,
            onClearBasket = onClearBasket,
        )

        Spacer(Modifier.height(d.spaceL))

        Text(
            text = stringResource(R.string.notes_label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(d.spaceS))
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onNotesChange,
            placeholder = { Text(stringResource(R.string.notes_placeholder)) },
            leadingIcon = {
                Icon(Icons.Outlined.StickyNote2, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            singleLine = false,
            shape = RoundedCornerShape(d.radiusL),
            colors = polishedFieldColors(),
        )
    }
}

@Composable
private fun SelectedWarehouseCard(
    selectedWarehouse: WarehouseOption?,
    onOpenSelector: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSelector),
        shape = RoundedCornerShape(d.radiusXL),
        color = PharmaBlue50,
        contentColor = PharmaBlue500,
        border = BorderStroke(1.dp, PharmaBlue500.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surface,
                contentColor = PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Warehouse, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = selectedWarehouse?.name ?: "اختر المستودع",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = selectedWarehouse?.let {
                        listOf(it.location, it.deliveryLabel).filter { value -> value.isNotBlank() }.joinToString(" - ")
                    }?.ifBlank { selectedWarehouse.statusLabel } ?: "مستودع واحد لكل طلب",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitSelector(
    value: String,
    options: List<String>,
    onUnitSelected: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text("الوحدة") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(d.radiusL),
            colors = polishedFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BasketCard(
    items: List<CreateRequestBasketItem>,
    onEditBasketItem: (String, Int, String) -> Unit,
    onRemoveBasketItem: (String) -> Unit,
    onClearBasket: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            ) {
                Text(
                    text = "السلة الحالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    shape = CircleShape,
                    color = PharmaBlue50,
                    contentColor = PharmaBlue500,
                ) {
                    Text(
                        text = "${items.size} صنف",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
                    )
                }
                if (items.isNotEmpty()) {
                    TextButton(onClick = onClearBasket) {
                        Text("مسح")
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "أضف الأدوية المطلوبة هنا قبل إرسال الطلب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                items.forEach { item ->
                    BasketItemRow(
                        item = item,
                        onEditBasketItem = onEditBasketItem,
                        onRemoveBasketItem = onRemoveBasketItem,
                    )
                }
            }
        }
    }
}

@Composable
private fun BasketItemRow(
    item: CreateRequestBasketItem,
    onEditBasketItem: (String, Int, String) -> Unit,
    onRemoveBasketItem: (String) -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusL),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
        ) {
            Icon(Icons.Outlined.Medication, contentDescription = null, tint = PharmaBlue500)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = item.medicineName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = listOf(item.medicineSubtitle, item.unit).filter { it.isNotBlank() }.joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = {
                    if (item.quantity > 1) {
                        onEditBasketItem(item.medicineId, item.quantity - 1, item.unit)
                    }
                },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "تقليل الكمية", tint = PharmaBlue500)
            }
            Text(
                text = item.quantity.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp),
            )
            IconButton(
                onClick = { onEditBasketItem(item.medicineId, item.quantity + 1, item.unit) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "زيادة الكمية", tint = PharmaBlue500)
            }
            IconButton(
                onClick = { onRemoveBasketItem(item.medicineId) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = "حذف الصنف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun SelectorLaunchCard(
    uiState: CreateRequestUiState,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = PharmaBlue50,
        contentColor = PharmaBlue500,
    ) {
        Row(
            modifier = Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surface,
                contentColor = PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(d.iconM))
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "اختيار الدواء والمستودع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append(uiState.selectedMedicine?.name ?: "اختر دواء من القائمة")
                        append(" • ")
                        append(uiState.selectedWarehouseName.ifBlank { "اختر المستودع" })
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null)
        }
    }
}

@Composable
private fun WarehouseQuickList(
    warehouses: List<WarehouseOption>,
    selectedWarehouseId: String,
    onWarehouseSelected: (String, String) -> Unit,
) {
    val d = MaterialTheme.dimens
    Column(verticalArrangement = Arrangement.spacedBy(d.spaceS)) {
        if (warehouses.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(d.radiusL),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Text(
                    text = "سيظهر المستودع هنا بعد تحميل البيانات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(d.spaceM),
                )
            }
        } else {
            warehouses.take(3).forEach { warehouse ->
                WarehouseOptionRow(
                    warehouse = warehouse,
                    selected = warehouse.id == selectedWarehouseId,
                    onClick = { onWarehouseSelected(warehouse.id, warehouse.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineWarehouseSelectorSheet(
    uiState: CreateRequestUiState,
    onMedicineSelected: (MedicineItem) -> Unit,
    onWarehouseSelected: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = d.spaceL, end = d.spaceL, bottom = d.spaceXL),
            verticalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            item {
                SectionTitle(
                    title = "اختيار الدواء والمستودع",
                    subtitle = "واجهة كتالوج آمنة تستخدم بيانات الأدوية الحالية والمستودعات المتاحة دون إضافة منطق وهمي.",
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(d.radiusXL),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(d.spaceM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = PharmaBlue500)
                        Text(
                            text = "ابحث لاحقا عن دواء أو مادة فعالة أو مستودع",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Text("الأدوية المتاحة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PharmaBlue500)
            }
            items(uiState.medicines.size) { index ->
                val medicine = uiState.medicines[index]
                MedicineOptionCard(
                    medicine = medicine,
                    selected = medicine == uiState.selectedMedicine,
                    onClick = { onMedicineSelected(medicine) },
                )
            }
            item {
                Text("اختيار المستودع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PharmaBlue500)
            }
            if (uiState.warehouses.isEmpty()) {
                item {
                    Text("لا توجد مستودعات محملة حاليا.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.warehouses.size) { index ->
                    val warehouse = uiState.warehouses[index]
                    WarehouseOptionRow(
                        warehouse = warehouse,
                        selected = warehouse.id == uiState.selectedWarehouseId,
                        onClick = { onWarehouseSelected(warehouse.id, warehouse.name) },
                    )
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(d.radiusXL),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onDismiss,
                ) {
                    Text(
                        text = "تأكيد الاختيار",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(d.spaceM),
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicineOptionCard(
    medicine: MedicineItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = if (selected) PharmaBlue50 else MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (selected) PharmaBlue500.copy(alpha = 0.45f) else Color.Transparent),
        shadowElevation = if (selected) 3.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = if (selected) PharmaBlue500 else PharmaBlue50,
                contentColor = if (selected) Color.White else PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Medication, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(medicine.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${medicine.brand} - ${medicine.strength}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = PharmaSuccess.copy(alpha = 0.12f), contentColor = PharmaSuccess) {
                Text("متوفر", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS))
            }
        }
    }
}

@Composable
private fun WarehouseOptionRow(
    warehouse: WarehouseOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(d.radiusXL),
        color = if (selected) PharmaBlue50 else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) PharmaBlue500.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
    ) {
        Row(
            modifier = Modifier.padding(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(d.radiusL),
                color = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                contentColor = if (selected) Color.White else PharmaBlue500,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (selected) Icons.Outlined.Store else Icons.Outlined.Warehouse, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(warehouse.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = listOf(warehouse.location, warehouse.deliveryLabel).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { warehouse.statusLabel },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${warehouse.stockPercent}%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) PharmaBlue500 else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun CreateRequestUiState.selectedWarehouseOption(): WarehouseOption? {
    val selectedId = selectedWarehouseId.trim()
    if (selectedId.isBlank()) return null
    return warehouses.firstOrNull { it.id == selectedId }
        ?: WarehouseOption(
            id = selectedId,
            name = selectedWarehouseName.ifBlank {
                items.firstOrNull { it.warehouseId == selectedId }?.warehouseName.orEmpty()
            },
            location = "",
            statusLabel = "",
            deliveryLabel = "",
            stockPercent = 0,
        ).takeIf { it.name.isNotBlank() }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "الكمية المطلوبة",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "يمكن تعديلها قبل إرسال الطلب.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = RoundedCornerShape(d.radiusXL),
                color = PharmaBlue50,
                contentColor = PharmaBlue500,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = d.spaceS, vertical = d.spaceXS),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(d.spaceS),
                ) {
                    StepperButton(
                        icon = Icons.Outlined.Remove,
                        contentDescription = "تقليل الكمية",
                        onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                    )
                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.dp),
                    )
                    StepperButton(
                        icon = Icons.Outlined.Add,
                        contentDescription = "زيادة الكمية",
                        onClick = { onQuantityChange(quantity + 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RequestStatusCard(
    uiState: CreateRequestUiState,
    onViewDetails: () -> Unit,
    onCreateAnother: () -> Unit,
    onDismissError: () -> Unit,
) {
    val d = MaterialTheme.dimens

    when {
        uiState.isLoading -> {
            FeedbackCard(
                icon = Icons.Outlined.HourglassTop,
                title = stringResource(R.string.sending_request_message),
                message = "نراجع البيانات ونرسل الطلب للمستودع.",
                containerColor = PharmaBlue50,
                contentColor = PharmaBlue500,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = PharmaBlue500,
                )
            }
        }
        uiState.errorMessage != null -> {
            FeedbackCard(
                icon = Icons.Outlined.ErrorOutline,
                title = "خطأ في إرسال الطلب",
                message = uiState.errorMessage,
                containerColor = PremiumUrgent.copy(alpha = 0.1f),
                contentColor = PremiumUrgent,
            ) {
                StitchButton(
                    onClick = onDismissError,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceS),
                ) {
                    Text(stringResource(R.string.request_dismiss_error))
                }
            }
        }
        !uiState.isLoading && uiState.errorMessage == null && !uiState.isSuccess -> {
            FeedbackCard(
                icon = Icons.Outlined.KeyboardArrowDown,
                title = "خطوة أخيرة",
                message = "بعد اختيار الدواء والكمية، اضغط إرسال الطلب.",
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CreateRequestBottomActions(
    isLoading: Boolean,
    canSubmit: Boolean,
    itemCount: Int,
    totalQuantity: Int,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(d.spaceS),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomSummaryMetric(
                label = "عدد الأصناف",
                value = itemCount.toString(),
                modifier = Modifier.weight(1f),
            )
            BottomSummaryMetric(
                label = "إجمالي الكمية",
                value = totalQuantity.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        StitchButton(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && canSubmit,
            contentPadding = PaddingValues(horizontal = d.spaceL, vertical = d.spaceM),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(d.spaceS))
            } else {
                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(d.iconS))
                Spacer(Modifier.width(d.spaceS))
            }
            Text(
                text = if (isLoading) stringResource(R.string.sending_request_message) else stringResource(R.string.send_request_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun BottomSummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusM),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceS),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FeedbackCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXL),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.16f)),
    ) {
        Column(Modifier.padding(d.spaceL)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.spaceM),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(d.iconM),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.78f),
                    )
                }
            }
            if (action != null) {
                Spacer(Modifier.height(d.spaceM))
                action()
            }
        }
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevationDp: Float = 2f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val d = MaterialTheme.dimens

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(d.radiusXXL),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationDp.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceL),
            horizontalAlignment = Alignment.Start,
            content = content,
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val d = MaterialTheme.dimens

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(d.radiusL),
        color = PharmaNeutral100,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
    ) {
        Column(
            modifier = Modifier.padding(d.spaceM),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = PharmaNeutral600,
            )
            Spacer(Modifier.height(d.spaceXS))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = PharmaBlue500,
        )
    }
}

@Composable
private fun polishedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PharmaBlue500,
    unfocusedBorderColor = Color.Transparent,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = PharmaBlue50.copy(alpha = 0.55f),
    cursorColor = PharmaBlue500,
    focusedLeadingIconColor = PharmaBlue500,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = PharmaBlue500,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Preview(showBackground = true)
@Composable
fun CreateRequestScreenPreview() {
    StitchTheme {
        CreateRequestScreen()
    }
}
