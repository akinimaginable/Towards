package org.etrange.towards.api.dto

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.TransportMode

fun CoordinateDto.toDomain() = Coordinate(
    latitude = latitude,
    longitude = longitude,
    level = level,
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
