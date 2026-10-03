package org.etrange.towards.ui.search

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.home.DestinationShortcutItem

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
