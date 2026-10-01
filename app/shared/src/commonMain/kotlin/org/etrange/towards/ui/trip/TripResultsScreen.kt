package org.etrange.towards.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.ui.home.HomeMap
import org.etrange.towards.ui.preview.previewItineraries
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
        userLocation = viewModel.userLocation,
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
    /** The device's own position; distinct from [origin], which may be any searched place. */
    userLocation: Coordinate? = null,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TripHeader(
                originName = originName,
                destinationName = destinationName,
                onBack = onBack,
                onSwap = onSwap,
                onChangeOrigin = onChangeOrigin,
                onChangeDestination = onChangeDestination,
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        // The header is opaque, so the map and list start below it instead of extending underneath.
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        ) {
            val mapHeight = (maxHeight * 0.38f).coerceAtLeast(180.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(mapHeight).clipToBounds(),
                ) {
                    HomeMap(
                        center = destination,
                        userLocation = userLocation,
                        contentPadding = PaddingValues(
                            top = 12.dp,
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
