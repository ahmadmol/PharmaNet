package com.pharmalink.feature.orders

import com.pharmalink.domain.model.CustomerRequestUrgency
import com.pharmalink.domain.model.FulfillmentType
import com.pharmalink.domain.model.OrderStatus
import com.pharmalink.domain.model.PharmacyCustomerOrder
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class PharmacyCustomerOrderFilter {
    ALL,
    NEW,
    CONFIRMED,
    READY,
    DELIVERY,
    COMPLETED,
    CLOSED,
}

data class PharmacyCustomerOrderUi(
    val id: String,
    val customerName: String,
    val medicineName: String,
    val quantityLabel: String,
    val status: OrderStatus,
    val statusLabel: String,
    val fulfillmentType: FulfillmentType,
    val fulfillmentLabel: String,
    val urgencyLabel: String,
    val createdAtLabel: String,
    val priceLabel: String,
    val deliveryAddress: String?,
    val deliveryPhone: String?,
    val prescriptionUrl: String?,
    val notes: String?,
)

internal fun PharmacyCustomerOrder.matches(filter: PharmacyCustomerOrderFilter): Boolean =
    when (filter) {
        PharmacyCustomerOrderFilter.ALL -> true
        PharmacyCustomerOrderFilter.NEW -> status == OrderStatus.PENDING
        PharmacyCustomerOrderFilter.CONFIRMED -> status == OrderStatus.CONFIRMED
        PharmacyCustomerOrderFilter.READY -> status == OrderStatus.READY_FOR_PICKUP
        PharmacyCustomerOrderFilter.DELIVERY -> status == OrderStatus.OUT_FOR_DELIVERY
        PharmacyCustomerOrderFilter.COMPLETED -> status == OrderStatus.DELIVERED
        PharmacyCustomerOrderFilter.CLOSED -> status == OrderStatus.REJECTED || status == OrderStatus.CANCELLED
    }

internal fun PharmacyCustomerOrder.toPharmacyCustomerOrderUi(): PharmacyCustomerOrderUi =
    PharmacyCustomerOrderUi(
        id = id,
        customerName = customerName?.takeIf { it.isNotBlank() } ?: "عميل",
        medicineName = medicineName,
        quantityLabel = "$quantity $unit",
        status = status,
        statusLabel = status.toPharmacyCustomerOrderStatusLabel(),
        fulfillmentType = fulfillmentType,
        fulfillmentLabel = fulfillmentType.toPharmacyCustomerFulfillmentLabel(),
        urgencyLabel = urgency.toPharmacyCustomerUrgencyLabel(),
        createdAtLabel = createdAt.toPharmacyCustomerDateLabel(),
        priceLabel = totalPriceCents?.toPharmacyCustomerCurrencyLabel(currency) ?: "بانتظار التسعير",
        deliveryAddress = deliveryAddress?.takeIf { fulfillmentType == FulfillmentType.DELIVERY && it.isNotBlank() },
        deliveryPhone = deliveryPhone?.takeIf { fulfillmentType == FulfillmentType.DELIVERY && it.isNotBlank() },
        prescriptionUrl = prescriptionUrl,
        notes = notes?.takeIf { it.isNotBlank() },
    )

internal fun PharmacyCustomerOrderFilter.label(): String = when (this) {
    PharmacyCustomerOrderFilter.ALL -> "الكل"
    PharmacyCustomerOrderFilter.NEW -> "جديد"
    PharmacyCustomerOrderFilter.CONFIRMED -> "مؤكد"
    PharmacyCustomerOrderFilter.READY -> "جاهز"
    PharmacyCustomerOrderFilter.DELIVERY -> "توصيل"
    PharmacyCustomerOrderFilter.COMPLETED -> "مكتمل"
    PharmacyCustomerOrderFilter.CLOSED -> "مرفوض/ملغي"
}

internal fun OrderStatus.toPharmacyCustomerOrderStatusLabel(): String = when (this) {
    OrderStatus.PENDING -> "جديد"
    OrderStatus.QUOTE_PENDING -> "بانتظار الموافقة"
    OrderStatus.CONFIRMED -> "مؤكد"
    OrderStatus.READY_FOR_PICKUP -> "جاهز للاستلام"
    OrderStatus.OUT_FOR_DELIVERY -> "خرج للتوصيل"
    OrderStatus.DELIVERED -> "تم التسليم"
    OrderStatus.REJECTED -> "مرفوض"
    OrderStatus.CANCELLED -> "ملغي"
    OrderStatus.IN_PROGRESS -> "قيد التجهيز"
}

internal fun FulfillmentType.toPharmacyCustomerFulfillmentLabel(): String = when (this) {
    FulfillmentType.PICKUP -> "استلام من الصيدلية"
    FulfillmentType.DELIVERY -> "توصيل"
}

private fun CustomerRequestUrgency.toPharmacyCustomerUrgencyLabel(): String = when (this) {
    CustomerRequestUrgency.URGENT -> "عاجل"
    CustomerRequestUrgency.NORMAL -> "عادي"
}

private fun Instant.toPharmacyCustomerDateLabel(): String {
    val formatter = DateTimeFormatter
        .ofPattern("yyyy/MM/dd", Locale.forLanguageTag("ar"))
        .withZone(ZoneId.systemDefault())
    return formatter.format(this)
}

private fun Long.toPharmacyCustomerCurrencyLabel(currencyCode: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("ar")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currencyCode ${formatter.format(this / 100.0)}"
}
