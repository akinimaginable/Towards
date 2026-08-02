package org.etrange.towards.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.data.rememberLocationPermissionLauncher
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val locationBias by viewModel.locationBias.collectAsStateWithLifecycle()
    var pendingLocationRequest by remember { mutableStateOf(false) }
    val requestLocationPermission = rememberLocationPermissionLauncher { granted ->
        if (pendingLocationRequest) {
            pendingLocationRequest = false
            if (granted) {
                viewModel.onUseCurrentLocation()
            } else {
                viewModel.onLocationPermissionDenied()
            }
        }
    }

    HomeScreen(
        destination = destination,
        shortcuts = shortcuts,
        suggestions = suggestions,
        isLoading = isLoading,
        isLocating = isLocating,
        errorMessage = errorMessage,
        mapCenter = selected?.coordinate ?: locationBias,
        onDestinationChange = viewModel::onDestinationChange,
        onShortcutClick = viewModel::onShortcutClick,
        onSuggestionClick = viewModel::onSuggestionClick,
        onUseCurrentLocation = {
            pendingLocationRequest = true
            requestLocationPermission()
        },
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    destination: String,
    shortcuts: List<DestinationShortcutItem>,
    suggestions: List<GeocodeResult>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    mapCenter: Coordinate? = null,
    onDestinationChange: (String) -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val topPadding = innerPadding.calculateTopPadding()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val mapPeekHeight = maxHeight / 3
            val shortcutsHeight = 48.dp
            val searchHeight = 56.dp
            val sectionSpacing = 4.dp
            val overlayHeight = shortcutsHeight + sectionSpacing + searchHeight
            val mapHeight = topPadding + mapPeekHeight + overlayHeight
            val horizontalPadding = innerPadding.calculateStartPadding(layoutDirection)

            HomeMap(
                center = mapCenter,
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = overlayHeight,
                ),
                modifier = Modifier.fillMaxWidth().height(mapHeight).align(Alignment.TopCenter),
            )

            Column(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .padding(top = topPadding + mapPeekHeight).padding(
                        start = horizontalPadding,
                        end = innerPadding.calculateEndPadding(layoutDirection),
                    ),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(shortcutsHeight)
                        .horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Button(
                        onClick = onUseCurrentLocation,
                        enabled = !isLocating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("My location")
                        }
                    }

                    for (shortcut in shortcuts) {
                        DestinationShortcut(
                            label = shortcut.label,
                            detail = shortcut.detail,
                            onClick = { onShortcutClick(shortcut) },
                            highlightDetail = shortcut.highlightDetail,
                        )
                    }

                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(start = 8.dp, end = 14.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            Text("Add shortcut")
                        }
                    }
                }

                TextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    modifier = Modifier.fillMaxWidth().height(searchHeight)
                        .padding(horizontal = 12.dp),
                    placeholder = {
                        Text(
                            text = "Search here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(32.dp),
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

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(top = mapHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ),
            ) {
                if (errorMessage != null) {
                    item(key = "error") {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                items(suggestions, key = { it.id }) { result ->
                    Button(
                        onClick = { onSuggestionClick(result) },
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.name,
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                )
                                Text(result.subtitle())
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun GeocodeResult.subtitle(): String {
    val address = listOfNotNull(
        listOfNotNull(street, houseNumber).joinToString(" ").ifBlank { null },
        postalCode,
        country,
    ).joinToString(", ")
    return address.ifBlank { kind.name.lowercase().replaceFirstChar { it.titlecase() } }
}

private fun previewSuggestions() = listOf(
    GeocodeResult(
        id = "stop:bru",
        kind = LocationKind.STOP,
        name = "Bruxelles-Central",
        coordinate = Coordinate(50.8453, 4.3570),
        country = "Belgium",
    ),
    GeocodeResult(
        id = "place:gp",
        kind = LocationKind.PLACE,
        name = "Grand Place",
        coordinate = Coordinate(50.8467, 4.3525),
        street = "Grand Place",
        country = "Belgium",
    ),
    GeocodeResult(
        id = "place:gp",
        kind = LocationKind.PLACE,
        name = "Grand Place",
        coordinate = Coordinate(50.8467, 4.3525),
        street = "Grand Place",
        country = "Belgium",
    ),
)

@Preview
@Composable
private fun HomeScreenLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "Centraal Station",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
                DestinationShortcutItem(label = "School", detail = "47 min"),
                DestinationShortcutItem(label = "Grand Place", detail = "7 min"),
            ),
            suggestions = previewSuggestions(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onUseCurrentLocation = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenNoShortcutsLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "Centraal Station",
            shortcuts = emptyList(),
            suggestions = previewSuggestions(),
            isLoading = true,
            isLocating = true,
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onUseCurrentLocation = {},
            onOpenSettings = {},
        )
    }
}
