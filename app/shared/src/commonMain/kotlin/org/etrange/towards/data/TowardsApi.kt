package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.ErrorResponseDto

class TowardsApi(
    @PublishedApi internal val client: HttpClient,
    @PublishedApi internal val config: ApiConfig,
    @PublishedApi internal val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
) {
    suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T = execute {
        client.get(config.baseUrl.trimEnd('/') + path) {
            accept(ContentType.Application.Json)
            configure()
        }
    }

    suspend inline fun <reified T> post(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T = execute {
        client.post(config.baseUrl.trimEnd('/') + path) {
            accept(ContentType.Application.Json)
            configure()
        }
    }

    @PublishedApi
    internal suspend inline fun <reified T> execute(send: () -> HttpResponse): T {
        val response = try {
            send()
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: HttpRequestTimeoutException) {
            throw ApiException(ApiError.Timeout, cause = cause)
        } catch (cause: Exception) {
            throw ApiException(ApiError.Offline, cause = cause)
        }

        if (response.status.isSuccess()) {
            return try {
                response.body()
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                throw ApiException(ApiError.MalformedResponse, cause = cause)
            }
        }

        val error = runCatching { json.decodeFromString<ErrorResponseDto>(response.bodyAsText()) }.getOrNull()
        throw ApiException(
            error = response.status.toApiError(error?.code),
            correlationId = error?.correlationId,
        )
    }
}

@PublishedApi
internal fun HttpStatusCode.toApiError(code: String?): ApiError = when {
    this == HttpStatusCode.TooManyRequests -> ApiError.RateLimited
    this == HttpStatusCode.NotFound -> ApiError.NotFound
    this == HttpStatusCode.BadRequest || this == HttpStatusCode.UnprocessableEntity ->
        ApiError.InvalidRequest(code ?: "INVALID_REQUEST")
    value in 500..599 -> ApiError.ServiceUnavailable
    else -> ApiError.Unexpected(value)
}

/** Turns a domain-mapping failure into [ApiError.MalformedResponse] without hiding cancellation. */
internal inline fun <T> mapDomain(block: () -> T): T =
    try {
        block()
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: ApiException) {
        throw cause
    } catch (cause: Exception) {
        throw ApiException(ApiError.MalformedResponse, cause = cause)
    }
