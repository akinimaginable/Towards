package org.etrange.towards.ui.trip

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.LocationReference
import org.etrange.towards.navigation.LocationPickerRoute
import org.etrange.towards.navigation.TripResultsRoute
import org.etrange.towards.ui.home.NearbyStop
import org.etrange.towards.ui.home.haversineMeters

data class TripEndpoint(
    val name: String,
    val coordinate: Coordinate,
    val stopId: String? = null,
) {
    fun toLocationReference(): LocationReference {
        val id = stopId?.takeIf { it.isNotBlank() }
        return if (id != null) {
            LocationReference.Stop(id)
        } else {
            LocationReference.Position(coordinate)
        }
    }

    fun isSamePlace(other: TripEndpoint): Boolean {
        val thisStop = stopId?.takeIf { it.isNotBlank() }
        val otherStop = other.stopId?.takeIf { it.isNotBlank() }
        if (thisStop != null && thisStop == otherStop) return true
        return haversineMeters(coordinate, other.coordinate) < 50
    }
}

fun GeocodeResult.toTripEndpoint(): TripEndpoint = TripEndpoint(
    name = name,
    coordinate = coordinate,
    stopId = id.takeIf { kind == LocationKind.STOP && isUsableStopId(it) },
)

fun NearbyStop.toTripEndpoint(): TripEndpoint = TripEndpoint(
    name = name,
    coordinate = coordinate,
    stopId = id.takeIf { isUsableStopId(it) },
)

fun TripEndpoint.toResultsRoute(destination: TripEndpoint) = TripResultsRoute(
    fromLatitude = coordinate.latitude,
    fromLongitude = coordinate.longitude,
    fromName = name,
    fromStopId = stopId.orEmpty(),
    toLatitude = destination.coordinate.latitude,
    toLongitude = destination.coordinate.longitude,
    toName = destination.name,
    toStopId = destination.stopId.orEmpty(),
)

fun TripResultsRoute.origin(): TripEndpoint = TripEndpoint(
    name = fromName,
    coordinate = Coordinate(fromLatitude, fromLongitude),
    stopId = fromStopId.takeIf { it.isNotBlank() },
)

fun TripResultsRoute.destination(): TripEndpoint = TripEndpoint(
    name = toName,
    coordinate = Coordinate(toLatitude, toLongitude),
    stopId = toStopId.takeIf { it.isNotBlank() },
)

fun TripEndpoint.toOriginPickerRoute() = LocationPickerRoute(
    editingOrigin = true,
    hasCounterpart = false,
)

fun TripEndpoint.toPickerRoute(
    editingOrigin: Boolean,
    counterpart: TripEndpoint,
) = LocationPickerRoute(
    editingOrigin = editingOrigin,
    hasCounterpart = true,
    counterpartLatitude = counterpart.coordinate.latitude,
    counterpartLongitude = counterpart.coordinate.longitude,
    counterpartName = counterpart.name,
    counterpartStopId = counterpart.stopId.orEmpty(),
)

fun LocationPickerRoute.counterpartOrNull(): TripEndpoint? {
    if (!hasCounterpart) return null
    return TripEndpoint(
        name = counterpartName,
        coordinate = Coordinate(counterpartLatitude, counterpartLongitude),
        stopId = counterpartStopId.takeIf { it.isNotBlank() },
    )
}

fun tripEndpointsAfterPicking(
    editingOrigin: Boolean,
    picked: TripEndpoint,
    counterpart: TripEndpoint,
): Pair<TripEndpoint, TripEndpoint> =
    if (editingOrigin) picked to counterpart else counterpart to picked

private fun isUsableStopId(id: String): Boolean =
    id.isNotBlank() &&
        !id.startsWith("geocode:") &&
        !id.startsWith("current:")
