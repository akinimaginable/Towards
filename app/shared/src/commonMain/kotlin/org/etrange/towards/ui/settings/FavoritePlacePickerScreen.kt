package org.etrange.towards.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.layout.ReadableContent
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.trip.LocationSearchField
import org.etrange.towards.ui.trip.LocationSearchPanel

@Composable
fun FavoritePlacePickerScreen(
    viewModel: FavoritePlacePickerViewModel,
    title: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    FavoritePlacePickerScreen(
        title = title,
        query = query,
        suggestions = suggestions,
        isLoading = isLoading,
        isLocating = isLocating,
        errorMessage = errorMessage,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onMyLocationClick = {
            viewModel.onMyLocationSelected { saved ->
                if (saved) onSaved()
            }
        },
        onSuggestionClick = { result ->
            if (viewModel.onSuggestionSelected(result)) onSaved()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritePlacePickerScreen(
    title: String,
    query: String,
    suggestions: List<GeocodeResult>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onMyLocationClick: () -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
        ReadableContent(Modifier.padding(innerPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            LocationSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "Search for a place",
                focusRequester = focusRequester,
                isLoading = isLoading,
                autoFocus = true,
            )
            LocationSearchPanel(
                query = query,
                suggestions = suggestions,
                history = emptyList(),
                shortcuts = emptyList(),
                isLocating = isLocating,
                errorMessage = errorMessage,
                showMyLocation = true,
                onMyLocationClick = onMyLocationClick,
                onShortcutClick = {},
                onSuggestionClick = onSuggestionClick,
                onClearHistory = {},
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritePlacePickerScreenPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            FavoritePlacePickerScreen(
                title = "Home",
                query = "",
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
                isLoading = false,
                isLocating = false,
                errorMessage = null,
                onQueryChange = {},
                onBack = {},
                onMyLocationClick = {},
                onSuggestionClick = {},
            )
        }
    }
}
