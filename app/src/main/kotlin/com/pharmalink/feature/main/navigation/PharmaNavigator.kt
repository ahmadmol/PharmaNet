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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pharmalink.R
import com.pharmalink.core.navigation.AppDestination
import com.pharmalink.core.navigation.matchesDestination
import com.pharmalink.core.navigation.navigateToInnerTopLevel
import com.pharmalink.core.navigation.topLevelDestinationFor
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.feature.admin.ui.audit.AdminAuditLogScreen
import com.pharmalink.feature.admin.ui.audit.AuditLogDetailScreen
import com.pharmalink.feature.admin.ui.dashboard.AdminDashboardScreen
import com.pharmalink.feature.admin.ui.inventory.WarehouseInventoryScreen
import com.pharmalink.feature.admin.ui.users.AdminUsersScreen
import com.pharmalink.feature.admin.ui.warehouses.AdminWarehousesScreen
import com.pharmalink.feature.admin.ui.pharmacies.AdminPharmaciesScreen
import com.pharmalink.feature.auth.AuthViewModel
import com.pharmalink.feature.compliance.presentation.ComplianceScreen
import com.pharmalink.feature.help.presentation.AboutAppScreen
import com.pharmalink.feature.help.presentation.ContactUsScreen
import com.pharmalink.feature.home.HomeScreen
import com.pharmalink.feature.home.MedicineSearchScreen
import com.pharmalink.feature.help.presentation.HelpScreen
import com.pharmalink.feature.home.homeScreen
import com.pharmalink.feature.notifications.NotificationsScreen
import com.pharmalink.feature.orders.CreateCustomerOrderScreen
import com.pharmalink.feature.orders.CustomerOrderDetailScreen
import com.pharmalink.feature.orders.CustomerOrderSuccessScreen
import com.pharmalink.feature.orders.OrdersScreen
import com.pharmalink.feature.orders.MedicineSummaryUi
import com.pharmalink.feature.orders.MyCustomerOrdersScreen
import com.pharmalink.feature.orders.PharmacySelectionScreen
import com.pharmalink.feature.orders.PharmacySummaryUi
import com.pharmalink.feature.orders.PublicUserOrderNavStateKeys
import com.pharmalink.domain.model.CustomerRequestScope
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
        currentRoute.matchesDestination(AppDestination.AdminDashboard) -> AppDestination.AdminDashboard
        currentRoute.matchesDestination(AppDestination.Home) -> AppDestination.Home
        currentRoute.matchesDestination(AppDestination.Resources) -> AppDestination.Resources
        currentRoute.matchesDestination(AppDestination.CreateRequest) -> AppDestination.CreateRequest
        currentRoute.matchesDestination(AppDestination.RequestList) -> AppDestination.RequestList
        currentRoute.matchesDestination(AppDestination.Orders) -> AppDestination.Orders
        currentRoute.matchesDestination(AppDestination.Notifications) -> AppDestination.Notifications
        currentRoute.matchesDestination(AppDestination.Profile) -> AppDestination.Profile
        currentRoute.matchesDestination(AppDestination.AdminAuditLog) -> AppDestination.AdminAuditLog
        else -> AppDestination.Home
    }

    val visibleTabs = when (accountType) {
        AccountType.WAREHOUSE ->
            listOf(AppDestination.Home, AppDestination.Resources, AppDestination.RequestList, AppDestination.Profile)
        AccountType.ADMIN ->
            listOf(AppDestination.AdminDashboard, AppDestination.AdminAuditLog, AppDestination.Profile)
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
    val defaultTab = if (accountType == AccountType.ADMIN) AppDestination.AdminDashboard else AppDestination.Home
    val safeSelectedTab = if (currentTab.route !in bottomBarTabRoutes) defaultTab else currentTab

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
            currentRoute.matchesDestination(AppDestination.PharmacySelection) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CreateCustomerOrder) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CustomerOrderSuccess) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.MyCustomerOrders) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CustomerOrderDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminAuditLogDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminCreateFacility) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminUsers) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminWarehouses) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminPharmacies) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminUserDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminWarehouseDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminPharmacyDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminDashboard) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.WarehouseInventory) -> navController.popBackStack()
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
                                AppDestination.AdminDashboard.route -> navController.navigateToInnerTopLevel(AppDestination.AdminDashboard)
                                AppDestination.Home.route -> navController.navigateToInnerTopLevel(AppDestination.Home)
                                AppDestination.Resources.route -> navController.navigateToInnerTopLevel(AppDestination.Resources)
                                AppDestination.RequestList.route -> navController.navigateToInnerTopLevel(AppDestination.RequestList)
                                AppDestination.Orders.route -> navController.navigateToInnerTopLevel(AppDestination.Orders)
                                AppDestination.Notifications.route -> navController.navigateToInnerTopLevel(AppDestination.Notifications)
                                AppDestination.Profile.route -> navController.navigateToInnerTopLevel(AppDestination.Profile)
                                AppDestination.AdminAuditLog.route -> navController.navigateToInnerTopLevel(AppDestination.AdminAuditLog)
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
                    onNavigateToOrders = {
                        if (accountType == AccountType.PUBLIC_USER) {
                            navController.navigate(AppDestination.MyCustomerOrders.route) {
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigateToInnerTopLevel(AppDestination.Orders)
                        }
                    },
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
                        onBackClick = { navController.popBackStack() },
                        onMedicineSelected = { medicine ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.MEDICINE_NAME,
                                medicine.name,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.MEDICINE_BRAND,
                                medicine.brand,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.MEDICINE_STRENGTH,
                                medicine.strength,
                            )
                            navController.navigate(AppDestination.PharmacySelection.createRoute(medicine.id))
                        },
                    )
                }
                composable(
                    route = AppDestination.PharmacySelection.route,
                    arguments = AppDestination.PharmacySelection.arguments,
                ) { entry ->
                    val medicineId = entry.arguments?.getString("medicineId").orEmpty()
                    val previousState = navController.previousBackStackEntry?.savedStateHandle
                    val medicineName = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_NAME).orEmpty()
                    val medicineBrand = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_BRAND).orEmpty()
                    val medicineStrength = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_STRENGTH).orEmpty()

                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_NAME] = medicineName
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_BRAND] = medicineBrand
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_STRENGTH] = medicineStrength

                    PharmacySelectionScreen(
                        medicineId = medicineId,
                        medicineName = medicineName,
                        medicineBrand = medicineBrand,
                        medicineStrength = medicineStrength,
                        onBackClick = { navController.popBackStack() },
                        onRetryClick = {},
                        onSearchAllPharmacies = {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_NAME,
                                "",
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_LOCATION,
                                "",
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_PICKUP,
                                true,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_DELIVERY,
                                true,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.REQUEST_SCOPE,
                                CustomerRequestScope.ALL_PHARMACIES.name,
                            )
                            navController.navigate(
                                AppDestination.CreateCustomerOrder.createAllPharmaciesRoute(medicineId),
                            )
                        },
                        onSelectPharmacy = { pharmacy ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_NAME,
                                pharmacy.name,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_LOCATION,
                                pharmacy.locationLabel,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_PICKUP,
                                pharmacy.supportsPickup,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_DELIVERY,
                                pharmacy.supportsDelivery,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.REQUEST_SCOPE,
                                CustomerRequestScope.SPECIFIC_PHARMACY.name,
                            )
                            navController.navigate(
                                AppDestination.CreateCustomerOrder.createRoute(medicineId, pharmacy.id),
                            )
                        },
                    )
                }
                composable(
                    route = AppDestination.CreateCustomerOrder.route,
                    arguments = AppDestination.CreateCustomerOrder.arguments,
                ) { entry ->
                    val medicineId = entry.arguments?.getString("medicineId").orEmpty()
                    val pharmacyId = entry.arguments?.getString("pharmacyId").orEmpty()
                    val previousState = navController.previousBackStackEntry?.savedStateHandle
                    val medicineName = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_NAME).orEmpty()
                    val medicineBrand = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_BRAND).orEmpty()
                    val medicineStrength = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_STRENGTH).orEmpty()
                    val pharmacyName = previousState?.get<String>(PublicUserOrderNavStateKeys.PHARMACY_NAME).orEmpty()
                    val pharmacyLocation = previousState?.get<String>(PublicUserOrderNavStateKeys.PHARMACY_LOCATION).orEmpty()
                    val supportsPickup = previousState?.get<Boolean>(PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_PICKUP) ?: true
                    val supportsDelivery = previousState?.get<Boolean>(PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_DELIVERY) ?: false
                    val requestScope = previousState?.get<String>(PublicUserOrderNavStateKeys.REQUEST_SCOPE)
                        ?.let { runCatching { CustomerRequestScope.valueOf(it) }.getOrNull() }
                        ?: CustomerRequestScope.SPECIFIC_PHARMACY

                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_NAME] = medicineName
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_BRAND] = medicineBrand
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.MEDICINE_STRENGTH] = medicineStrength
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.PHARMACY_NAME] = pharmacyName
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.PHARMACY_LOCATION] = pharmacyLocation
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_PICKUP] = supportsPickup
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.PHARMACY_SUPPORTS_DELIVERY] = supportsDelivery
                    entry.savedStateHandle[PublicUserOrderNavStateKeys.REQUEST_SCOPE] = requestScope.name

                    CreateCustomerOrderScreen(
                        medicine = MedicineSummaryUi(
                            id = medicineId,
                            name = medicineName,
                            brand = medicineBrand,
                            strength = medicineStrength,
                        ),
                        pharmacy = PharmacySummaryUi(
                            id = pharmacyId.takeUnless { it == "all_pharmacies" }.orEmpty(),
                            name = pharmacyName,
                            locationLabel = pharmacyLocation,
                            supportsPickup = supportsPickup,
                            supportsDelivery = supportsDelivery,
                            isAllPharmaciesRequest = requestScope == CustomerRequestScope.ALL_PHARMACIES,
                        ),
                        onBackClick = { navController.popBackStack() },
                        onOrderCreated = { orderId, fulfillmentType ->
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.MEDICINE_NAME,
                                medicineName,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.PHARMACY_NAME,
                                pharmacyName,
                            )
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.FULFILLMENT_TYPE,
                                fulfillmentType.name,
                            )
                            navController.navigate(
                                AppDestination.CustomerOrderSuccess.createRoute(orderId),
                            ) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(
                    route = AppDestination.CustomerOrderSuccess.route,
                    arguments = AppDestination.CustomerOrderSuccess.arguments,
                ) {
                    val previousState = navController.previousBackStackEntry?.savedStateHandle
                    val medicineName = previousState?.get<String>(PublicUserOrderNavStateKeys.MEDICINE_NAME).orEmpty()
                    val pharmacyName = previousState?.get<String>(PublicUserOrderNavStateKeys.PHARMACY_NAME).orEmpty()
                    val fulfillmentTypeName = previousState?.get<String>(PublicUserOrderNavStateKeys.FULFILLMENT_TYPE).orEmpty()
                    val fulfillmentType = runCatching {
                        com.pharmalink.domain.model.FulfillmentType.valueOf(fulfillmentTypeName)
                    }.getOrElse { com.pharmalink.domain.model.FulfillmentType.PICKUP }

                    CustomerOrderSuccessScreen(
                        medicineName = medicineName,
                        pharmacyName = pharmacyName,
                        fulfillmentType = fulfillmentType,
                        showPrimaryAction = true,
                        onGoToMyOrdersClick = {
                            navController.navigate(AppDestination.MyCustomerOrders.route) {
                                launchSingleTop = true
                            }
                        },
                        onBackToSearchClick = {
                            navController.navigate(AppDestination.MedicineSearch.route) {
                                launchSingleTop = true
                            }
                        },
                        onCloseClick = {
                            navController.navigateToInnerTopLevel(AppDestination.Home)
                        },
                    )
                }
                composable(AppDestination.MyCustomerOrders.route) {
                    val refreshRequested = it.savedStateHandle.getStateFlow(
                        PublicUserOrderNavStateKeys.CUSTOMER_ORDERS_REFRESH_REQUIRED,
                        false,
                    ).collectAsState().value

                    MyCustomerOrdersScreen(
                        onBackClick = { navController.popBackStack() },
                        onStartSearchClick = {
                            navController.navigate(AppDestination.MedicineSearch.route) {
                                launchSingleTop = true
                            }
                        },
                        onOpenOrderDetail = { orderId ->
                            navController.navigate(AppDestination.CustomerOrderDetail.createRoute(orderId))
                        },
                        refreshRequested = refreshRequested,
                        onRefreshHandled = {
                            it.savedStateHandle[PublicUserOrderNavStateKeys.CUSTOMER_ORDERS_REFRESH_REQUIRED] = false
                        },
                    )
                }
                composable(
                    route = AppDestination.CustomerOrderDetail.route,
                    arguments = AppDestination.CustomerOrderDetail.arguments,
                ) {
                    CustomerOrderDetailScreen(
                        onBackClick = { navController.popBackStack() },
                        onOrderCancelled = {
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                PublicUserOrderNavStateKeys.CUSTOMER_ORDERS_REFRESH_REQUIRED,
                                true,
                            )
                            navController.popBackStack()
                        },
                    )
                }
                composable(AppDestination.Resources.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.Home) }
                    } else {
                        WarehousesScreen(
                            viewModel = hiltViewModel(),
                            accountType = accountType,
                            onWarehouseClick = { warehouseId ->
                                navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                            },
                            onViewIncomingRequests = { navController.navigateToInnerTopLevel(AppDestination.RequestList) },
                        )
                    }
                }
                composable(AppDestination.FeaturedWarehouses.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        FeaturedWarehousesScreen(
                            onBack = { navController.popBackStack() },
                            onWarehouseClick = { warehouseId ->
                                navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                            },
                        )
                    }
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
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) {
                            navController.navigate(AppDestination.MyCustomerOrders.route) {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        OrdersScreen(
                            onOpenOrder = { orderId ->
                                navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                            },
                        )
                    }
                }
                composable(AppDestination.RequestList.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.Home) }
                    } else {
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
                }
                composable(AppDestination.AdminAuditLog.route) {
                    if (accountType == AccountType.ADMIN) {
                        AdminAuditLogScreen(
                            onOpenLogDetail = { logId ->
                                navController.navigate(AppDestination.AdminAuditLogDetail.route(logId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminDashboard.route) {
                    if (accountType == AccountType.ADMIN) {
                        AdminDashboardScreen(
                            onNavigateToAddFacility = {
                                navController.navigate(AppDestination.AdminCreateFacility.route)
                            },
                            onNavigateToNotifications = {
                                navController.navigate(AppDestination.Notifications.route)
                            },
                            onNavigateToUsers = {
                                navController.navigate(AppDestination.AdminUsers.route)
                            },
                            onNavigateToPharmacies = {
                                navController.navigate(AppDestination.AdminPharmacies.route)
                            },
                            onNavigateToWarehouses = {
                                navController.navigate(AppDestination.AdminWarehouses.route)
                            },
                            onNavigateToAuditLog = {
                                navController.navigateToInnerTopLevel(AppDestination.AdminAuditLog)
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.AdminAuditLogDetail.route,
                    arguments = AppDestination.AdminAuditLogDetail.arguments,
                ) {
                    if (accountType == AccountType.ADMIN) {
                        AuditLogDetailScreen(
                            onBack = { navController.popBackStack() },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminCreateFacility.route) {
                    if (accountType == AccountType.ADMIN) {
                        com.pharmalink.feature.admin.ui.facility.CreateFacilityScreen(
                            onBackClick = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() },
                            onPickLocation = {
                                // TODO: Navigate to map picker screen
                                // Example: navController.navigate(AppDestination.MapPicker.route)
                                // For now, this is a placeholder that does nothing
                                // The map picker should return coordinates via savedStateHandle
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminUsers.route) {
                    if (accountType == AccountType.ADMIN) {
                        AdminUsersScreen(
                            onNavigateToCreateUser = {
                                // TODO: Navigate to create user screen when implemented
                            },
                            onNavigateToUserDetail = { userId ->
                                navController.navigate(AppDestination.AdminUserDetail.createRoute(userId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminWarehouses.route) {
                    if (accountType == AccountType.ADMIN) {
                        AdminWarehousesScreen(
                            onNavigateToCreateWarehouse = {
                                navController.navigate(AppDestination.AdminCreateFacility.route)
                            },
                            onNavigateToWarehouseDetail = { warehouseId ->
                                navController.navigate(AppDestination.AdminWarehouseDetail.createRoute(warehouseId))
                            },
                            onNavigateToInventory = { warehouseId ->
                                navController.navigate(AppDestination.WarehouseInventory.createRoute(warehouseId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminPharmacies.route) {
                    if (accountType == AccountType.ADMIN) {
                        AdminPharmaciesScreen(
                            onNavigateToCreatePharmacy = {
                                navController.navigate(AppDestination.AdminCreateFacility.route)
                            },
                            onNavigateToPharmacyDetail = { pharmacyId ->
                                navController.navigate(AppDestination.AdminPharmacyDetail.createRoute(pharmacyId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.AdminUserDetail.route,
                    arguments = AppDestination.AdminUserDetail.arguments,
                ) {
                    if (accountType == AccountType.ADMIN) {
                        com.pharmalink.feature.admin.ui.users.UserDetailsScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.AdminWarehouseDetail.route,
                    arguments = AppDestination.AdminWarehouseDetail.arguments,
                ) {
                    if (accountType == AccountType.ADMIN) {
                        com.pharmalink.feature.admin.ui.warehouses.WarehouseDetailsScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateToInventory = { warehouseId ->
                                navController.navigate(AppDestination.WarehouseInventory.createRoute(warehouseId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.AdminPharmacyDetail.route,
                    arguments = AppDestination.AdminPharmacyDetail.arguments,
                ) {
                    if (accountType == AccountType.ADMIN) {
                        com.pharmalink.feature.admin.ui.pharmacies.PharmacyDetailsScreen(
                            onBackClick = { navController.popBackStack() },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.WarehouseInventory.route,
                    arguments = AppDestination.WarehouseInventory.arguments,
                ) {
                    if (accountType == AccountType.ADMIN) {
                        WarehouseInventoryScreen(
                            onBackClick = { navController.popBackStack() },
                            onAddMedicine = {
                                // TODO: Navigate to add medicine screen when implemented
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
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
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
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
                            }
                        )
                    }
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
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        RequestDetailsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenOrder = { orderId ->
                                navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                            },
                            viewModel = hiltViewModel(),
                        )
                    }
                }
                composable(
                    route = AppDestination.OrderDetail.route,
                    arguments = AppDestination.OrderDetail.arguments,
                ) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        val orderId = it.arguments?.getString("orderId").orEmpty()
                        LaunchedEffect(orderId) {
                            navController.navigate(AppDestination.CustomerOrderDetail.createRoute(orderId)) {
                                launchSingleTop = true
                            }
                        }
                    } else {
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
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
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
}
