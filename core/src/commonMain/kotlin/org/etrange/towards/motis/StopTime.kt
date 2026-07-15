package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class StopTime(
    val place: Place,
    val mode: Mode,
    val realTime: Boolean,
    val headsign: String,
    val tripFrom: Place,
    val tripTo: Place,
    val agencyId: String,
    val agencyName: String,
    val agencyUrl: String,
    val tripId: String,
    val routeId: String,
    val directionId: String,
    val routeShortName: String,
    val routeLongName: String,
    val tripShortName: String,
    val displayName: String,
    val pickupDropoffType: PickupDropoffType,
    val cancelled: Boolean,
    val tripCancelled: Boolean,
    val source: String,
    val routeUrl: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val routeType: Int? = null,
    val previousStops: List<Place>? = null,
    val nextStops: List<Place>? = null,
)
