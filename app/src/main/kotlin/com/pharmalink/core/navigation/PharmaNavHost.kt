package com.pharmalink.core.navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pharmalink.core.common.ui.UiState
import com.pharmalink.domain.model.AccountType
import com.pharmalink.domain.model.AuthSessionState
import com.pharmalink.feature.auth.AuthViewModel
import com.pharmalink.feature.auth.LoginViewModel
import com.pharmalink.feature.auth.SignUpViewModel
import com.pharmalink.feature.auth.SplashViewModel
import com.pharmalink.feature.auth.screens.ForgotPasswordScreen
import com.pharmalink.feature.auth.screens.LoginScreen
import com.pharmalink.feature.auth.screens.SignUpScreen
import com.pharmalink.feature.auth.screens.SplashScreen
import com.pharmalink.feature.main.navigation.PharmaNavigator

private val authRoutes = setOf(
    AppDestination.Splash.route,
    AppDestination.Login.route,
    AppDestination.SignUp.route,
    AppDestination.ForgotPassword.route,
)

@Composable
fun PharmaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val splashViewModel: SplashViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val splashState by splashViewModel.uiState.collectAsStateWithLifecycle()
    val logoutState by authViewModel.uiState.collectAsStateWithLifecycle()
    val userSnapshot by authViewModel.userSnapshot.collectAsStateWithLifecycle()

    val accountType = userSnapshot?.accountType
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        splashViewModel.checkSession()
    }

    LaunchedEffect(authState, currentRoute, splashState.isLoading, userSnapshot, splashState.errorMessage) {
        if (splashState.isLoading || authState is AuthSessionState.Loading || currentRoute == null) {
            return@LaunchedEffect
        }

        val hasAuthenticatedSession = authState is AuthSessionState.Authenticated
        val hasResolvedSnapshot = userSnapshot != null
        val canEnterMainTabs = hasAuthenticatedSession && hasResolvedSnapshot && splashState.errorMessage == null

        when {
            currentRoute == AppDestination.Splash.route -> {
                val destination = if (canEnterMainTabs) {
                    AppDestination.MainTabs.route
                } else {
                    AppDestination.Login.route
                }
                navController.navigate(destination) {
                    popUpTo(AppDestination.Splash.route) { inclusive = true }
                    launchSingleTop = true
                }
            }

            (!hasAuthenticatedSession || !hasResolvedSnapshot) &&
                currentRoute == AppDestination.MainTabs.route -> {
                navController.navigate(AppDestination.Login.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }

            canEnterMainTabs && currentRoute in authRoutes -> {
                navController.navigate(AppDestination.MainTabs.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(logoutState) {
        if (logoutState is UiState.Error) {
            // No-op for now; logout stays on current screen and error remains in viewmodel state.
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route,
        modifier = modifier,
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onNavigateToLogin = {},
            )
        }

        composable(AppDestination.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = loginState,
                onPhoneNumberChange = loginViewModel::updatePhoneNumber,
                onPasswordChange = loginViewModel::updatePassword,
                onLoginClick = loginViewModel::login,
                onBiometricClick = loginViewModel::loginWithBiometrics,
                onForgotPasswordClick = {
                    navController.navigate(AppDestination.ForgotPassword.route) {
                        launchSingleTop = true
                    }
                },
                onSignUpClick = {
                    navController.navigate(AppDestination.SignUp.route) {
                        launchSingleTop = true
                    }
                },
                onGuestClick = null,
            )
        }

        composable(AppDestination.SignUp.route) {
            val signUpViewModel: SignUpViewModel = hiltViewModel()
            val signUpState by signUpViewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val activity = context.findActivity()
            val locationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions(),
                onResult = { permissions ->
                    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        signUpViewModel.requestCurrentLocation()
                    } else {
                        val permanentlyDenied = activity?.let {
                            !ActivityCompat.shouldShowRequestPermissionRationale(
                                it,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                            ) && !ActivityCompat.shouldShowRequestPermissionRationale(
                                it,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        } ?: true
                        signUpViewModel.onLocationPermissionDenied(permanentlyDenied)
                    }
                },
            )

            LaunchedEffect(signUpState.navigateToLogin) {
                if (signUpState.navigateToLogin) {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.SignUp.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    signUpViewModel.onLoginNavigationConsumed()
                }
            }

            SignUpScreen(
                uiState = signUpState,
                onAccountTypeChange = signUpViewModel::updateAccountType,
                onFullNameChange = signUpViewModel::updateFullName,
                onPharmacyNameChange = signUpViewModel::updatePharmacyName,
                onPharmacyLocationChange = signUpViewModel::updatePharmacyLocation,
                onWarehouseNameChange = signUpViewModel::updateWarehouseName,
                onWarehouseLocationChange = signUpViewModel::updateWarehouseLocation,
                onRequestCurrentLocationClick = {
                    if (context.hasLocationPermission()) {
                        signUpViewModel.requestCurrentLocation()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                onPhoneNumberChange = signUpViewModel::updatePhoneNumber,
                onPasswordChange = signUpViewModel::updatePassword,
                onConfirmPasswordChange = signUpViewModel::updateConfirmPassword,
                onAgreedToTermsChange = signUpViewModel::updateAgreedToTerms,
                onSignUpClick = signUpViewModel::signUp,
                onLoginClick = { navController.popBackStack() },
            )
        }

        composable(AppDestination.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() },
            )
        }

        composable(AppDestination.MainTabs.route) {
            val mainTabsStartDestination = when (accountType) {
                AccountType.WAREHOUSE -> AppDestination.WarehouseDashboard.route
                AccountType.ADMIN -> AppDestination.AdminDashboard.route
                AccountType.PHARMACY -> AppDestination.PharmacyDashboard.route
                else -> AppDestination.Home.route
            }
            PharmaNavigator(
                onProfileLogout = {
                    authViewModel.logout()
                },
                startDestination = mainTabsStartDestination,
                accountType = accountType   // ← مهم جداً
            )
        }
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
