package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.ui.icons.liveIcon
import org.etrange.towards.ui.parseHexColor
import org.etrange.towards.ui.theme.ThemeMode
import org.etrange.towards.ui.theme.TowardsPreview
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

fun LazyListScope.nearbyStopsSection(
    nearbyStops: List<NearbyStop>,
    isLoadingNearby: Boolean,
    nearbyMessage: String?,
    onStopClick: (NearbyStop) -> Unit = {},
) {
    if (isLoadingNearby && nearbyStops.isEmpty()) {
        item(key = "nearby-loading") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        }
        return
    }

    if (nearbyMessage != null && nearbyStops.isEmpty()) {
        item(key = "nearby-message") {
            Text(
                text = nearbyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    nearbyStops.forEachIndexed { stopIndex, stop ->
        item(key = "stop-header-$stopIndex-${stop.id}") {
            NearbyStopHeader(stop = stop, onClick = { onStopClick(stop) })
        }
        itemsIndexed(
            items = stop.departures,
            key = { departureIndex, departure -> "dep-$stopIndex-$departureIndex-${departure.id}" },
        ) { _, departure -> NearbyDepartureRow(departure = departure) }
    }
}

@Composable
fun NearbyStopHeader(stop: NearbyStop, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatDistanceMeters(stop.distanceMeters),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NearbyDepartureRow(departure: NearbyDeparture) {
    val now by produceState(initialValue = Clock.System.now(), key1 = departure.id) {
        while (isActive) {
            value = Clock.System.now()
            delay(30.seconds)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(
                color = parseHexColor(departure.routeColor)
                    ?: MaterialTheme.colorScheme.secondaryContainer
            ).padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(departure.lineName)
        Text(
            text = departure.headsign.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = relativeLabel(departure.time, now),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (departure.realTime) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (departure.realTime) {
                Icon(
                    imageVector = liveIcon,
                    contentDescription = "Live",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Preview
@Composable
fun NearbyStopsSectionPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        LazyColumn {
            nearbyStopsSection(
                nearbyStops = previewNearbyStops(),
                isLoadingNearby = false,
                nearbyMessage = null,
            )
        }
    }
}

@Preview
@Composable
fun NearbyStopHeaderPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        NearbyStopHeader(stop = previewNearbyStops().first())
    }
}

@Preview
@Composable
fun NearbyDepartureRowPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        NearbyDepartureRow(departure = previewNearbyStops().first().departures.first())
    }
}

private fun previewNearbyStops(): List<NearbyStop> {
    val now = Clock.System.now()
    return listOf(
        NearbyStop(
            id = "stop:bourse",
            name = "Bourse",
            coordinate = Coordinate(50.8481, 4.3497),
            distanceMeters = 120,
            departures = listOf(
                NearbyDeparture(
                    id = "1",
                    lineName = "3",
                    headsign = "Churchill",
                    mode = TransportMode.SUBWAY,
                    routeColor = "FFDD00",
                    routeTextColor = "000000",
                    time = now + 3.minutes,
                    realTime = true,
                ),
                NearbyDeparture(
                    id = "2",
                    lineName = "4",
                    headsign = "Stalle",
                    mode = TransportMode.SUBWAY,
                    routeColor = "F4C300",
                    routeTextColor = "000000",
                    time = now + 7.minutes,
                    realTime = false,
                ),
            ),
        ),
        NearbyStop(
            id = "stop:anneessens",
            name = "Anneessens",
            coordinate = Coordinate(50.8469, 4.3458),
            distanceMeters = 280,
            departures = listOf(
                NearbyDeparture(
                    id = "3",
                    lineName = "46",
                    headsign = "Moortebeek",
                    mode = TransportMode.BUS,
                    routeColor = "E30613",
                    routeTextColor = "FFFFFF",
                    time = now + 5.minutes,
                    realTime = true,
                ),
            ),
        ),
    )
}
