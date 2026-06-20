package com.pharmalink.feature.auth.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.pharmalink.feature.auth.R
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AccountType

/**
 * Soft ambient blobs (Stitch "Clinical Sanctuary" depth) — RTL-friendly; no manual RTL.
 */
@Composable
fun StitchAmbientBackground(modifier: Modifier = Modifier) {
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val secondaryTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(220.dp)
                .offset(x = (-40).dp, y = (-40).dp)
                .background(primaryTint, CircleShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(180.dp)
                .offset(x = 40.dp, y = 40.dp)
                .background(secondaryTint, CircleShape),
        )
    }
}

/**
 * National digits only for display/editing; merges to +963… for ViewModel.
 */
internal fun nationalDigitsFromStoredPhone(full: String): String {
    val d = full.filter { it.isDigit() }
    return when {
        d.startsWith("963") && d.length > 3 -> d.drop(3)
        d.length == 10 && d[0] == '0' && d[1] == '9' -> d.drop(1)
        else -> d
    }
}

private fun formatNational(digits: String): String {
    val d = digits.filter { it.isDigit() }
    return buildString {
        d.forEachIndexed { index, c ->
            append(c)
            if ((index == 2 || index == 5) && index < d.length - 1) {
                append(" ")
            }
        }
    }
}

internal fun mergeSyrianE164(nationalDigits: String): String {
    val d = nationalDigits.filter { it.isDigit() }.take(9)
    return if (d.isEmpty()) "" else "+963$d"
}

@Composable
fun StitchPhoneRow(
    fullPhoneNumber: String,
    onFullPhoneNumberChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    val national = nationalDigitsFromStoredPhone(fullPhoneNumber)
    val containerShape = RoundedCornerShape(d.radiusM)
    val surfaceHigh = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    Column(modifier) {
        Text(
            text = stringResource(R.string.auth_phone_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = d.spaceXS),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(d.spaceS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = containerShape,
                color = surfaceHigh,
                modifier = Modifier.height(56.dp),
            ) {
                Box(
                    Modifier.padding(horizontal = d.spaceM),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+963",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Surface(
                shape = containerShape,
                color = surfaceHigh,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = d.spaceM),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Smartphone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(d.spaceS))
                    BasicTextField(
                        value = formatNational(national),
                        onValueChange = { typed ->
                            val clean = typed.filter { it.isDigit() }.take(9)
                            val next = mergeSyrianE164(clean)
                            onFullPhoneNumberChange(next)
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = d.spaceS),
                        decorationBox = { inner ->
                            Box {
                                if (national.isEmpty()) {
                                    Text(
                                        "9XX XXX XXX",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = d.spaceXS),
            )
        }
    }
}

@Composable
fun StitchPasswordRow(
    password: String,
    onPasswordChange: (String) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    var visible by remember { mutableStateOf(false) }
    val containerShape = RoundedCornerShape(d.radiusM)
    val surfaceHigh = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    Column(modifier) {
        Text(
            text = stringResource(R.string.auth_password_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = d.spaceXS),
        )
        Surface(
            shape = containerShape,
            color = surfaceHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .padding(horizontal = d.spaceM)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(d.spaceS))
                BasicTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = d.spaceS),
                )
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = if (visible) {
                            stringResource(R.string.auth_password_hide)
                        } else {
                            stringResource(R.string.auth_password_show)
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, start = d.spaceXS),
            )
        }
    }
}

@Composable
fun StitchGradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    showTrailingIcon: Boolean = true,
) {
    val d = MaterialTheme.dimens
    val scale by animateFloatAsState(
        targetValue = if (isLoading) 0.96f else 1f,
        label = "ctaScale",
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
        ),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(brush)
            .clickable(
                enabled = enabled && !isLoading,
                onClick = onClick,
            )
            .padding(horizontal = d.spaceXL),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
                } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                if (showTrailingIcon) {
                    Spacer(Modifier.width(d.spaceM))
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier,
) {
    if (password.isEmpty()) return
    val d = MaterialTheme.dimens
    
    val strength = remember(password) {
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        score
    }
    
    val (label, color) = when {
        strength <= 1 -> "ضعيفة" to Color(0xFFEF4444)
        strength <= 2 -> "متوسطة" to Color(0xFFF59E0B)
        else -> "قوية" to Color(0xFF10B981)
    }
    
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "قوة كلمة المرور",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                val segmentColor = if (index < strength) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(segmentColor)
                )
            }
        }
    }
}

@Composable
fun TermsAndConditionsRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Row(
        modifier = modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d.spaceS)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.size(24.dp)
        )
        
        val text = buildAnnotatedString {
            append("أوافق على ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append("سياسة الخصوصية")
            }
            append(" و ")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append("شروط الاستخدام")
            }
            append(" الخاصة بـ ")
            append(stringResource(id = DsR.string.app_name))
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StitchOrDivider(modifier: Modifier = Modifier) {
    val d = MaterialTheme.dimens
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        )
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = d.spaceM),
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        )
    }
}

@Composable
fun StitchStepChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = d.spaceM, vertical = d.spaceXS),
        )
    }
}

@Composable
fun StitchAccountTypeSelector(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = MaterialTheme.dimens
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(d.spaceM),
    ) {
        StitchAccountTypeCard(
            title = stringResource(R.string.auth_account_type_pharmacy),
            icon = Icons.Outlined.LocalPharmacy,
            selected = selected == AccountType.PHARMACY,
            onClick = { onSelect(AccountType.PHARMACY) },
        )
        StitchAccountTypeCard(
            title = stringResource(R.string.auth_account_type_warehouse),
            icon = Icons.Outlined.Inventory,
            selected = selected == AccountType.WAREHOUSE,
            onClick = { onSelect(AccountType.WAREHOUSE) },
        )
        StitchAccountTypeCard(
            title = stringResource(R.string.auth_account_type_public_user),
            icon = Icons.Outlined.Person,
            selected = selected == AccountType.PUBLIC_USER,
            onClick = { onSelect(AccountType.PUBLIC_USER) },
        )
        StitchAccountTypeCard(
            title = stringResource(R.string.auth_account_type_admin),
            icon = Icons.Outlined.VerifiedUser,
            selected = selected == AccountType.ADMIN,
            onClick = { onSelect(AccountType.ADMIN) },
        )
    }
}

@Composable
private fun StitchAccountTypeCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val d = MaterialTheme.dimens
    val shape = RoundedCornerShape(d.radiusL)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }
    
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = d.spaceL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(d.spaceM),
        ) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                )
            }
        }
    }
}
