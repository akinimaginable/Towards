package org.etrange.towards.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val _destination = MutableStateFlow("")
    val destination: StateFlow<String> = _destination.asStateFlow()

    private val _shortcuts = MutableStateFlow(
        listOf(
            DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
            DestinationShortcutItem(label = "Work", detail = "17 min"),
            DestinationShortcutItem(label = "School", detail = "47 min"),
            DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
        ),
    )
    val shortcuts: StateFlow<List<DestinationShortcutItem>> = _shortcuts.asStateFlow()

    fun onDestinationChange(value: String) {
        _destination.value = value
    }

    fun onShortcutClick(shortcut: DestinationShortcutItem) {
        _destination.value = shortcut.label
    }
}
