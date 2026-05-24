package com.pharmalink.feature.admin.ui.audit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.data.repository.PharmaRepository
import com.pharmalink.domain.model.AuditLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull

@HiltViewModel
class AuditLogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val pharmaRepository: PharmaRepository,
) : ViewModel() {

    private val logId: String = savedStateHandle[NavArgs.LOG_ID] ?: ""

    private val _uiState = MutableStateFlow<AuditLogDetailUiState>(AuditLogDetailUiState.Loading)
    val uiState: StateFlow<AuditLogDetailUiState> = _uiState.asStateFlow()

    init {
        if (logId.isBlank()) {
            _uiState.value = AuditLogDetailUiState.Error("معرف السجل غير صالح")
        } else {
            loadLogDetail()
        }
    }

    fun onAction(action: AuditLogDetailAction) {
        when (action) {
            AuditLogDetailAction.OnRetryClicked -> loadLogDetail()
        }
    }

    private fun loadLogDetail() {
        if (logId.isBlank()) {
            _uiState.value = AuditLogDetailUiState.Error("معرف السجل غير صالح")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuditLogDetailUiState.Loading
            pharmaRepository.getAuditLogById(logId)
                .onSuccess { log ->
                    _uiState.value = AuditLogDetailUiState.Success(log.toUiModel())
                }
                .onFailure {
                    _uiState.value = AuditLogDetailUiState.Error(AUDIT_DETAIL_ERROR_MESSAGE)
                }
        }
    }

    private fun AuditLog.toUiModel(): AuditLogDetailModel {
        val formatter = DateTimeFormatter.ofPattern(
            "d MMMM yyyy - hh:mm a",
            Locale.forLanguageTag("ar"),
        )
        val formattedDateTime = createdAt.atZone(ZoneId.systemDefault()).format(formatter)
        return AuditLogDetailModel(
            actionLabel = actionLabel,
            isSuccess = isSuccess,
            adminName = adminName,
            formattedDateTime = formattedDateTime,
            targetEntityName = targetEntityName,
            targetWarehouseName = targetWarehouseName,
            targetSku = targetSku,
            changedFields = buildChangedFields(oldValue, newValue),
        )
    }

    private fun buildChangedFields(oldRaw: String, newRaw: String): List<AuditChangedFieldModel> {
        val oldValues = flattenJson(oldRaw)
        val newValues = flattenJson(newRaw)
        val keys = (oldValues.keys + newValues.keys).sorted()

        return keys.mapNotNull { key ->
            if (isHiddenAuditPath(key)) return@mapNotNull null
            val oldValue = oldValues[key].toDisplayValue()
            val newValue = newValues[key].toDisplayValue()
            if (!oldValue.isMeaningfulAuditValue() && !newValue.isMeaningfulAuditValue()) return@mapNotNull null
            if (oldValue == newValue) return@mapNotNull null
            if (oldValue.isPrivateAuditValue() || newValue.isPrivateAuditValue()) return@mapNotNull null

            AuditChangedFieldModel(
                label = key.toArabicAuditFieldLabel(),
                oldValue = oldValue.ifBlank { AUDIT_EMPTY_VALUE_LABEL },
                newValue = newValue.ifBlank { AUDIT_EMPTY_VALUE_LABEL },
            )
        }
    }

    private fun flattenJson(raw: String): Map<String, String?> {
        val root = runCatching { Json.parseToJsonElement(raw) }.getOrNull() ?: return emptyMap()
        val values = linkedMapOf<String, String?>()
        collectJsonLeaves(root, "", values)
        return values
    }

    private fun collectJsonLeaves(
        element: JsonElement,
        path: String,
        values: MutableMap<String, String?>,
    ) {
        when (element) {
            is JsonObject -> {
                element.forEach { (key, value) ->
                    val nextPath = if (path.isBlank()) key else "$path.$key"
                    collectJsonLeaves(value, nextPath, values)
                }
            }
            is kotlinx.serialization.json.JsonArray -> {
                element.forEachIndexed { index, value ->
                    val nextPath = if (path.isBlank()) index.toString() else "$path.$index"
                    collectJsonLeaves(value, nextPath, values)
                }
            }
            JsonNull -> {
                if (path.isNotBlank()) values[path] = null
            }
            is JsonPrimitive -> {
                if (path.isNotBlank()) values[path] = element.toCleanString()
            }
        }
    }

    private fun JsonPrimitive.toCleanString(): String {
        booleanOrNull?.let { return if (it) AUDIT_BOOLEAN_TRUE_LABEL else AUDIT_BOOLEAN_FALSE_LABEL }
        doubleOrNull?.let {
            val asLong = it.toLong()
            return if (it == asLong.toDouble()) asLong.toString() else it.toString()
        }
        return contentOrNull.orEmpty().trim()
    }

    private fun String?.toDisplayValue(): String = this?.trim().orEmpty()

    private fun String.isMeaningfulAuditValue(): Boolean {
        val normalized = trim()
        return normalized.isNotBlank() &&
            !normalized.equals("null", ignoreCase = true) &&
            normalized != "{}" &&
            normalized != "[]"
    }

    private fun String.isPrivateAuditValue(): Boolean {
        val normalized = trim()
        return UUID_PATTERN.matches(normalized) ||
            TRANSACTION_PATTERN.matches(normalized) ||
            IP_ADDRESS_PATTERN.matches(normalized)
    }

    private fun isHiddenAuditPath(path: String): Boolean {
        val normalized = path.lowercase(Locale.ROOT)
        val compact = normalized.replace("_", "").replace("-", "")
        val segments = normalized.split('.', '_', '-')
        return normalized == "id" ||
            normalized.endsWith("_id") ||
            normalized.endsWith(".id") ||
            "uuid" in segments ||
            "ip" in segments ||
            "useragent" in compact ||
            "transaction" in segments ||
            "token" in segments ||
            "session" in segments ||
            "password" in segments
    }

    private fun String.toArabicAuditFieldLabel(): String {
        val key = substringAfterLast('.')
        return AUDIT_FIELD_LABELS[key.lowercase(Locale.ROOT)] ?: key
            .replace("_", " ")
            .trim()
            .ifBlank { AUDIT_UNKNOWN_FIELD_LABEL }
    }

    private companion object {
        private const val AUDIT_DETAIL_ERROR_MESSAGE = "تعذر تحميل تفاصيل السجل. حاول مرة أخرى."
        private const val AUDIT_EMPTY_VALUE_LABEL = "غير محدد"
        private const val AUDIT_UNKNOWN_FIELD_LABEL = "حقل"
        private const val AUDIT_BOOLEAN_TRUE_LABEL = "نعم"
        private const val AUDIT_BOOLEAN_FALSE_LABEL = "لا"

        private val UUID_PATTERN =
            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        private val TRANSACTION_PATTERN =
            Regex("^(TRX|TX|txn|transaction)[-_]?[A-Za-z0-9-]{6,}$", RegexOption.IGNORE_CASE)
        private val IP_ADDRESS_PATTERN =
            Regex("^((25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.|$)){4}$")

        private val AUDIT_FIELD_LABELS = mapOf(
            "name" to "الاسم",
            "full_name" to "الاسم الكامل",
            "email" to "البريد الإلكتروني",
            "phone" to "رقم الهاتف",
            "phone_number" to "رقم الهاتف",
            "contact_number" to "رقم التواصل",
            "location" to "الموقع",
            "formatted_address" to "العنوان",
            "address" to "العنوان",
            "city" to "المدينة",
            "district" to "الحي",
            "license_number" to "رقم الترخيص",
            "account_type" to "نوع الحساب",
            "role" to "الدور",
            "is_active" to "الحالة",
            "status" to "الحالة",
            "quantity" to "الكمية",
            "current_quantity" to "الكمية الحالية",
            "capacity" to "السعة",
            "price" to "السعر",
            "sku" to "رمز المنتج",
            "medicine_name" to "اسم الدواء",
            "product_name" to "اسم المنتج",
            "warehouse_name" to "اسم المستودع",
            "pharmacy_name" to "اسم الصيدلية",
            "supports_cold_chain" to "يدعم التبريد",
            "in_stock_percent" to "نسبة التوفر",
            "low_stock_count" to "عدد المخزون المنخفض",
            "out_of_stock_count" to "عدد النفاد",
        )
    }
}
