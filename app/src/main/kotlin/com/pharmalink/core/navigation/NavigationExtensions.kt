package com.pharmalink.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

internal fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

internal fun String?.matchesDestination(destination: AppDestination): Boolean {
    if (this == null) return false
    return substringBefore("/") == destination.route.substringBefore("/")
}
