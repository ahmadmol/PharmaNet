package com.pharmalink.feature.main.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pharmalink.R
import com.pharmalink.core.navigation.AppDestination
import com.pharmalink.core.navigation.matchesDestination
import com.pharmalink.core.navigation.navigateToInnerTopLevel
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.feature.compliance.presentation.ComplianceScreen
import com.pharmalink.feature.help.presentation.AboutAppScreen
import com.pharmalink.feature.help.presentation.ContactUsScreen
import com.pharmalink.feature.help.presentation.HelpScreen
import com.pharmalink.feature.home.homeScreen
import com.pharmalink.feature.notifications.NotificationsScreen
import com.pharmalink.feature.orders.OrdersScreen
import com.pharmalink.feature.orders.presentation.OrderDetailScreen
import com.pharmalink.feature.profile.ChangePasswordScreen
import com.pharmalink.feature.profile.EditProfileScreen
import com.pharmalink.feature.profile.ProfileScreen
import com.pharmalink.feature.request.CreateRequestScreen
import com.pharmalink.feature.request.RequestDetailsScreen
import com.pharmalink.feature.request.RequestListScreen
import com.pharmalink.feature.resources.presentation.WarehouseDetailScreen
import com.pharmalink.feature.tracking.DeliveryTrackingScreen
import com.pharmalink.feature.warehouses.WarehousesScreen

private val bottomBarRoutes = setOf(
    AppDestination.Home.route,
    AppDestination.Resources.route,
    AppDestination.CreateRequest.route,
    AppDestination.RequestList.route,
    AppDestination.Profile.route,
)

@Composable
fun PharmaNavigator(
    onProfileLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabSelectedIndex = when {
        currentRoute.matchesDestination(AppDestination.Home) -> 0
        currentRoute.matchesDestination(AppDestination.Resources) -> 1
        currentRoute.matchesDestination(AppDestination.RequestList) -> 2
        currentRoute.matchesDestination(AppDestination.Profile) -> 3
        else -> -1
    }

    val isBottomBarVisible = currentRoute in bottomBarRoutes

    BackHandler(enabled = true) {
        when {
            currentRoute.matchesDestination(AppDestination.WarehouseDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Notifications) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Help) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AboutApp) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.ContactUs) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Compliance) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.EditProfile) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.ChangePassword) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.RequestDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.OrderDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.DeliveryTracking) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Orders) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Home) -> Unit
            currentRoute in bottomBarRoutes -> {
                navController.navigateToInnerTopLevel(AppDestination.Home)
            }
            else -> Unit
        }
    }

    val fabScale by animateFloatAsState(
        targetValue = if (currentRoute.matchesDestination(AppDestination.CreateRequest)) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_scale",
    )
    val layoutDirection = LocalLayoutDirection.current
    val removeTopInsetForCurrentRoute = currentRoute.matchesDestination(AppDestination.Resources)

    Scaffold(
        modifier = modifier,
        containerColor = ClinicalCanvas,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (isBottomBarVisible) {
                FloatingActionButton(
                    onClick = { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) },
                    modifier = Modifier
                        .size(56.dp)
                        .scale(fabScale),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 6.dp,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_medicine),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                PharmaBottomNavigation(
                    selectedItem = tabSelectedIndex,
                    onTabSelected = { route, _ ->
                        when (route) {
                            AppDestination.Home.route -> navController.navigateToInnerTopLevel(AppDestination.Home)
                            AppDestination.Resources.route -> navController.navigateToInnerTopLevel(AppDestination.Resources)
                            AppDestination.RequestList.route -> navController.navigateToInnerTopLevel(AppDestination.RequestList)
                            AppDestination.Profile.route -> navController.navigateToInnerTopLevel(AppDestination.Profile)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = if (removeTopInsetForCurrentRoute) 0.dp else innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding(),
                ),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
            ) {
                homeScreen(
                    onNavigateToHome = { navController.navigateToInnerTopLevel(AppDestination.Home) },
                    onNavigateToOrders = { navController.navigateToInnerTopLevel(AppDestination.Orders) },
                    onNavigateToNotifications = {
                        navController.navigate(AppDestination.Notifications.route) { launchSingleTop = true }
                    },
                    onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                    onNavigateToWarehouses = { navController.navigateToInnerTopLevel(AppDestination.Resources) },
                    onNavigateToCreateRequest = { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) },
                )
                composable(AppDestination.Resources.route) {
                    WarehousesScreen(
                        viewModel = hiltViewModel(),
                        onWarehouseClick = { warehouseId ->
                            navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                        },
                    )
                }
                composable(AppDestination.CreateRequest.route) {
                    CreateRequestScreen(
                        viewModel = hiltViewModel(),
                        onNavigateToRequestList = { navController.navigate(AppDestination.RequestList.route) },
                        onNavigateToRequestDetails = { requestId ->
                            navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                        },
                    )
                }
                composable(AppDestination.Orders.route) {
                    OrdersScreen(
                        onOpenOrder = { orderId ->
                            navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                        },
                    )
                }
                composable(AppDestination.RequestList.route) {
                    RequestListScreen(
                        onNavigateToCreateRequest = { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) },
                        onNavigateToRequestDetails = { requestId ->
                            navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                        },
                    )
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        onLogout = onProfileLogout,
                        onEditProfile = { navController.navigate(AppDestination.EditProfile.route) },
                        onChangePassword = { navController.navigate(AppDestination.ChangePassword.route) },
                        onOpenHelpSupport = { navController.navigate(AppDestination.Help.route) },
                        onOpenAboutApp = { navController.navigate(AppDestination.AboutApp.route) },
                        onOpenContactUs = { navController.navigate(AppDestination.ContactUs.route) },
                    )
                }
                composable(AppDestination.Notifications.route) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onNotificationOpen = { notification ->
                            when (notification.destination) {
                                NotificationDestination.ORDER ->
                                    notification.destinationId?.let { orderId ->
                                        navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                                    }

                                NotificationDestination.REQUEST ->
                                    notification.destinationId?.let { requestId ->
                                        navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                                    }

                                NotificationDestination.WAREHOUSE ->
                                    notification.destinationId?.let { warehouseId ->
                                        navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                                    }

                                NotificationDestination.COMPLIANCE ->
                                    navController.navigate(AppDestination.Compliance.route)

                                NotificationDestination.HELP ->
                                    navController.navigate(AppDestination.Help.route)

                                null -> Unit
                            }
                        },
                    )
                }
                composable(AppDestination.Help.route) {
                    HelpScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCompliance = { navController.navigate(AppDestination.Compliance.route) },
                        onOpenContactUs = { navController.navigate(AppDestination.ContactUs.route) },
                    )
                }
                composable(AppDestination.AboutApp.route) {
                    AboutAppScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.ContactUs.route) {
                    ContactUsScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.Compliance.route) {
                    ComplianceScreen(
                        onBack = { navController.popBackStack() },
                        onOpenHelp = { navController.navigate(AppDestination.Help.route) },
                    )
                }
                composable(AppDestination.EditProfile.route) {
                    EditProfileScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.ChangePassword.route) {
                    ChangePasswordScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(AppDestination.RequestDetail.route) {
                    RequestDetailsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenOrder = { orderId ->
                            navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                        },
                        viewModel = hiltViewModel(),
                    )
                }
                composable(
                    route = AppDestination.OrderDetail.route,
                    arguments = AppDestination.OrderDetail.arguments,
                ) {
                    OrderDetailScreen(
                        onBack = { navController.popBackStack() },
                        onOpenRequest = { requestId ->
                            navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                        },
                        onTrackDelivery = { orderId ->
                            navController.navigate(AppDestination.DeliveryTracking.createRoute(orderId))
                        },
                        viewModel = hiltViewModel(),
                    )
                }
                composable(
                    route = AppDestination.DeliveryTracking.route,
                    arguments = AppDestination.DeliveryTracking.arguments,
                ) {
                    DeliveryTrackingScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = AppDestination.WarehouseDetail.route,
                    arguments = AppDestination.WarehouseDetail.arguments,
                ) { entry ->
                    val warehouseId = entry.arguments?.getString("warehouseId").orEmpty()
                    WarehouseDetailScreen(
                        warehouseId = warehouseId,
                        onBack = { navController.popBackStack() },
                        onCreateRequest = {
                            navController.navigateToInnerTopLevel(AppDestination.CreateRequest)
                        },
                        viewModel = hiltViewModel(),
                    )
                }
            }
        }
    }
}
