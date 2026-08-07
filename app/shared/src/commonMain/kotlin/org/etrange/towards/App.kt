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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import org.etrange.towards.data.ApiConfig
import org.etrange.towards.data.ApiEndpointStore
import org.etrange.towards.data.HttpGeocoder
import org.etrange.towards.data.HttpTimetableProvider
import org.etrange.towards.data.createHttpClient
import org.etrange.towards.data.rememberLocationProvider
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.navigation.HomeRoute
import org.etrange.towards.navigation.SettingsRoute
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.home.HomeScreen
import org.etrange.towards.ui.home.HomeViewModel
import org.etrange.towards.ui.home.NearbyDeparture
import org.etrange.towards.ui.home.NearbyStop
import org.etrange.towards.ui.settings.SettingsScreen
import org.etrange.towards.ui.settings.SettingsViewModel
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.theme.TowardsTheme

@Composable
fun App() {
    val endpointStore = remember { ApiEndpointStore() }
    App(settingsViewModel = viewModel { SettingsViewModel(endpointStore) })
}

@Composable
fun App(
    settingsViewModel: SettingsViewModel,
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val apiConfig = remember(settingsViewModel) { ApiConfig(settingsViewModel.endpointStore) }
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
    val locationProvider = rememberLocationProvider()

    TowardsTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    val homeViewModel: HomeViewModel = viewModel {
                        HomeViewModel(geocoder, locationProvider, timetableProvider)
                    }
                    HomeScreen(
                        viewModel = homeViewModel,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
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

private fun previewNearbyStops(): List<NearbyStop> {
    val now = Clock.System.now()
    return listOf(
        NearbyStop(
            id = "stop:bourse",
            name = "Bourse",
            distanceMeters = 120,
            departures = listOf(
                NearbyDeparture(
                    id = "1",
                    lineName = "3",
                    headsign = "Churchill",
                    mode = TransportMode.SUBWAY,
                    routeColor = "FFDD00",
                    routeTextColor = "000000",
                    time = now + 3.minutes,
                    realTime = true,
                ),
            ),
        ),
    )
}
