package org.etrange.towards.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

data class ActorContext(
    val userId: UserId,
)

data class Coordinate(
    val latitude: Double,
    val longitude: Double,
    val level: Double? = null,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "longitude must be between -180 and 180" }
    }
}

sealed interface LocationReference {
    data class Stop(val id: String) : LocationReference
    data class Position(val coordinate: Coordinate) : LocationReference
}

enum class TransportMode {
    WALK,
    BIKE,
    CAR,
    CAR_PARKING,
    CAR_DROPOFF,
    FLEX,
    TRANSIT,
    TRAM,
    SUBWAY,
    FERRY,
    AIRPLANE,
    BUS,
    COACH,
    RAIL,
    HIGHSPEED_RAIL,
    LONG_DISTANCE,
    NIGHT_RAIL,
    REGIONAL_FAST_RAIL,
    REGIONAL_RAIL,
    SUBURBAN,
    FUNICULAR,
    AERIAL_LIFT,
    OTHER,
}

enum class LocationKind {
    ADDRESS,
    PLACE,
    STOP,
}

data class TripPlanningRequest(
    val from: LocationReference,
    val to: LocationReference,
    val time: String? = null,
    val arriveBy: Boolean = false,
    val transitModes: Set<TransportMode> = setOf(TransportMode.TRANSIT),
    val directModes: Set<TransportMode> = setOf(TransportMode.WALK),
    val maxTransfers: Int? = null,
    val pageCursor: String? = null,
    val language: List<String> = emptyList(),
)

data class TripLookupRequest(
    val tripId: String,
    val includeScheduledSkippedStops: Boolean = false,
    val language: List<String> = emptyList(),
)

data class ItineraryRefreshRequest(
    val itineraryId: String,
    val language: List<String> = emptyList(),
)

data class StopTimesRequest(
    val stopId: String? = null,
    val center: Coordinate? = null,
    val radiusMeters: Int? = null,
    val time: String? = null,
    val arriveBy: Boolean = false,
    val numberOfEvents: Int? = null,
    val transportModes: Set<TransportMode> = setOf(TransportMode.TRANSIT),
    val pageCursor: String? = null,
    val language: List<String> = emptyList(),
)

data class GeocodeRequest(
    val text: String,
    val languages: List<String> = emptyList(),
    val kinds: Set<LocationKind> = emptySet(),
    val modes: Set<TransportMode> = emptySet(),
    val bias: Coordinate? = null,
    val numberOfResults: Int? = null,
)

data class ReverseGeocodeRequest(
    val coordinate: Coordinate,
    val kinds: Set<LocationKind> = emptySet(),
    val numberOfResults: Int? = null,
)

data class MapBounds(
    val minimum: Coordinate,
    val maximum: Coordinate,
)

data class MapStopsRequest(
    val bounds: MapBounds,
    val grouped: Boolean? = null,
    val modes: Set<TransportMode> = emptySet(),
    val languages: List<String> = emptyList(),
)

data class MapTripsRequest(
    val bounds: MapBounds,
    val zoom: Double,
    val startTime: String,
    val endTime: String,
    val precision: Int = 5,
    val languages: List<String> = emptyList(),
)

data class Place(
    val id: String?,
    val name: String,
    val coordinate: Coordinate,
    val parentId: String? = null,
    val timezone: String? = null,
    val platform: String? = null,
    val modes: Set<TransportMode> = emptySet(),
)

data class EncodedPath(
    val points: String,
    val precision: Int,
    val length: Int,
)

data class JourneyLeg(
    val mode: TransportMode,
    val from: Place,
    val to: Place,
    val startTime: String,
    val endTime: String,
    val scheduledStartTime: String,
    val scheduledEndTime: String,
    val durationSeconds: Int,
    val realTime: Boolean,
    val routeId: String? = null,
    val tripId: String? = null,
    val displayName: String? = null,
    val headsign: String? = null,
    val agencyName: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val distanceMeters: Double? = null,
    val geometry: EncodedPath? = null,
    val cancelled: Boolean = false,
    val intermediateStops: List<Place> = emptyList(),
)

data class Itinerary(
    val id: String,
    val durationSeconds: Int,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val legs: List<JourneyLeg>,
)

data class TripPlan(
    val from: Place,
    val to: Place,
    val direct: List<Itinerary>,
    val itineraries: List<Itinerary>,
    val previousPageCursor: String?,
    val nextPageCursor: String?,
)

data class StopTime(
    val place: Place,
    val mode: TransportMode,
    val time: String?,
    val scheduledTime: String?,
    val realTime: Boolean,
    val headsign: String?,
    val tripId: String,
    val routeId: String?,
    val displayName: String?,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val cancelled: Boolean,
)

data class StopTimes(
    val place: Place,
    val events: List<StopTime>,
    val previousPageCursor: String?,
    val nextPageCursor: String?,
)

data class GeocodeResult(
    val id: String,
    val kind: LocationKind,
    val name: String,
    val coordinate: Coordinate,
    val country: String? = null,
    val postalCode: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val modes: Set<TransportMode> = emptySet(),
)

/** MOTIS ADDRESS matches often have blank ids; LazyColumn keys require uniqueness. */
fun List<GeocodeResult>.ensureUniqueIds(): List<GeocodeResult> =
    mapIndexed { index, result ->
        if (result.id.isNotBlank()) {
            result
        } else {
            result.copy(
                id = "geocode:$index:${result.kind.name}:" +
                    "${result.coordinate.latitude},${result.coordinate.longitude}:${result.name}",
            )
        }
    }

data class MapTrip(
    val tripIds: List<String>,
    val mode: TransportMode,
    val from: Place,
    val to: Place,
    val departure: String,
    val arrival: String,
    val polyline: String,
    val routeColor: String? = null,
)

data class MapInitialView(
    val center: Coordinate,
    val zoom: Double,
    val motisVersion: String,
    val streetRoutingAvailable: Boolean,
)
