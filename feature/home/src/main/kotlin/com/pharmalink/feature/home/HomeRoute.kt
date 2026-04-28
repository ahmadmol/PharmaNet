package com.pharmalink.feature.home

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val HOME_ROUTE = "home"

fun NavController.navigateToHome() = navigate(HOME_ROUTE)

fun NavGraphBuilder.homeScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWarehouses: () -> Unit,
    onNavigateToFeaturedWarehouses: () -> Unit,
    onNavigateToCreateRequest: () -> Unit,
    onNavigateToMedicineSearch: () -> Unit,
) {
    composable(route = HOME_ROUTE) {
        val viewModel: HomeViewModel = hiltViewModel()
        HomeScreen(
            viewModel = viewModel,
            onNavigateToHome = onNavigateToHome,
            onNavigateToOrders = onNavigateToOrders,
            onNavigateToNotifications = onNavigateToNotifications,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToWarehouses = onNavigateToWarehouses,
            onNavigateToFeaturedWarehouses = onNavigateToFeaturedWarehouses,
            onNavigateToCreateRequest = onNavigateToCreateRequest,
            onNavigateToMedicineSearch = onNavigateToMedicineSearch,
        )
    }
}
