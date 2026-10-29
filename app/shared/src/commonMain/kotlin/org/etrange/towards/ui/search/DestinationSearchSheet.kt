package org.etrange.towards.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.layout.horizontalPanelWidth
import org.etrange.towards.ui.preview.previewSuggestions
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationSearchSheet(
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
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
    onClearHistory: () -> Unit,
    onAddShortcut: (() -> Unit)? = null,
    /** When set, search fills a left panel of this width and the rest of the window is a scrim. */
    panelWidth: Dp? = null,
) {
    val originFocusRequester = remember { FocusRequester() }
    val destinationFocusRequester = remember { FocusRequester() }
    if (panelWidth != null) {
        HorizontalDestinationSearch(
            panelWidth = panelWidth,
            query = query,
            suggestions = suggestions,
            history = history,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            originName = originName,
            pickingOrigin = pickingOrigin,
            onQueryChange = onQueryChange,
            onDismiss = onDismiss,
            onPickOrigin = onPickOrigin,
            onSearchDestination = onSearchDestination,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            onClearHistory = onClearHistory,
            onAddShortcut = onAddShortcut,
            originFocusRequester = originFocusRequester,
            destinationFocusRequester = destinationFocusRequester,
        )
    } else {
        PhoneDestinationSearch(
            query = query,
            suggestions = suggestions,
            history = history,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            originName = originName,
            pickingOrigin = pickingOrigin,
            onQueryChange = onQueryChange,
            onDismiss = onDismiss,
            onPickOrigin = onPickOrigin,
            onSearchDestination = onSearchDestination,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            onClearHistory = onClearHistory,
            onAddShortcut = onAddShortcut,
            originFocusRequester = originFocusRequester,
            destinationFocusRequester = destinationFocusRequester,
        )
    }
}

@Composable
private fun HorizontalDestinationSearch(
    panelWidth: Dp,
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
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
    onClearHistory: () -> Unit,
    onAddShortcut: (() -> Unit)?,
    originFocusRequester: FocusRequester,
    destinationFocusRequester: FocusRequester,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .width(panelWidth)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            color = MaterialTheme.colorScheme.background,
            shadowElevation = 6.dp,
        ) {
            PlaceSearchContent(
                query = query,
                originText = originName,
                destinationText = null,
                pickingOrigin = pickingOrigin,
                suggestions = suggestions,
                history = history,
                shortcuts = shortcuts,
                isLoading = isLoading,
                isLocating = isLocating,
                errorMessage = errorMessage,
                onQueryChange = onQueryChange,
                onMyLocationClick = onMyLocationClick,
                onShortcutClick = onShortcutClick,
                onAddShortcut = onAddShortcut,
                onSuggestionClick = onSuggestionClick,
                onClearHistory = onClearHistory,
                originFocusRequester = originFocusRequester,
                destinationFocusRequester = destinationFocusRequester,
                onPickOrigin = onPickOrigin,
                onPickDestination = onSearchDestination,
                modifier = Modifier.fillMaxSize().windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Start + WindowInsetsSides.Top + WindowInsetsSides.Bottom,
                    ),
                ),
            )
        }
        BoxScrim(onDismiss = onDismiss, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun BoxScrim(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            .clickable(onClick = onDismiss),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneDestinationSearch(
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
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
    onClearHistory: () -> Unit,
    onAddShortcut: (() -> Unit)?,
    originFocusRequester: FocusRequester,
    destinationFocusRequester: FocusRequester,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        PlaceSearchContent(
            query = query,
            originText = originName,
            destinationText = null,
            pickingOrigin = pickingOrigin,
            suggestions = suggestions,
            history = history,
            shortcuts = shortcuts,
            isLoading = isLoading,
            isLocating = isLocating,
            errorMessage = errorMessage,
            onQueryChange = onQueryChange,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onAddShortcut = onAddShortcut,
            onSuggestionClick = onSuggestionClick,
            onClearHistory = onClearHistory,
            originFocusRequester = originFocusRequester,
            destinationFocusRequester = destinationFocusRequester,
            onPickOrigin = onPickOrigin,
            onPickDestination = onSearchDestination,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}

@Preview(name = "Search · Wide", widthDp = 1200, heightDp = 800)
@Composable
private fun DestinationSearchSheetWidePreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            DestinationSearchSheet(
                query = "Grand",
                suggestions = previewSuggestions(),
                history = emptyList(),
                shortcuts = listOf(
                    DestinationShortcutItem(label = "Home", detail = "0 min", highlightDetail = true),
                    DestinationShortcutItem(label = "Work", detail = "17 min"),
                ),
                isLoading = false,
                isLocating = false,
                errorMessage = null,
                originName = "My location",
                pickingOrigin = false,
                onQueryChange = {},
                onDismiss = {},
                onPickOrigin = {},
                onSearchDestination = {},
                onMyLocationClick = {},
                onShortcutClick = {},
                onSuggestionClick = {},
                onClearHistory = {},
                panelWidth = horizontalPanelWidth(1200.dp),
            )
        }
    }
}
