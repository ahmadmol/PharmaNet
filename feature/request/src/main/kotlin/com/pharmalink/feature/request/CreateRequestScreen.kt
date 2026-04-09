package com.pharmalink.feature.request

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.pharmalink.core.common.ui.UiState
import com.pharmalink.designsystem.components.PharmaButton
import com.pharmalink.designsystem.components.PharmaButtonStyle
import com.pharmalink.designsystem.components.PharmaCard
import com.pharmalink.designsystem.components.PharmaSectionHeader
import com.pharmalink.designsystem.components.PharmaTextField
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.PharmaSuccess
import com.pharmalink.designsystem.theme.PremiumUrgent
import com.pharmalink.designsystem.theme.dimens
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRequestScreen(
    onSubmitted: (requestId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateRequestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val d = MaterialTheme.dimens
    val placeholderDash = stringResource(R.string.request_placeholder_dash)

    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var unitExpanded by remember { mutableStateOf(false) }
    var warehouseExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::onImagePicked)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
            viewModel.onImagePicked(pendingCameraUri!!)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(state.submitState) {
        when (val submitState = state.submitState) {
            is UiState.Success -> snackbarHostState.showSnackbar(context.getString(R.string.request_success))
            is UiState.Error -> {
                val message = when (submitState.message) {
                    "validation" -> context.getString(R.string.request_validation_fix_fields)
                    else -> submitState.message ?: context.getString(R.string.request_error_generic)
                }
                snackbarHostState.showSnackbar(message)
                viewModel.clearError()
            }
            else -> Unit
        }
    }

    val suggestions = remember(state.medicineName) { viewModel.suggestionsFor(state.medicineName) }
    val selectedWarehouse = state.availableWarehouses.firstOrNull { it.id == state.selectedWarehouseId }
    val urgentScale by animateFloatAsState(
        targetValue = if (state.urgent) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "urgent_scale",
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ClinicalCanvas,
        bottomBar = {
            Column(Modifier.padding(d.spaceL)) {
                if (state.submitState is UiState.Loading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Spacer(Modifier.height(d.spaceS))
                }
                PharmaButton(
                    text = stringResource(R.string.request_submit),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.submit()
                    },
                    enabled = state.submitState !is UiState.Loading,
                    style = PharmaButtonStyle.GradientAccent,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = d.spaceL)
                .padding(bottom = d.spaceL),
        ) {
            Spacer(Modifier.height(d.spaceM))
            Text(text = stringResource(R.string.request_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = stringResource(R.string.request_step, state.step.coerceIn(1, 3)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(d.spaceL))
            LinearProgressIndicator(progress = { state.step / 3f }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(d.spaceXL))
            PharmaSectionHeader(title = stringResource(R.string.request_step_medicine), subtitle = stringResource(R.string.request_section_medicine_hint))
            Spacer(Modifier.height(d.spaceM))
            PharmaTextField(
                value = state.medicineName,
                onValueChange = viewModel::onMedicineChange,
                label = stringResource(R.string.request_medicine_label),
                errorMessage = state.medicineError,
            )
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(d.spaceS))
                Column(verticalArrangement = Arrangement.spacedBy(d.spaceXS)) {
                    suggestions.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .clickable {
                                    viewModel.onMedicineChange(suggestion)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                .padding(d.spaceM),
                        )
                    }
                }
            }

            Spacer(Modifier.height(d.spaceXL))
            PharmaSectionHeader(title = stringResource(R.string.request_step_quantity))
            Spacer(Modifier.height(d.spaceM))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundCounterButton(icon = Icons.Outlined.Remove) { viewModel.updateQuantity(-1) }
                    Spacer(Modifier.width(d.spaceM))
                    Text(state.quantity.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(d.spaceM))
                    RoundCounterButton(icon = Icons.Outlined.Add) { viewModel.updateQuantity(1) }
                }
                ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = !unitExpanded }) {
                    PharmaTextField(
                        value = state.unit,
                        onValueChange = {},
                        label = stringResource(R.string.request_unit),
                        modifier = Modifier.menuAnchor().width(140.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        fullWidth = false,
                    )
                    DropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }, modifier = Modifier.menuAnchor()) {
                        viewModel.unitOptions().forEach { unit ->
                            DropdownMenuItem(text = { Text(unit) }, onClick = {
                                viewModel.updateUnit(unit)
                                unitExpanded = false
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(d.spaceXL))
            PharmaSectionHeader(title = stringResource(R.string.resources_screen_title), subtitle = stringResource(R.string.request_section_warehouse_hint))
            Spacer(Modifier.height(d.spaceM))
            ExposedDropdownMenuBox(expanded = warehouseExpanded, onExpandedChange = { warehouseExpanded = !warehouseExpanded }) {
                PharmaTextField(
                    value = selectedWarehouse?.name ?: "",
                    onValueChange = {},
                    label = stringResource(R.string.resources_screen_title),
                    modifier = Modifier.menuAnchor(),
                    errorMessage = state.warehouseError,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = warehouseExpanded) },
                )
                DropdownMenu(expanded = warehouseExpanded, onDismissRequest = { warehouseExpanded = false }, modifier = Modifier.menuAnchor()) {
                    state.availableWarehouses.forEach { warehouse ->
                        DropdownMenuItem(text = { Text("${warehouse.name} • ${warehouse.estimatedDeliveryLabel}") }, onClick = {
                            viewModel.updateWarehouse(warehouse.id)
                            warehouseExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(d.spaceXL))
            PharmaSectionHeader(title = stringResource(R.string.request_step_review), subtitle = stringResource(R.string.request_section_review_hint))
            Spacer(Modifier.height(d.spaceM))
            PharmaTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = stringResource(R.string.request_notes_label),
                singleLine = false,
                errorMessage = state.notesError,
            )

            Spacer(Modifier.height(d.spaceL))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                RoundMediaButton(
                    icon = Icons.Outlined.CameraAlt,
                    contentDescription = stringResource(R.string.request_camera),
                    brush = PharmaGradients.primaryHorizontal,
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                )
                RoundMediaButton(
                    icon = Icons.Outlined.Image,
                    contentDescription = stringResource(R.string.request_gallery),
                    brush = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                )
            }

            state.imageUri?.let { uri ->
                Spacer(Modifier.height(d.spaceXL))
                Text(text = stringResource(R.string.request_image_preview), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(d.spaceS))
                PharmaCard(elevationDp = 4f) {
                    SubcomposeAsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp),
                                )
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(d.spaceXL))
            Text(text = stringResource(R.string.request_urgency_normal) + " / " + stringResource(R.string.request_urgency_urgent), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(d.spaceM))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.spaceM)) {
                UrgencyOption(
                    label = stringResource(R.string.request_urgency_normal),
                    selected = !state.urgent,
                    accent = PharmaSuccess,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        viewModel.onUrgentChange(false)
                    },
                )
                UrgencyOption(
                    label = stringResource(R.string.request_urgency_urgent),
                    selected = state.urgent,
                    accent = PremiumUrgent,
                    modifier = Modifier.weight(1f).scale(urgentScale),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        viewModel.onUrgentChange(true)
                    },
                )
            }

            if (selectedWarehouse != null) {
                Spacer(Modifier.height(d.spaceXL))
                PharmaCard {
                    Column(Modifier.padding(d.spaceL)) {
                        Text(stringResource(R.string.request_summary_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(d.spaceS))
                        Text(
                            stringResource(
                                R.string.request_review_line,
                                state.medicineName.ifBlank { placeholderDash },
                                state.quantity,
                                state.unit,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            stringResource(
                                R.string.request_eta_line,
                                selectedWarehouse.name,
                                selectedWarehouse.estimatedDeliveryLabel,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = state.showSuccess && state.submitState is UiState.Success) {
                val request = (state.submitState as? UiState.Success)?.data
                Column {
                    Spacer(Modifier.height(d.spaceXL))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.large).padding(d.spaceL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(d.spaceM),
                    ) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.request_created_banner), fontWeight = FontWeight.SemiBold)
                            Text(request?.id ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = stringResource(R.string.request_success_cta),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                val requestId = request?.id.orEmpty()
                                viewModel.consumeSubmitSuccess()
                                onSubmitted(requestId)
                            },
                        )
                    }
                }
            }

            if (state.submitState is UiState.Loading) {
                Spacer(Modifier.height(d.spaceL))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(d.space6XL))
        }
    }
}

@Composable
private fun RoundMediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    brush: Brush,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(72.dp).clip(CircleShape).background(brush).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(),
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun RoundCounterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(),
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun UrgencyOption(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val bg = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val border = if (selected) accent else Color.Transparent
    Column(
        modifier = modifier.clip(MaterialTheme.shapes.large).background(bg).border(2.dp, border, MaterialTheme.shapes.large).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = androidx.compose.material3.ripple(),
            onClick = onClick,
        ).padding(vertical = d.spaceL),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontWeight = FontWeight.Bold, color = if (selected) accent else MaterialTheme.colorScheme.onSurface)
    }
}
