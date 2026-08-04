package org.etrange.towards.ui.home

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TransportMode

data class NearbyDeparture(
    val id: String,
    val lineName: String,
    val headsign: String?,
    val mode: TransportMode,
    val routeColor: String?,
    val routeTextColor: String?,
    val time: Instant,
    val realTime: Boolean,
)

data class NearbyStop(
    val id: String,
    val name: String,
    val distanceMeters: Int,
    val departures: List<NearbyDeparture>,
)

fun groupNearbyDepartures(
    stopTimes: StopTimes,
    origin: Coordinate,
    maxStops: Int = 5,
    maxDeparturesPerStop: Int = 4,
): List<NearbyStop> {
    val eventsByStop = stopTimes.events
        .asSequence()
        .filterNot { it.cancelled }
        .mapNotNull { event ->
            val instant = event.time?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: return@mapNotNull null
            stopKey(event) to (event to instant)
        }
        .groupBy({ it.first }, { it.second })

    return eventsByStop
        .mapNotNull { (stopId, events) ->
            val firstPlace = events.firstOrNull()?.first?.place ?: return@mapNotNull null
            val departures = events
                .groupBy { (event, _) ->
                    (event.routeId ?: event.displayName.orEmpty()) to event.headsign
                }
                .map { (_, lineEvents) ->
                    val (event, instant) = lineEvents.minBy { it.second }
                    NearbyDeparture(
                        id = "${stopId}:${event.routeId ?: event.displayName}:${event.headsign}:${event.tripId}",
                        lineName = event.displayName ?: event.routeId ?: event.mode.name,
                        headsign = event.headsign,
                        mode = event.mode,
                        routeColor = event.routeColor,
                        routeTextColor = event.routeTextColor,
                        time = instant,
                        realTime = event.realTime,
                    )
                }
                .sortedBy { it.time }
                .take(maxDeparturesPerStop)

            if (departures.isEmpty()) return@mapNotNull null

            NearbyStop(
                id = stopId,
                name = firstPlace.name,
                distanceMeters = haversineMeters(origin, firstPlace.coordinate),
                departures = departures,
            )
        }
        .sortedBy { it.distanceMeters }
        .take(maxStops)
}

fun relativeLabel(time: Instant, now: Instant): String {
    val delta: Duration = time - now
    if (delta < 1.minutes) return "now"
    val totalMinutes = delta.inWholeMinutes
    if (totalMinutes < 60) return "$totalMinutes min"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (minutes == 0L) "$hours h" else "$hours h $minutes"
}

fun formatDistanceMeters(meters: Int): String =
    if (meters < 1000) "$meters m" else "${(meters / 100.0).roundToInt() / 10.0} km"

internal fun haversineMeters(from: Coordinate, to: Coordinate): Int {
    val earthRadiusMeters = 6_371_000.0
    val dLat = (to.latitude - from.latitude).toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()
    val lat1 = from.latitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadiusMeters * c).roundToInt()
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun stopKey(event: StopTime): String =
    event.place.parentId ?: event.place.id ?: event.place.name
