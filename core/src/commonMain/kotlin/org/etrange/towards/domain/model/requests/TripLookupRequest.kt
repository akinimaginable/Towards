package org.etrange.towards.domain.model.requests

data class TripLookupRequest(
    val tripId: String,
    val includeScheduledSkippedStops: Boolean = false,
    val language: List<String> = emptyList(),
)
