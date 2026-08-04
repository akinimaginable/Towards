package org.etrange.towards.api.dto

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TransportMode

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
