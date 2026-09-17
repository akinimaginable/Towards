package org.etrange.towards.ui.settings

import com.russhwolf.settings.MapSettings
import org.etrange.towards.data.SettingsStore
import org.etrange.towards.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsViewModelEndpointTest {

    @Test
    fun saveUpdatesDraftAndClearsErrorOnValidEndpoint() {
        val viewModel = SettingsViewModel(
            settingsStore = SettingsStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange(" https://api.example.com/ ")
        val saved = viewModel.onApiEndpointSave()

        assertTrue(saved)
        assertEquals("https://api.example.com", viewModel.apiEndpointDraft.value)
        assertEquals("https://api.example.com", viewModel.apiEndpoint.value)
        assertNull(viewModel.apiEndpointError.value)
    }

    @Test
    fun saveKeepsDraftAndSetsErrorOnInvalidEndpoint() {
        val viewModel = SettingsViewModel(
            settingsStore = SettingsStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange("not-a-url")
        val saved = viewModel.onApiEndpointSave()

        assertFalse(saved)
        assertEquals("not-a-url", viewModel.apiEndpointDraft.value)
        assertEquals("http://127.0.0.1:8081", viewModel.apiEndpoint.value)
        assertEquals(SettingsStore.INVALID_ENDPOINT_MESSAGE, viewModel.apiEndpointError.value)
    }

    @Test
    fun draftChangeClearsPreviousError() {
        val viewModel = SettingsViewModel(
            settingsStore = SettingsStore(
                settings = MapSettings(),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onApiEndpointDraftChange("bad")
        viewModel.onApiEndpointSave()
        viewModel.onApiEndpointDraftChange("https://api.example.com")

        assertNull(viewModel.apiEndpointError.value)
    }

    @Test
    fun themeModeChangePersistsAndUpdatesViewModel() {
        val settings = MapSettings()
        val viewModel = SettingsViewModel(
            settingsStore = SettingsStore(
                settings = settings,
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        viewModel.onThemeModeChange(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, viewModel.themeMode.value)
        assertEquals(ThemeMode.Dark.name, settings.getString(SettingsStore.KEY_THEME_MODE, ""))
    }

    @Test
    fun loadsPersistedThemeMode() {
        val viewModel = SettingsViewModel(
            settingsStore = SettingsStore(
                settings = MapSettings(
                    SettingsStore.KEY_THEME_MODE to ThemeMode.Light.name,
                ),
                defaultUrl = "http://127.0.0.1:8081",
            ),
        )

        assertEquals(ThemeMode.Light, viewModel.themeMode.value)
    }
}
