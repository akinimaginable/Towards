package org.etrange.towards.routes

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
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
import java.security.MessageDigest
import kotlin.time.Instant
import org.etrange.towards.api.dto.HealthResponseDto
import org.etrange.towards.api.dto.RefreshItineraryRequestDto
import org.etrange.towards.application.BadRequestException
import org.etrange.towards.application.MobilityService
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.LocationReference
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ItineraryRefreshRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.model.requests.TripLookupRequest
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import org.etrange.towards.plugins.API_RATE_LIMIT

private const val METRICS_AUTH = "metrics"
private val RadiusMeters = 50..2_000
private val TransferLimit = 0..10
private const val TextLimit = 120
private const val IdentifierLimit = 2_048
private const val OpaqueLimit = 16 * 1_024
private const val LanguageTagLimit = 35
private const val MaxLanguages = 8
private const val MaxListValues = 16
private const val CoordinateLimit = 80

fun Application.configureRoutes(
    service: MobilityService,
    prometheusRegistry: PrometheusMeterRegistry,
    metricsToken: String,
) {
    install(Authentication) {
        bearer(METRICS_AUTH) {
            authenticate { credential ->
                if (credential.token.matchesSecret(metricsToken)) {
                    UserIdPrincipal("prometheus")
                } else {
                    null
                }
            }
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponseDto(status = "UP"))
        }
        authenticate(METRICS_AUTH) {
            get("/metrics") {
                call.respondText(
                    text = prometheusRegistry.scrape(),
                    contentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8"),
                )
            }
        }

        rateLimit(RateLimitName(API_RATE_LIMIT)) {
            route("/api/v1") {
                get("/trips/plan") {
                    val request = TripPlanningRequest(
                        from = call.requiredQuery("from").toLocationReference(),
                        to = call.requiredQuery("to").toLocationReference(),
                        time = call.isoInstant("time"),
                        arriveBy = call.booleanQuery("arriveBy") ?: false,
                        transitModes = call.enumList("transitModes", TransportMode.TRANSIT),
                        directModes = call.enumList("directModes", TransportMode.WALK),
                        maxTransfers = call.boundedInt("maxTransfers", TransferLimit),
                        pageCursor = call.optionalText("pageCursor", OpaqueLimit),
                        language = call.languages("language"),
                    )
                    call.respond(service.plan(call.actor(), call.correlationId(), request).toDto())
                }

                get("/trips/{tripId}") {
                    val tripId = call.parameters["tripId"]?.takeIf { it.isNotBlank() }
                        ?: throw BadRequestException("tripId is required")
                    if (tripId.length > IdentifierLimit) {
                        throw BadRequestException("tripId is too long")
                    }
                    val request = TripLookupRequest(
                        tripId = tripId,
                        includeScheduledSkippedStops = call.booleanQuery("withScheduledSkippedStops")
                            ?: false,
                        language = call.languages("language"),
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
                    if (body.itineraryId.length > OpaqueLimit) {
                        throw BadRequestException("itineraryId is too long")
                    }
                    val request = ItineraryRefreshRequest(
                        itineraryId = body.itineraryId,
                        language = body.language.boundedLanguages(),
                    )
                    call.respond(
                        service.refreshItinerary(
                            call.actor(), call.correlationId(), request
                        ).toDto()
                    )
                }

                get("/stop-times") {
                    val center = call.request.queryParameters["center"]?.toCoordinate()
                    val request = StopTimesRequest(
                        stopId = call.optionalText("stopId", IdentifierLimit),
                        center = center,
                        radiusMeters = call.boundedInt("radius", RadiusMeters),
                        time = call.isoInstant("time"),
                        arriveBy = call.booleanQuery("arriveBy") ?: false,
                        numberOfEvents = call.boundedInt("n", StopTimesRequest.EVENT_COUNT)
                            ?: StopTimesRequest.DEFAULT_EVENT_COUNT,
                        transportModes = call.enumList("modes", TransportMode.TRANSIT),
                        pageCursor = call.optionalText("pageCursor", OpaqueLimit),
                        language = call.languages("language"),
                    )
                    call.respond(
                        service.getStopTimes(call.actor(), call.correlationId(), request).toDto()
                    )
                }

                get("/nearby") {
                    call.respond(
                        service.nearbyDepartures(
                            call.actor(),
                            call.correlationId(),
                            call.requiredQuery("center").toCoordinate(),
                        ).map { it.toDto() },
                    )
                }

                get("/geocode") {
                    val request = GeocodeRequest(
                        text = call.boundedText("text", TextLimit),
                        languages = call.languages("language"),
                        kinds = call.locationKinds("types"),
                        modes = call.enumList("modes"),
                        bias = call.request.queryParameters["bias"]?.toCoordinate(),
                        numberOfResults = call.boundedInt("limit", GeocodeRequest.RESULT_COUNT)
                            ?: GeocodeRequest.DEFAULT_RESULT_COUNT,
                    )
                    call.respond(
                        service.geocode(call.actor(), call.correlationId(), request)
                            .map { it.toDto() })
                }

                get("/reverse-geocode") {
                    val request = ReverseGeocodeRequest(
                        coordinate = call.requiredQuery("place").toCoordinate(),
                        kinds = call.locationKinds("types"),
                        numberOfResults = call.boundedInt("limit", GeocodeRequest.RESULT_COUNT)
                            ?: GeocodeRequest.DEFAULT_RESULT_COUNT,
                    )
                    call.respond(
                        service.reverseGeocode(call.actor(), call.correlationId(), request)
                            .map { it.toDto() })
                }

            }
        }
    }
}

private fun ApplicationCall.actor(): org.etrange.towards.domain.model.ActorContext? = null

private fun ApplicationCall.correlationId(): String = callId ?: "unknown"

private fun ApplicationCall.requiredQuery(name: String): String =
    request.queryParameters[name]?.takeIf { it.isNotBlank() }
        ?: throw BadRequestException("$name is required")

private fun ApplicationCall.listQuery(name: String): List<String> =
    request.queryParameters.getAll(name).orEmpty().flatMap { value -> value.split(',') }
        .map { it.trim() }.filter { it.isNotEmpty() }

private fun ApplicationCall.boundedText(name: String, max: Int): String {
    val value = requiredQuery(name)
    if (value.length > max) throw BadRequestException("$name is too long")
    return value
}

private fun ApplicationCall.optionalText(name: String, max: Int): String? {
    val value = request.queryParameters[name] ?: return null
    if (value.length > max) throw BadRequestException("$name is too long")
    return value.takeIf { it.isNotBlank() }
}

private fun ApplicationCall.boundedInt(name: String, range: IntRange): Int? =
    intQuery(name)?.also { value ->
        if (value !in range) throw BadRequestException("$name must be within $range")
    }

private fun ApplicationCall.isoInstant(name: String): String? =
    optionalText(name, 40)?.also { value ->
        runCatching { Instant.parse(value) }.getOrElse {
            throw BadRequestException("$name must be ISO-8601")
        }
    }

private fun ApplicationCall.languages(name: String): List<String> =
    listQuery(name).boundedLanguages()

private fun List<String>.boundedLanguages(): List<String> {
    if (size > MaxLanguages) throw BadRequestException("language has too many values")
    if (any { it.length > LanguageTagLimit }) throw BadRequestException("language is too long")
    return this
}

private fun ApplicationCall.enumList(
    name: String,
    vararg defaults: TransportMode,
): Set<TransportMode> {
    val values = listQuery(name)
    if (values.isEmpty()) return defaults.toSet()
    if (values.size > MaxListValues) throw BadRequestException("$name has too many values")
    return values.mapTo(linkedSetOf()) { value ->
        runCatching { TransportMode.valueOf(value.uppercase()) }.getOrElse {
            throw BadRequestException(
                "Unsupported transport mode",
                mapOf(name to value),
            )
        }
    }
}

private fun ApplicationCall.locationKinds(name: String): Set<LocationKind> {
    val values = listQuery(name)
    if (values.size > MaxListValues) throw BadRequestException("$name has too many values")
    return values.mapTo(linkedSetOf()) { value ->
        runCatching { LocationKind.valueOf(value.uppercase()) }.getOrElse {
            throw BadRequestException(
                "Unsupported location type",
                mapOf(name to value),
            )
        }
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

private fun String.toLocationReference(): LocationReference {
    if (length > IdentifierLimit) throw BadRequestException("A location is too long")
    return if (contains(',')) LocationReference.Position(toCoordinate()) else LocationReference.Stop(this)
}

private fun String.toCoordinate(): Coordinate {
    if (length > CoordinateLimit) throw BadRequestException("A coordinate is too long")
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

private fun String.matchesSecret(expected: String): Boolean =
    MessageDigest.isEqual(encodeToByteArray(), expected.encodeToByteArray())
