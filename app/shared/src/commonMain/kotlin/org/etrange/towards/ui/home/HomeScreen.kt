package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.data.rememberLocationPermissionLauncher
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.icons.settingsIcon
import org.etrange.towards.ui.search.DestinationSearchSheet
import org.etrange.towards.ui.preview.previewNearbyStops
import org.etrange.towards.ui.preview.previewSuggestions
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.trip.TripEndpoint

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
    onPlanTrip: (origin: TripEndpoint, destination: TripEndpoint) -> Unit,
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val locationBias by viewModel.locationBias.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val followMap by viewModel.followMap.collectAsStateWithLifecycle()
    val focusedPlaceLabel by viewModel.focusedPlaceLabel.collectAsStateWithLifecycle()
    val nearbyStops by viewModel.nearbyStops.collectAsStateWithLifecycle()
    val isLoadingNearby by viewModel.isLoadingNearby.collectAsStateWithLifecycle()
    val nearbyMessage by viewModel.nearbyMessage.collectAsStateWithLifecycle()
    val searchTarget by viewModel.searchTarget.collectAsStateWithLifecycle()

    var pendingLocationRequest by remember { mutableStateOf(false) }
    var autoPermissionRequested by rememberSaveable { mutableStateOf(false) }

    val requestLocationPermission = rememberLocationPermissionLauncher { granted ->
        if (pendingLocationRequest) {
            pendingLocationRequest = false
            if (granted) {
                viewModel.onUseCurrentLocation()
            } else {
                viewModel.onLocationPermissionDenied(fromUserAction = true)
            }
        } else {
            if (granted) {
                viewModel.onLocationPermissionGranted()
            } else {
                viewModel.onLocationPermissionDenied(fromUserAction = false)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!autoPermissionRequested && !viewModel.hasLocationPermission()) {
            autoPermissionRequested = true
            requestLocationPermission()
        } else if (viewModel.hasLocationPermission()) {
            autoPermissionRequested = true
        }
    }

    HomeScreen(
        destination = destination,
        shortcuts = shortcuts,
        suggestions = suggestions,
        isLoading = isLoading,
        isLocating = isLocating,
        errorMessage = errorMessage,
        mapCenter = locationBias,
        userLocation = userLocation,
        followMap = followMap,
        focusedPlaceLabel = focusedPlaceLabel,
        nearbyStops = nearbyStops,
        isLoadingNearby = isLoadingNearby,
        nearbyMessage = nearbyMessage,
        originName = origin.name,
        onDestinationChange = viewModel::onDestinationChange,
        onUserMovedCamera = viewModel::onUserMovedCamera,
        onMapCameraIdle = viewModel::onMapCameraIdle,
        onShortcutClick = { shortcut ->
            viewModel.onShortcutSelected(shortcut)?.let { (origin, destination) ->
                onPlanTrip(origin, destination)
            }
        },
        onSuggestionClick = { result ->
            viewModel.onSuggestionSelected(result)?.let { (origin, destination) ->
                onPlanTrip(origin, destination)
            }
        },
        onNearbyStopClick = { stop ->
            viewModel.onNearbyStopSelected(stop)?.let { (origin, destination) ->
                onPlanTrip(origin, destination)
            }
        },
        onUseCurrentLocation = {
            pendingLocationRequest = true
            requestLocationPermission()
        },
        onOpenSettings = onOpenSettings,
        searchSheetVisible = searchTarget != null,
        pickingOrigin = searchTarget == HomeSearchTarget.Origin,
        onOpenSearch = viewModel::openSearch,
        onPickOrigin = viewModel::pickOrigin,
        onSearchDestination = viewModel::searchDestination,
        onMyLocationAsOrigin = {
            pendingLocationRequest = true
            requestLocationPermission()
            viewModel.returnToDestinationSearch()
        },
        onDismissSearch = viewModel::dismissSearch,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    destination: String,
    shortcuts: List<DestinationShortcutItem>,
    suggestions: List<GeocodeResult>,
    isLoading: Boolean,
    isLocating: Boolean,
    errorMessage: String?,
    mapCenter: Coordinate? = null,
    userLocation: Coordinate? = null,
    followMap: Boolean = true,
    focusedPlaceLabel: String? = null,
    nearbyStops: List<NearbyStop> = emptyList(),
    isLoadingNearby: Boolean = false,
    nearbyMessage: String? = null,
    originName: String = "My location",
    onDestinationChange: (String) -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onNearbyStopClick: (NearbyStop) -> Unit = {},
    onUserMovedCamera: () -> Unit = {},
    onMapCameraIdle: (Coordinate) -> Unit = {},
    searchSheetVisible: Boolean = false,
    pickingOrigin: Boolean = false,
    onOpenSearch: () -> Unit = {},
    onPickOrigin: () -> Unit = {},
    onSearchDestination: () -> Unit = {},
    onMyLocationAsOrigin: () -> Unit = {},
    onDismissSearch: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = settingsIcon,
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

            val density = LocalDensity.current
            // Height of the shortcuts + search overlay as actually laid out. It grows with the
            // system font size, so the map and list insets follow it instead of fixed constants.
            var measuredOverlayHeight by remember(density) { mutableStateOf<Dp?>(null) }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val mapPeekHeight = maxHeight / 5
                val shortcutsMinHeight = 48.dp
                val searchMinHeight = 56.dp
                val sectionSpacing = 4.dp
                val searchListOverlap = 16.dp
                val overlayHeight = measuredOverlayHeight
                    ?: (shortcutsMinHeight + sectionSpacing + searchMinHeight)
                val mapHeight =
                    (topPadding + mapPeekHeight + overlayHeight).coerceAtMost(maxHeight * 0.58f)
                val mapBodyHeight = (mapHeight - searchListOverlap).coerceAtLeast(0.dp)
                val overlayOnMapHeight = (overlayHeight - searchListOverlap).coerceAtLeast(0.dp)
                val horizontalPadding = innerPadding.calculateStartPadding(layoutDirection)

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(mapBodyHeight).clipToBounds(),
                        ) {
                            HomeMap(
                                center = mapCenter,
                                userLocation = userLocation,
                                followCenter = followMap,
                                onUserMovedCamera = onUserMovedCamera,
                                onMapCameraIdle = onMapCameraIdle,
                                contentPadding = PaddingValues(
                                    top = topPadding,
                                    bottom = overlayOnMapHeight,
                                ),
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(
                                start = innerPadding.calculateStartPadding(layoutDirection),
                                end = innerPadding.calculateEndPadding(layoutDirection),
                                top = searchListOverlap,
                                bottom = innerPadding.calculateBottomPadding(),
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

                            nearbyStopsSection(
                                nearbyStops = nearbyStops,
                                isLoadingNearby = isLoadingNearby,
                                nearbyMessage = nearbyMessage,
                                onStopClick = onNearbyStopClick,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(
                            top = mapHeight - overlayHeight,
                            start = horizontalPadding,
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        ).onSizeChanged { size ->
                            val measured = with(density) { size.height.toDp() }
                            if (measured != measuredOverlayHeight) measuredOverlayHeight = measured
                        },
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = shortcutsMinHeight)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
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

                        SearchLaunchBar(
                            modifier = Modifier.fillMaxWidth().heightIn(min = searchMinHeight)
                                .padding(horizontal = 12.dp),
                            onClick = onOpenSearch,
                            onUseCurrentLocation = onUseCurrentLocation,
                            followMap = followMap,
                            isLocating = isLocating,
                            focusedPlaceLabel = focusedPlaceLabel,
                        )
                    }
                }
            }
        }

        if (searchSheetVisible) {
            DestinationSearchSheet(
                query = destination,
                suggestions = suggestions,
                shortcuts = shortcuts,
                isLoading = isLoading,
                isLocating = isLocating,
                errorMessage = errorMessage,
                originName = originName,
                pickingOrigin = pickingOrigin,
                onQueryChange = onDestinationChange,
                onDismiss = onDismissSearch,
                onPickOrigin = onPickOrigin,
                onSearchDestination = onSearchDestination,
                onMyLocationClick = onMyLocationAsOrigin,
                onShortcutClick = onShortcutClick,
                onSuggestionClick = onSuggestionClick,
            )
        }
    }
}

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
private fun HomeScreenNearbyPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "now", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
            ),
            suggestions = emptyList(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            nearbyStops = previewNearbyStops(),
            isLoadingNearby = false,
            nearbyMessage = null,
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
