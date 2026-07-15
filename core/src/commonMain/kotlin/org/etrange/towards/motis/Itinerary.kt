package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class LegId(
    val displayName: String,
    val tripId: String,
    val fromId: String,
    val fromLat: Double,
    val fromLon: Double,
    val toId: String,
    val toLat: Double,
    val toLon: Double,
    val schedStart: Long,
    val schedEnd: Long,
    val mode: Mode,
    val scheduled: Boolean,
    val fromLevel: Double? = null,
    val toLevel: Double? = null,
)

@Serializable
data class ItineraryId(
    val legs: List<LegId>,
)

@Serializable
data class Itinerary(
    val duration: Int,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val id: String,
    val legs: List<Leg>,
    val fareTransfers: List<FareTransfer>? = null,
)

@Serializable
data class RefreshItineraryPostBody(
    val id: ItineraryId,
    val requireDisplayNameMatch: Boolean? = null,
    val joinInterlinedLegs: Boolean? = null,
    val detailedTransfers: Boolean? = null,
    val detailedLegs: Boolean? = null,
    val withFares: Boolean? = null,
    val withScheduledSkippedStops: Boolean? = null,
    val numLegAlternatives: Int? = null,
    val transitModes: List<Mode>? = null,
    val pedestrianProfile: PedestrianProfile? = null,
    val useRoutedTransfers: Boolean? = null,
    val requireBikeTransport: Boolean? = null,
    val requireCarTransport: Boolean? = null,
    val language: List<String>? = null,
)
