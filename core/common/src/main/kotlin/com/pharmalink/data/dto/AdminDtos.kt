package com.pharmalink.data.dto

import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AdminUser
import com.pharmalink.domain.model.AuditLog
import com.pharmalink.domain.model.Pharmacy
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class FacilityDto(
    val id: String,
    val name: String,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val location: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("contact_number") val contactNumber: String? = null,
    @SerialName("license_number") val licenseNumber: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String
) {
    private val resolvedAddress: String?
        get() = formattedAddress ?: location

    fun toPharmacy() = Pharmacy(
        id = id,
        name = name,
        location = resolvedAddress,
        contactNumber = contactNumber,
        licenseNumber = licenseNumber,
        isActive = isActive,
        createdAt = createdAt
    )

    fun toWarehouse() = com.pharmalink.domain.model.Warehouse(
        id = id,
        name = name,
        city = resolvedAddress.orEmpty(),
        district = "",
        supportsColdChain = false,
        inStockPercent = 0,
        lowStockCount = 0,
        outOfStockCount = 0,
        estimatedDeliveryLabel = "",
        distanceLabel = "",
        phoneNumber = contactNumber.orEmpty(),
        lastUpdatedLabel = createdAt,
    )
}

@Serializable
data class AdminUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("account_type") val accountType: String,
    @SerialName("pharmacy_id") val pharmacyId: String? = null,
    @SerialName("warehouse_id") val warehouseId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("phone_number") val phoneNumber: String? = null,
    @SerialName("pharmacy_name") val pharmacyName: String? = null,
    @SerialName("warehouse_name") val warehouseName: String? = null,
    @SerialName("created_at") val createdAt: String
) {
    fun toDomain() = AdminUser(
        id = id,
        email = email.orEmpty(),
        accountType = AccountType.valueOf(accountType),
        pharmacyId = pharmacyId,
        warehouseId = warehouseId,
        isActive = isActive,
        fullName = fullName,
        phoneNumber = phoneNumber,
        pharmacyName = pharmacyName,
        warehouseName = warehouseName,
        createdAt = createdAt
    )
}

@Serializable
data class AuditLogDto(
    val id: String,
    @SerialName("admin_id") val adminId: String,
    @SerialName("admin_email") val adminEmail: String? = null,
    val action: String,
    @SerialName("target_user_id") val targetUserId: String? = null,
    @SerialName("target_user_email") val targetUserEmail: String? = null,
    @SerialName("old_value") val oldValue: JsonElement? = null,
    @SerialName("new_value") val newValue: JsonElement? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("user_agent") val userAgent: String? = null,
    @SerialName("transaction_id") val transactionId: String? = null,
) {
    fun toDomain(): AuditLog {
        val normalizedAction = action.uppercase()
        return AuditLog(
            id = id,
            action = action,
            actionLabel = action.toArabicActionLabel(),
            adminId = adminId,
            adminName = resolveAdminDisplayName(adminEmail),
            adminEmail = adminEmail.orEmpty(),
            targetEntityName = resolveTargetEntityName(action, newValue, targetUserEmail),
            targetWarehouseName = newValue?.jsonObject?.get("warehouse_name")?.jsonPrimitive?.contentOrNull
                ?: newValue?.jsonObject?.get("warehouse")?.jsonPrimitive?.contentOrNull,
            targetSku = newValue?.jsonObject?.get("sku")?.jsonPrimitive?.contentOrNull,
            oldValue = oldValue.toPrettyJsonString(),
            newValue = newValue.toPrettyJsonString(),
            ipAddress = ipAddress,
            userAgent = userAgent,
            transactionId = transactionId ?: id.replace("-", "").take(12).uppercase(),
            createdAt = parseAuditInstant(createdAt),
            isSuccess = !normalizedAction.contains("FAIL") &&
                !normalizedAction.contains("ERROR") &&
                !normalizedAction.contains("DELETE"),
        )
    }
}

private val auditLogJsonFormat = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

private fun JsonElement?.toPrettyJsonString(): String {
    val el = this ?: return "{}"
    if (el is JsonNull) return "{}"
    return auditLogJsonFormat.encodeToString(JsonElement.serializer(), el)
}

private fun parseAuditInstant(raw: String): Instant = runCatching {
    Instant.parse(raw)
}.getOrElse { Instant.EPOCH }

private fun resolveAdminDisplayName(email: String?): String {
    val e = email.orEmpty()
    if (e.isBlank()) return "—"
    val local = e.substringBefore("@")
    return local.replace(".", " ").replace("_", " ").trim().ifBlank { e }
}

private fun resolveTargetEntityName(
    action: String,
    newValue: JsonElement?,
    targetUserEmail: String?,
): String {
    val obj = newValue?.jsonObject
    if (obj != null) {
        for (key in listOf("name", "medicine_name", "title", "product_name")) {
            val text = obj[key]?.jsonPrimitive?.contentOrNull
            if (!text.isNullOrBlank()) return text
        }
    }
    if (action == "PROFILE_UPDATE" && !targetUserEmail.isNullOrBlank()) return targetUserEmail
    return "—"
}

private fun String.toArabicActionLabel(): String = when (this) {
    "ROLE_CHANGE" -> "تغيير الصلاحية"
    "PROFILE_UPDATE" -> "تحديث ملف تعريف"
    "STOCK_UPDATE" -> "تعديل بيانات المخزون"
    "USER_CREATED" -> "إنشاء مستخدم"
    "USER_DELETED" -> "حذف مستخدم"
    "PHARMACY_ADDED", "PHARMACY_CREATE" -> "إضافة صيدلية"
    "WAREHOUSE_ADDED", "WAREHOUSE_CREATE" -> "إضافة مستودع"
    else -> this
}

// RPC Parameter DTOs
@Serializable
data class UpdateUserProfileRpcParams(
    @SerialName("p_target_user_id") val targetUserId: String,
    @SerialName("p_full_name") val fullName: String? = null,
    @SerialName("p_account_type") val accountType: String,
    @SerialName("p_pharmacy_id") val pharmacyId: String? = null,
    @SerialName("p_warehouse_id") val warehouseId: String? = null,
    @SerialName("p_is_active") val isActive: Boolean = true
)

@Serializable
data class CreatePharmacyRpcParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_location") val location: String,
    @SerialName("p_contact_number") val contactNumber: String,
    @SerialName("p_license_number") val licenseNumber: String
)

@Serializable
data class CreateWarehouseRpcParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_location") val location: String,
    @SerialName("p_contact_number") val contactNumber: String
)

@Serializable
data class CreatePharmacyWithCoordinatesRpcParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_location") val location: String,
    @SerialName("p_contact_number") val contactNumber: String,
    @SerialName("p_license_number") val licenseNumber: String,
    @SerialName("p_latitude") val latitude: Double,
    @SerialName("p_longitude") val longitude: Double,
)

@Serializable
data class CreateWarehouseWithCoordinatesRpcParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_location") val location: String,
    @SerialName("p_contact_number") val contactNumber: String,
    @SerialName("p_latitude") val latitude: Double,
    @SerialName("p_longitude") val longitude: Double,
)

@Serializable
data class GetAuditLogsRpcParams(
    @SerialName("p_limit") val limit: Int = 100
)
