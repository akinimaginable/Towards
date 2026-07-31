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
import org.etrange.towards.data.ApiConfig
import org.etrange.towards.data.HttpGeocoder
import org.etrange.towards.data.createHttpClient
import org.etrange.towards.data.defaultApiBaseUrl
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.navigation.HomeRoute
import org.etrange.towards.navigation.SettingsRoute
import org.etrange.towards.ui.home.HomeScreen
import org.etrange.towards.ui.home.HomeViewModel
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.settings.SettingsScreen
import org.etrange.towards.ui.settings.SettingsViewModel
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.theme.TowardsTheme

@Composable
fun App() {
    App(
        settingsViewModel = viewModel { SettingsViewModel() },
    )
}

@Composable
fun App(
    settingsViewModel: SettingsViewModel,
) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val geocoder = remember {
        HttpGeocoder(
            client = createHttpClient(),
            config = ApiConfig(baseUrl = defaultApiBaseUrl()),
        )
    }

    TowardsTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(geocoder) }
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
            destination = "Centraal Station",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
                DestinationShortcutItem(label = "School", detail = "47 min"),
                DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
            ),
            suggestions = listOf(
                GeocodeResult(
                    id = "stop:bru",
                    kind = LocationKind.STOP,
                    name = "Bruxelles-Central",
                    coordinate = Coordinate(50.8453, 4.3570),
                ),
            ),
            isLoading = false,
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
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
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onOpenSettings = {},
        )
    }
}
