package org.etrange.towards.ui.trip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.yield
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.home.DestinationShortcut
import org.etrange.towards.ui.home.DestinationShortcutItem
import org.etrange.towards.ui.home.subtitle
import org.etrange.towards.ui.icons.myLocationIcon
import org.etrange.towards.ui.icons.searchIcon

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
    onActivate: () -> Unit = {},
) {
    var fieldValue by remember(active) { mutableStateOf(TextFieldValue(query)) }
    var hasEdited by remember(active) { mutableStateOf(false) }

    LaunchedEffect(active, query) {
        if (!active) return@LaunchedEffect
        val reconciled = reconcileSearchQuery(fieldValue.text, query)
        if (reconciled != fieldValue.text) {
            fieldValue = TextFieldValue(reconciled, TextRange(reconciled.length))
        }
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
                val merged = mergeSearchInput(fieldValue.text, incoming.text)
                fieldValue = if (merged == incoming.text) {
                    incoming
                } else {
                    TextFieldValue(merged, TextRange(merged.length))
                }
                if (merged != query) {
                    onQueryChange(merged)
                }
            },
            placeholder = placeholder,
            isLoading = isLoading,
            readOnly = false,
            modifier = fieldModifier,
        )
    } else {
        SearchTextField(
            value = TextFieldValue(displayText.orEmpty()),
            onValueChange = {},
            placeholder = placeholder,
            isLoading = false,
            readOnly = true,
            modifier = fieldModifier,
        )
    }
}

@Composable
private fun SearchTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    isLoading: Boolean,
    readOnly: Boolean,
    modifier: Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        readOnly = readOnly,
        placeholder = {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = searchIcon,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun LocationSearchPanel(
    query: String,
    suggestions: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    showMyLocation: Boolean,
    onQueryChange: (String) -> Unit,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    placeholder: String = "Stop, address, or place",
    showQueryField: Boolean = true,
) {
    Column(modifier = modifier) {
        if (showQueryField) {
            LocationSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                focusRequester = focusRequester,
                isLoading = isLoading,
                autoFocus = focusRequester != null,
            )
        }

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
                shortcuts = shortcuts,
                isLocating = isLocating,
                showMyLocation = showMyLocation,
                onMyLocationClick = onMyLocationClick,
                onShortcutClick = onShortcutClick,
                onSuggestionClick = onSuggestionClick,
            )
        }
    }
}

fun LazyListScope.locationSearchResults(
    query: String,
    suggestions: List<GeocodeResult>,
    shortcuts: List<DestinationShortcutItem>,
    isLocating: Boolean,
    showMyLocation: Boolean,
    onMyLocationClick: () -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
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
    }

    itemsIndexed(
        items = suggestions,
        key = { index, result ->
            result.id.ifBlank {
                "geo:$index:${result.kind}:${result.coordinate.latitude}," +
                    "${result.coordinate.longitude}:${result.name}"
            }
        },
    ) { _, result ->
        Button(
            onClick = { onSuggestionClick(result) },
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
}

internal fun mergeSearchInput(previous: String, incoming: String): String {
    if (incoming == previous) return incoming
    if (incoming.startsWith(previous) || previous.startsWith(incoming)) return incoming
    if (incoming.endsWith(previous) && incoming.length > previous.length) {
        return previous + incoming.removeSuffix(previous)
    }
    return incoming
}

internal fun reconcileSearchQuery(fieldText: String, externalQuery: String): String {
    if (externalQuery == fieldText) return fieldText
    val inFlightTyping = fieldText.isNotEmpty() &&
        externalQuery.isNotEmpty() &&
        (fieldText.startsWith(externalQuery) || externalQuery.startsWith(fieldText))
    return if (inFlightTyping) fieldText else externalQuery
}

