package org.etrange.towards.ui.trip

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.search.PlaceSearchContent
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun LocationPickerScreen(
    viewModel: LocationPickerViewModel,
    editingOrigin: Boolean,
    counterpartName: String,
    onBack: () -> Unit,
    onPlacePicked: (TripEndpoint) -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LocationPickerScreen(
        editingOrigin = editingOrigin,
        counterpartName = counterpartName,
        query = query,
        suggestions = suggestions,
        history = history,
        shortcuts = viewModel.shortcuts,
        isLoading = isLoading,
        isLocating = isLocating,
        errorMessage = errorMessage,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onMyLocationClick = {
            viewModel.onMyLocationClick { endpoint ->
                if (endpoint != null) onPlacePicked(endpoint)
            }
        },
        onShortcutClick = { shortcut ->
            val endpoint = viewModel.onShortcutClick(shortcut)
            if (endpoint != null) onPlacePicked(endpoint)
        },
        onSuggestionClick = { result ->
            val endpoint = viewModel.onSuggestionClick(result)
            if (endpoint != null) onPlacePicked(endpoint)
        },
        onClearHistory = viewModel::clearHistory,
    )
}

/**
 * Full-screen place picker for an existing trip. It renders the same [PlaceSearchContent] as the
 * Home search sheet; the field that is not being edited shows the trip's other endpoint.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    editingOrigin: Boolean,
    counterpartName: String,
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onClearHistory: () -> Unit,
) {
    val originFocusRequester = remember { FocusRequester() }
    val destinationFocusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (editingOrigin) "Change starting point" else "Change destination")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = arrow_backIcon,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PlaceSearchContent(
            query = query,
            originText = if (editingOrigin) "" else counterpartName,
            destinationText = if (editingOrigin) counterpartName else "",
            pickingOrigin = editingOrigin,
            suggestions = suggestions,
            history = history,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            onQueryChange = onQueryChange,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            onClearHistory = onClearHistory,
            originFocusRequester = originFocusRequester,
            destinationFocusRequester = destinationFocusRequester,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Preview
@Composable
private fun LocationPickerScreenPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LocationPickerScreen(
            editingOrigin = true,
            counterpartName = "Grand Place",
            query = "",
            suggestions = emptyList(),
            history = listOf(
                GeocodeResult(
                    id = "place:gp",
                    kind = LocationKind.PLACE,
                    name = "Grand Place",
                    coordinate = Coordinate(50.8467, 4.3525),
                    street = "Grand Place",
                    country = "Belgium",
                ),
            ),
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
            ),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            onQueryChange = {},
            onBack = {},
            onMyLocationClick = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onClearHistory = {},
        )
    }
}

@Preview
@Composable
private fun LocationPickerScreenSuggestionsPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LocationPickerScreen(
            editingOrigin = false,
            counterpartName = "My location",
            query = "Grand",
            history = emptyList(),
            suggestions = listOf(
                GeocodeResult(
                    id = "place:gp",
                    kind = LocationKind.PLACE,
                    name = "Grand Place",
                    coordinate = Coordinate(50.8467, 4.3525),
                    street = "Grand Place",
                    country = "Belgium",
                ),
            ),
            shortcuts = emptyList(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            onQueryChange = {},
            onBack = {},
            onMyLocationClick = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onClearHistory = {},
        )
    }
}
