package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val shortName: String,
)

@Serializable
data class Leg(
    val mode: Mode,
    val startTime: String,
    val endTime: String,
    val scheduledStartTime: String,
    val scheduledEndTime: String,
    val realTime: Boolean,
    val scheduled: Boolean,
    val duration: Int,
    val from: Place,
    val to: Place,
    val legGeometry: EncodedPolyline,
    val distance: Double? = null,
    val interlineWithPreviousLeg: Boolean? = null,
    val headsign: String? = null,
    val tripFrom: Place? = null,
    val tripTo: Place? = null,
    val category: Category? = null,
    val routeId: String? = null,
    val routeUrl: String? = null,
    val directionId: String? = null,
    val routeColor: String? = null,
    val routeTextColor: String? = null,
    val routeType: Int? = null,
    val agencyName: String? = null,
    val agencyUrl: String? = null,
    val agencyId: String? = null,
    val tripId: String? = null,
    val routeShortName: String? = null,
    val routeLongName: String? = null,
    val tripShortName: String? = null,
    val displayName: String? = null,
    val cancelled: Boolean? = null,
    val source: String? = null,
    val intermediateStops: List<Place>? = null,
    val steps: List<StepInstruction>? = null,
    val rental: Rental? = null,
    val fareTransferIndex: Int? = null,
    val effectiveFareLegIndex: Int? = null,
    val alerts: List<Alert>? = null,
    val loopedCalendarSince: String? = null,
    val bikesAllowed: Boolean? = null,
    val wheelchairAccessible: WheelchairAccessibility? = null,
    val alternatives: List<List<Leg>>? = null,
)
