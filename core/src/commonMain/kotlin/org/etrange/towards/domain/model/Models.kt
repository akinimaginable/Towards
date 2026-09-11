package org.etrange.towards.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class UserId(val value: String)

data class ActorContext(
    val userId: UserId,
)

sealed interface LocationReference {
    data class Stop(val id: String) : LocationReference
    data class Position(val coordinate: Coordinate) : LocationReference
}

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
