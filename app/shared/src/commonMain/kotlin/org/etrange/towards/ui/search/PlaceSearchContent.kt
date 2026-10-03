package org.etrange.towards.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.icons.placeIcon
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.trip.LocationSearchField
import org.etrange.towards.ui.trip.LocationSearchPanel

/** Placeholder copy shared by every place search surface. */
internal const val OriginPlaceholder = "Starting point"
internal const val DestinationPlaceholder = "Destination"

/** Filled dot used wherever a trip's starting point is shown (search fields, trip header, map). */
@Composable
fun OriginMarker(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(24.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** Pin used wherever a trip's destination is shown. */
@Composable
fun DestinationMarker(modifier: Modifier = Modifier) {
    Icon(
        imageVector = placeIcon,
        contentDescription = null,
        modifier = modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.error,
    )
}

/**
 * The single place-search layout: a "starting point" field and a "destination" field (one of them
 * active), followed by the results panel. Used by the Home search sheet and the trip location picker.
 *
 * A field is only switchable when the matching `onPick…` callback is provided.
 */
@Composable
fun PlaceSearchContent(
    query: String,
    originText: String,
    destinationText: String?,
    pickingOrigin: Boolean,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    onQueryChange: (String) -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onClearHistory: () -> Unit,
    originFocusRequester: FocusRequester,
    destinationFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onPickOrigin: (() -> Unit)? = null,
    onPickDestination: (() -> Unit)? = null,
    autoFocus: Boolean = true,
) {
    Column(modifier = modifier) {
        LocationSearchField(
            query = if (pickingOrigin) query else "",
            onQueryChange = onQueryChange,
            placeholder = OriginPlaceholder,
            focusRequester = originFocusRequester,
            isLoading = pickingOrigin && isLoading,
            active = pickingOrigin,
            displayText = originText,
            autoFocus = autoFocus && pickingOrigin,
            switchable = onPickOrigin != null,
            leadingIcon = { OriginMarker() },
            onActivate = { onPickOrigin?.invoke() },
        )
        LocationSearchField(
            query = if (!pickingOrigin) query else "",
            onQueryChange = onQueryChange,
            placeholder = DestinationPlaceholder,
            focusRequester = destinationFocusRequester,
            isLoading = !pickingOrigin && isLoading,
            active = !pickingOrigin,
            displayText = destinationText,
            autoFocus = autoFocus && !pickingOrigin,
            switchable = onPickDestination != null,
            leadingIcon = { DestinationMarker() },
            onActivate = { onPickDestination?.invoke() },
        )
        LocationSearchPanel(
            query = query,
            suggestions = suggestions,
            history = history,
            shortcuts = shortcuts,
            isLocating = isLocating,
            errorMessage = errorMessage,
            showMyLocation = pickingOrigin,
            onMyLocationClick = onMyLocationClick,
            onShortcutClick = onShortcutClick,
            onSuggestionClick = onSuggestionClick,
            onClearHistory = onClearHistory,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceSearchContentPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            PlaceSearchContent(
                query = "",
                originText = "My location",
                destinationText = null,
                pickingOrigin = false,
                suggestions = emptyList(),
                history = emptyList(),
                shortcuts = listOf(
                    DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                    DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
                ),
                isLoading = false,
                isLocating = false,
                errorMessage = null,
                onQueryChange = {},
                onMyLocationClick = {},
                onShortcutClick = {},
                onSuggestionClick = {},
                onClearHistory = {},
                originFocusRequester = remember { FocusRequester() },
                destinationFocusRequester = remember { FocusRequester() },
                onPickOrigin = {},
                onPickDestination = {},
                autoFocus = false,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
