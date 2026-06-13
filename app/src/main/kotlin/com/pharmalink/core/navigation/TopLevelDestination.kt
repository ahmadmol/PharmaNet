package com.pharmalink.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.pharmalink.R

data class TopLevelDestination(
    val route: String,
    val selectedBaseRoute: String,
    val icon: ImageVector,
    val labelRes: Int,
)

val topLevelDestinations = listOf(
    TopLevelDestination(AppDestination.Home.route, AppDestination.Home.route, Icons.Outlined.Home, R.string.home_screen),
    TopLevelDestination(AppDestination.PharmacyDashboard.route, AppDestination.PharmacyDashboard.route, Icons.Outlined.LocalPharmacy, R.string.pharmacy_dashboard_tab),
    TopLevelDestination(AppDestination.PharmacyRadar.route, AppDestination.PharmacyRadar.route, Icons.Outlined.LocationOn, R.string.pharmacy_radar_tab),
    TopLevelDestination(AppDestination.WarehouseDashboard.route, AppDestination.WarehouseDashboard.route, Icons.Outlined.Inventory2, R.string.warehouse_dashboard_tab),
    TopLevelDestination(AppDestination.Resources.route, AppDestination.Resources.route, Icons.Outlined.GridView, R.string.resources),
    TopLevelDestination(AppDestination.PharmacyCustomerOrders.route, AppDestination.PharmacyCustomerOrders.route, Icons.AutoMirrored.Outlined.ReceiptLong, R.string.pharmacy_customer_orders_tab),
    TopLevelDestination(AppDestination.RequestList.route, AppDestination.RequestList.route, Icons.Outlined.ShoppingCart, R.string.pharmacy_warehouse_requests_tab),
    TopLevelDestination(AppDestination.Profile.route, AppDestination.Profile.route, Icons.Outlined.Person, R.string.account),
    TopLevelDestination(AppDestination.Orders.route, AppDestination.Orders.route, Icons.Outlined.ShoppingCart, R.string.orders_screen_title),
    TopLevelDestination(
        AppDestination.Notifications.route,
        AppDestination.Notifications.route,
        Icons.Outlined.NotificationsNone,
        R.string.profile_settings_notifications,
    ),
    TopLevelDestination(
        AppDestination.AdminAuditLog.route,
        AppDestination.AdminAuditLog.route,
        Icons.Outlined.History,
        R.string.admin_audit_log_tab,
    ),
    TopLevelDestination(
        AppDestination.AdminDashboard.route,
        AppDestination.AdminDashboard.route,
        Icons.Outlined.Home,
        R.string.admin_dashboard_tab,
    ),
    TopLevelDestination(
        AppDestination.MedicineSearch.route,
        AppDestination.MedicineSearch.route,
        Icons.Outlined.Search,
        R.string.search,
    ),
    TopLevelDestination(
        AppDestination.PublicPharmacies.route,
        AppDestination.PublicPharmacies.route,
        Icons.Outlined.LocalPharmacy,
        R.string.pharmacies,
    ),
    TopLevelDestination(
        AppDestination.MyCustomerOrders.route,
        AppDestination.MyCustomerOrders.route,
        Icons.AutoMirrored.Outlined.ReceiptLong,
        R.string.my_orders,
    ),
)

fun topLevelDestinationFor(route: String): TopLevelDestination? =
    topLevelDestinations.firstOrNull { it.route == route }
