package org.etrange.towards.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    HomeScreen(
        destination = destination,
        shortcuts = shortcuts,
        suggestions = suggestions,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onDestinationChange = viewModel::onDestinationChange,
        onShortcutClick = viewModel::onShortcutClick,
        onSuggestionClick = viewModel::onSuggestionClick,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun HomeScreen(
    destination: String,
    shortcuts: List<DestinationShortcutItem>,
    suggestions: List<GeocodeResult>,
    isLoading: Boolean,
    errorMessage: String?,
    onDestinationChange: (String) -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Towards", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        BoxWithConstraints(
            modifier = Modifier.padding(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                ).fillMaxSize()
        ) {
            val halfScreenHeight = maxHeight / 3

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                // Placeholder for future map / upper content; scrolls away continuously.
                item(key = "half_screen_spacer") {
                    Spacer(modifier = Modifier.height(halfScreenHeight))
                }

                item(key = "shortcuts") {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
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
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Add shortcut")
                            }
                        }
                    }
                }

                item(key = "destination_field") {
                    TextField(
                        value = destination,
                        onValueChange = onDestinationChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        placeholder = {
                            Text(
                                text = "Towards",
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

                if (errorMessage != null) {
                    item(key = "error") {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                items(suggestions, key = { it.id }) { result ->
                    Button(
                        onClick = { onSuggestionClick(result) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(32.dp),
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
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
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
            errorMessage = null,
            onDestinationChange = {},
            onShortcutClick = {},
            onSuggestionClick = {},
            onOpenSettings = {},
        )
    }
}
