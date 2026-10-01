package org.etrange.towards.ui.home

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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
    /** The following departures of the same line and direction, earliest first. */
    val laterTimes: List<Instant> = emptyList(),
)

data class NearbyStop(
    val id: String,
    val name: String,
    val coordinate: Coordinate,
    val distanceMeters: Int,
    val departures: List<NearbyDeparture>,
)

/** Departures further away than this are not shown, so lines that aren't running disappear. */
val MaxDepartureWait: Duration = 2.hours

/** How departures of one stop are split into labelled sections, in display order. */
enum class ModeGroup {
    METRO,
    TRAIN,
    TRAM,
    BUS,
    OTHER,
}

fun TransportMode.modeGroup(): ModeGroup = when (this) {
    TransportMode.SUBWAY -> ModeGroup.METRO
    TransportMode.RAIL,
    TransportMode.HIGHSPEED_RAIL,
    TransportMode.LONG_DISTANCE,
    TransportMode.NIGHT_RAIL,
    TransportMode.REGIONAL_FAST_RAIL,
    TransportMode.REGIONAL_RAIL,
    TransportMode.SUBURBAN,
    -> ModeGroup.TRAIN
    TransportMode.TRAM -> ModeGroup.TRAM
    TransportMode.BUS, TransportMode.COACH -> ModeGroup.BUS
    else -> ModeGroup.OTHER
}

fun NearbyStop.departuresByMode(): List<Pair<ModeGroup, List<NearbyDeparture>>> =
    departures.groupBy { it.mode.modeGroup() }.toList().sortedBy { (group, _) -> group.ordinal }

fun groupNearbyDepartures(
    stopTimes: StopTimes,
    origin: Coordinate,
    maxStops: Int = 20,
    now: Instant? = null,
    laterTimesPerLine: Int = 2,
    maxWait: Duration = MaxDepartureWait,
): List<NearbyStop> {
    val candidates = stopTimes.events
        .asSequence()
        .filterNot { it.cancelled }
        .mapNotNull { event ->
            val instant = event.time?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: return@mapNotNull null
            if (now != null && instant < now - 1.minutes) return@mapNotNull null
            // A line whose next departure is hours away is effectively not running: hide it.
            if (now != null && instant > now + maxWait) return@mapNotNull null
            stopKey(event) to (event to instant)
        }
        .groupBy({ it.first }, { it.second })
        .map { (stopId, events) ->
            val place = events.first().first.place
            StopCandidate(
                id = stopId,
                name = place.name,
                coordinate = place.coordinate,
                distanceMeters = haversineMeters(origin, place.coordinate),
                eventsByLine = events.groupBy { (event, _) -> lineKey(event) },
            )
        }

    val stops = mergeSameNamedStops(candidates)

    val nearestStopByLine = stops
        .flatMap { stop -> stop.eventsByLine.keys.map { lineKey -> lineKey to stop } }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, lineStops) -> lineStops.minBy { it.distanceMeters }.id }

    return stops.mapNotNull { stop ->
        val departures = stop.eventsByLine
            .filterKeys { lineKey -> nearestStopByLine[lineKey] == stop.id }
            .values
            .map { lineEvents -> lineEvents.toDeparture(stop.id, laterTimesPerLine) }
            .sortedBy { it.time }
        if (departures.isEmpty()) return@mapNotNull null
        NearbyStop(
            id = stop.id,
            name = stop.name,
            coordinate = stop.coordinate,
            distanceMeters = stop.distanceMeters,
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

/** Whole minutes until [time], or null when it leaves within the minute ("now"). */
fun minutesUntil(time: Instant, now: Instant): Long? {
    val delta = time - now
    return if (delta < 1.minutes) null else delta.inWholeMinutes
}

fun formatDistanceMeters(meters: Int): String =
    if (meters < 1000) "$meters m" else "${(meters / 100.0).roundToInt() / 10.0} km"

/**
 * Converts all-caps feed text ("GARE DE L'OUEST") to title case ("Gare de l'Ouest").
 * Mixed-case text is returned unchanged, since it was already written by a person.
 */
fun String.toDisplayCase(): String {
    if (none { it.isLetter() } || any { it.isLowerCase() }) return this
    var wordIndex = 0
    return split(' ').joinToString(" ") { word ->
        val cased = word.titleCaseWord(isFirst = wordIndex == 0)
        if (word.isNotEmpty()) wordIndex++
        cased
    }
}

private val lowercaseParticles = setOf(
    "de", "du", "des", "la", "le", "les", "et", "en", "sur", "aux", "au",
    "ten", "van", "der", "den", "het", "op", "te",
)

private fun String.titleCaseWord(isFirst: Boolean): String {
    val lower = lowercase()
    val elision = lower.indexOf('\'')
    if (elision in 1..2) {
        val prefix = lower.substring(0, elision + 1)
        val rest = lower.substring(elision + 1).capitalizeParts()
        return if (isFirst) prefix.replaceFirstChar { it.titlecase() } + rest else prefix + rest
    }
    if (!isFirst && lower in lowercaseParticles) return lower
    return lower.capitalizeParts()
}

private fun String.capitalizeParts(): String =
    split('-').joinToString("-") { part -> part.replaceFirstChar { it.titlecase() } }

fun haversineMeters(from: Coordinate, to: Coordinate): Int {
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

/** Stops sharing a name within this distance are shown as one (e.g. metro and street platforms). */
private const val SameStopRadiusMeters = 200

private data class StopCandidate(
    val id: String,
    val name: String,
    val coordinate: Coordinate,
    val distanceMeters: Int,
    val eventsByLine: Map<Pair<String, String?>, List<Pair<StopTime, Instant>>>,
)

/** Merges same-named nearby stops into the one closest to the user, which keeps its id. */
private fun mergeSameNamedStops(stops: List<StopCandidate>): List<StopCandidate> {
    val merged = mutableListOf<StopCandidate>()
    for (stop in stops.sortedBy { it.distanceMeters }) {
        val index = merged.indexOfFirst { kept ->
            kept.name.trim().equals(stop.name.trim(), ignoreCase = true) &&
                haversineMeters(kept.coordinate, stop.coordinate) <= SameStopRadiusMeters
        }
        if (index < 0) {
            merged += stop
        } else {
            val kept = merged[index]
            merged[index] = kept.copy(
                eventsByLine = (kept.eventsByLine.keys + stop.eventsByLine.keys).associateWith { key ->
                    kept.eventsByLine[key].orEmpty() + stop.eventsByLine[key].orEmpty()
                },
            )
        }
    }
    return merged
}

private fun List<Pair<StopTime, Instant>>.toDeparture(
    stopId: String,
    laterTimesPerLine: Int,
): NearbyDeparture {
    val ordered = distinctBy { (event, _) -> event.tripId }.sortedBy { it.second }
    val (event, instant) = ordered.first()
    return NearbyDeparture(
        id = "${stopId}:${event.routeId ?: event.displayName}:${event.headsign}:${event.tripId}",
        lineName = event.displayName ?: event.routeId ?: event.mode.name,
        headsign = event.headsign,
        mode = event.mode,
        routeColor = event.routeColor,
        routeTextColor = event.routeTextColor,
        time = instant,
        realTime = event.realTime,
        laterTimes = ordered.drop(1).take(laterTimesPerLine).map { it.second },
    )
}

private fun lineKey(event: StopTime): Pair<String, String?> =
    (event.routeId ?: event.displayName.orEmpty()) to event.headsign

private fun stopKey(event: StopTime): String =
    event.place.parentId?.takeIf { it.isNotBlank() }
        ?: event.place.id?.takeIf { it.isNotBlank() }
        ?: event.place.name
