package org.etrange.towards.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.trip.LocationSearchField
import org.etrange.towards.ui.trip.LocationSearchPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSearchSheet(
    query: String,
    suggestions: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    originName: String,
    pickingOrigin: Boolean,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPickOrigin: () -> Unit,
    onSearchDestination: () -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val originFocusRequester = remember { FocusRequester() }
    val destinationFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        DestinationSearchSheetContent(
            query = query,
            suggestions = suggestions,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            originName = originName,
            pickingOrigin = pickingOrigin,
            onQueryChange = onQueryChange,
            onPickOrigin = onPickOrigin,
            onSearchDestination = onSearchDestination,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            originFocusRequester = originFocusRequester,
            destinationFocusRequester = destinationFocusRequester,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Composable
private fun DestinationSearchSheetContent(
    query: String,
    suggestions: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    originName: String,
    pickingOrigin: Boolean,
    onQueryChange: (String) -> Unit,
    onPickOrigin: () -> Unit,
    onSearchDestination: () -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    originFocusRequester: FocusRequester,
    destinationFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
) {
    Column(modifier = modifier) {
        LocationSearchField(
            query = if (pickingOrigin) query else "",
            onQueryChange = onQueryChange,
            placeholder = "From",
            focusRequester = originFocusRequester,
            isLoading = pickingOrigin && isLoading,
            active = pickingOrigin,
            displayText = originName,
            autoFocus = autoFocus && pickingOrigin,
            onActivate = onPickOrigin,
        )
        LocationSearchField(
            query = if (!pickingOrigin) query else "",
            onQueryChange = onQueryChange,
            placeholder = "Line or destination",
            focusRequester = destinationFocusRequester,
            isLoading = !pickingOrigin && isLoading,
            active = !pickingOrigin,
            autoFocus = autoFocus && !pickingOrigin,
            onActivate = onSearchDestination,
        )
        LocationSearchPanel(
            query = query,
            suggestions = suggestions,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            showMyLocation = pickingOrigin,
            onQueryChange = onQueryChange,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            showQueryField = false,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DestinationSearchSheetPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DestinationSearchSheetContent(
                query = "",
                suggestions = emptyList(),
                shortcuts = listOf(
                    DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                    DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
                ),
                isLoading = false,
                isLocating = false,
                errorMessage = null,
                originName = "My location",
                pickingOrigin = false,
                onQueryChange = {},
                onPickOrigin = {},
                onSearchDestination = {},
                onMyLocationClick = {},
                onShortcutClick = {},
                onSuggestionClick = {},
                originFocusRequester = remember { FocusRequester() },
                destinationFocusRequester = remember { FocusRequester() },
                autoFocus = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
