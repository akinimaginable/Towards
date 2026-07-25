package org.etrange.towards.infrastructure.motis

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.EncodedPath
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.MapInitialView
import org.etrange.towards.domain.model.MapTrip
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan

private fun String.isUnsupportedMotisMode(): Boolean = this == "RENTAL"

internal fun String.toDomainMode(): TransportMode? {
    if (isUnsupportedMotisMode()) return null
    return when (this) {
        "METRO" -> TransportMode.SUBWAY
        "AREAL_LIFT" -> TransportMode.AERIAL_LIFT
        else -> TransportMode.entries.firstOrNull { it.name == this } ?: TransportMode.OTHER
    }
}

private fun List<String>.toDomainModes(): Set<TransportMode> =
    mapNotNullTo(linkedSetOf()) { it.toDomainMode() }

private fun MotisItinerary.containsUnsupportedMode(): Boolean =
    legs.any { it.mode.isUnsupportedMotisMode() }

internal fun MotisPlace.toDomain(): Place = Place(
    id = stopId,
    name = name,
    coordinate = Coordinate(lat, lon, level),
    parentId = parentId,
    timezone = tz,
    platform = track ?: scheduledTrack,
    modes = modes.toDomainModes(),
)

internal fun MotisEncodedPolyline.toDomain(): EncodedPath = EncodedPath(
    points = points,
    precision = precision,
    length = length,
)

internal fun MotisLeg.toDomain(): JourneyLeg? {
    val domainMode = mode.toDomainMode() ?: return null
    return JourneyLeg(
        mode = domainMode,
        from = from.toDomain(),
        to = to.toDomain(),
        startTime = startTime,
        endTime = endTime,
        scheduledStartTime = scheduledStartTime,
        scheduledEndTime = scheduledEndTime,
        durationSeconds = duration,
        realTime = realTime,
        routeId = routeId,
        tripId = tripId,
        displayName = displayName,
        headsign = headsign,
        agencyName = agencyName,
        routeColor = routeColor,
        routeTextColor = routeTextColor,
        distanceMeters = distance,
        geometry = legGeometry?.toDomain(),
        cancelled = cancelled,
        intermediateStops = intermediateStops.map { it.toDomain() },
    )
}

internal fun MotisItinerary.toDomain(): Itinerary? {
    if (containsUnsupportedMode()) return null
    val domainLegs = legs.mapNotNull { it.toDomain() }
    if (domainLegs.size != legs.size) return null
    return Itinerary(
        id = id,
        durationSeconds = duration,
        startTime = startTime,
        endTime = endTime,
        transfers = transfers,
        legs = domainLegs,
    )
}

internal fun MotisPlanResponse.toDomain(): TripPlan = TripPlan(
    from = from.toDomain(),
    to = to.toDomain(),
    direct = direct.mapNotNull { it.toDomain() },
    itineraries = itineraries.mapNotNull { it.toDomain() },
    previousPageCursor = previousPageCursor.ifBlank { null },
    nextPageCursor = nextPageCursor.ifBlank { null },
)

internal fun MotisStopTime.toDomain(): StopTime? {
    val domainMode = mode.toDomainMode() ?: return null
    return StopTime(
        place = place.toDomain(),
        mode = domainMode,
        time = place.departure ?: place.arrival,
        scheduledTime = place.scheduledDeparture ?: place.scheduledArrival,
        realTime = realTime,
        headsign = headsign,
        tripId = tripId,
        routeId = routeId,
        displayName = displayName,
        cancelled = cancelled || tripCancelled,
    )
}

internal fun MotisStopTimesResponse.toDomain(): StopTimes = StopTimes(
    place = place.toDomain(),
    events = stopTimes.mapNotNull { it.toDomain() },
    previousPageCursor = previousPageCursor.ifBlank { null },
    nextPageCursor = nextPageCursor.ifBlank { null },
)

internal fun MotisMatch.toDomain(): GeocodeResult = GeocodeResult(
    id = id,
    kind = LocationKind.entries.firstOrNull { it.name == type } ?: LocationKind.PLACE,
    name = name,
    coordinate = Coordinate(lat, lon, level),
    country = country,
    postalCode = zip,
    street = street,
    houseNumber = houseNumber,
    modes = modes.toDomainModes(),
)

internal fun MotisTripSegment.toDomain(): MapTrip? {
    val domainMode = mode.toDomainMode() ?: return null
    return MapTrip(
        tripIds = trips.map { it.tripId },
        mode = domainMode,
        from = from.toDomain(),
        to = to.toDomain(),
        departure = departure,
        arrival = arrival,
        polyline = polyline,
        routeColor = routeColor,
    )
}

internal fun MotisInitialResponse.toDomain(): MapInitialView = MapInitialView(
    center = Coordinate(lat, lon),
    zoom = zoom,
    motisVersion = serverConfig.motisVersion,
    streetRoutingAvailable = serverConfig.hasStreetRouting,
)
