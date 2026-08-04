package org.etrange.towards.infrastructure.motis

import kotlinx.serialization.Serializable

@Serializable
internal data class MotisError(
    val error: String = "Unknown MOTIS error",
)

@Serializable
internal data class MotisPlanResponse(
    val from: MotisPlace,
    val to: MotisPlace,
    val direct: List<MotisItinerary> = emptyList(),
    val itineraries: List<MotisItinerary> = emptyList(),
    val previousPageCursor: String = "",
    val nextPageCursor: String = "",
)

@Serializable
internal data class MotisPlace(
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val stopId: String? = null,
    val parentId: String? = null,
    val level: Double? = null,
    val tz: String? = null,
    val track: String? = null,
    val scheduledTrack: String? = null,
    val arrival: String? = null,
    val departure: String? = null,
    val scheduledArrival: String? = null,
    val scheduledDeparture: String? = null,
    val modes: List<String> = emptyList(),
)

@Serializable
internal data class MotisEncodedPolyline(
    val points: String = "",
    val precision: Int = 0,
    val length: Int = 0,
)

@Serializable
internal data class MotisLeg(
    val mode: String,
    val startTime: String = "",
    val endTime: String = "",
    val scheduledStartTime: String = "",
    val scheduledEndTime: String = "",
    val realTime: Boolean = false,
    val duration: Int = 0,
    val from: MotisPlace,
    val to: MotisPlace,
    val legGeometry: MotisEncodedPolyline? = null,
    val distance: Double? = null,
    val headsign: String? = null,
    val routeId: String? = null,
    val tripId: String? = null,
    val displayName: String? = null,
    val agencyName: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val cancelled: Boolean = false,
    val intermediateStops: List<MotisPlace> = emptyList(),
)

@Serializable
internal data class MotisItinerary(
    val duration: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val transfers: Int = 0,
    val id: String = "",
    val legs: List<MotisLeg> = emptyList(),
)

@Serializable
internal data class MotisStopTimesResponse(
    val stopTimes: List<MotisStopTime> = emptyList(),
    val place: MotisPlace,
    val previousPageCursor: String = "",
    val nextPageCursor: String = "",
)

@Serializable
internal data class MotisStopTime(
    val place: MotisPlace,
    val mode: String,
    val realTime: Boolean = false,
    val headsign: String? = null,
    val tripId: String = "",
    val routeId: String? = null,
    val displayName: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val cancelled: Boolean = false,
    val tripCancelled: Boolean = false,
)

@Serializable
internal data class MotisMatch(
    val type: String,
    val name: String,
    val id: String,
    val lat: Double,
    val lon: Double,
    val country: String? = null,
    val zip: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val level: Double? = null,
    val modes: List<String> = emptyList(),
)

@Serializable
internal data class MotisTripInfo(
    val tripId: String,
)

@Serializable
internal data class MotisTripSegment(
    val trips: List<MotisTripInfo> = emptyList(),
    val mode: String,
    val from: MotisPlace,
    val to: MotisPlace,
    val departure: String,
    val arrival: String,
    val polyline: String,
    val routeColor: String? = null,
)

@Serializable
internal data class MotisServerConfig(
    val motisVersion: String = "unknown",
    val hasStreetRouting: Boolean = false,
)

@Serializable
internal data class MotisInitialResponse(
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val serverConfig: MotisServerConfig,
)
