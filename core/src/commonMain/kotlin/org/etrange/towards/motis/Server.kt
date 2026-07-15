package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val motisVersion: String,
    val hasElevation: Boolean,
    val hasRoutedTransfers: Boolean,
    val hasStreetRouting: Boolean,
    val maxOneToManySize: Double,
    val maxOneToAllTravelTimeLimit: Double,
    val maxPrePostTransitTimeLimit: Double,
    val maxDirectTimeLimit: Double,
    val shapesDebugEnabled: Boolean,
)

@Serializable
data class Error(
    val error: String,
)

@Serializable
data class HealthResponse(
    val rt: Boolean? = null,
    val gbfs: Boolean? = null,
)

@Serializable
data class InitialResponse(
    val lat: Double,
    val lon: Double,
    val zoom: Double,
    val serverConfig: ServerConfig,
)
