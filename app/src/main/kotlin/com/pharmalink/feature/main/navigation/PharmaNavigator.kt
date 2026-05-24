package com.pharmalink.feature.main.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pharmalink.R
import com.pharmalink.core.navigation.AppDestination
import com.pharmalink.core.navigation.NavArgs
import com.pharmalink.core.navigation.matchesDestination
import com.pharmalink.core.navigation.navigateToInnerTopLevel
import com.pharmalink.core.navigation.topLevelDestinationFor
import com.pharmalink.designsystem.theme.ClinicalCanvas
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.NotificationDestination
import com.pharmalink.feature.admin.ui.audit.AdminAuditLogScreen
import com.pharmalink.feature.admin.ui.components.AdminActionDestination
import com.pharmalink.feature.admin.ui.components.AdminActionsBottomSheet
import com.pharmalink.feature.admin.ui.components.AdminChromeViewModel
import com.pharmalink.feature.admin.ui.audit.AuditLogDetailScreen
import com.pharmalink.feature.admin.ui.dashboard.AdminDashboardScreen
import com.pharmalink.feature.admin.ui.inventory.AddMedicineScreen
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
import com.pharmalink.feature.help.presentation.LanguageScreen
import com.pharmalink.feature.help.presentation.SecurityPrivacyScreen
import com.pharmalink.feature.home.homeScreen
import com.pharmalink.feature.notifications.NotificationsScreen
import com.pharmalink.feature.orders.CreateCustomerOrderScreen
import com.pharmalink.feature.orders.CustomerOrderDetailScreen
import com.pharmalink.feature.orders.CustomerOrderSuccessScreen
import com.pharmalink.feature.orders.OrdersScreen
import com.pharmalink.feature.orders.MedicineSummaryUi
import com.pharmalink.feature.orders.MyCustomerOrdersScreen
import com.pharmalink.feature.orders.PharmacySelectionScreen
import com.pharmalink.feature.orders.PharmacyCustomerOrderDetailScreen
import com.pharmalink.feature.orders.PharmacyCustomerOrdersScreen
import com.pharmalink.feature.orders.PharmacySummaryUi
import com.pharmalink.feature.orders.PublicPharmaciesScreen
import com.pharmalink.feature.orders.PublicUserOrderNavStateKeys
import com.pharmalink.domain.model.CustomerRequestScope
import com.pharmalink.feature.orders.presentation.OrderDetailScreen
import com.pharmalink.feature.pharmacy.PharmacyDashboardScreen
import com.pharmalink.feature.pharmacy.PharmacyRadarScreen
import com.pharmalink.feature.pharmacy.WarehouseProductsScreen
import com.pharmalink.feature.profile.ChangePasswordScreen
import com.pharmalink.feature.profile.EditProfileScreen
import com.pharmalink.feature.profile.ProfileScreen
import com.pharmalink.feature.request.CreateRequestScreen
import com.pharmalink.feature.request.RequestDetailsScreen
import com.pharmalink.feature.request.RequestListScreen
import com.pharmalink.feature.resources.presentation.WarehouseDetailScreen
import com.pharmalink.feature.tracking.DeliveryTrackingScreen
import com.pharmalink.feature.warehouses.FeaturedWarehousesScreen
import com.pharmalink.feature.warehouses.WarehouseDashboardScreen
import com.pharmalink.feature.warehouses.WarehousesScreen
import com.pharmalink.feature.auth.Splash.PharmaSplashScreen

private val bottomBarRoutes = setOf(
    AppDestination.Home.route,
    AppDestination.PharmacyDashboard.route,
    AppDestination.PharmacyRadar.route,
    AppDestination.WarehouseDashboard.route,
    AppDestination.Resources.route,
    AppDestination.CreateRequest.route,
    AppDestination.RequestList.route,
    AppDestination.Orders.route,
    AppDestination.Notifications.route,
    AppDestination.Profile.route,
    AppDestination.MedicineSearch.route,
    AppDestination.PublicPharmacies.route,
    AppDestination.MyCustomerOrders.route,
    AppDestination.PharmacyCustomerOrders.route,
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
    val adminChromeViewModel: AdminChromeViewModel = hiltViewModel()
    val adminChromeState by adminChromeViewModel.state.collectAsState()
    val adminProfileImageUrl = adminChromeState.profileImageUrl
    val accountType = userSnapshot?.accountType
    val currentWarehouseId = userSnapshot?.warehouseId.orEmpty()
    var showAdminMenu by remember { mutableStateOf(false) }

    val currentTab = when {
        currentRoute.matchesDestination(AppDestination.AdminDashboard) -> AppDestination.AdminDashboard
        currentRoute.matchesDestination(AppDestination.PharmacyDashboard) -> AppDestination.PharmacyDashboard
        currentRoute.matchesDestination(AppDestination.PharmacyRadar) -> AppDestination.PharmacyRadar
        currentRoute.matchesDestination(AppDestination.Home) -> AppDestination.Home
        currentRoute.matchesDestination(AppDestination.MedicineSearch) -> AppDestination.MedicineSearch
        currentRoute.matchesDestination(AppDestination.PublicPharmacies) -> AppDestination.PublicPharmacies
        currentRoute.matchesDestination(AppDestination.MyCustomerOrders) -> AppDestination.MyCustomerOrders
        currentRoute.matchesDestination(AppDestination.PharmacyCustomerOrders) -> AppDestination.PharmacyCustomerOrders
        currentRoute.matchesDestination(AppDestination.WarehouseDashboard) -> AppDestination.WarehouseDashboard
        currentRoute.matchesDestination(AppDestination.Resources) -> AppDestination.Resources
        currentRoute.matchesDestination(AppDestination.CreateRequest) -> AppDestination.CreateRequest
        currentRoute.matchesDestination(AppDestination.RequestList) -> AppDestination.RequestList
        currentRoute.matchesDestination(AppDestination.Orders) -> AppDestination.Orders
        currentRoute.matchesDestination(AppDestination.Notifications) -> AppDestination.Notifications
        currentRoute.matchesDestination(AppDestination.Profile) -> AppDestination.Profile
        currentRoute.matchesDestination(AppDestination.AdminAuditLog) -> AppDestination.AdminAuditLog
        else -> if (accountType == AccountType.WAREHOUSE) AppDestination.WarehouseDashboard else AppDestination.Home
    }

    val visibleTabs = when (accountType) {
        AccountType.WAREHOUSE ->
            listOf(AppDestination.WarehouseDashboard, AppDestination.RequestList, AppDestination.Notifications, AppDestination.Profile)
        AccountType.ADMIN ->
            listOf(AppDestination.AdminDashboard, AppDestination.AdminAuditLog, AppDestination.Profile)
        AccountType.PUBLIC_USER ->
            listOf(
                AppDestination.Home,
                AppDestination.MedicineSearch,
                AppDestination.PublicPharmacies,
                AppDestination.MyCustomerOrders,
                AppDestination.Profile,
            )
        AccountType.PHARMACY ->
            listOf(
                AppDestination.PharmacyDashboard,
                AppDestination.PharmacyRadar,
                AppDestination.PharmacyCustomerOrders,
                AppDestination.Profile,
            )
        null -> listOf(AppDestination.Home, AppDestination.Profile)
    }

    val bottomBarTabs = visibleTabs
        .mapNotNull { destination -> topLevelDestinationFor(destination.route) }

    val bottomBarTabRoutes = bottomBarTabs.map { it.route }.toSet()
    val defaultTab = when (accountType) {
        AccountType.ADMIN -> AppDestination.AdminDashboard
        AccountType.PHARMACY -> AppDestination.PharmacyDashboard
        AccountType.WAREHOUSE -> AppDestination.WarehouseDashboard
        else -> AppDestination.Home
    }
    val safeSelectedTab = if (currentTab.route !in bottomBarTabRoutes) defaultTab else currentTab

    val tabSelectedIndex = bottomBarTabs
        .indexOfFirst { destination -> destination.route == safeSelectedTab.route }
        .coerceAtLeast(0)

    val visibleTabRoutes = visibleTabs.map { it.route }.toSet()
    val bottomBarVisibleRoutes = if (accountType == AccountType.PHARMACY) {
        visibleTabRoutes + AppDestination.CreateRequest.route
    } else {
        visibleTabRoutes
    }
    val isBottomBarVisible = currentRoute in bottomBarVisibleRoutes
    val safeWarehouseTopLevelRoute = if (accountType == AccountType.WAREHOUSE) {
        AppDestination.WarehouseDashboard.route
    } else {
        AppDestination.Home.route
    }

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
            currentRoute.matchesDestination(AppDestination.CreateRequestPrefilled) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CreateCustomerOrder) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CustomerOrderSuccess) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.PharmacyWarehouseProducts) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.MyCustomerOrders) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.CustomerOrderDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.PharmacyCustomerOrderDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminAuditLogDetail) -> navController.popBackStack()
            currentRoute.matchesDestination(AppDestination.AdminOrderDetail) -> navController.popBackStack()
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
            currentRoute.matchesDestination(AppDestination.Home) -> {
                if (accountType != AccountType.WAREHOUSE) Unit else navController.navigateToInnerTopLevel(AppDestination.WarehouseDashboard)
            }
            currentRoute.matchesDestination(AppDestination.WarehouseDashboard) -> Unit
            currentRoute.matchesDestination(AppDestination.PharmacyDashboard) -> Unit
            currentRoute.matchesDestination(AppDestination.PharmacyRadar) -> Unit
            currentRoute in bottomBarRoutes -> {
                if (accountType == AccountType.WAREHOUSE) {
                    navController.navigateToInnerTopLevel(AppDestination.WarehouseDashboard)
                } else {
                    navController.navigateToInnerTopLevel(AppDestination.Home)
                }
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
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            if (isBottomBarVisible && accountType == AccountType.PHARMACY) {
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
                    items = bottomBarTabs,
                    selectedItem = tabSelectedIndex,
                    labelOverrides = if (accountType == AccountType.WAREHOUSE) {
                        mapOf(AppDestination.RequestList.route to "\u0627\u0644\u0648\u0627\u0631\u062f\u0629")
                    } else {
                        emptyMap()
                    },
                    onTabSelected = { route, _ ->
                        when (route) {
                            AppDestination.AdminDashboard.route -> navController.navigateToInnerTopLevel(AppDestination.AdminDashboard)
                            AppDestination.PharmacyDashboard.route -> navController.navigateToInnerTopLevel(AppDestination.PharmacyDashboard)
                            AppDestination.PharmacyRadar.route -> navController.navigateToInnerTopLevel(AppDestination.PharmacyRadar)
                            AppDestination.Home.route -> navController.navigateToInnerTopLevel(AppDestination.Home)
                            AppDestination.MedicineSearch.route -> navController.navigateToInnerTopLevel(AppDestination.MedicineSearch)
                            AppDestination.PublicPharmacies.route -> navController.navigateToInnerTopLevel(AppDestination.PublicPharmacies)
                            AppDestination.MyCustomerOrders.route -> navController.navigateToInnerTopLevel(AppDestination.MyCustomerOrders)
                            AppDestination.PharmacyCustomerOrders.route -> navController.navigateToInnerTopLevel(AppDestination.PharmacyCustomerOrders)
                            AppDestination.WarehouseDashboard.route -> navController.navigateToInnerTopLevel(AppDestination.WarehouseDashboard)
                            AppDestination.Resources.route -> navController.navigateToInnerTopLevel(AppDestination.Resources)
                            AppDestination.RequestList.route -> navController.navigateToInnerTopLevel(AppDestination.RequestList)
                            AppDestination.Orders.route -> navController.navigateToInnerTopLevel(AppDestination.Orders)
                            AppDestination.Notifications.route -> navController.navigateToInnerTopLevel(AppDestination.Notifications)
                            AppDestination.Profile.route -> navController.navigateToInnerTopLevel(AppDestination.Profile)
                            AppDestination.AdminAuditLog.route -> navController.navigateToInnerTopLevel(AppDestination.AdminAuditLog)
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
                startDestination = AppDestination.Splash.route,
            ) {
                composable(AppDestination.Splash.route) {
                    PharmaSplashScreen(
                        onNavigateToLogin = { navController.navigate(AppDestination.Login.route) { popUpTo(AppDestination.Splash.route) { inclusive = true } } },
                        onNavigateToAdminDashboard = { navController.navigate(AppDestination.AdminDashboard.route) { popUpTo(AppDestination.Splash.route) { inclusive = true } } },
                        onNavigateToPharmacyHome = { navController.navigate(AppDestination.PharmacyDashboard.route) { popUpTo(AppDestination.Splash.route) { inclusive = true } } },
                        onNavigateToWarehouseHome = { navController.navigate(AppDestination.WarehouseDashboard.route) { popUpTo(AppDestination.Splash.route) { inclusive = true } } },
                        onNavigateToUserHome = { navController.navigate(AppDestination.Home.route) { popUpTo(AppDestination.Splash.route) { inclusive = true } } }
                    )
                }
                composable(AppDestination.PharmacyDashboard.route) {
                    if (accountType == AccountType.PHARMACY) {
                        PharmacyDashboardScreen(
                            onNavigateToNotifications = { navController.navigateToInnerTopLevel(AppDestination.Notifications) },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                composable(AppDestination.PharmacyRadar.route) {
                    if (accountType == AccountType.PHARMACY) {
                        PharmacyRadarScreen(
                            onNavigateToOrderDetail = { orderId ->
                                navController.navigate(AppDestination.PharmacyCustomerOrderDetail.createRoute(orderId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                homeScreen(
                    onNavigateToHome = {
                        if (accountType == AccountType.WAREHOUSE) {
                            navController.navigateToInnerTopLevel(AppDestination.WarehouseDashboard)
                        } else {
                            navController.navigateToInnerTopLevel(AppDestination.Home)
                        }
                    },
                    onNavigateToOrders = {
                        when (accountType) {
                            AccountType.PUBLIC_USER -> {
                                navController.navigate(AppDestination.MyCustomerOrders.route) {
                                    launchSingleTop = true
                                }
                            }
                            AccountType.PHARMACY -> {
                                navController.navigateToInnerTopLevel(AppDestination.PharmacyCustomerOrders)
                            }
                            AccountType.ADMIN -> {
                                navController.navigateToInnerTopLevel(AppDestination.Orders)
                            }
                            AccountType.WAREHOUSE, null -> {
                                navController.navigateToInnerTopLevel(AppDestination.RequestList)
                            }
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
                composable(AppDestination.WarehouseDashboard.route) {
                    if (accountType == AccountType.WAREHOUSE) {
                        WarehouseDashboardScreen(
                            warehouseId = currentWarehouseId,
                            warehouseName = userSnapshot?.warehouseName.orEmpty(),
                            onManageInventory = {
                                if (currentWarehouseId.isNotBlank()) {
                                    navController.navigate(AppDestination.WarehouseInventory.createRoute(currentWarehouseId))
                                }
                            },
                            onAddProduct = {
                                if (currentWarehouseId.isNotBlank()) {
                                    navController.navigate(AppDestination.AddMedicine.createRoute(currentWarehouseId))
                                }
                            },
                            onOpenRequests = { navController.navigateToInnerTopLevel(AppDestination.RequestList) },
                            onOpenNotifications = { navController.navigateToInnerTopLevel(AppDestination.Notifications) },
                            onOpenProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                composable(AppDestination.MedicineSearch.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
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
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                composable(
                    route = AppDestination.PharmacySelection.route,
                    arguments = AppDestination.PharmacySelection.arguments,
                ) { entry ->
                    if (accountType != AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                        return@composable
                    }
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
                    if (accountType != AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                        return@composable
                    }
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
                    if (accountType != AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                        return@composable
                    }
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
                    if (accountType != AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.Home) }
                        return@composable
                    }
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
                composable(AppDestination.PublicPharmacies.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        PublicPharmaciesScreen()
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                composable(
                    route = AppDestination.CustomerOrderDetail.route,
                    arguments = AppDestination.CustomerOrderDetail.arguments,
                ) {
                    if (accountType == AccountType.PUBLIC_USER) {
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
                    } else {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.Home) }
                    }
                }
                composable(AppDestination.Resources.route) {
                    if (accountType == AccountType.WAREHOUSE) {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.WarehouseDashboard) }
                    } else if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    } else {
                        WarehousesScreen(
                            viewModel = hiltViewModel(),
                            accountType = accountType,
                            onWarehouseClick = { warehouseId ->
                                when (accountType) {
                                    AccountType.PHARMACY -> {
                                        if (warehouseId.isNotBlank()) {
                                            navController.navigate(AppDestination.PharmacyWarehouseProducts.createRoute(warehouseId))
                                        }
                                    }
                                    AccountType.ADMIN -> {
                                        navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                                    }
                                    AccountType.WAREHOUSE -> {
                                        if (warehouseId.isNotBlank() && warehouseId == currentWarehouseId) {
                                            navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                                        }
                                    }
                                    else -> Unit
                                }
                            },
                            onViewIncomingRequests = { navController.navigateToInnerTopLevel(AppDestination.RequestList) },
                            onManageInventory = {
                                if (accountType == AccountType.WAREHOUSE && currentWarehouseId.isNotBlank()) {
                                    navController.navigate(AppDestination.WarehouseInventory.createRoute(currentWarehouseId))
                                }
                            },
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
                                if (accountType == AccountType.PHARMACY) {
                                    navController.navigate(AppDestination.PharmacyWarehouseProducts.createRoute(warehouseId))
                                } else {
                                    navController.navigate(AppDestination.WarehouseDetail.createRoute(warehouseId))
                                }
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
                composable(
                    route = AppDestination.CreateRequestPrefilled.route,
                    arguments = AppDestination.CreateRequestPrefilled.arguments,
                ) {
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
                    } else if (accountType == AccountType.WAREHOUSE) {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.RequestList) }
                    } else {
                        OrdersScreen(
                            onOpenOrder = { orderId ->
                                navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                            },
                        )
                    }
                }
                composable(AppDestination.PharmacyCustomerOrders.route) {
                    if (accountType == AccountType.PHARMACY) {
                        PharmacyCustomerOrdersScreen(
                            onOpenOrder = { orderId ->
                                navController.navigate(AppDestination.PharmacyCustomerOrderDetail.createRoute(orderId))
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
                    }
                }
                composable(AppDestination.RequestList.route) {
                    if (accountType == AccountType.PUBLIC_USER) {
                        LaunchedEffect(Unit) { navController.navigate(safeWarehouseTopLevelRoute) { launchSingleTop = true } }
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
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            onShowAdminMenu = { showAdminMenu = true },
                            profileImageUrl = adminProfileImageUrl,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                composable(AppDestination.AdminDashboard.route) {
                    if (accountType == AccountType.ADMIN) {
                        var showReportDialog by remember { mutableStateOf(false) }

                        AdminDashboardScreen(
                            onNavigateToAddFacility = {
                                navController.navigate(AppDestination.AdminCreateFacility.route)
                            },
                            onNavigateToNotifications = {
                                navController.navigate(AppDestination.Notifications.route)
                            },
                            onNavigateToProfile = {
                                navController.navigateToInnerTopLevel(AppDestination.Profile)
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
                            onNavigateToOrders = {
                                navController.navigate(AppDestination.AdminOrders.route)
                            },
                            onNavigateToOrderDetail = { orderId ->
                                navController.navigate(AppDestination.AdminOrderDetail.createRoute(orderId))
                            },
                            onShowAdminMenu = {
                                showAdminMenu = true
                            },
                            onShowReportDialog = {
                                showReportDialog = true
                            },
                            profileImageUrl = adminProfileImageUrl,
                        )

                        // Report Dialog
                        if (showReportDialog) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showReportDialog = false },
                                title = { Text("تقرير لوحة التحكم") },
                                text = {
                                    Text("سيتم إضافة ميزة التقارير قريباً. حالياً يمكنك الوصول إلى سجل التدقيق للحصول على معلومات مفصلة.")
                                },
                                confirmButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            showReportDialog = false
                                            navController.navigateToInnerTopLevel(AppDestination.AdminAuditLog)
                                        }
                                    ) {
                                        Text("فتح سجل التدقيق")
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { showReportDialog = false }
                                    ) {
                                        Text("إغلاق")
                                    }
                                }
                            )
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.AdminOrders.route) {
                    if (accountType == AccountType.ADMIN) {
                        com.pharmalink.feature.admin.ui.orders.AdminOrdersScreen(
                            onBackClick = { navController.popBackStack() },
                            onNavigateToOrderDetail = { orderId ->
                                navController.navigate(AppDestination.AdminOrderDetail.createRoute(orderId))
                            },
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            profileImageUrl = adminProfileImageUrl,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(
                    route = AppDestination.AdminOrderDetail.route,
                    arguments = AppDestination.AdminOrderDetail.arguments,
                ) { entry ->
                    if (accountType == AccountType.ADMIN) {
                        val orderId = entry.arguments?.getString("orderId").orEmpty()
                        com.pharmalink.feature.admin.ui.orders.AdminOrderDetailScreen(
                            orderId = orderId,
                            onBackClick = { navController.popBackStack() },
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            profileImageUrl = adminProfileImageUrl,
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
                            profileImageUrl = adminProfileImageUrl,
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
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            profileImageUrl = adminProfileImageUrl,
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
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            onShowAdminMenu = { showAdminMenu = true },
                            profileImageUrl = adminProfileImageUrl,
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
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            onShowAdminMenu = { showAdminMenu = true },
                            profileImageUrl = adminProfileImageUrl,
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
                            onNavigateToProfile = { navController.navigateToInnerTopLevel(AppDestination.Profile) },
                            onShowAdminMenu = { showAdminMenu = true },
                            profileImageUrl = adminProfileImageUrl,
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
                            profileImageUrl = adminProfileImageUrl,
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
                            profileImageUrl = adminProfileImageUrl,
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
                            profileImageUrl = adminProfileImageUrl,
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
                    val warehouseId = it.arguments?.getString(NavArgs.WAREHOUSE_ID) ?: ""
                    val canAccessWarehouseInventory = when (accountType) {
                        AccountType.ADMIN -> true
                        AccountType.WAREHOUSE -> warehouseId.isNotBlank() && warehouseId == currentWarehouseId
                        else -> false
                    }
                    if (canAccessWarehouseInventory) {
                        WarehouseInventoryScreen(
                            onBackClick = { navController.popBackStack() },
                            onAddMedicine = {
                                navController.navigate(AppDestination.AddMedicine.createRoute(warehouseId))
                            },
                            profileImageUrl = adminProfileImageUrl,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }

                composable(
                    route = AppDestination.AddMedicine.route,
                    arguments = AppDestination.AddMedicine.arguments,
                ) { entry ->
                    val warehouseId = entry.arguments?.getString(NavArgs.WAREHOUSE_ID) ?: ""
                    val canAccessAddMedicine = when (accountType) {
                        AccountType.ADMIN -> true
                        AccountType.WAREHOUSE -> warehouseId.isNotBlank() && warehouseId == currentWarehouseId
                        else -> false
                    }
                    if (canAccessAddMedicine) {
                        AddMedicineScreen(
                            onBackClick = { navController.popBackStack() },
                            onSuccess = { navController.popBackStack() },
                            profileImageUrl = adminProfileImageUrl,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    }
                }
                composable(AppDestination.Profile.route) {
                    ProfileScreen(
                        onLogout = {
                            onProfileLogout()
                            navController.navigate(AppDestination.Splash.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onEditProfile = { navController.navigate(AppDestination.EditProfile.route) },
                        onChangePassword = { navController.navigate(AppDestination.SecurityPrivacy.route) },
                        onOpenHelpSupport = { navController.navigate(AppDestination.Help.route) },
                        onOpenAboutApp = { navController.navigate(AppDestination.AboutApp.route) },
                        onOpenContactUs = { navController.navigate(AppDestination.ContactUs.route) },
                        onOpenLanguage = { navController.navigate(AppDestination.Language.route) },
                    )
                }
                composable(AppDestination.Notifications.route) {
                    NotificationsScreen(
                        onBack = { navController.popBackStack() },
                        onNotificationOpen = { notification ->
                            when (notification.destination) {
                                NotificationDestination.ORDER ->
                                    notification.destinationId?.let { orderId ->
                                        if (accountType == AccountType.PHARMACY) {
                                            navController.navigate(AppDestination.PharmacyCustomerOrderDetail.createRoute(orderId))
                                        } else {
                                            navController.navigate(AppDestination.OrderDetail.createRoute(orderId))
                                        }
                                    }

                                NotificationDestination.PHARMACY_CUSTOMER_ORDER ->
                                    notification.destinationId?.let { orderId ->
                                        navController.navigate(AppDestination.PharmacyCustomerOrderDetail.createRoute(orderId))
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
                composable(AppDestination.Help.route) {
                    if (accountType == AccountType.ADMIN) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        HelpScreen(
                            onBack = { navController.popBackStack() },
                            onOpenCompliance = { navController.navigate(AppDestination.SecurityPrivacy.route) },
                            onOpenContactUs = { navController.navigate(AppDestination.ContactUs.route) },
                        )
                    }
                }
                composable(AppDestination.AboutApp.route) {
                    AboutAppScreen(
                        onBack = { navController.popBackStack() },
                        onPrimaryCtaClick = { navController.navigate(AppDestination.ContactUs.route) },
                        onSecondaryCtaClick = { navController.navigate(AppDestination.Help.route) },
                    )
                }
                composable(AppDestination.ContactUs.route) {
                    if (accountType == AccountType.ADMIN) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        ContactUsScreen(
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(AppDestination.Language.route) {
                    LanguageScreen(onBack = { navController.popBackStack() })
                }
                composable(AppDestination.SecurityPrivacy.route) {
                    SecurityPrivacyScreen(onBack = { navController.popBackStack() })
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
                    route = AppDestination.PharmacyCustomerOrderDetail.route,
                    arguments = AppDestination.PharmacyCustomerOrderDetail.arguments,
                ) {
                    if (accountType == AccountType.PHARMACY) {
                        PharmacyCustomerOrderDetailScreen(
                            onBack = { navController.popBackStack() },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigateToInnerTopLevel(AppDestination.Home) }
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
                    route = AppDestination.PharmacyWarehouseProducts.route,
                    arguments = AppDestination.PharmacyWarehouseProducts.arguments,
                ) { entry ->
                    if (accountType == AccountType.PHARMACY) {
                        val warehouseId = entry.arguments?.getString(NavArgs.WAREHOUSE_ID).orEmpty()
                        WarehouseProductsScreen(
                            onBack = { navController.popBackStack() },
                            onAddToBasket = { product ->
                                navController.navigate(
                                    AppDestination.CreateRequestPrefilled.createPrefilledRoute(
                                        warehouseId = warehouseId,
                                        medicineId = product.id,
                                        medicineName = product.name,
                                        medicineSubtitle = product.subtitle,
                                        unit = product.unit,
                                    ),
                                )
                            },
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    }
                }
                composable(
                    route = AppDestination.WarehouseDetail.route,
                    arguments = AppDestination.WarehouseDetail.arguments,
                ) { entry ->
                    val warehouseId = entry.arguments?.getString("warehouseId").orEmpty()
                    val canAccessWarehouseDetail = when (accountType) {
                        AccountType.ADMIN -> true
                        AccountType.WAREHOUSE -> warehouseId.isNotBlank() && warehouseId == currentWarehouseId
                        AccountType.PHARMACY -> false
                        AccountType.PUBLIC_USER, null -> false
                    }
                    if (!canAccessWarehouseDetail) {
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

            if (accountType == AccountType.ADMIN && showAdminMenu) {
                AdminActionsBottomSheet(
                    onDismiss = { showAdminMenu = false },
                    profileImageUrl = adminProfileImageUrl,
                    onActionClick = { destination ->
                        showAdminMenu = false
                        when (destination) {
                            AdminActionDestination.USERS -> navController.navigate(AppDestination.AdminUsers.route)
                            AdminActionDestination.PHARMACIES -> navController.navigate(AppDestination.AdminPharmacies.route)
                            AdminActionDestination.WAREHOUSES -> navController.navigate(AppDestination.AdminWarehouses.route)
                            AdminActionDestination.ORDERS -> navController.navigate(AppDestination.AdminOrders.route)
                            AdminActionDestination.AUDIT_LOG -> navController.navigateToInnerTopLevel(AppDestination.AdminAuditLog)
                            AdminActionDestination.NOTIFICATIONS -> navController.navigate(AppDestination.Notifications.route)
                            AdminActionDestination.PROFILE -> navController.navigateToInnerTopLevel(AppDestination.Profile)
                        }
                    },
                )
            }
        }
    }
}
