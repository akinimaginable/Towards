package org.etrange.towards.motis

import kotlinx.serialization.Serializable

@Serializable
data class Rental(
    val providerId: String,
    val providerGroupId: String,
    val systemId: String,
    val systemName: String? = null,
    val url: String? = null,
    val color: String? = null,
    val stationName: String? = null,
    val fromStationName: String? = null,
    val toStationName: String? = null,
    val rentalUriAndroid: String? = null,
    val rentalUriIOS: String? = null,
    val rentalUriWeb: String? = null,
    val formFactor: RentalFormFactor? = null,
    val propulsionType: RentalPropulsionType? = null,
    val returnConstraint: RentalReturnConstraint? = null,
)

@Serializable
data class RentalZoneRestrictions(
    val vehicleTypeIdxs: List<Int>,
    val rideStartAllowed: Boolean,
    val rideEndAllowed: Boolean,
    val rideThroughAllowed: Boolean,
    val stationParking: Boolean? = null,
)

@Serializable
data class RentalVehicleType(
    val id: String,
    val formFactor: RentalFormFactor,
    val propulsionType: RentalPropulsionType,
    val returnConstraint: RentalReturnConstraint,
    val returnConstraintGuessed: Boolean,
    val name: String? = null,
)

@Serializable
data class RentalProvider(
    val id: String,
    val name: String,
    val groupId: String,
    val bbox: List<Double>,
    val vehicleTypes: List<RentalVehicleType>,
    val formFactors: List<RentalFormFactor>,
    val defaultRestrictions: RentalZoneRestrictions,
    val globalGeofencingRules: List<RentalZoneRestrictions>,
    val operator: String? = null,
    val url: String? = null,
    val purchaseUrl: String? = null,
    val color: String? = null,
)

@Serializable
data class RentalProviderGroup(
    val id: String,
    val name: String,
    val providers: List<String>,
    val formFactors: List<RentalFormFactor>,
    val color: String? = null,
)

@Serializable
data class RentalStation(
    val id: String,
    val providerId: String,
    val providerGroupId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val isRenting: Boolean,
    val isReturning: Boolean,
    val numVehiclesAvailable: Int,
    val formFactors: List<RentalFormFactor>,
    val vehicleTypesAvailable: Map<String, Int>,
    val vehicleDocksAvailable: Map<String, Int>,
    val bbox: List<Double>,
    val address: String? = null,
    val crossStreet: String? = null,
    val rentalUriAndroid: String? = null,
    val rentalUriIOS: String? = null,
    val rentalUriWeb: String? = null,
    val stationArea: MultiPolygon? = null,
)

@Serializable
data class RentalVehicle(
    val id: String,
    val providerId: String,
    val providerGroupId: String,
    val typeId: String,
    val lat: Double,
    val lon: Double,
    val formFactor: RentalFormFactor,
    val propulsionType: RentalPropulsionType,
    val returnConstraint: RentalReturnConstraint,
    val isReserved: Boolean,
    val isDisabled: Boolean,
    val stationId: String? = null,
    val homeStationId: String? = null,
    val rentalUriAndroid: String? = null,
    val rentalUriIOS: String? = null,
    val rentalUriWeb: String? = null,
)

@Serializable
data class RentalZone(
    val providerId: String,
    val providerGroupId: String,
    val z: Int,
    val bbox: List<Double>,
    val area: MultiPolygon,
    val rules: List<RentalZoneRestrictions>,
    val name: String? = null,
)

@Serializable
data class RentalsResponse(
    val providerGroups: List<RentalProviderGroup>,
    val providers: List<RentalProvider>,
    val stations: List<RentalStation>,
    val vehicles: List<RentalVehicle>,
    val zones: List<RentalZone>,
)
