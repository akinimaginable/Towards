package org.etrange.towards.domain.model

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
