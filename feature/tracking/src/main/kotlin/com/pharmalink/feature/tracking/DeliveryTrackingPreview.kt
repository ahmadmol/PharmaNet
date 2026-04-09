package com.pharmalink.feature.tracking

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pharmalink.domain.model.DeliveryDelegate
import com.pharmalink.domain.model.DeliveryStatus
import com.pharmalink.domain.model.DeliveryTracking

@Preview(showBackground = true)
@Composable
private fun DeliveryTrackingScreenPreview() {
    // Note: This preview would need to be wrapped in proper theme and navigation
    // For now, this is a placeholder showing the preview structure
}

@Preview(showBackground = true)
@Composable
private fun InTransitTrackingPreview() {
    val mockTracking = DeliveryTracking(
        orderId = "ORD-001",
        delegate = DeliveryDelegate(
            name = "أحمد محمد",
            phone = "+966501234567",
            isActive = true
        ),
        startPoint = "مستودع الرياض الرئيسي",
        destinationPoint = "صيدلية النور",
        currentStatus = DeliveryStatus.IN_TRANSIT,
        departureTime = "2:30 م",
        lastUpdate = "10 دقائق مضت",
        orderNumber = "ORD-001",
        estimatedArrival = "25 دقيقة",
        deliveryNotes = "التسليم عند الباب الخلفي",
        
        // Future map fields (null for now)
        startLatitude = 24.7136,
        startLongitude = 46.6753,
        destinationLatitude = 24.6877,
        destinationLongitude = 46.7212,
        driverCurrentLatitude = 24.7000,
        driverCurrentLongitude = 46.6980,
        routePolyline = null,
        lastLocationTimestamp = System.currentTimeMillis()
    )
    
    // Preview would show the tracking screen with this data
}

@Preview(showBackground = true)
@Composable
private fun NoDelegateTrackingPreview() {
    val mockTracking = DeliveryTracking(
        orderId = "ORD-002",
        delegate = null,
        startPoint = "مستودع جدة",
        destinationPoint = "صيدلية الأمل",
        currentStatus = DeliveryStatus.PREPARING,
        departureTime = null,
        lastUpdate = "5 دقائق مضت",
        orderNumber = "ORD-002",
        estimatedArrival = "2-3 ساعات",
        deliveryNotes = null,
        
        // Future map fields (null for now)
        startLatitude = null,
        startLongitude = null,
        destinationLatitude = null,
        destinationLongitude = null,
        driverCurrentLatitude = null,
        driverCurrentLongitude = null,
        routePolyline = null,
        lastLocationTimestamp = null
    )
    
    // Preview would show the tracking screen with no delegate assigned
}

@Preview(showBackground = true)
@Composable
private fun DeliveredTrackingPreview() {
    val mockTracking = DeliveryTracking(
        orderId = "ORD-003",
        delegate = DeliveryDelegate(
            name = "محمد خالد",
            phone = "+966557654321",
            isActive = true
        ),
        startPoint = "مستودع الدمام",
        destinationPoint = "صيدلية الشفاء",
        currentStatus = DeliveryStatus.DELIVERED,
        departureTime = "10:00 ص",
        lastUpdate = "ساعتان مضت",
        orderNumber = "ORD-003",
        estimatedArrival = "تم التسليم",
        deliveryNotes = null,
        
        // Future map fields (null for now)
        startLatitude = 26.4295,
        startLongitude = 50.0878,
        destinationLatitude = 26.4200,
        destinationLongitude = 50.0900,
        driverCurrentLatitude = null,
        driverCurrentLongitude = null,
        routePolyline = null,
        lastLocationTimestamp = null
    )
    
    // Preview would show the tracking screen with delivered status
}
