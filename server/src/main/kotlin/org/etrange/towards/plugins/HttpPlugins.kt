package org.etrange.towards.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.principal
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.ErrorResponseDto
import org.etrange.towards.application.AuditAction
import org.etrange.towards.application.AuditOutcome
import org.etrange.towards.application.AuditRecord
import org.etrange.towards.application.AuditService
import org.etrange.towards.application.ApiException
import org.etrange.towards.config.AppConfig
import org.etrange.towards.domain.model.UserId
import org.slf4j.event.Level
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

const val API_RATE_LIMIT = "api"

fun Application.configureHttpPlugins(
    config: AppConfig,
    prometheusRegistry: PrometheusMeterRegistry,
    auditService: AuditService,
) {
    val applicationLogger = log

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        verify { it.length in 1..128 && it.all { character -> character.isLetterOrDigit() || character in "-_." } }
        generate { UUID.randomUUID().toString() }
        replyToHeader(HttpHeaders.XRequestId)
    }

    install(CallLogging) {
        level = Level.INFO
        mdc("correlationId") { call -> call.callId }
        mdc("httpMethod") { call -> call.request.httpMethod.value }
        mdc("path") { call -> call.request.path() }
        format { call ->
            "http_request method=${call.request.httpMethod.value} path=${call.request.path()} " +
                "status=${call.response.status()?.value ?: 0} latency_ms=${call.processingTimeMillis()}"
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            },
        )
    }

    install(Authentication) {
        dummy {
            userId = UserId(config.authentication.dummyUserId)
        }
    }

    install(RateLimit) {
        register(RateLimitName(API_RATE_LIMIT)) {
            rateLimiter(
                limit = config.rateLimit.requests,
                refillPeriod = config.rateLimit.periodSeconds.seconds,
            )
            requestKey { call ->
                call.principal<DummyPrincipal>()?.userId?.value
                    ?: call.request.local.remoteHost
            }
        }
    }

    install(MicrometerMetrics) {
        registry = prometheusRegistry
    }

    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(
                status,
                ErrorResponseDto(
                    code = "RATE_LIMIT_EXCEEDED",
                    message = "Too many requests. Please retry later.",
                    correlationId = call.callId,
                ),
            )
        }
        exception<ApiException> { call, cause ->
            auditService.recordSystemError(call, cause)
            call.respond(
                cause.status,
                ErrorResponseDto(
                    code = cause.code,
                    message = cause.message,
                    correlationId = call.callId,
                    details = cause.details,
                ),
            )
        }
        exception<SerializationException> { call, cause ->
            auditService.recordSystemError(call, cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(
                    code = "INVALID_JSON",
                    message = cause.message ?: "The request body is invalid",
                    correlationId = call.callId,
                ),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            auditService.recordSystemError(call, cause)
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponseDto(
                    code = "INVALID_REQUEST",
                    message = cause.message ?: "The request is invalid",
                    correlationId = call.callId,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            auditService.recordSystemError(call, cause)
            applicationLogger.error("unhandled_request_error correlationId={}", call.callId, cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponseDto(
                    code = "INTERNAL_ERROR",
                    message = "An unexpected error occurred",
                    correlationId = call.callId,
                ),
            )
        }
    }
}

private suspend fun AuditService.recordSystemError(
    call: ApplicationCall,
    cause: Throwable,
) {
    record(
        AuditRecord(
            actor = call.principal<DummyPrincipal>()?.toActorContext(),
            correlationId = call.callId ?: "unknown",
            action = AuditAction.SYSTEM_ERROR,
            outcome = AuditOutcome.FAILURE,
            requestSummary = "${call.request.httpMethod.value} ${call.request.path()}",
            resultSummary = null,
            errorDetails = "${cause::class.simpleName}: ${cause.message}".take(2_000),
            durationMillis = call.processingTimeMillis(),
        ),
    )
}
