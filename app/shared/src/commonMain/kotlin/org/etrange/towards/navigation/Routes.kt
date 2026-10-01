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
    val counterpartLatitude: Double,
    val counterpartLongitude: Double,
    val counterpartName: String,
    val counterpartStopId: String = "",
)
