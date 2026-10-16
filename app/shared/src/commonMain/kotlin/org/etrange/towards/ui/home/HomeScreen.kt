package org.etrange.towards.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.data.rememberLocationPermissionLauncher
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.ui.icons.closeIcon
import org.etrange.towards.ui.icons.settingsIcon
import org.etrange.towards.ui.preview.previewLinePreview
import org.etrange.towards.ui.search.DestinationSearchSheet
import org.etrange.towards.ui.settings.AddPlaceLabelDialog
import org.etrange.towards.ui.preview.previewNearbyStops
import org.etrange.towards.ui.preview.previewSuggestions
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import org.etrange.towards.ui.trip.TripEndpoint
import kotlinx.coroutines.launch
import org.etrange.towards.ui.home.nearby.LinePreview
import org.etrange.towards.ui.home.nearby.LineSheet
import org.etrange.towards.ui.home.nearby.NearbyDeparture
import org.etrange.towards.ui.home.nearby.NearbyStop
import org.etrange.towards.ui.home.nearby.nearbyStopsSection

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSettings: () -> Unit,
    onPlanTrip: (origin: TripEndpoint, destination: TripEndpoint) -> Unit,
    onAddShortcut: (String) -> Unit,
) {
    var addShortcutOpen by remember { mutableStateOf(false) }
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val shortcuts by viewModel.shortcuts.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
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
    val linePreview by viewModel.linePreview.collectAsStateWithLifecycle()
    val focusedStopIndex by viewModel.focusedStopIndex.collectAsStateWithLifecycle()
    val selectedDepartureId by viewModel.selectedDepartureId.collectAsStateWithLifecycle()
    val isLoadingLinePreview by viewModel.isLoadingLinePreview.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
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
        history = history,
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
        linePreview = linePreview,
        focusedStop = linePreview?.stops?.getOrNull(focusedStopIndex ?: -1)?.coordinate,
        selectedDepartureId = selectedDepartureId,
        isLoadingLinePreview = isLoadingLinePreview,
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
        onClearHistory = viewModel::clearHistory,
        onNearbyStopClick = { stop ->
            viewModel.onNearbyStopSelected(stop)?.let { (origin, destination) ->
                onPlanTrip(origin, destination)
            }
        },
        onDepartureClick = { stop, departure ->
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.onDepartureSelected(stop, departure)
        },
        onDismissLinePreview = viewModel::onDismissLinePreview,
        onLineStopSelected = viewModel::onLineStopSelected,
        onLineDepartureSelected = viewModel::onLineDepartureSelected,
        onUseCurrentLocation = {
            pendingLocationRequest = true
            requestLocationPermission()
        },
        onOpenSettings = onOpenSettings,
        onAddShortcut = { addShortcutOpen = true },
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

    if (addShortcutOpen) {
        AddPlaceLabelDialog(
            onDismiss = { addShortcutOpen = false },
            onConfirm = { label ->
                addShortcutOpen = false
                onAddShortcut(label)
            },
            labelError = viewModel::shortcutLabelError,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    destination: String,
    shortcuts: List<DestinationShortcutItem>,
    suggestions: List<GeocodeResult>,
    history: List<GeocodeResult> = emptyList(),
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
    linePreview: LinePreview? = null,
    focusedStop: Coordinate? = null,
    selectedDepartureId: String? = null,
    isLoadingLinePreview: Boolean = false,
    originName: String = "My location",
    onDestinationChange: (String) -> Unit,
    onShortcutClick: (DestinationShortcutItem) -> Unit,
    onSuggestionClick: (GeocodeResult) -> Unit,
    onClearHistory: () -> Unit = {},
    onUseCurrentLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddShortcut: () -> Unit = {},
    onNearbyStopClick: (NearbyStop) -> Unit = {},
    onDepartureClick: (NearbyStop, NearbyDeparture) -> Unit = { _, _ -> },
    onDismissLinePreview: () -> Unit = {},
    onLineStopSelected: (Int) -> Unit = {},
    onLineDepartureSelected: (Int) -> Unit = {},
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
                                contentDescription = "Settings"
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
                val lineOpen = linePreview != null
                val mapPeekHeight = maxHeight / 5
                val shortcutsMinHeight = 48.dp
                val searchMinHeight = 56.dp
                val sectionSpacing = 4.dp
                val searchListOverlap = 16.dp
                val overlayHeight = measuredOverlayHeight
                    ?: (shortcutsMinHeight + sectionSpacing + searchMinHeight)
                val restingMapHeight =
                    (topPadding + mapPeekHeight + overlayHeight).coerceAtMost(maxHeight * 0.58f)
                val scope = rememberCoroutineScope()
                var sheetDragPx by remember { mutableFloatStateOf(0f) }
                var sheetDragGeneration by remember { mutableIntStateOf(0) }
                val dismissDistancePx = with(density) { 120.dp.toPx() }
                if (!lineOpen && sheetDragPx != 0f) sheetDragPx = 0f
                val onSheetDrag: (Float) -> Unit = { delta ->
                    sheetDragGeneration += 1
                    sheetDragPx = (sheetDragPx + delta).coerceAtLeast(0f)
                }
                val onSheetDragEnd: (Float) -> Unit = { velocity ->
                    val generation = sheetDragGeneration
                    val dragged = sheetDragPx
                    val dismiss = dragged > dismissDistancePx || (dragged > 0f && velocity > 1_400f)
                    if (dismiss) {
                        sheetDragPx = 0f
                        onDismissLinePreview()
                    } else {
                        scope.launch {
                            animate(dragged, 0f, animationSpec = tween(220)) { value, _ ->
                                if (sheetDragGeneration == generation) sheetDragPx = value
                            }
                        }
                    }
                }
                val mapHeight by animateDpAsState(
                    targetValue = if (lineOpen) maxHeight * LineOpenMapFraction else restingMapHeight,
                    animationSpec = tween(320),
                    label = "mapHeight",
                )
                val sheetDrag = with(density) { sheetDragPx.toDp() }
                val mapBodyHeight = if (lineOpen) {
                    (mapHeight + sheetDrag).coerceAtMost(maxHeight - 72.dp)
                } else {
                    (mapHeight - searchListOverlap).coerceAtLeast(0.dp)
                }
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
                                followCenter = followMap && linePreview == null && !isLoadingLinePreview,
                                linePreview = linePreview,
                                holdingLinePreview = linePreview != null || isLoadingLinePreview,
                                focusStop = focusedStop,
                                onUserMovedCamera = onUserMovedCamera,
                                onMapCameraIdle = onMapCameraIdle,
                                contentPadding = PaddingValues(
                                    top = topPadding,
                                    bottom = if (lineOpen) 0.dp else overlayOnMapHeight,
                                ),
                                modifier = Modifier.fillMaxSize(),
                            )
                            LineCloseButton(
                                visible = lineOpen,
                                onClick = onDismissLinePreview,
                                modifier = Modifier.align(Alignment.TopStart)
                                    .padding(top = topPadding + 8.dp, start = 12.dp),
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background),
                        ) {
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            AnimatedContent(
                                targetState = linePreview,
                                contentKey = { it != null },
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                transitionSpec = {
                                    val opening = targetState != null
                                    val enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { full ->
                                        if (opening) full / 3 else -full / 5
                                    }
                                    val exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { full ->
                                        if (opening) -full / 5 else full / 3
                                    }
                                    enter togetherWith exit
                                },
                                label = "lineSheet",
                            ) { preview ->
                                if (preview != null) {
                                    LineSheet(
                                        preview = preview,
                                        loading = isLoadingLinePreview,
                                        onStopClick = onLineStopSelected,
                                        onDepartureClick = onLineDepartureSelected,
                                        sheetDragPx = sheetDragPx,
                                        onSheetDrag = onSheetDrag,
                                        onSheetDragEnd = onSheetDragEnd,
                                        contentPadding = PaddingValues(
                                            bottom = innerPadding.calculateBottomPadding(),
                                        ),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        contentPadding = PaddingValues(
                                            start = innerPadding.calculateStartPadding(layoutDirection),
                                            end = innerPadding.calculateEndPadding(layoutDirection),
                                            top = searchListOverlap,
                                            bottom = innerPadding.calculateBottomPadding(),
                                        ),
                                    ) {
                                        nearbyStopsSection(
                                            nearbyStops = nearbyStops,
                                            isLoadingNearby = isLoadingNearby,
                                            nearbyMessage = nearbyMessage,
                                            onStopClick = onNearbyStopClick,
                                            selectedDepartureId = selectedDepartureId,
                                            isLoadingLinePreview = isLoadingLinePreview,
                                            onDepartureClick = onDepartureClick,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = !lineOpen,
                        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { -it / 2 },
                        exit = fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it },
                        modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(
                            top = ((if (lineOpen) restingMapHeight else mapHeight) - overlayHeight)
                                .coerceAtLeast(0.dp),
                            start = horizontalPadding,
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        ),
                    ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
                            val measured = with(density) { size.height.toDp() }
                            if (measured > 0.dp && measured != measuredOverlayHeight) {
                                measuredOverlayHeight = measured
                            }
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
                            DestinationShortcut(
                                label = "Add shortcut",
                                detail = "",
                                onClick = onAddShortcut,
                            )
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
        }

        if (searchSheetVisible) {
            DestinationSearchSheet(
                query = destination,
                suggestions = suggestions,
                history = history,
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
                onAddShortcut = onAddShortcut,
                onSuggestionClick = onSuggestionClick,
                onClearHistory = onClearHistory,
            )
        }
    }
}

/** Share of the screen the map keeps while a line sheet is open. The sheet fills the rest. */
private const val LineOpenMapFraction = 0.30f

@Composable
private fun LineCloseButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180)),
        modifier = modifier,
    ) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
        ) {
            Icon(
                imageVector = closeIcon,
                contentDescription = "Close line preview",
                modifier = Modifier.padding(12.dp),
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
                DestinationShortcutItem(label = "Home", detail = "0 min", highlightDetail = true),
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
                DestinationShortcutItem(label = "Home", detail = "0 min", highlightDetail = true),
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
private fun HomeScreenLinePreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        HomeScreen(
            destination = "",
            shortcuts = listOf(
                DestinationShortcutItem(label = "Home", detail = "0 min", highlightDetail = true),
                DestinationShortcutItem(label = "Work", detail = "17 min"),
            ),
            suggestions = emptyList(),
            isLoading = false,
            isLocating = false,
            errorMessage = null,
            linePreview = previewLinePreview(),
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
