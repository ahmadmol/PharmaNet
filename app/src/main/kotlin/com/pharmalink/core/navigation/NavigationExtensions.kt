package com.pharmalink.core.navigation

import androidx.navigation.NavController

internal fun NavController.navigateToTopLevel(route: String) {
    navigate(route) {
        // Safe navigation for inner NavHost - don't use graph.findStartDestination()
        // This avoids the crash: "navigation destination X is not a direct child of this NavGraph"
        popUpTo(route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Safe navigation for inner authenticated NavHost context
 * Uses AppDestination.Home as stable anchor for consistent tab navigation
 * Prevents duplicate navigation when already on target route
 * Runtime-hardened with edge case handling
 */
internal fun NavController.navigateToInnerTopLevel(appDestination: AppDestination) {
    try {
        // Prevent duplicate navigation if already on the target route
        val currentRoute = currentBackStackEntry?.destination?.route
        if (currentRoute?.matchesDestination(appDestination) == true) {
            return // Already on the target route, no need to navigate
        }
        
        navigate(appDestination.route) {
            popUpTo(graph.startDestinationId) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: Exception) {
        // Ultimate fallback: try simple navigation
        navigate(appDestination.route) {
            launchSingleTop = true
        }
    }
}

/**
 * Utility function to check if a destination is reachable in the current NavHost
 * Provides additional runtime safety for navigation operations
 */
internal fun NavController.canNavigateTo(destination: AppDestination): Boolean {
    return try {
        // Check if destination is in the current graph
        val currentGraph = graph
        currentGraph.findNode(destination.route) != null
    } catch (e: Exception) {
        false // Assume not navigable if there's an error
    }
}

internal fun String?.matchesDestination(destination: AppDestination): Boolean {
    if (this == null) return false
    return substringBefore("/") == destination.route.substringBefore("/")
}
