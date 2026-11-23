package io.github.ephym6.plantid.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.ephym6.plantid.PlantIdApplication
import io.github.ephym6.plantid.ui.identify.PhotoSelectionHost
import io.github.ephym6.plantid.ui.identify.PhotoSelectionViewModel
import io.github.ephym6.plantid.ui.navigation.PlantIdNavHost
import io.github.ephym6.plantid.ui.navigation.TopLevelDestination
import io.github.ephym6.plantid.ui.navigation.isTopLevel
import io.github.ephym6.plantid.ui.navigation.navigateToPhotoPreview
import io.github.ephym6.plantid.ui.navigation.navigateToTopLevel

/**
 * App shell: bottom navigation for top-level destinations, the navigation
 * host, and the app-wide photo selection flow.
 *
 * Insets: this outer Scaffold only reserves space for the bottom bar (which
 * handles the system navigation bar itself). Each screen's own Scaffold handles
 * the status bar, so top bars can draw edge-to-edge without double padding.
 *
 * Needs the real [PlantIdApplication], so it has no Compose preview; preview
 * the individual screens instead.
 */
@Composable
fun PlantIdApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // Scoped to the activity, so the flow survives navigation between screens.
    photoSelection: PhotoSelectionViewModel = viewModel(factory = PhotoSelectionViewModel.Factory),
) {
    val container = (LocalContext.current.applicationContext as PlantIdApplication).container
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The bar is hidden on non-tab screens, such as the photo preview.
    val showBottomBar = currentDestination == null ||
        TopLevelDestination.entries.any { currentDestination.isTopLevel(it) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                PlantIdBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = navController::navigateToTopLevel,
                )
            }
        },
    ) { innerPadding ->
        PlantIdNavHost(
            navController = navController,
            container = container,
            onIdentifyPlant = photoSelection::openSourceChooser,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }

    PhotoSelectionHost(
        viewModel = photoSelection,
        onPhotoReady = navController::navigateToPhotoPreview,
    )
}

@Composable
private fun PlantIdBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isTopLevel(destination),
                onClick = { onNavigate(destination) },
                // Label is always visible and is what screen readers announce.
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
