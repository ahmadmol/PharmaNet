package com.pharmalink.feature.main.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pharmalink.R
import com.pharmalink.core.navigation.AppDestination
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.navigation.matchesDestination
import com.pharmalink.core.navigation.navigateToTopLevel
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.designsystem.theme.PharmaGradients
import com.pharmalink.designsystem.theme.StitchShellTokens
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.domain.model.AppNotification
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.feature.compliance.presentation.ComplianceScreen
import com.pharmalink.feature.help.presentation.HelpScreen
import com.pharmalink.feature.home.HomeScreen
import com.pharmalink.feature.notifications.NotificationsScreen
import com.pharmalink.feature.orders.presentation.OrderDetailScreen
import com.pharmalink.feature.orders.presentation.OrdersScreen
import com.pharmalink.feature.profile.presentation.ProfileScreen
import com.pharmalink.feature.request.CreateRequestScreen
import com.pharmalink.feature.request.RequestDetailsScreen
import com.pharmalink.feature.resources.presentation.ResourcesScreen
import com.pharmalink.feature.resources.presentation.WarehouseDetailScreen
import com.pharmalink.feature.tracking.DeliveryTrackingScreen

private val bottomBarRoutes = setOf(
    AppDestination.Home.route,
    AppDestination.Resources.route,
    AppDestination.CreateRequest.route,
    AppDestination.Orders.route,
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
        currentRoute.matchesDestination(AppDestination.Orders) -> 2
        currentRoute.matchesDestination(AppDestination.Profile) -> 3
        else -> -1
    }

    val isBottomBarVisible = currentRoute in bottomBarRoutes

    BackHandler(enabled = true) {
        when {
            currentRoute.matchesDestination(AppDestination.WarehouseDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Notifications) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Help) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Compliance) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.RequestDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.OrderDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.DeliveryTracking) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Home) -> Unit
            currentRoute in bottomBarRoutes -> {
                if (!currentRoute.matchesDestination(AppDestination.Home)) {
                    navController.navigateToTopLevel(AppDestination.Home.route)
                }
            }
            else -> Unit
        }
    }

    val dimens = MaterialTheme.dimens
    val fabScale by animateFloatAsState(
        targetValue = if (currentRoute.matchesDestination(AppDestination.CreateRequest)) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "fab_scale",
    )

    Scaffold(
        modifier = modifier,
        containerColor = ClinicalCanvas,
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (isBottomBarVisible) {
                FloatingActionButton(
                    onClick = { navController.navigateToTopLevel(AppDestination.CreateRequest.route) },
                    modifier = Modifier
                        .size(dimens.fabSize)
                        .offset(y = 32.dp)
                        .scale(fabScale)
                        .border(3.dp, StitchShellTokens.fabRing, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = dimens.fabElevation,
                        pressedElevation = 4.dp,
                    ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PharmaGradients.fabOrange, RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add_medicine),
                            tint = Color.White,
                            modifier = Modifier.size(dimens.fabIconSize),
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isBottomBarVisible) {
                PharmaBottomNavigation(
                    selectedItem = tabSelectedIndex,
                    onTabSelected = { route, _ -> navController.navigateToTopLevel(route) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.Home.route,
            ) {
                composable(AppDestination.Home.route) {
                    HomeScreen(
                        onCreateRequest = { navController.navigateToTopLevel(AppDestination.CreateRequest.route) },
                        onSearchMedicine = { navController.navigateToTopLevel(AppDestination.Resources.route) },
                        onOpenWarehouses = { navController.navigateToTopLevel(AppDestination.Resources.route) },
                        onEmergencyRequest = { navController.navigateToTopLevel(AppDestination.CreateRequest.route) },
                        onOpenOrders = { navController.navigateToTopLevel(AppDestination.Orders.route) },
                        onOpenNotifications = { navController.navigate(AppDestination.Notifications.route) },
                        onOpenRequest = { requestId ->
                            navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                        },
                    )
                }
                composable(AppDestination.Resources.route) {
                    ResourcesScreen(
                        onWarehouseClick = { warehouseId ->
                            navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                        },
                    )
                }
                composable(AppDestination.CreateRequest.route) {
                    CreateRequestScreen(
                        onSubmitted = { requestId ->
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
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        onLogout = onProfileLogout,
                        onOpenNotifications = { navController.navigate(AppDestination.Notifications.route) },
                        onOpenCompliance = { navController.navigate(AppDestination.Compliance.route) },
                        onOpenHelp = { navController.navigate(AppDestination.Help.route) },
                    )
                }
                composable(
                    route = AppDestination.WarehouseDetail.route,
                    arguments = AppDestination.WarehouseDetail.arguments,
                ) { backStack ->
                    WarehouseDetailScreen(
                        warehouseId = backStack.arguments?.getString(NavArgs.WAREHOUSE_ID).orEmpty(),
                        onBack = { navController.popBackStack() },
                        onCreateRequest = { navController.navigateToTopLevel(AppDestination.CreateRequest.route) },
                    )
                }
                composable(
                    route = AppDestination.RequestDetail.route,
                    arguments = AppDestination.RequestDetail.arguments,
                ) {
                    RequestDetailsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenOrder = { orderId ->
                            navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                        },
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
                composable(AppDestination.Notifications.route) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onNotificationOpen = { notification ->
                            navigateFromNotification(navController, notification)
                        },
                    )
                }
                composable(AppDestination.Help.route) {
                    HelpScreen(
                        onBack = { navController.popBackStack() },
                        onOpenCompliance = { navController.navigate(AppDestination.Compliance.route) },
                    )
                }
                composable(AppDestination.Compliance.route) {
                    ComplianceScreen(
                        onBack = { navController.popBackStack() },
                        onOpenHelp = { navController.navigate(AppDestination.Help.route) },
                    )
                }
            }
        }
    }
}

private fun navigateFromNotification(
    navController: androidx.navigation.NavHostController,
    notification: AppNotification,
) {
    when (notification.destination) {
        NotificationDestination.ORDER -> {
            notification.destinationId?.let { navController.navigate(AppDestination.OrderDetail.createRoute(it)) }
        }
        NotificationDestination.REQUEST -> {
            notification.destinationId?.let { navController.navigate(AppDestination.RequestDetail.createRoute(it)) }
        }
        NotificationDestination.WAREHOUSE -> {
            notification.destinationId?.let { navController.navigate(AppDestination.WarehouseDetail.createRoute(it)) }
        }
        NotificationDestination.COMPLIANCE -> navController.navigate(AppDestination.Compliance.route)
        NotificationDestination.HELP -> navController.navigate(AppDestination.Help.route)
        null -> Unit
    }
}
