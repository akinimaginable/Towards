package org.etrange.towards.motis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlertCause {
    @SerialName("UNKNOWN_CAUSE") UNKNOWN_CAUSE,
    @SerialName("OTHER_CAUSE") OTHER_CAUSE,
    @SerialName("TECHNICAL_PROBLEM") TECHNICAL_PROBLEM,
    @SerialName("STRIKE") STRIKE,
    @SerialName("DEMONSTRATION") DEMONSTRATION,
    @SerialName("ACCIDENT") ACCIDENT,
    @SerialName("HOLIDAY") HOLIDAY,
    @SerialName("WEATHER") WEATHER,
    @SerialName("MAINTENANCE") MAINTENANCE,
    @SerialName("CONSTRUCTION") CONSTRUCTION,
    @SerialName("POLICE_ACTIVITY") POLICE_ACTIVITY,
    @SerialName("MEDICAL_EMERGENCY") MEDICAL_EMERGENCY,
    @SerialName("SPECIAL_EVENT") SPECIAL_EVENT,
}

@Serializable
enum class AlertEffect {
    @SerialName("NO_SERVICE") NO_SERVICE,
    @SerialName("REDUCED_SERVICE") REDUCED_SERVICE,
    @SerialName("SIGNIFICANT_DELAYS") SIGNIFICANT_DELAYS,
    @SerialName("DETOUR") DETOUR,
    @SerialName("ADDITIONAL_SERVICE") ADDITIONAL_SERVICE,
    @SerialName("MODIFIED_SERVICE") MODIFIED_SERVICE,
    @SerialName("OTHER_EFFECT") OTHER_EFFECT,
    @SerialName("UNKNOWN_EFFECT") UNKNOWN_EFFECT,
    @SerialName("STOP_MOVED") STOP_MOVED,
    @SerialName("NO_EFFECT") NO_EFFECT,
    @SerialName("ACCESSIBILITY_ISSUE") ACCESSIBILITY_ISSUE,
}

@Serializable
enum class AlertSeverityLevel {
    @SerialName("UNKNOWN_SEVERITY") UNKNOWN_SEVERITY,
    @SerialName("INFO") INFO,
    @SerialName("WARNING") WARNING,
    @SerialName("SEVERE") SEVERE,
}

@Serializable
enum class LocationType {
    @SerialName("ADDRESS") ADDRESS,
    @SerialName("PLACE") PLACE,
    @SerialName("STOP") STOP,
}

@Serializable
enum class Mode {
    // Street
    @SerialName("WALK") WALK,
    @SerialName("BIKE") BIKE,
    @SerialName("RENTAL") RENTAL,
    @SerialName("CAR") CAR,
    @SerialName("CAR_PARKING") CAR_PARKING,
    @SerialName("CAR_DROPOFF") CAR_DROPOFF,
    @SerialName("ODM") ODM,
    @SerialName("RIDE_SHARING") RIDE_SHARING,
    @SerialName("FLEX") FLEX,
    @SerialName("DEBUG_BUS_ROUTE") DEBUG_BUS_ROUTE,
    @SerialName("DEBUG_RAILWAY_ROUTE") DEBUG_RAILWAY_ROUTE,
    @SerialName("DEBUG_FERRY_ROUTE") DEBUG_FERRY_ROUTE,
    // Transit
    @SerialName("TRANSIT") TRANSIT,
    @SerialName("TRAM") TRAM,
    @SerialName("SUBWAY") SUBWAY,
    @SerialName("FERRY") FERRY,
    @SerialName("AIRPLANE") AIRPLANE,
    @SerialName("BUS") BUS,
    @SerialName("COACH") COACH,
    @SerialName("RAIL") RAIL,
    @SerialName("HIGHSPEED_RAIL") HIGHSPEED_RAIL,
    @SerialName("LONG_DISTANCE") LONG_DISTANCE,
    @SerialName("NIGHT_RAIL") NIGHT_RAIL,
    @SerialName("REGIONAL_FAST_RAIL") REGIONAL_FAST_RAIL,
    @SerialName("REGIONAL_RAIL") REGIONAL_RAIL,
    @SerialName("SUBURBAN") SUBURBAN,
    @SerialName("FUNICULAR") FUNICULAR,
    @SerialName("AERIAL_LIFT") AERIAL_LIFT,
    @SerialName("OTHER") OTHER,
    // Deprecated
    @SerialName("AREAL_LIFT") AREAL_LIFT,
    @SerialName("METRO") METRO,
    @SerialName("CABLE_CAR") CABLE_CAR,
}

@Serializable
enum class ElevationCosts {
    @SerialName("NONE") NONE,
    @SerialName("LOW") LOW,
    @SerialName("HIGH") HIGH,
}

@Serializable
enum class PedestrianProfile {
    @SerialName("FOOT") FOOT,
    @SerialName("WHEELCHAIR") WHEELCHAIR,
}

@Serializable
enum class VertexType {
    @SerialName("NORMAL") NORMAL,
    @SerialName("BIKESHARE") BIKESHARE,
    @SerialName("TRANSIT") TRANSIT,
}

@Serializable
enum class PickupDropoffType {
    @SerialName("NORMAL") NORMAL,
    @SerialName("NOT_ALLOWED") NOT_ALLOWED,
}

@Serializable
enum class Direction {
    @SerialName("DEPART") DEPART,
    @SerialName("HARD_LEFT") HARD_LEFT,
    @SerialName("LEFT") LEFT,
    @SerialName("SLIGHTLY_LEFT") SLIGHTLY_LEFT,
    @SerialName("CONTINUE") CONTINUE,
    @SerialName("SLIGHTLY_RIGHT") SLIGHTLY_RIGHT,
    @SerialName("RIGHT") RIGHT,
    @SerialName("HARD_RIGHT") HARD_RIGHT,
    @SerialName("CIRCLE_CLOCKWISE") CIRCLE_CLOCKWISE,
    @SerialName("CIRCLE_COUNTERCLOCKWISE") CIRCLE_COUNTERCLOCKWISE,
    @SerialName("STAIRS") STAIRS,
    @SerialName("ELEVATOR") ELEVATOR,
    @SerialName("UTURN_LEFT") UTURN_LEFT,
    @SerialName("UTURN_RIGHT") UTURN_RIGHT,
}

@Serializable
enum class WheelchairAccessibility {
    @SerialName("ACCESSIBLE") ACCESSIBLE,
    @SerialName("NOT_ACCESSIBLE") NOT_ACCESSIBLE,
}

@Serializable
enum class RentalFormFactor {
    @SerialName("BICYCLE") BICYCLE,
    @SerialName("CARGO_BICYCLE") CARGO_BICYCLE,
    @SerialName("CAR") CAR,
    @SerialName("MOPED") MOPED,
    @SerialName("SCOOTER_STANDING") SCOOTER_STANDING,
    @SerialName("SCOOTER_SEATED") SCOOTER_SEATED,
    @SerialName("OTHER") OTHER,
}

@Serializable
enum class RentalPropulsionType {
    @SerialName("HUMAN") HUMAN,
    @SerialName("ELECTRIC_ASSIST") ELECTRIC_ASSIST,
    @SerialName("ELECTRIC") ELECTRIC,
    @SerialName("COMBUSTION") COMBUSTION,
    @SerialName("COMBUSTION_DIESEL") COMBUSTION_DIESEL,
    @SerialName("HYBRID") HYBRID,
    @SerialName("PLUG_IN_HYBRID") PLUG_IN_HYBRID,
    @SerialName("HYDROGEN_FUEL_CELL") HYDROGEN_FUEL_CELL,
}

@Serializable
enum class RentalReturnConstraint {
    @SerialName("NONE") NONE,
    @SerialName("ANY_STATION") ANY_STATION,
    @SerialName("ROUNDTRIP_STATION") ROUNDTRIP_STATION,
}

@Serializable
enum class FareMediaType {
    @SerialName("NONE") NONE,
    @SerialName("PAPER_TICKET") PAPER_TICKET,
    @SerialName("TRANSIT_CARD") TRANSIT_CARD,
    @SerialName("CONTACTLESS_EMV") CONTACTLESS_EMV,
    @SerialName("MOBILE_APP") MOBILE_APP,
}

@Serializable
enum class FareTransferRule {
    @SerialName("A_AB") A_AB,
    @SerialName("A_AB_B") A_AB_B,
    @SerialName("AB") AB,
}

@Serializable
enum class RoutePathSource {
    @SerialName("NONE") NONE,
    @SerialName("TIMETABLE") TIMETABLE,
    @SerialName("ROUTED") ROUTED,
}
