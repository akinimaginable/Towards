package org.etrange.towards.routes

import org.etrange.towards.api.dto.CoordinateDto
import org.etrange.towards.api.dto.EncodedPathDto
import org.etrange.towards.api.dto.GeocodeResultDto
import org.etrange.towards.api.dto.ItineraryDto
import org.etrange.towards.api.dto.JourneyLegDto
import org.etrange.towards.api.dto.MapInitialViewDto
import org.etrange.towards.api.dto.MapTripDto
import org.etrange.towards.api.dto.PlaceDto
import org.etrange.towards.api.dto.StopTimeDto
import org.etrange.towards.api.dto.StopTimesDto
import org.etrange.towards.api.dto.TripPlanDto
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.EncodedPath
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.MapInitialView
import org.etrange.towards.domain.model.MapTrip
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTime
import org.etrange.towards.domain.model.StopTimes
import org.etrange.towards.domain.model.TripPlan

fun Coordinate.toDto() = CoordinateDto(latitude, longitude, level)

fun Place.toDto() = PlaceDto(
    id = id,
    name = name,
    coordinate = coordinate.toDto(),
    parentId = parentId,
    timezone = timezone,
    platform = platform,
    modes = modes.map { it.name },
)

fun EncodedPath.toDto() = EncodedPathDto(points, precision, length)

fun JourneyLeg.toDto() = JourneyLegDto(
    mode = mode.name,
    from = from.toDto(),
    to = to.toDto(),
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
    geometry = geometry?.toDto(),
    cancelled = cancelled,
    intermediateStops = intermediateStops.map { it.toDto() },
)

fun Itinerary.toDto() = ItineraryDto(
    id = id,
    durationSeconds = durationSeconds,
    startTime = startTime,
    endTime = endTime,
    transfers = transfers,
    legs = legs.map { it.toDto() },
)

fun TripPlan.toDto() = TripPlanDto(
    from = from.toDto(),
    to = to.toDto(),
    direct = direct.map { it.toDto() },
    itineraries = itineraries.map { it.toDto() },
    previousPageCursor = previousPageCursor,
    nextPageCursor = nextPageCursor,
)

fun StopTime.toDto() = StopTimeDto(
    place = place.toDto(),
    mode = mode.name,
    time = time,
    scheduledTime = scheduledTime,
    realTime = realTime,
    headsign = headsign,
    tripId = tripId,
    routeId = routeId,
    displayName = displayName,
    cancelled = cancelled,
)

fun StopTimes.toDto() = StopTimesDto(
    place = place.toDto(),
    events = events.map { it.toDto() },
    previousPageCursor = previousPageCursor,
    nextPageCursor = nextPageCursor,
)

fun GeocodeResult.toDto() = GeocodeResultDto(
    id = id,
    kind = kind.name,
    name = name,
    coordinate = coordinate.toDto(),
    country = country,
    postalCode = postalCode,
    street = street,
    houseNumber = houseNumber,
    modes = modes.map { it.name },
)

fun MapTrip.toDto() = MapTripDto(
    tripIds = tripIds,
    mode = mode.name,
    from = from.toDto(),
    to = to.toDto(),
    departure = departure,
    arrival = arrival,
    polyline = polyline,
    routeColor = routeColor,
)

fun MapInitialView.toDto() = MapInitialViewDto(
    center = center.toDto(),
    zoom = zoom,
    motisVersion = motisVersion,
    streetRoutingAvailable = streetRoutingAvailable,
)
