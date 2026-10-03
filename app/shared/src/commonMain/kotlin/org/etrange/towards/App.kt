package org.etrange.towards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import org.etrange.towards.data.ApiConfig
import org.etrange.towards.data.SettingsStore
import org.etrange.towards.data.HttpGeocoder
import org.etrange.towards.data.HttpTimetableProvider
import org.etrange.towards.data.HttpTripPlanner
import org.etrange.towards.data.LocationBiasStore
import org.etrange.towards.data.SearchHistoryStore
import org.etrange.towards.data.createHttpClient
import org.etrange.towards.data.rememberLocationProvider
import org.etrange.towards.navigation.HomeRoute
import org.etrange.towards.navigation.LocationPickerRoute
import org.etrange.towards.navigation.SettingsRoute
import org.etrange.towards.navigation.TripResultsRoute
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.home.HomeScreen
import org.etrange.towards.ui.home.HomeViewModel
import org.etrange.towards.ui.preview.previewNearbyStops
import org.etrange.towards.ui.settings.SettingsScreen
import org.etrange.towards.ui.settings.SettingsViewModel
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.theme.TowardsTheme
import org.etrange.towards.ui.trip.LocationPickerScreen
import org.etrange.towards.ui.trip.LocationPickerViewModel
import org.etrange.towards.ui.trip.TripEndpoint
import org.etrange.towards.ui.trip.TripResultsScreen
import org.etrange.towards.ui.trip.TripResultsViewModel
import org.etrange.towards.ui.trip.counterpart
import org.etrange.towards.ui.trip.destination
import org.etrange.towards.ui.trip.origin
import org.etrange.towards.ui.trip.toPickerRoute
import org.etrange.towards.ui.trip.toResultsRoute
import org.etrange.towards.ui.trip.tripEndpointsAfterPicking

@Composable
fun App() {
    val endpointStore = remember { SettingsStore() }
    App(settingsViewModel = viewModel { SettingsViewModel(endpointStore) })
}

@Composable
fun App(
    settingsViewModel: SettingsViewModel,
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val apiConfig = remember(settingsViewModel) { ApiConfig(settingsViewModel.settingsStore) }
    val httpClient = remember { createHttpClient() }
    val geocoder = remember(apiConfig) {
        HttpGeocoder(
            client = httpClient,
            config = apiConfig,
        )
    }
    val timetableProvider = remember(apiConfig) {
        HttpTimetableProvider(
            client = httpClient,
            config = apiConfig,
        )
    }
    val tripPlanner = remember(apiConfig) {
        HttpTripPlanner(
            client = httpClient,
            config = apiConfig,
        )
    }
    val locationProvider = rememberLocationProvider()
    val locationBiasStore = remember { LocationBiasStore() }
    val searchHistoryStore = remember { SearchHistoryStore() }

    TowardsTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    val homeViewModel: HomeViewModel = viewModel {
                        HomeViewModel(
                            geocoder = geocoder,
                            locationProvider = locationProvider,
                            timetableProvider = timetableProvider,
                            locationBiasStore = locationBiasStore,
                            searchHistoryStore = searchHistoryStore,
                        )
                    }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onPlanTrip = { origin, destination ->
                            navController.navigate(origin.toResultsRoute(destination))
                        },
                    )
                }
                composable<TripResultsRoute> { entry ->
                    val route = entry.toRoute<TripResultsRoute>()
                    val tripViewModel: TripResultsViewModel = viewModel(
                        key = "${route.fromLatitude},${route.fromLongitude}:" +
                            "${route.toLatitude},${route.toLongitude}",
                    ) {
                        TripResultsViewModel(
                            tripPlanner = tripPlanner,
                            origin = route.origin(),
                            destination = route.destination(),
                            userLocation = if (locationProvider.hasPermission()) {
                                locationProvider.lastKnownCoordinate()
                            } else {
                                null
                            },
                        )
                    }
                    TripResultsScreen(
                        viewModel = tripViewModel,
                        onBack = { navController.popBackStack() },
                        onChangeOrigin = {
                            navController.navigate(
                                tripViewModel.origin.value.toPickerRoute(
                                    editingOrigin = true,
                                    counterpart = tripViewModel.destination.value,
                                ),
                            )
                        },
                        onChangeDestination = {
                            navController.navigate(
                                tripViewModel.destination.value.toPickerRoute(
                                    editingOrigin = false,
                                    counterpart = tripViewModel.origin.value,
                                ),
                            )
                        },
                    )
                }
                composable<LocationPickerRoute> { entry ->
                    val route = entry.toRoute<LocationPickerRoute>()
                    val pickerViewModel: LocationPickerViewModel = viewModel(
                        key = "picker:${route.editingOrigin}:${route.counterpartName}",
                    ) {
                        LocationPickerViewModel(
                            geocoder = geocoder,
                            locationProvider = locationProvider,
                            locationBiasStore = locationBiasStore,
                            searchHistoryStore = searchHistoryStore,
                            otherPlace = route.counterpart(),
                        )
                    }
                    LocationPickerScreen(
                        viewModel = pickerViewModel,
                        editingOrigin = route.editingOrigin,
                        counterpartName = route.counterpartName,
                        onBack = { navController.popBackStack() },
                        onPlacePicked = { picked ->
                            val (origin, destination) = tripEndpointsAfterPicking(
                                editingOrigin = route.editingOrigin,
                                picked = picked,
                                counterpart = route.counterpart(),
                            )
                            navController.navigate(origin.toResultsRoute(destination)) {
                                popUpTo<TripResultsRoute> { inclusive = true }
                            }
                        },
                    )
                }
                composable<SettingsRoute> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
                DestinationShortcutItem(label = "School", detail = "47 min"),
                DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
            ),
            suggestions = emptyList(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            nearbyStops = previewNearbyStops(),
            isLoadingNearby = false,
            nearbyMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onUseCurrentLocation = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun AppDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        HomeScreen(
            destination = "Centraal Station",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
                DestinationShortcutItem(label = "School", detail = "47 min"),
                DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
            ),
            suggestions = emptyList(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onUseCurrentLocation = {},
            onOpenSettings = {},
        )
    }
}

