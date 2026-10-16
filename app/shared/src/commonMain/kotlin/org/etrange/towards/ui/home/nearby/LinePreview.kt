package org.etrange.towards.ui.home.nearby

import kotlin.time.Instant
import org.etrange.towards.data.decodeCoordinates
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.Place

/** One stop of the selected direction, in travel order from the start of the trip. */
data class LinePreviewStop(
    val name: String,
    val coordinate: Coordinate,
    val current: Boolean,
    /** This stop, or one the vehicle still has to call at. */
    val upcoming: Boolean,
    val terminus: Boolean,
    /** When the vehicle reaches this stop. Arrival, or departure when arrival is missing. */
    val time: Instant?,
)

/** A trip drawn from the tapped stop: [passed] is behind, [ahead] continues toward the headsign. */
data class LinePreview(
    val stop: Coordinate,
    val routeColor: String?,
    val routeTextColor: String?,
    val lineName: String,
    val headsign: String?,
    /** The tapped departure and the later ones of this line and direction, earliest first. */
    val departures: List<TripDeparture> = emptyList(),
    /** Which of [departures] the stops and times belong to. */
    val selectedDeparture: Int = 0,
    /** Walking time from the rider to the boarding stop, when the rider's location is known. */
    val walkMinutes: Int? = null,
    /** Geometry behind the tapped stop. */
    val passed: List<Coordinate>,
    /** Geometry from the tapped stop toward the headsign. */
    val ahead: List<Coordinate>,
    /** Every stop of this direction, from the start of the trip to the terminus. */
    val stops: List<LinePreviewStop>,
)

/** Builds a preview from a trip. Missing geometry falls back to the stop sequence. */
fun linePreviewOf(
    itinerary: Itinerary,
    stop: Coordinate,
    routeColor: String?,
    routeTextColor: String? = null,
    lineName: String = "",
    headsign: String? = null,
    tripId: String? = null,
    departures: List<TripDeparture> = emptyList(),
    selectedDeparture: Int = 0,
    walkMinutes: Int? = null,
): LinePreview {
    val direction = itinerary.legsInDirection(tripId)
    val places = direction.stopPlaces()
    val shape = direction.flatMap { leg -> leg.lineCoordinates() }.dedupeConsecutive()
    val (passed, ahead) = splitLineAtStop(shape, stop)
    return LinePreview(
        stop = stop,
        routeColor = routeColor,
        routeTextColor = routeTextColor,
        lineName = lineName,
        headsign = headsign,
        departures = departures,
        selectedDeparture = selectedDeparture,
        walkMinutes = walkMinutes,
        passed = passed,
        ahead = ahead,
        stops = places.toPreviewStops(stop),
    )
}

/**
 * Splits [coordinates] at the vertex nearest [stop].
 * The two pieces share that vertex. A stop at the first vertex leaves [passed] empty.
 */
internal fun splitLineAtStop(
    coordinates: List<Coordinate>,
    stop: Coordinate,
): Pair<List<Coordinate>, List<Coordinate>> {
    if (coordinates.isEmpty()) return emptyList<Coordinate>() to emptyList()
    val index = coordinates.indices.minBy { haversineMeters(coordinates[it], stop) }
    if (index == 0) return emptyList<Coordinate>() to coordinates
    return coordinates.subList(0, index + 1) to coordinates.subList(index, coordinates.size)
}

/** The legs for [tripId], or every leg when this trip was not split out. */
private fun Itinerary.legsInDirection(tripId: String?): List<JourneyLeg> {
    if (tripId.isNullOrBlank()) return legs
    val matched = legs.filter { it.tripId == tripId }
    return matched.ifEmpty { legs }
}

private fun List<JourneyLeg>.stopPlaces(): List<Place> =
    flatMap { leg ->
        buildList {
            add(leg.from)
            addAll(leg.intermediateStops)
            add(leg.to)
        }
    }.dedupePlaces()

/** Every stop on the trip. [current] is the one nearest the stop that was tapped. */
private fun List<Place>.toPreviewStops(current: Coordinate): List<LinePreviewStop> {
    if (isEmpty()) return emptyList()
    val index = indices.minBy { haversineMeters(this[it].coordinate, current) }
    return mapIndexed { offset, place ->
        LinePreviewStop(
            name = place.name,
            coordinate = place.coordinate,
            current = offset == index,
            upcoming = offset >= index,
            terminus = offset == lastIndex,
            time = place.arrivalTime(),
        )
    }
}

private fun Place.arrivalTime(): Instant? {
    val raw = arrival ?: departure ?: return null
    return runCatching { Instant.parse(raw) }.getOrNull()
}

private fun List<Place>.dedupePlaces(): List<Place> =
    filterIndexed { index, place ->
        index == 0 || place.coordinate != this[index - 1].coordinate
    }

private fun JourneyLeg.lineCoordinates(): List<Coordinate> {
    val decoded = geometry?.decodeCoordinates().orEmpty()
    if (decoded.size >= 2) return decoded
    return buildList {
        add(from.coordinate)
        intermediateStops.forEach { add(it.coordinate) }
        add(to.coordinate)
    }
}

private fun List<Coordinate>.dedupeConsecutive(): List<Coordinate> =
    filterIndexed { index, coordinate ->
        index == 0 || coordinate != this[index - 1]
    }
