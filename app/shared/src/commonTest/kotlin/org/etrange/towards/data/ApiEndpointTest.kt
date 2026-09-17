package org.etrange.towards.data

import com.russhwolf.settings.MapSettings
import org.etrange.towards.ui.theme.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiEndpointTest {

    @Test
    fun normalizeTrimsWhitespaceAndTrailingSlash() {
        assertEquals(
            "http://127.0.0.1:8081",
            normalizeApiEndpoint("  http://127.0.0.1:8081/  "),
        )
    }

    @Test
    fun validateAcceptsHttpAndHttpsUrlsWithHost() {
        assertEquals("http://10.0.2.2:8081", validateApiEndpoint("http://10.0.2.2:8081/"))
        assertEquals("https://api.example.com", validateApiEndpoint(" https://api.example.com "))
    }

    @Test
    fun validateRejectsInvalidEndpoints() {
        assertNull(validateApiEndpoint(""))
        assertNull(validateApiEndpoint("   "))
        assertNull(validateApiEndpoint("not-a-url"))
        assertNull(validateApiEndpoint("ftp://example.com"))
        assertNull(validateApiEndpoint("http://"))
    }
}

class SettingsStoreTest {

    @Test
    fun loadsPlatformDefaultWhenNothingPersisted() {
        val store = SettingsStore(
            settings = MapSettings(),
            defaultUrl = "http://127.0.0.1:8081",
        )

        assertEquals("http://127.0.0.1:8081", store.endpoint.value)
    }

    @Test
    fun persistsValidEndpointAndExposesItImmediately() {
        val settings = MapSettings()
        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        val result = store.save(" https://api.example.com/ ")

        assertTrue(result.isSuccess)
        assertEquals("https://api.example.com", result.getOrNull())
        assertEquals("https://api.example.com", store.endpoint.value)
        assertEquals(
            "https://api.example.com",
            settings.getString(SettingsStore.KEY_API_ENDPOINT, ""),
        )
    }

    @Test
    fun rejectsInvalidEndpointWithoutPersisting() {
        val settings = MapSettings()
        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        val result = store.save("not-a-url")

        assertTrue(result.isFailure)
        assertEquals("http://127.0.0.1:8081", store.endpoint.value)
        assertFalse(settings.hasKey(SettingsStore.KEY_API_ENDPOINT))
    }

    @Test
    fun reloadsPersistedEndpointOnNewStore() {
        val settings = MapSettings(
            SettingsStore.KEY_API_ENDPOINT to "https://api.example.com",
        )

        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        assertEquals("https://api.example.com", store.endpoint.value)
    }

    @Test
    fun fallsBackToDefaultWhenPersistedValueIsInvalid() {
        val settings = MapSettings(
            SettingsStore.KEY_API_ENDPOINT to "not-a-url",
        )

        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://10.0.2.2:8081/",
        )

        assertEquals("http://10.0.2.2:8081", store.endpoint.value)
    }

    @Test
    fun loadsSystemThemeWhenNothingPersisted() {
        val store = SettingsStore(
            settings = MapSettings(),
            defaultUrl = "http://127.0.0.1:8081",
        )

        assertEquals(ThemeMode.System, store.themeMode.value)
    }

    @Test
    fun persistsThemeModeAndExposesItImmediately() {
        val settings = MapSettings()
        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        store.saveThemeMode(ThemeMode.Dark)

        assertEquals(ThemeMode.Dark, store.themeMode.value)
        assertEquals(ThemeMode.Dark.name, settings.getString(SettingsStore.KEY_THEME_MODE, ""))
    }

    @Test
    fun reloadsPersistedThemeModeOnNewStore() {
        val settings = MapSettings(
            SettingsStore.KEY_THEME_MODE to ThemeMode.Light.name,
        )

        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        assertEquals(ThemeMode.Light, store.themeMode.value)
    }

    @Test
    fun fallsBackToSystemWhenPersistedThemeModeIsInvalid() {
        val settings = MapSettings(
            SettingsStore.KEY_THEME_MODE to "Midnight",
        )

        val store = SettingsStore(
            settings = settings,
            defaultUrl = "http://127.0.0.1:8081",
        )

        assertEquals(ThemeMode.System, store.themeMode.value)
    }
}

class ApiConfigLiveUpdateTest {

    @Test
    fun resolvesCurrentEndpointOnEachAccess() {
        val store = SettingsStore(
            settings = MapSettings(),
            defaultUrl = "http://127.0.0.1:8081",
        )
        val config = ApiConfig(store)

        assertEquals("http://127.0.0.1:8081", config.baseUrl)

        store.save("https://api.example.com")

        assertEquals("https://api.example.com", config.baseUrl)
    }
}
