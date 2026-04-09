package com.pharmalink.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
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
    TopLevelDestination(AppDestination.Resources.route, AppDestination.Resources.route, Icons.Outlined.GridView, R.string.resources),
    TopLevelDestination(AppDestination.Orders.route, AppDestination.Orders.route, Icons.Outlined.ShoppingCart, R.string.my_orders),
    TopLevelDestination(AppDestination.Profile.route, AppDestination.Profile.route, Icons.Outlined.Person, R.string.account),
)
