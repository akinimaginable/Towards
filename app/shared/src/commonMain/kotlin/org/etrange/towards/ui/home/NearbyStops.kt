package org.etrange.towards.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

fun LazyListScope.nearbyStopsSection(
    nearbyStops: List<NearbyStop>,
    isLoadingNearby: Boolean,
    nearbyMessage: String?,
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
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    nearbyStops.forEach { stop ->
        item(key = "stop-header-${stop.id}") {
            NearbyStopHeader(stop = stop)
        }
        items(stop.departures, key = { it.id }) { departure ->
            NearbyDepartureRow(departure = departure)
        }
    }
}

@Composable
fun NearbyStopHeader(stop: NearbyStop) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stop.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
            delay(30_000)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LineBadge(
            label = departure.lineName,
            routeColor = departure.routeColor,
            routeTextColor = departure.routeTextColor,
        )
        Text(
            text = departure.headsign.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
    }
}

@Composable
fun LineBadge(
    label: String,
    routeColor: String?,
    routeTextColor: String?,
) {
    val background = parseHexColor(routeColor)
        ?: MaterialTheme.colorScheme.secondaryContainer
    val foreground = parseHexColor(routeTextColor)
        ?: MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foreground,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

internal fun parseHexColor(value: String?): Color? {
    if (value.isNullOrBlank()) return null
    val hex = value.removePrefix("#")
    val normalized = when (hex.length) {
        6 -> "FF$hex"
        8 -> hex
        else -> return null
    }
    return runCatching {
        Color(normalized.toLong(16))
    }.getOrNull()
}
