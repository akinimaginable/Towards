package org.etrange.towards.api.dto

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.EncodedPath
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan

fun CoordinateDto.toDomain() = Coordinate(
    latitude = latitude,
    longitude = longitude,
    level = level,
)

fun PlaceDto.toDomain() = Place(
    id = id,
    name = name,
    coordinate = coordinate.toDomain(),
    parentId = parentId,
    timezone = timezone,
    platform = platform,
    modes = modes.mapNotNull { mode ->
        runCatching { TransportMode.valueOf(mode) }.getOrNull()
    }.toSet(),
)

fun StopTimeDto.toDomain() = StopTime(
    place = place.toDomain(),
    mode = runCatching { TransportMode.valueOf(mode) }.getOrDefault(TransportMode.OTHER),
    time = time,
    scheduledTime = scheduledTime,
    realTime = realTime,
    headsign = headsign,
    tripId = tripId,
    routeId = routeId,
    displayName = displayName,
    routeColor = routeColor,
    routeTextColor = routeTextColor,
    cancelled = cancelled,
)

fun StopTimesDto.toDomain() = StopTimes(
    place = place.toDomain(),
    events = events.map { it.toDomain() },
    previousPageCursor = previousPageCursor,
    nextPageCursor = nextPageCursor,
)

fun EncodedPathDto.toDomain() = EncodedPath(
    points = points,
    precision = precision,
    length = length,
)

fun JourneyLegDto.toDomain() = JourneyLeg(
    mode = runCatching { TransportMode.valueOf(mode) }.getOrDefault(TransportMode.OTHER),
    from = from.toDomain(),
    to = to.toDomain(),
    startTime = startTime,
    endTime = endTime,
    scheduledStartTime = scheduledStartTime,
    scheduledEndTime = scheduledEndTime,
    durationSeconds = durationSeconds,
    realTime = realTime,
    routeId = routeId,
    tripId = tripId,
    displayName = displayName,
    headsign = headsign,
    agencyName = agencyName,
    routeColor = routeColor,
    routeTextColor = routeTextColor,
    distanceMeters = distanceMeters,
    geometry = geometry?.toDomain(),
    cancelled = cancelled,
    intermediateStops = intermediateStops.map { it.toDomain() },
)

fun ItineraryDto.toDomain() = Itinerary(
    id = id,
    durationSeconds = durationSeconds,
    startTime = startTime,
    endTime = endTime,
    transfers = transfers,
    legs = legs.map { it.toDomain() },
)

fun TripPlanDto.toDomain() = TripPlan(
    from = from.toDomain(),
    to = to.toDomain(),
    direct = direct.map { it.toDomain() },
    itineraries = itineraries.map { it.toDomain() },
    previousPageCursor = previousPageCursor,
    nextPageCursor = nextPageCursor,
)

fun GeocodeResultDto.toDomain() = GeocodeResult(
    id = id,
    kind = LocationKind.valueOf(kind),
    name = name,
    coordinate = coordinate.toDomain(),
    country = country,
    postalCode = postalCode,
    street = street,
    houseNumber = houseNumber,
    modes = modes.mapNotNull { mode ->
        runCatching { TransportMode.valueOf(mode) }.getOrNull()
    }.toSet(),
)
