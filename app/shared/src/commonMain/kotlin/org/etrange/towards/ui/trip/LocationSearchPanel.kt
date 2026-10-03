package org.etrange.towards.ui.trip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.ui.home.DestinationShortcut
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.home.subtitle
import org.etrange.towards.ui.icons.myLocationIcon
import org.etrange.towards.ui.icons.searchIcon
import org.etrange.towards.ui.search.SearchTextField
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun LocationSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isLoading: Boolean = false,
    active: Boolean = true,
    displayText: String? = null,
    autoFocus: Boolean = false,
    /** When false, the field is display-only while [active] is false (it can't take over editing). */
    switchable: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    onActivate: () -> Unit = {},
) {
    // The field owns its text while active. The external query is applied only when it differs
    // from what this field last reported, so typing never fights the echoed state.
    var fieldValue by remember(active) { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    var reportedText by remember(active) { mutableStateOf(query) }
    var hasEdited by remember(active) { mutableStateOf(false) }

    LaunchedEffect(active, query) {
        if (!active || query == reportedText) return@LaunchedEffect
        reportedText = query
        fieldValue = TextFieldValue(query, TextRange(query.length))
    }

    LaunchedEffect(active, autoFocus, focusRequester) {
        if (!active || !autoFocus || focusRequester == null) return@LaunchedEffect
        yield()
        if (!hasEdited) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val fieldModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .then(
            if (focusRequester != null) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            },
        )
        .onFocusChanged { focusState ->
            if (focusState.isFocused && !active) {
                onActivate()
            }
        }

    if (active) {
        SearchTextField(
            value = fieldValue,
            onValueChange = { incoming ->
                hasEdited = true
                fieldValue = incoming
                if (incoming.text != reportedText) {
                    reportedText = incoming.text
                    onQueryChange(incoming.text)
                }
            },
            placeholder = placeholder,
            isLoading = isLoading,
            modifier = fieldModifier,
            leadingIcon = leadingIcon ?: { DefaultSearchLeadingIcon() },
        )
    } else {
        SearchTextField(
            value = TextFieldValue(displayText.orEmpty()),
            onValueChange = {},
            placeholder = placeholder,
            isLoading = false,
            modifier = fieldModifier,
            readOnly = !switchable,
            leadingIcon = leadingIcon ?: { DefaultSearchLeadingIcon() },
        )
    }
}

@Composable
private fun DefaultSearchLeadingIcon() {
    Icon(
        imageVector = searchIcon,
        contentDescription = "Search",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun LocationSearchPanel(
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLocating: Boolean,
    errorMessage: String?,
    showMyLocation: Boolean,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            locationSearchResults(
                query = query,
                suggestions = suggestions,
                history = history,
                shortcuts = shortcuts,
                isLocating = isLocating,
                showMyLocation = showMyLocation,
                onMyLocationClick = onMyLocationClick,
                onShortcutClick = onShortcutClick,
                onSuggestionClick = onSuggestionClick,
                onClearHistory = onClearHistory,
            )
        }
    }
}

fun LazyListScope.locationSearchResults(
    query: String,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLocating: Boolean,
    showMyLocation: Boolean,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onClearHistory: () -> Unit,
) {
    if (query.isBlank()) {
        if (showMyLocation) {
            item(key = "my-location") {
                Button(
                    onClick = onMyLocationClick,
                    enabled = !isLocating,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = myLocationIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My location",
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Use your current position",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (shortcuts.isNotEmpty()) {
            item(key = "shortcuts") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (shortcut in shortcuts) {
                        DestinationShortcut(
                            label = shortcut.label,
                            detail = shortcut.detail,
                            onClick = { onShortcutClick(shortcut) },
                            highlightDetail = shortcut.highlightDetail,
                        )
                    }
                }
            }
        }

        item(key = "divider") {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        }

        if (history.isNotEmpty()) {
            item(key = "recent-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearHistory) {
                        Text("Clear")
                    }
                }
            }
            itemsIndexed(
                items = history,
                key = { index, result -> "recent:${placeResultKey(index, result)}" },
            ) { _, result ->
                PlaceResultRow(result = result, onClick = { onSuggestionClick(result) })
            }
        }
    }

    itemsIndexed(
        items = suggestions,
        key = { index, result -> placeResultKey(index, result) },
    ) { _, result ->
        PlaceResultRow(result = result, onClick = { onSuggestionClick(result) })
    }
}

@Composable
private fun PlaceResultRow(
    result: GeocodeResult,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = result.name,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = result.subtitle(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun placeResultKey(index: Int, result: GeocodeResult): String =
    result.id.ifBlank {
        "geo:$index:${result.kind}:${result.coordinate.latitude}," +
            "${result.coordinate.longitude}:${result.name}"
    }

@Preview(showBackground = true)
@Composable
private fun LocationSearchPanelPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LocationSearchPanel(
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
                isLocating = false,
                errorMessage = null,
                showMyLocation = true,
                onMyLocationClick = {},
                onShortcutClick = {},
                onSuggestionClick = {},
                onClearHistory = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchPanelSuggestionsPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LocationSearchPanel(
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
                history = emptyList(),
                shortcuts = emptyList(),
                isLocating = false,
                errorMessage = null,
                showMyLocation = false,
                onMyLocationClick = {},
                onShortcutClick = {},
                onSuggestionClick = {},
                onClearHistory = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

