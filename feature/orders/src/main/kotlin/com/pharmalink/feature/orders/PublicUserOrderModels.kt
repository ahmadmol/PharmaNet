package com.pharmalink.feature.orders

import android.content.Context
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.Medicine
import com.pharmalink.domain.model.Order
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.OrderType
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.PublicPharmacyAvailabilityStatus
import com.pharmalink.domain.model.PublicPharmacyForMedicine
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object PublicUserOrderNavStateKeys {
    const val MEDICINE_NAME = "public_user_order_medicine_name"
    const val MEDICINE_BRAND = "public_user_order_medicine_brand"
    const val MEDICINE_STRENGTH = "public_user_order_medicine_strength"
    const val PHARMACY_NAME = "public_user_order_pharmacy_name"
    const val PHARMACY_LOCATION = "public_user_order_pharmacy_location"
    const val PHARMACY_SUPPORTS_PICKUP = "public_user_order_pharmacy_supports_pickup"
    const val PHARMACY_SUPPORTS_DELIVERY = "public_user_order_pharmacy_supports_delivery"
    const val FULFILLMENT_TYPE = "public_user_order_fulfillment_type"
    const val REQUEST_SCOPE = "public_user_order_request_scope"
    const val CUSTOMER_ORDERS_REFRESH_REQUIRED = "public_user_customer_orders_refresh_required"
}

data class MedicineSummaryUi(
    val id: String = "",
    val name: String = "",
    val brand: String = "",
    val strength: String = "",
)

data class PharmacySummaryUi(
    val id: String = "",
    val name: String = "",
    val locationLabel: String = "",
    val supportsPickup: Boolean = true,
    val supportsDelivery: Boolean = true,
    val isAllPharmaciesRequest: Boolean = false,
)

data class PharmacySelectionItemUi(
    val pharmacyId: String,
    val pharmacyName: String,
    val locationLabel: String,
    val distanceLabel: String?,
    val isOnDuty: Boolean,
    val supportsPickup: Boolean,
    val supportsDelivery: Boolean,
    val availabilityStatus: PublicPharmacyAvailabilityStatus,
    val estimatedTimeLabel: String?,
)

enum class PharmacySelectionFilter {
    NEARBY,
    ON_DUTY,
    ALL,
}

data class CustomerOrderListItemUi(
    val id: String,
    val medicineName: String,
    val pharmacyName: String,
    val quantity: Int,
    val unit: String,
    val status: OrderStatus,
    val statusLabel: String,
    val statusSupportingText: String,
    val fulfillmentType: FulfillmentType,
    val fulfillmentLabel: String,
    val urgencyLabel: String,
    val requestScopeLabel: String,
    val totalPriceLabel: String?,
    val createdAtLabel: String,
)

data class CustomerOrderDetailUi(
    val id: String,
    val medicineName: String,
    val pharmacyName: String,
    val quantity: Int,
    val unit: String,
    val status: OrderStatus,
    val statusLabel: String,
    val statusSupportingText: String,
    val fulfillmentType: FulfillmentType,
    val fulfillmentLabel: String,
    val urgencyLabel: String,
    val requestScopeLabel: String,
    val pharmacyLocation: String?,
    val deliveryAddress: String?,
    val deliveryPhone: String?,
    val notes: String?,
    val totalPriceLabel: String?,
    val createdAtLabel: String,
    val canCancel: Boolean,
)

fun PublicPharmacyForMedicine.toPharmacySelectionItemUi(): PharmacySelectionItemUi =
    PharmacySelectionItemUi(
        pharmacyId = pharmacyId,
        pharmacyName = pharmacyName,
        locationLabel = listOfNotNull(
            location.takeIf { it.isNotBlank() },
            area?.takeIf { it.isNotBlank() },
            district?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() },
        ).distinct().joinToString(" - ").ifBlank { "-" },
        distanceLabel = distanceLabel,
        isOnDuty = isOnDuty,
        supportsPickup = supportsPickup,
        supportsDelivery = supportsDelivery,
        availabilityStatus = availabilityStatus,
        estimatedTimeLabel = estimatedTimeLabel,
    )

fun Medicine.toMedicineSummaryUi(): MedicineSummaryUi = MedicineSummaryUi(
    id = id,
    name = name,
    brand = brand,
    strength = strength,
)

internal fun Order.isVisiblePublicUserOrder(): Boolean {
    if (orderType != OrderType.CUSTOMER_PHARMACY) return false
    return status in publicUserSupportedStatuses
}

internal fun Order.toCustomerOrderListItemUi(context: Context): CustomerOrderListItemUi {
    val pharmacyName = resolvePublicUserPharmacyName(context)
    return CustomerOrderListItemUi(
        id = id,
        medicineName = medicineName,
        pharmacyName = pharmacyName,
        quantity = quantity,
        unit = unit,
        status = status,
        statusLabel = status.toPublicUserStatusLabel(context),
        statusSupportingText = status.toPublicUserSupportingText(context),
        fulfillmentType = fulfillmentType,
        fulfillmentLabel = fulfillmentType.toPublicUserFulfillmentLabel(context),
        urgencyLabel = urgency.toPublicUserUrgencyLabel(context),
        requestScopeLabel = requestScope.toPublicUserRequestScopeLabel(context),
        totalPriceLabel = totalPriceCents?.toCurrencyLabel(currency),
        createdAtLabel = createdAt.toPublicUserDateLabel(),
    )
}

internal fun Order.toCustomerOrderDetailUi(context: Context): CustomerOrderDetailUi {
    val pharmacyName = resolvePublicUserPharmacyName(context)
    return CustomerOrderDetailUi(
        id = id,
        medicineName = medicineName,
        pharmacyName = pharmacyName,
        quantity = quantity,
        unit = unit,
        status = status,
        statusLabel = status.toPublicUserStatusLabel(context),
        statusSupportingText = status.toPublicUserSupportingText(context),
        fulfillmentType = fulfillmentType,
        fulfillmentLabel = fulfillmentType.toPublicUserFulfillmentLabel(context),
        urgencyLabel = urgency.toPublicUserUrgencyLabel(context),
        requestScopeLabel = requestScope.toPublicUserRequestScopeLabel(context),
        pharmacyLocation = pharmacyLocation?.takeIf { it.isNotBlank() },
        deliveryAddress = deliveryAddress?.takeIf { fulfillmentType == FulfillmentType.DELIVERY && it.isNotBlank() },
        deliveryPhone = deliveryPhone?.takeIf { fulfillmentType == FulfillmentType.DELIVERY && it.isNotBlank() },
        notes = notes?.takeIf { it.isNotBlank() },
        totalPriceLabel = totalPriceCents?.toCurrencyLabel(currency),
        createdAtLabel = createdAt.toPublicUserDateLabel(),
        canCancel = status == OrderStatus.PENDING,
    )
}

private val publicUserSupportedStatuses = setOf(
    OrderStatus.PENDING,
    OrderStatus.CONFIRMED,
    OrderStatus.IN_PROGRESS,
    OrderStatus.REJECTED,
    OrderStatus.READY_FOR_PICKUP,
    OrderStatus.OUT_FOR_DELIVERY,
    OrderStatus.DELIVERED,
    OrderStatus.CANCELLED,
)

private fun Order.resolvePublicUserPharmacyName(context: Context): String {
    return pharmacyName
        ?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.customer_order_pharmacy_fallback)
}

private fun CustomerRequestUrgency.toPublicUserUrgencyLabel(context: Context): String = when (this) {
    CustomerRequestUrgency.URGENT -> context.getString(R.string.customer_order_urgency_urgent)
    CustomerRequestUrgency.NORMAL -> context.getString(R.string.customer_order_urgency_normal)
}

private fun CustomerRequestScope.toPublicUserRequestScopeLabel(context: Context): String = when (this) {
    CustomerRequestScope.SPECIFIC_PHARMACY -> context.getString(R.string.customer_order_scope_specific)
    CustomerRequestScope.ALL_PHARMACIES -> context.getString(R.string.customer_order_scope_all)
}

private fun OrderStatus.toPublicUserStatusLabel(context: Context): String = when (this) {
    OrderStatus.PENDING -> context.getString(R.string.customer_order_status_pending)
    OrderStatus.QUOTE_PENDING -> context.getString(R.string.customer_order_status_pending)
    OrderStatus.CONFIRMED -> context.getString(R.string.customer_order_status_confirmed)
    OrderStatus.IN_PROGRESS -> context.getString(R.string.customer_order_status_in_progress)
    OrderStatus.REJECTED -> context.getString(R.string.customer_order_status_rejected)
    OrderStatus.READY_FOR_PICKUP -> context.getString(R.string.customer_order_status_ready_for_pickup)
    OrderStatus.OUT_FOR_DELIVERY -> context.getString(R.string.customer_order_status_out_for_delivery)
    OrderStatus.DELIVERED -> context.getString(R.string.customer_order_status_delivered)
    OrderStatus.CANCELLED -> context.getString(R.string.customer_order_status_cancelled)
}

private fun OrderStatus.toPublicUserSupportingText(context: Context): String = when (this) {
    OrderStatus.PENDING -> context.getString(R.string.customer_order_status_pending_supporting)
    OrderStatus.QUOTE_PENDING -> context.getString(R.string.customer_order_status_pending_supporting)
    OrderStatus.CONFIRMED -> context.getString(R.string.customer_order_status_confirmed_supporting)
    OrderStatus.IN_PROGRESS -> context.getString(R.string.customer_order_status_in_progress_supporting)
    OrderStatus.REJECTED -> context.getString(R.string.customer_order_status_rejected_supporting)
    OrderStatus.READY_FOR_PICKUP -> context.getString(R.string.customer_order_status_ready_for_pickup_supporting)
    OrderStatus.OUT_FOR_DELIVERY -> context.getString(R.string.customer_order_status_out_for_delivery_supporting)
    OrderStatus.DELIVERED -> context.getString(R.string.customer_order_status_delivered_supporting)
    OrderStatus.CANCELLED -> context.getString(R.string.customer_order_status_cancelled_supporting)
}

private fun FulfillmentType.toPublicUserFulfillmentLabel(context: Context): String = when (this) {
    FulfillmentType.PICKUP -> context.getString(R.string.customer_order_pickup_option)
    FulfillmentType.DELIVERY -> context.getString(R.string.customer_order_delivery_option)
}

private fun Instant.toPublicUserDateLabel(): String {
    val formatter = DateTimeFormatter
        .ofPattern("yyyy/MM/dd", Locale.forLanguageTag("ar"))
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

private fun Long.toCurrencyLabel(currencyCode: String): String {
    val amount = this / 100.0
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ar")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currencyCode ${formatter.format(amount)}"
}
