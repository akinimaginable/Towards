package org.etrange.towards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.etrange.towards.navigation.HomeRoute
import org.etrange.towards.navigation.SettingsRoute
import org.etrange.towards.ui.home.HomeScreen
import org.etrange.towards.ui.home.HomeViewModel
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

    TowardsTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
            ) {
                composable<HomeRoute> {
                    val homeViewModel: HomeViewModel = viewModel { HomeViewModel() }
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

@Preview(name = "App · Light")
@Composable
private fun AppLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "",
            onDestinationChange = {},
            onOpenSettings = {},
        )
    }
}

/*@Preview(name = "App · Dark")
@Composable
private fun AppDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        HomeScreen(
            destination = "",
            onDestinationChange = {},
            onOpenSettings = {},
        )
    }
}*/
