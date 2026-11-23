package io.github.ephym6.plantid.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ephym6.plantid.BuildConfig
import io.github.ephym6.plantid.di.AppContainer
import io.github.ephym6.plantid.domain.plant.PlantCatalog
import io.github.ephym6.plantid.ui.about.AboutScreen
import io.github.ephym6.plantid.ui.care.CareGuideScreen
import io.github.ephym6.plantid.ui.care.CareGuideViewModel
import io.github.ephym6.plantid.ui.history.HistoryDetailScreen
import io.github.ephym6.plantid.ui.history.HistoryDetailViewModel
import io.github.ephym6.plantid.ui.history.HistoryScreen
import io.github.ephym6.plantid.ui.history.HistoryViewModel
import io.github.ephym6.plantid.ui.home.HomeScreen
import io.github.ephym6.plantid.ui.home.HomeViewModel
import io.github.ephym6.plantid.ui.identify.IdentificationScreen
import io.github.ephym6.plantid.ui.identify.IdentificationViewModel
import io.github.ephym6.plantid.ui.identify.PhotoPreviewScreen

/**
 * All app destinations, and the only place that wires screens to app
 * dependencies. Screens receive state and callbacks, never the NavController
 * or the [AppContainer].
 *
 * @param onIdentifyPlant opens the photo source chooser (handled at app level).
 */
@Composable
fun PlantIdNavHost(
    navController: NavHostController,
    container: AppContainer,
    onIdentifyPlant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> { entry ->
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container.historyRepository))
            val recent by viewModel.recent.collectAsStateWithLifecycle()
            HomeScreen(
                recent = recent,
                isDemo = container.demoScenarios != null,
                onIdentifyPlant = onIdentifyPlant,
                onOpenHistory = { entry.ifResumed { navController.navigateToTopLevel(TopLevelDestination.History) } },
                onOpenRecord = { id -> entry.ifResumed { navController.navigate(HistoryDetailRoute(id)) { launchSingleTop = true } } },
            )
        }
        composable<HistoryRoute> { entry ->
            val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container.historyRepository))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val clearFailed by viewModel.clearFailed.collectAsStateWithLifecycle()
            HistoryScreen(
                state = state,
                clearFailed = clearFailed,
                onIdentifyPlant = onIdentifyPlant,
                onOpenRecord = { id -> entry.ifResumed { navController.navigate(HistoryDetailRoute(id)) { launchSingleTop = true } } },
                onClearAll = viewModel::clearAll,
                onDismissClearError = viewModel::dismissClearError,
                onRetry = viewModel::retry,
            )
        }
        composable<HistoryDetailRoute> { entry ->
            val recordId = entry.toRoute<HistoryDetailRoute>().recordId
            val viewModel: HistoryDetailViewModel = viewModel(
                factory = HistoryDetailViewModel.factory(recordId, container.historyRepository),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val deleted by viewModel.deleted.collectAsStateWithLifecycle()
            val deleteFailed by viewModel.deleteFailed.collectAsStateWithLifecycle()
            HistoryDetailScreen(
                state = state,
                deleted = deleted,
                deleteFailed = deleteFailed,
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onViewCareGuide = { plantId -> entry.ifResumed { navController.navigate(CareGuideRoute(plantId)) { launchSingleTop = true } } },
                onDelete = viewModel::delete,
                onDismissDeleteError = viewModel::dismissDeleteError,
                onRetry = viewModel::retry,
            )
        }
        composable<AboutRoute> {
            val catalog by produceState<Result<PlantCatalog>?>(initialValue = null) {
                value = container.plantRepository.catalog()
            }
            AboutScreen(versionName = BuildConfig.VERSION_NAME, catalog = catalog)
        }
        composable<PhotoPreviewRoute> { entry ->
            val photoId = entry.toRoute<PhotoPreviewRoute>().photoId
            val demo = container.demoScenarios
            val demoScenario = demo?.scenario?.collectAsStateWithLifecycle()?.value
            PhotoPreviewScreen(
                photo = remember(photoId) { container.photoStore.fileFor(photoId) },
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onChooseAnother = onIdentifyPlant,
                onIdentify = { entry.ifResumed { navController.navigate(IdentificationRoute(photoId)) { launchSingleTop = true } } },
                demoScenario = demoScenario,
                onDemoScenarioSelected = { demo?.select(it) },
            )
        }
        composable<IdentificationRoute> { entry ->
            val photoId = entry.toRoute<IdentificationRoute>().photoId
            val photo = remember(photoId) { container.photoStore.fileFor(photoId) }
            val viewModel: IdentificationViewModel = viewModel(
                factory = IdentificationViewModel.factory(
                    photo,
                    container.identificationService,
                    container.historyRepository,
                ),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val saveState by viewModel.saveState.collectAsStateWithLifecycle()
            IdentificationScreen(
                state = state,
                photo = photo,
                saveState = saveState,
                onSave = viewModel::save,
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onRetry = viewModel::retry,
                onIdentifyAnother = onIdentifyPlant,
                onBackToHome = { entry.ifResumed { navController.popBackStack<HomeRoute>(inclusive = false) } },
                onViewCareGuide = { plantId -> entry.ifResumed { navController.navigate(CareGuideRoute(plantId)) { launchSingleTop = true } } },
            )
        }
        composable<CareGuideRoute> { entry ->
            val plantId = entry.toRoute<CareGuideRoute>().plantId
            val viewModel: CareGuideViewModel = viewModel(
                factory = CareGuideViewModel.factory(plantId, container.plantRepository),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            CareGuideScreen(
                state = state,
                onBack = { entry.ifResumed { navController.popBackStack() } },
                onRetry = viewModel::retry,
            )
        }
    }
}

/**
 * Switches bottom-bar tab. Keeps only Home underneath, so Back from any tab
 * returns to Home and Back from Home leaves the app. Each tab's state is saved
 * and restored when switching.
 */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Opens the preview for a newly selected photo. An existing preview, and any
 * result above it ("Identify another plant"), is replaced, so Back still goes
 * to where the flow started.
 */
fun NavHostController.navigateToPhotoPreview(photoId: String) {
    navigate(PhotoPreviewRoute(photoId)) {
        popUpTo<PhotoPreviewRoute> { inclusive = true }
    }
}

/**
 * Runs [action] only while this destination is the resumed, top screen. A
 * second tap during a navigation animation is ignored, so double-tapping Back
 * can't pop two screens and a double tap can't navigate twice (Sprint 6).
 */
internal inline fun NavBackStackEntry.ifResumed(action: () -> Unit) {
    if (lifecycle.currentState == Lifecycle.State.RESUMED) action()
}

/** True if this destination is [destination] or nested inside it. */
fun NavDestination?.isTopLevel(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
