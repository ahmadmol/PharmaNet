package com.pharmalink.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pharmalink.feature.auth.LoginViewModel
import com.pharmalink.feature.auth.SignUpViewModel
import com.pharmalink.feature.auth.SplashViewModel
import com.pharmalink.feature.auth.screens.LoginScreen
import com.pharmalink.feature.auth.screens.SignUpScreen
import com.pharmalink.feature.auth.screens.SplashScreen
import com.pharmalink.feature.main.navigation.PharmaNavigator

@Composable
fun PharmaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route,
        modifier = modifier,
    ) {
        // ✅ Splash screen for session checking
        composable(AppDestination.Splash.route) {
            val viewModel: SplashViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            // ✅ استدعاء فحص الجلسة عند الدخول
            LaunchedEffect(Unit) {
                viewModel.checkSession()
            }
            
            // ✅ التوجيه بناءً على نتيجة الفحص
            LaunchedEffect(state.destinationRoute) {
                state.destinationRoute?.let { route ->
                    navController.navigate(route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            
            // ✅ عرض شاشة التحميل
            SplashScreen()
        }
        
        composable(AppDestination.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val loginState by loginViewModel.uiState.collectAsStateWithLifecycle()

            // ✅ تأكد من استخدام isSuccess بدلاً من isLoginSuccessful
            LaunchedEffect(loginState.isLoginSuccessful) {
                if (loginState.isLoginSuccessful) {
                    navController.navigate(AppDestination.MainTabs.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                        launchSingleTop = true  // ✅ منع تكرار الشاشات في الـ back stack
                    }
                }
            }

            LoginScreen(
                uiState = loginState,
                onPhoneNumberChange = loginViewModel::updatePhoneNumber,
                onPasswordChange = loginViewModel::updatePassword,
                onLoginClick = loginViewModel::login,
                onForgotPasswordClick = { /* TODO: Implement forgot password */ },
                onSignUpClick = {
                    navController.navigate(AppDestination.SignUp.route) {
                        launchSingleTop = true
                    }
                },
                onGuestClick = { /* TODO: Implement guest mode */ },
            )
        }
        composable(AppDestination.SignUp.route) {
            val signUpViewModel: SignUpViewModel = hiltViewModel()
            val signUpState by signUpViewModel.uiState.collectAsStateWithLifecycle()

            // ✅ استخدام isSuccess بدلاً من isSignUpSuccessful
            LaunchedEffect(signUpState.isSuccess) {
                if (signUpState.isSuccess) {
                    navController.navigate(AppDestination.MainTabs.route) {
                        popUpTo(AppDestination.SignUp.route) { inclusive = true }
                        launchSingleTop = true
                    }
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
                onPhoneNumberChange = signUpViewModel::updatePhoneNumber,
                onPasswordChange = signUpViewModel::updatePassword,
                onConfirmPasswordChange = signUpViewModel::updateConfirmPassword,
                onSignUpClick = signUpViewModel::signUp,
                onLoginClick = { navController.popBackStack() },
            )
        }
        composable(AppDestination.MainTabs.route) {
            PharmaNavigator(
                onProfileLogout = {
                    navController.navigate(AppDestination.Login.route) {
                        popUpTo(AppDestination.MainTabs.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
