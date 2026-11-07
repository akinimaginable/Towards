package org.etrange.towards.data

/**
 * A failure the app can react to. Server and parser text stays out of this type;
 * [userMessage] is the only string that should be shown.
 */
sealed interface ApiError {
    data object Offline : ApiError

    data object Timeout : ApiError

    data object RateLimited : ApiError

    data object NotFound : ApiError

    data object ServiceUnavailable : ApiError

    data object MalformedResponse : ApiError

    data class InvalidRequest(val code: String) : ApiError

    data class Unexpected(val status: Int) : ApiError
}

fun ApiError.userMessage(): String = when (this) {
    ApiError.Offline -> "Unable to reach the Towards API"
    ApiError.Timeout -> "The request timed out. Please try again."
    ApiError.RateLimited -> "Too many requests. Please retry later."
    ApiError.NotFound -> "That could not be found."
    ApiError.ServiceUnavailable -> "The trip planning service is unavailable. Please try again."
    ApiError.MalformedResponse -> "The Towards API sent an unexpected response."
    is ApiError.InvalidRequest -> "The request could not be completed."
    is ApiError.Unexpected -> "The Towards API rejected the request."
}

class ApiException(
    val error: ApiError,
    val correlationId: String? = null,
    cause: Throwable? = null,
) : Exception(error.userMessage(), cause)

fun ApiException.userMessage(): String = error.userMessage()
