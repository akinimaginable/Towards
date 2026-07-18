package org.etrange.towards.application

import io.ktor.http.HttpStatusCode

open class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: Map<String, String> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class BadRequestException(
    message: String,
    details: Map<String, String> = emptyMap(),
) : ApiException(HttpStatusCode.BadRequest, "INVALID_REQUEST", message, details)

class NotFoundException(message: String) :
    ApiException(HttpStatusCode.NotFound, "NOT_FOUND", message)

class UpstreamServiceException(
    status: HttpStatusCode,
    message: String,
    cause: Throwable? = null,
) : ApiException(status, "PLANNER_UNAVAILABLE", message, cause = cause)
