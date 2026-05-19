package com.pharmalink.feature.auth.Splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pharmalink.designsystem.stitch.StitchTheme
import com.pharmalink.designsystem.theme.PharmaBlue500
import com.pharmalink.designsystem.theme.dimens
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.feature.auth.R
import kotlinx.coroutines.delay

@Composable
fun PharmaSplashScreen(
    viewModel: SplashScreenViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToPharmacyHome: () -> Unit,
    onNavigateToWarehouseHome: () -> Unit,
    onNavigateToUserHome: () -> Unit,
) {
    var logoVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(3000L) // Delay for 3 seconds
        viewModel.routeUser(
            onNavigateToLogin,
            onNavigateToAdminDashboard,
            onNavigateToPharmacyHome,
            onNavigateToWarehouseHome,
            onNavigateToUserHome
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = logoVisible,
                enter = fadeIn(animationSpec = tween(1000)),
                exit = fadeOut(animationSpec = tween(1000))
            ) {
                androidx.compose.material3.Icon(
                    painter = painterResource(id = DsR.drawable.sydaliti_logo_full),
                    contentDescription = stringResource(R.string.auth_cd_logo),
                    tint = Color.White,
                    modifier = Modifier.size(88.dp),
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceL))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.spaceXXL))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White,
                strokeWidth = 4.dp
            )
        }
    }
}

// Preview for development
@Preview(showBackground = true)
@Composable
fun PreviewPharmaSplashScreen() {
    StitchTheme {
        PharmaSplashScreen(
            onNavigateToLogin = {},
            onNavigateToAdminDashboard = {},
            onNavigateToPharmacyHome = {},
            onNavigateToWarehouseHome = {},
            onNavigateToUserHome = {},
        )
    }
}

