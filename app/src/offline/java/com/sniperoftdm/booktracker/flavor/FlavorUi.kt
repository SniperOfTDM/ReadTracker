package com.sniperoftdm.booktracker.flavor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.sniperoftdm.booktracker.AppContainer

/**
 * Empty UI hooks for the offline flavor where online search is unavailable.
 */
object FlavorUi {

    /** Renders nothing in offline flavor. */
    @Composable
    fun SearchOnlineButton(
        onNavigateToSearch: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        // No-op in offline flavor
    }

    /** Renders nothing in offline flavor. */
    @Composable
    fun ScanIsbnButton(
        onScanResult: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        // No-op in offline flavor
    }

    /** Renders nothing in offline flavor. */
    @Composable
    fun RedownloadCoversButton(
        modifier: Modifier = Modifier
    ) {
        // No-op in offline flavor
    }

    /** Adds no flavor-specific navigation routes in offline flavor. */
    fun NavGraphBuilder.flavorRoutes(
        navController: NavHostController,
        appContainer: AppContainer
    ) {
        // No-op in offline flavor
    }
}
