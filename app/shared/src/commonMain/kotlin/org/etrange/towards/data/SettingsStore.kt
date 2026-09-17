package org.etrange.towards.data

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.etrange.towards.ui.theme.ThemeMode

class SettingsStore(
    private val settings: Settings = Settings(),
    private val defaultUrl: String = defaultApiBaseUrl(),
) {
    private val _endpoint = MutableStateFlow(loadPersistedEndpointOrDefault())
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    private val _themeMode = MutableStateFlow(loadPersistedThemeModeOrDefault())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun save(raw: String): Result<String> {
        val normalized = validateApiEndpoint(raw)
            ?: return Result.failure(IllegalArgumentException(INVALID_ENDPOINT_MESSAGE))
        settings[KEY_API_ENDPOINT] = normalized
        _endpoint.value = normalized
        return Result.success(normalized)
    }

    fun saveThemeMode(mode: ThemeMode) {
        settings[KEY_THEME_MODE] = mode.name
        _themeMode.value = mode
    }

    private fun loadPersistedEndpointOrDefault(): String {
        val saved = settings.getStringOrNull(KEY_API_ENDPOINT) ?: return normalizeApiEndpoint(defaultUrl)
        return validateApiEndpoint(saved) ?: normalizeApiEndpoint(defaultUrl)
    }

    private fun loadPersistedThemeModeOrDefault(): ThemeMode {
        val saved = settings.getStringOrNull(KEY_THEME_MODE) ?: return ThemeMode.System
        return ThemeMode.entries.find { it.name == saved } ?: ThemeMode.System
    }

    companion object {
        const val KEY_API_ENDPOINT = "api_endpoint"
        const val KEY_THEME_MODE = "theme_mode"
        const val INVALID_ENDPOINT_MESSAGE =
            "Enter a valid http or https URL with a host, for example http://127.0.0.1:8081"
    }
}
