package com.pharmalink.feature.main.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pharmalink.R
import com.pharmalink.core.navigation.AppDestination
import com.pharmalink.core.navigation.matchesDestination
import com.pharmalink.core.navigation.navigateToInnerTopLevel
import com.pharmalink.core.navigation.topLevelDestinationFor
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.feature.auth.AuthViewModel
import com.pharmalink.feature.compliance.presentation.ComplianceScreen
import com.pharmalink.feature.help.presentation.AboutAppScreen
import com.pharmalink.feature.help.presentation.ContactUsScreen
import com.pharmalink.feature.home.HomeScreen
import com.pharmalink.feature.home.MedicineSearchScreen
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
import com.pharmalink.feature.warehouses.FeaturedWarehousesScreen
import com.pharmalink.feature.warehouses.WarehousesScreen

private val bottomBarRoutes = setOf(
    AppDestination.Home.route,
    AppDestination.Resources.route,
    AppDestination.CreateRequest.route,
    AppDestination.RequestList.route,
    AppDestination.Orders.route,
    AppDestination.Notifications.route,
    AppDestination.Profile.route,
)

@Composable
fun PharmaNavigator(
    onProfileLogout: () -> Unit,
    startDestination: String = AppDestination.Home.route,
    modifier: Modifier = Modifier,
    accountType: AccountType?,   // ← مهم جداً
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val authViewModel: AuthViewModel = hiltViewModel()
    val userSnapshot by authViewModel.userSnapshot.collectAsState()
    val accountType = userSnapshot?.accountType

    val currentTab = when {
        currentRoute.matchesDestination(AppDestination.Home) -> AppDestination.Home
        currentRoute.matchesDestination(AppDestination.Resources) -> AppDestination.Resources
        currentRoute.matchesDestination(AppDestination.CreateRequest) -> AppDestination.CreateRequest
        currentRoute.matchesDestination(AppDestination.RequestList) -> AppDestination.RequestList
        currentRoute.matchesDestination(AppDestination.Orders) -> AppDestination.Orders
        currentRoute.matchesDestination(AppDestination.Notifications) -> AppDestination.Notifications
        currentRoute.matchesDestination(AppDestination.Profile) -> AppDestination.Profile
        else -> AppDestination.Home
    }

    val visibleTabs = when (accountType) {
        AccountType.WAREHOUSE ->
            listOf(AppDestination.Home, AppDestination.Resources, AppDestination.RequestList, AppDestination.Profile)
        AccountType.ADMIN ->
            listOf(AppDestination.Home, AppDestination.Profile)
        AccountType.PUBLIC_USER ->
            listOf(
                AppDestination.Home,
                AppDestination.Profile,
            )
        AccountType.PHARMACY ->
            listOf(
                AppDestination.Home,
                AppDestination.Resources,
                AppDestination.CreateRequest,
                AppDestination.RequestList,
                AppDestination.Profile,
            )
        null -> listOf(AppDestination.Home, AppDestination.Profile)
    }

    val bottomBarTabs = visibleTabs
        .filterNot { tab -> accountType == AccountType.PHARMACY && tab == AppDestination.CreateRequest }
        .mapNotNull { destination -> topLevelDestinationFor(destination.route) }

    val bottomBarTabRoutes = bottomBarTabs.map { it.route }.toSet()
    val safeSelectedTab = if (currentTab.route !in bottomBarTabRoutes) AppDestination.Home else currentTab

    val tabSelectedIndex = bottomBarTabs
        .indexOfFirst { destination -> destination.route == safeSelectedTab.route }
        .coerceAtLeast(0)

    val visibleTabRoutes = visibleTabs.map { it.route }.toSet()
    val isBottomBarVisible = currentRoute in visibleTabRoutes

    BackHandler(enabled = true) {
        when {
            currentRoute.matchesDestination(AppDestination.WarehouseDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Notifications) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.Help) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AboutApp) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.FeaturedWarehouses) -> navController.popBackStack()
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
//        floatingActionButtonPosition = FabPosition.Center,
//        floatingActionButton = {
//            if (isBottomBarVisible && accountType != AccountType.WAREHOUSE && accountType != AccountType.ADMIN) {
//                FloatingActionButton(
//                    onClick = { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) },
//                    modifier = Modifier
//                        .size(56.dp)
//                        .scale(fabScale),
//                    shape = CircleShape,
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    contentColor = MaterialTheme.colorScheme.onPrimary,
//                    elevation = FloatingActionButtonDefaults.elevation(
//                        defaultElevation = 4.dp,
//                        pressedElevation = 6.dp,
//                    ),
//                ) {
//                    Icon(
//                        imageVector = Icons.Rounded.Add,
//                        contentDescription = stringResource(R.string.add_medicine),
//                        modifier = Modifier.size(24.dp),
//                    )
//                }
//            }
//        },
        bottomBar = {
            if (isBottomBarVisible) {
                Box {
                    PharmaBottomNavigation(
                        items = bottomBarTabs,
                        selectedItem = tabSelectedIndex,
                        onTabSelected = { route, _ ->
                            when (route) {
                                AppDestination.Home.route -> navController.navigateToInnerTopLevel(AppDestination.Home)
                                AppDestination.Resources.route -> navController.navigateToInnerTopLevel(AppDestination.Resources)
                                AppDestination.RequestList.route -> navController.navigateToInnerTopLevel(AppDestination.RequestList)
                                AppDestination.Orders.route -> navController.navigateToInnerTopLevel(AppDestination.Orders)
                                AppDestination.Notifications.route -> navController.navigateToInnerTopLevel(AppDestination.Notifications)
                                AppDestination.Profile.route -> navController.navigateToInnerTopLevel(AppDestination.Profile)
                            }
                        },
                    )

                    if (accountType == AccountType.PHARMACY) {
                        FloatingActionButton(
                            onClick = { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (+5).dp)
                                .size(56.dp)
                                .scale(fabScale)
                                .zIndex(1f),
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
                }
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
                startDestination = startDestination,
            ) {
                homeScreen(
                    onNavigateToHome = { navController.navigateToInnerTopLevel(AppDestination.Home) },
                    onNavigateToOrders = { navController.navigateToInnerTopLevel(AppDestination.Orders) },
                    onNavigateToNotifications = {
                        navController.navigate(AppDestination.Notifications.route) { launchSingleTop = true }
                    },
                    onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                    onNavigateToWarehouses = { navController.navigateToInnerTopLevel(AppDestination.Resources) },
                    onNavigateToFeaturedWarehouses = { navController.navigate(AppDestination.FeaturedWarehouses.route) },
                    onNavigateToCreateRequest = {
                        if (accountType == AccountType.PHARMACY) {
                            navController.navigateToInnerTopLevel(AppDestination.CreateRequest)
                        }
                    },
                    onNavigateToMedicineSearch = {
                        navController.navigate(AppDestination.MedicineSearch.route) { launchSingleTop = true }
                    },
                )
                composable(AppDestination.MedicineSearch.route) {
                    MedicineSearchScreen(
                        viewModel = hiltViewModel(),
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToWarehouseDetail = { medicineId ->
                            // Navigate to warehouse detail for now (showing where medicine is available)
                            // In future, this could navigate to a list of warehouses with this medicine
                            navController.navigate(AppDestination.Resources.route)
                        },
                    )
                }
                composable(AppDestination.Resources.route) {
                    WarehousesScreen(
                        viewModel = hiltViewModel(),
                        accountType = accountType,
                        onWarehouseClick = { warehouseId ->
                            navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                        },
                        onViewIncomingRequests = { navController.navigateToInnerTopLevel(AppDestination.RequestList) },
                    )
                }
                composable(AppDestination.FeaturedWarehouses.route) {
                    FeaturedWarehousesScreen(
                        onBack = { navController.popBackStack() },
                        onWarehouseClick = { warehouseId ->
                            navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                        },
                    )
                }
                composable(AppDestination.CreateRequest.route) {
                    if (accountType == AccountType.PHARMACY) {
                        CreateRequestScreen(
                            viewModel = hiltViewModel(),
                            onNavigateToRequestList = { navController.navigate(AppDestination.RequestList.route) },
                            onNavigateToRequestDetails = { requestId ->
                                navController.navigate(AppDestination.RequestDetail.createRoute(requestId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
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
                        onNavigateToCreateRequest = {
                            if (accountType == AccountType.PHARMACY) {
                                navController.navigateToInnerTopLevel(AppDestination.CreateRequest)
                            }
                        },
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
                        onPrimaryCtaClick = { navController.navigate(AppDestination.ContactUs.route) },
                        onSecondaryCtaClick = { navController.navigate(AppDestination.Help.route) },
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
                        onCreateRequest = if (accountType == AccountType.PHARMACY) {
                            { navController.navigateToInnerTopLevel(AppDestination.CreateRequest) }
                        } else {
                            null
                        },
                        viewModel = hiltViewModel(),
                    )
                }
            }
        }
    }
}
