package org.etrange.towards.ui.trip

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun LocationPickerScreen(
    viewModel: LocationPickerViewModel,
    editingOrigin: Boolean,
    onBack: () -> Unit,
    onPlacePicked: (TripEndpoint) -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LocationPickerScreen(
        editingOrigin = editingOrigin,
        query = query,
        suggestions = suggestions,
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    editingOrigin: Boolean,
    query: String,
    suggestions: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (editingOrigin) "Starting from" else "Going to")
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
        LocationSearchPanel(
            query = query,
            suggestions = suggestions,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            showMyLocation = editingOrigin,
            onQueryChange = onQueryChange,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            focusRequester = focusRequester,
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
            query = "",
            suggestions = emptyList(),
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
        )
    }
}

@Preview
@Composable
private fun LocationPickerScreenSuggestionsPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LocationPickerScreen(
            editingOrigin = false,
            query = "Grand",
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
        )
    }
}
