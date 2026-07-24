package org.etrange.towards.routes

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.etrange.towards.api.dto.HealthResponseDto
import org.etrange.towards.api.dto.RefreshItineraryRequestDto
import org.etrange.towards.application.BadRequestException
import org.etrange.towards.application.MobilityService
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeRequest
import org.etrange.towards.domain.model.ItineraryRefreshRequest
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.LocationReference
import org.etrange.towards.domain.model.MapBounds
import org.etrange.towards.domain.model.MapStopsRequest
import org.etrange.towards.domain.model.MapTripsRequest
import org.etrange.towards.domain.model.ReverseGeocodeRequest
import org.etrange.towards.domain.model.StopTimesRequest
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripLookupRequest
import org.etrange.towards.domain.model.TripPlanningRequest
import org.etrange.towards.plugins.API_RATE_LIMIT
import org.etrange.towards.plugins.DummyPrincipal

fun Application.configureRoutes(
    service: MobilityService,
    prometheusRegistry: PrometheusMeterRegistry,
) {
    routing {
        get("/health") {
            call.respond(HealthResponseDto(status = "UP"))
        }
        get("/metrics") {
            call.respondText(
                text = prometheusRegistry.scrape(),
                contentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8"),
            )
        }

        authenticate("dummy") {
            rateLimit(RateLimitName(API_RATE_LIMIT)) {
                route("/api/v1") {
                get("/trips/plan") {
                    val request = TripPlanningRequest(
                        from = call.requiredQuery("from").toLocationReference(),
                        to = call.requiredQuery("to").toLocationReference(),
                        time = call.request.queryParameters["time"],
                        arriveBy = call.booleanQuery("arriveBy") ?: false,
                        transitModes = call.enumList("transitModes", TransportMode.TRANSIT),
                        directModes = call.enumList("directModes", TransportMode.WALK),
                        maxTransfers = call.intQuery("maxTransfers"),
                        pageCursor = call.request.queryParameters["pageCursor"],
                        language = call.listQuery("language"),
                    )
                    call.respond(service.plan(call.actor(), call.correlationId(), request).toDto())
                }

                get("/trips/{tripId}") {
                    val tripId = call.parameters["tripId"]?.takeIf { it.isNotBlank() }
                        ?: throw BadRequestException("tripId is required")
                    val request = TripLookupRequest(
                        tripId = tripId,
                        includeScheduledSkippedStops = call.booleanQuery("withScheduledSkippedStops")
                            ?: false,
                        language = call.listQuery("language"),
                    )
                    call.respond(
                        service.getTrip(call.actor(), call.correlationId(), request).toDto()
                    )
                }

                post("/itineraries/refresh") {
                    val body = call.receive<RefreshItineraryRequestDto>()
                    if (body.itineraryId.isBlank()) {
                        throw BadRequestException("itineraryId must not be blank")
                    }
                    val request = ItineraryRefreshRequest(body.itineraryId, body.language)
                    call.respond(
                        service.refreshItinerary(
                            call.actor(),
                            call.correlationId(),
                            request
                        ).toDto()
                    )
                }

                get("/stop-times") {
                    val center = call.request.queryParameters["center"]?.toCoordinate()
                    val request = StopTimesRequest(
                        stopId = call.request.queryParameters["stopId"],
                        center = center,
                        radiusMeters = call.intQuery("radius"),
                        time = call.request.queryParameters["time"],
                        arriveBy = call.booleanQuery("arriveBy") ?: false,
                        numberOfEvents = call.intQuery("n"),
                        transportModes = call.enumList("modes", TransportMode.TRANSIT),
                        pageCursor = call.request.queryParameters["pageCursor"],
                        language = call.listQuery("language"),
                    )
                    call.respond(
                        service.getStopTimes(call.actor(), call.correlationId(), request).toDto()
                    )
                }

                get("/geocode") {
                    val request = GeocodeRequest(
                        text = call.requiredQuery("text"),
                        languages = call.listQuery("language"),
                        kinds = call.locationKinds("types"),
                        modes = call.enumList("modes"),
                        bias = call.request.queryParameters["bias"]?.toCoordinate(),
                        numberOfResults = call.intQuery("limit"),
                    )
                    call.respond(
                        service.geocode(call.actor(), call.correlationId(), request)
                            .map { it.toDto() })
                }

                get("/reverse-geocode") {
                    val request = ReverseGeocodeRequest(
                        coordinate = call.requiredQuery("place").toCoordinate(),
                        kinds = call.locationKinds("types"),
                        numberOfResults = call.intQuery("limit"),
                    )
                    call.respond(
                        service.reverseGeocode(call.actor(), call.correlationId(), request)
                            .map { it.toDto() })
                }

                route("/map") {
                    get("/initial") {
                        call.respond(
                            service.getInitialMapView(call.actor(), call.correlationId()).toDto()
                        )
                    }
                    get("/stops") {
                        val request = MapStopsRequest(
                            bounds = call.mapBounds(),
                            grouped = call.booleanQuery("grouped"),
                            modes = call.enumList("modes"),
                            languages = call.listQuery("language"),
                        )
                        call.respond(
                            service.getMapStops(
                                call.actor(),
                                call.correlationId(),
                                request
                            ).map { it.toDto() })
                    }
                    get("/trips") {
                        val precision = call.intQuery("precision") ?: 5
                        if (precision !in 0..6) {
                            throw BadRequestException("precision must be between 0 and 6")
                        }
                        val request = MapTripsRequest(
                            bounds = call.mapBounds(),
                            zoom = call.doubleQuery("zoom")
                                ?: throw BadRequestException("zoom is required"),
                            startTime = call.requiredQuery("startTime"),
                            endTime = call.requiredQuery("endTime"),
                            precision = precision,
                            languages = call.listQuery("language"),
                        )
                        call.respond(
                            service.getMapTrips(
                                call.actor(),
                                call.correlationId(),
                                request
                            ).map { it.toDto() })
                    }
                    get("/levels") {
                        call.respond(
                            service.getMapLevels(
                                call.actor(),
                                call.correlationId(),
                                call.mapBounds()
                            )
                        )
                    }
                }
            }
            }
        }
    }
}

private fun ApplicationCall.actor(): org.etrange.towards.domain.model.ActorContext =
    principal<DummyPrincipal>()?.toActorContext()
        ?: throw IllegalStateException("The authenticated principal is missing")

private fun ApplicationCall.correlationId(): String = callId ?: "unknown"

private fun ApplicationCall.requiredQuery(name: String): String =
    request.queryParameters[name]?.takeIf { it.isNotBlank() }
        ?: throw BadRequestException("$name is required")

private fun ApplicationCall.listQuery(name: String): List<String> =
    request.queryParameters.getAll(name).orEmpty().flatMap { value -> value.split(',') }
        .map { it.trim() }.filter { it.isNotEmpty() }

private fun ApplicationCall.enumList(
    name: String,
    vararg defaults: TransportMode,
): Set<TransportMode> {
    val values = listQuery(name)
    if (values.isEmpty()) return defaults.toSet()
    return values.mapTo(linkedSetOf()) { value ->
        runCatching { TransportMode.valueOf(value.uppercase()) }.getOrElse {
                throw BadRequestException(
                    "Unsupported transport mode",
                    mapOf(name to value),
                )
            }
    }
}

private fun ApplicationCall.locationKinds(name: String): Set<LocationKind> =
    listQuery(name).mapTo(linkedSetOf()) { value ->
        runCatching { LocationKind.valueOf(value.uppercase()) }.getOrElse {
                throw BadRequestException(
                    "Unsupported location type",
                    mapOf(name to value),
                )
            }
    }

private fun ApplicationCall.booleanQuery(name: String): Boolean? =
    request.queryParameters[name]?.let { value ->
        value.toBooleanStrictOrNull() ?: throw BadRequestException("$name must be true or false")
    }

private fun ApplicationCall.intQuery(name: String): Int? =
    request.queryParameters[name]?.let { value ->
        value.toIntOrNull() ?: throw BadRequestException("$name must be an integer")
    }

private fun ApplicationCall.doubleQuery(name: String): Double? =
    request.queryParameters[name]?.let { value ->
        value.toDoubleOrNull() ?: throw BadRequestException("$name must be a number")
    }

private fun ApplicationCall.mapBounds() = MapBounds(
    minimum = requiredQuery("min").toCoordinate(),
    maximum = requiredQuery("max").toCoordinate(),
)

private fun String.toLocationReference(): LocationReference =
    if (contains(',')) LocationReference.Position(toCoordinate()) else LocationReference.Stop(this)

private fun String.toCoordinate(): Coordinate {
    val parts = split(',').map { it.trim() }
    if (parts.size !in 2..3) {
        throw BadRequestException("A coordinate must use latitude,longitude[,level] format")
    }
    val latitude = parts[0].toDoubleOrNull()
        ?: throw BadRequestException("Coordinate latitude must be a number")
    val longitude = parts[1].toDoubleOrNull()
        ?: throw BadRequestException("Coordinate longitude must be a number")
    val level = parts.getOrNull(2)?.toDoubleOrNull()
        ?: if (parts.size == 3) throw BadRequestException("Coordinate level must be a number") else null
    return Coordinate(latitude, longitude, level)
}
