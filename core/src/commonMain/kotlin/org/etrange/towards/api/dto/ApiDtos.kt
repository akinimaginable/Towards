package org.etrange.towards.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val code: String,
    val message: String,
    val correlationId: String? = null,
    val details: Map<String, String> = emptyMap(),
)

@Serializable
data class HealthResponseDto(
    val status: String,
)

@Serializable
data class CoordinateDto(
    val latitude: Double,
    val longitude: Double,
    val level: Double? = null,
)

@Serializable
data class PlaceDto(
    val id: String? = null,
    val name: String,
    val coordinate: CoordinateDto,
    val parentId: String? = null,
    val timezone: String? = null,
    val platform: String? = null,
    val modes: List<String> = emptyList(),
)

@Serializable
data class EncodedPathDto(
    val points: String,
    val precision: Int,
    val length: Int,
)

@Serializable
data class JourneyLegDto(
    val mode: String,
    val from: PlaceDto,
    val to: PlaceDto,
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
    val geometry: EncodedPathDto? = null,
    val cancelled: Boolean = false,
    val intermediateStops: List<PlaceDto> = emptyList(),
)

@Serializable
data class ItineraryDto(
    val id: String,
    val durationSeconds: Int,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val legs: List<JourneyLegDto>,
)

@Serializable
data class TripPlanDto(
    val from: PlaceDto,
    val to: PlaceDto,
    val direct: List<ItineraryDto>,
    val itineraries: List<ItineraryDto>,
    val previousPageCursor: String? = null,
    val nextPageCursor: String? = null,
)

@Serializable
data class RefreshItineraryRequestDto(
    val itineraryId: String,
    val language: List<String> = emptyList(),
)

@Serializable
data class StopTimeDto(
    val place: PlaceDto,
    val mode: String,
    val time: String? = null,
    val scheduledTime: String? = null,
    val realTime: Boolean,
    val headsign: String? = null,
    val tripId: String,
    val routeId: String? = null,
    val displayName: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val cancelled: Boolean,
)

@Serializable
data class StopTimesDto(
    val place: PlaceDto,
    val events: List<StopTimeDto>,
    val previousPageCursor: String? = null,
    val nextPageCursor: String? = null,
)

@Serializable
data class GeocodeResultDto(
    val id: String,
    val kind: String,
    val name: String,
    val coordinate: CoordinateDto,
    val country: String? = null,
    val postalCode: String? = null,
    val street: String? = null,
    val houseNumber: String? = null,
    val modes: List<String> = emptyList(),
)

@Serializable
data class MapTripDto(
    val tripIds: List<String>,
    val mode: String,
    val from: PlaceDto,
    val to: PlaceDto,
    val departure: String,
    val arrival: String,
    val polyline: String,
    val routeColor: String? = null,
)

@Serializable
data class MapInitialViewDto(
    val center: CoordinateDto,
    val zoom: Double,
    val motisVersion: String,
    val streetRoutingAvailable: Boolean,
)
