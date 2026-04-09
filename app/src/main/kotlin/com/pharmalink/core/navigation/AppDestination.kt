package com.pharmalink.core.navigation

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
    data object PharmacyMain : AppDestination("pharmacy_main")
    data object WarehouseMain : AppDestination("warehouse_main")
    data object PublicUserMain : AppDestination("public_main")
    data object Home : AppDestination("home")
    data object Resources : AppDestination("resources")
    data object CreateRequest : AppDestination("create_request")
    data object Orders : AppDestination("orders")
    data object Profile : AppDestination("profile")
    data object Notifications : AppDestination("notifications")
    data object Help : AppDestination("help")
    data object Compliance : AppDestination("compliance")

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
