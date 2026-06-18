package com.pharmalink.feature.auth.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pharmalink.designsystem.R as DsR
import com.pharmalink.feature.auth.R

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit, // Kept for navigation logic (usually triggered by a LaunchedEffect)
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White), // White background to match the logo style
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = DsR.drawable.ic_app_logo),
            contentDescription = stringResource(R.string.auth_cd_logo),
            modifier = Modifier.size(120.dp),
        )
    }
}
