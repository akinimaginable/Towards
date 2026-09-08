package org.etrange.towards.ui.trip

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.ui.home.HomeMap
import org.etrange.towards.ui.icons.arrow_backIcon
import org.etrange.towards.ui.icons.swapVertIcon
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview

@Composable
fun TripResultsScreen(
    viewModel: TripResultsViewModel,
    onBack: () -> Unit,
    onChangeOrigin: () -> Unit,
    onChangeDestination: () -> Unit,
) {
    val origin by viewModel.origin.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val itineraries by viewModel.itineraries.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val selected = itineraries.firstOrNull { it.id == selectedId } ?: itineraries.firstOrNull()

    TripResultsScreen(
        originName = origin.name,
        destinationName = destination.name,
        origin = origin.coordinate,
        destination = destination.coordinate,
        itineraries = itineraries,
        selectedId = selected?.id,
        selectedLegs = selected?.legs.orEmpty(),
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onSwap = viewModel::swapEndpoints,
        onRetry = viewModel::refresh,
        onItineraryClick = viewModel::onItineraryClick,
        onChangeOrigin = onChangeOrigin,
        onChangeDestination = onChangeDestination,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripResultsScreen(
    originName: String,
    destinationName: String,
    origin: Coordinate,
    destination: Coordinate,
    itineraries: List<Itinerary>,
    selectedId: String?,
    selectedLegs: List<JourneyLeg>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSwap: () -> Unit,
    onRetry: () -> Unit,
    onItineraryClick: (String) -> Unit,
    onChangeOrigin: () -> Unit = {},
    onChangeDestination: () -> Unit = {},
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = originName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onChangeOrigin)
                                    .padding(vertical = 2.dp),
                            )
                            Text(
                                text = destinationName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onChangeDestination)
                                    .padding(vertical = 2.dp),
                            )
                        }
                        IconButton(onClick = onSwap) {
                            Icon(
                                imageVector = swapVertIcon,
                                contentDescription = "Swap origin and destination",
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Text(
                                text = "Leave now",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = arrow_backIcon,
                            contentDescription = "Back",
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

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val mapHeight = (maxHeight * 0.38f).coerceAtLeast(180.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(mapHeight).clipToBounds(),
                ) {
                    HomeMap(
                        center = destination,
                        userLocation = origin,
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = 12.dp,
                        ),
                        routeLegs = selectedLegs,
                        origin = origin,
                        destination = destination,
                        fitRoute = selectedLegs.isNotEmpty(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        top = 8.dp,
                        bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isLoading && itineraries.isEmpty()) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            }
                        }
                    }

                    if (errorMessage != null) {
                        item(key = "error") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = if (itineraries.isEmpty()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                if (itineraries.isEmpty()) {
                                    Button(onClick = onRetry) {
                                        Text("Try again")
                                    }
                                }
                            }
                        }
                    }

                    items(
                        items = itineraries,
                        key = { it.id },
                    ) { itinerary ->
                        ItineraryCard(
                            itinerary = itinerary,
                            selected = itinerary.id == selectedId,
                            onClick = { onItineraryClick(itinerary.id) },
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun TripResultsScreenPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        TripResultsScreen(
            originName = "My location",
            destinationName = "Grand Place",
            origin = Coordinate(50.8503, 4.3517),
            destination = Coordinate(50.8467, 4.3525),
            itineraries = previewItineraries(),
            selectedId = "opt-1",
            selectedLegs = previewItineraries().first().legs,
            isLoading = false,
            errorMessage = null,
            onBack = {},
            onSwap = {},
            onRetry = {},
            onItineraryClick = {},
        )
    }
}

private fun previewItineraries(): List<Itinerary> {
    val bourse = Place("stop:bourse", "Bourse", Coordinate(50.848, 4.35))
    val grandPlace = Place("place:gp", "Grand Place", Coordinate(50.8467, 4.3525))
    val origin = Place(null, "My location", Coordinate(50.8503, 4.3517))
    return listOf(
        Itinerary(
            id = "opt-1",
            durationSeconds = 12 * 60,
            startTime = "2026-09-07T10:04:00+02:00",
            endTime = "2026-09-07T10:16:00+02:00",
            transfers = 0,
            legs = listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = bourse,
                    startTime = "2026-09-07T10:04:00+02:00",
                    endTime = "2026-09-07T10:08:00+02:00",
                    scheduledStartTime = "2026-09-07T10:04:00+02:00",
                    scheduledEndTime = "2026-09-07T10:08:00+02:00",
                    durationSeconds = 240,
                    realTime = false,
                    distanceMeters = 280.0,
                ),
                JourneyLeg(
                    mode = TransportMode.TRAM,
                    from = bourse,
                    to = grandPlace,
                    startTime = "2026-09-07T10:08:00+02:00",
                    endTime = "2026-09-07T10:16:00+02:00",
                    scheduledStartTime = "2026-09-07T10:08:00+02:00",
                    scheduledEndTime = "2026-09-07T10:16:00+02:00",
                    durationSeconds = 480,
                    realTime = true,
                    displayName = "3",
                    headsign = "Churchill",
                    routeColor = "FFDD00",
                    routeTextColor = "000000",
                ),
            ),
        ),
        Itinerary(
            id = "opt-2",
            durationSeconds = 18 * 60,
            startTime = "2026-09-07T10:06:00+02:00",
            endTime = "2026-09-07T10:24:00+02:00",
            transfers = 0,
            legs = listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = grandPlace,
                    startTime = "2026-09-07T10:06:00+02:00",
                    endTime = "2026-09-07T10:24:00+02:00",
                    scheduledStartTime = "2026-09-07T10:06:00+02:00",
                    scheduledEndTime = "2026-09-07T10:24:00+02:00",
                    durationSeconds = 18 * 60,
                    realTime = false,
                    distanceMeters = 1_200.0,
                ),
            ),
        ),
    )
}
