package org.etrange.towards.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.etrange.towards.ui.theme.ThemeMode

class SettingsViewModel : ViewModel() {
    private val _themeMode = MutableStateFlow(ThemeMode.System)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun onThemeModeChange(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
