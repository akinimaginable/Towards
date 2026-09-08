package org.etrange.towards.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object SettingsRoute

@Serializable
data class TripResultsRoute(
    val fromLatitude: Double,
    val fromLongitude: Double,
    val fromName: String,
    val fromStopId: String = "",
    val toLatitude: Double,
    val toLongitude: Double,
    val toName: String,
    val toStopId: String = "",
)

@Serializable
data class LocationPickerRoute(
    val editingOrigin: Boolean,
    val hasCounterpart: Boolean = false,
    val counterpartLatitude: Double = 0.0,
    val counterpartLongitude: Double = 0.0,
    val counterpartName: String = "",
    val counterpartStopId: String = "",
)
