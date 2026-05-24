package com.pharmalink.core.navigation

import android.net.Uri
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

sealed class AppDestination(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
) {
    // Auth Destinations
    data object Splash : AppDestination("splash")

    data object Login : AppDestination("login")

    data object SignUp : AppDestination("signup")

    // Main App Destinations
    data object MainTabs : AppDestination("main_tabs")
    data object Home : AppDestination("home")
    data object MedicineSearch : AppDestination("medicine_search")
    data object PharmacySelection : AppDestination(
        route = "pharmacy_selection/{medicineId}",
        arguments = listOf(navArgument(NavArgs.MEDICINE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(medicineId: String): String = "pharmacy_selection/$medicineId"
    }
    data object CreateCustomerOrder : AppDestination(
        route = "create_customer_order/{medicineId}/{pharmacyId}",
        arguments = listOf(
            navArgument(NavArgs.MEDICINE_ID) { type = NavType.StringType },
            navArgument(NavArgs.PHARMACY_ID) { type = NavType.StringType },
        ),
    ) {
        fun createRoute(medicineId: String, pharmacyId: String): String =
            "create_customer_order/$medicineId/$pharmacyId"

        fun createAllPharmaciesRoute(medicineId: String): String =
            "create_customer_order/$medicineId/all_pharmacies"
    }
    data object CustomerOrderSuccess : AppDestination(
        route = "customer_order_success/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "customer_order_success/$orderId"
    }
    data object MyCustomerOrders : AppDestination("my_customer_orders")
    data object CustomerOrderDetail : AppDestination(
        route = "customer_order_detail/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "customer_order_detail/$orderId"
    }
    data object PublicPharmacies : AppDestination("public_pharmacies")
    data object PharmacyDashboard : AppDestination("pharmacy_dashboard")
    data object PharmacyRadar : AppDestination("pharmacy_radar")
    data object PharmacyCustomerOrders : AppDestination("pharmacy_customer_orders")
    data object PharmacyWarehouseProducts : AppDestination(
        route = "pharmacy_warehouse_products/{warehouseId}",
        arguments = listOf(navArgument(NavArgs.WAREHOUSE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(warehouseId: String): String = "pharmacy_warehouse_products/$warehouseId"
    }
    data object PharmacyCustomerOrderDetail : AppDestination(
        route = "pharmacy_customer_order/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "pharmacy_customer_order/$orderId"
    }
    data object WarehouseDashboard : AppDestination("warehouse_dashboard")
    data object Resources : AppDestination("resources")
    data object FeaturedWarehouses : AppDestination("featured_warehouses")
    data object CreateRequest : AppDestination("create_request")
    data object CreateRequestPrefilled : AppDestination(
        route = "create_request_prefilled?warehouseId={warehouseId}&medicineId={medicineId}&medicineName={medicineName}&medicineSubtitle={medicineSubtitle}&unit={unit}",
        arguments = listOf(
            navArgument(NavArgs.WAREHOUSE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument(NavArgs.MEDICINE_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("medicineName") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("medicineSubtitle") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
            navArgument("unit") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        fun createRoute(): String = "create_request"

        fun createPrefilledRoute(
            warehouseId: String,
            medicineId: String,
            medicineName: String,
            medicineSubtitle: String,
            unit: String,
        ): String = buildString {
            append("create_request_prefilled")
            append("?warehouseId=${Uri.encode(warehouseId)}")
            append("&medicineId=${Uri.encode(medicineId)}")
            append("&medicineName=${Uri.encode(medicineName)}")
            append("&medicineSubtitle=${Uri.encode(medicineSubtitle)}")
            append("&unit=${Uri.encode(unit)}")
        }
    }
    data object Orders : AppDestination("orders")
    data object Profile : AppDestination("profile")
    data object EditProfile : AppDestination("edit_profile")
    data object ChangePassword : AppDestination("change_password")
    data object RequestList : AppDestination("request_list")
    data object ForgotPassword : AppDestination("forgot_password")

    // Stitch App Destinations
    data object StitchHome : AppDestination("stitch_home")
    data object StitchCreateOrder : AppDestination("stitch_create_order")
    data object StitchMyOrders : AppDestination("stitch_my_orders")
    data object StitchProfile : AppDestination("stitch_profile")
    data object StitchWarehouses : AppDestination("stitch_warehouses")
    data object Notifications : AppDestination("notifications")
    data object Help : AppDestination("help")
    data object AboutApp : AppDestination("about_app")
    data object ContactUs : AppDestination("contact_us")
    data object Compliance : AppDestination("compliance")
    data object Language : AppDestination("language")
    data object SecurityPrivacy : AppDestination("security_privacy")

    data object AdminAuditLog : AppDestination("admin_audit_log")

    data object AdminOrders : AppDestination("admin_orders")

    data object AdminOrderDetail : AppDestination(
        route = "admin_order_detail/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "admin_order_detail/$orderId"
    }

    data object AdminCreateFacility : AppDestination("admin_create_facility")

    data object AdminUsers : AppDestination("admin_users")

    data object AdminWarehouses : AppDestination("admin_warehouses")

    data object AdminPharmacies : AppDestination("admin_pharmacies")

    data object AdminUserDetail : AppDestination(
        route = "admin_user_detail/{userId}",
        arguments = listOf(navArgument(NavArgs.USER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(userId: String): String = "admin_user_detail/$userId"
    }

    data object AdminWarehouseDetail : AppDestination(
        route = "admin_warehouse_detail/{warehouseId}",
        arguments = listOf(navArgument(NavArgs.WAREHOUSE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(warehouseId: String): String = "admin_warehouse_detail/$warehouseId"
    }

    data object AdminPharmacyDetail : AppDestination(
        route = "admin_pharmacy_detail/{pharmacyId}",
        arguments = listOf(navArgument(NavArgs.PHARMACY_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(pharmacyId: String): String = "admin_pharmacy_detail/$pharmacyId"
    }

    data object AdminAuditLogDetail : AppDestination(
        route = "admin_audit_log_detail/{logId}",
        arguments = listOf(navArgument(NavArgs.LOG_ID) { type = NavType.StringType }),
    ) {
        fun route(logId: String): String = "admin_audit_log_detail/$logId"
    }

    data object AdminDashboard : AppDestination("admin_dashboard")

    data object WarehouseInventory : AppDestination(
        route = "warehouse_inventory/{warehouseId}",
        arguments = listOf(navArgument(NavArgs.WAREHOUSE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(warehouseId: String): String = "warehouse_inventory/$warehouseId"
    }

    data object AddMedicine : AppDestination(
        route = "add_medicine/{warehouseId}",
        arguments = listOf(navArgument(NavArgs.WAREHOUSE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(warehouseId: String): String = "add_medicine/$warehouseId"
    }

    data object WarehouseDetail : AppDestination(
        route = "warehouse/{warehouseId}",
        arguments = listOf(navArgument(NavArgs.WAREHOUSE_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(warehouseId: String): String = "warehouse/$warehouseId"
    }

    data object RequestDetail : AppDestination(
        route = "request/{requestId}",
        arguments = listOf(navArgument(NavArgs.REQUEST_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(requestId: String): String = "request/$requestId"
    }

    data object OrderDetail : AppDestination(
        route = "order/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "order/$orderId"
    }

    data object DeliveryTracking : AppDestination(
        route = "delivery_tracking/{orderId}",
        arguments = listOf(navArgument(NavArgs.ORDER_ID) { type = NavType.StringType }),
    ) {
        fun createRoute(orderId: String): String = "delivery_tracking/$orderId"
    }
}
