package org.etrange.towards.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.etrange.towards.api.dto.HealthResponseDto

class TowardsApiTest {
    @Test
    fun serverErrorTextIsNotShownToTheUser() = runTest {
        val api = api(
            status = HttpStatusCode.BadRequest,
            body = """{"code":"INVALID_REQUEST","message":"latitude must be between -90 and 90","correlationId":"abc"}""",
        )

        val error = assertIs<ApiException>(failureOf(api))

        assertEquals(ApiError.InvalidRequest("INVALID_REQUEST"), error.error)
        assertEquals("abc", error.correlationId)
        assertFalse(error.userMessage().contains("latitude"))
        assertEquals(error.userMessage(), error.message)
    }

    @Test
    fun statusCodesBecomeTypedErrors() = runTest {
        assertEquals(ApiError.RateLimited, failure(HttpStatusCode.TooManyRequests).error)
        assertEquals(ApiError.NotFound, failure(HttpStatusCode.NotFound).error)
        assertEquals(ApiError.ServiceUnavailable, failure(HttpStatusCode.BadGateway).error)
        assertEquals(ApiError.Unexpected(418), failure(HttpStatusCode(418, "Teapot")).error)
    }

    @Test
    fun aBrokenSuccessBodyIsMalformed() = runTest {
        val error = assertIs<ApiException>(failureOf(api(HttpStatusCode.OK, "{")))
        assertEquals(ApiError.MalformedResponse, error.error)
    }

    @Test
    fun aFailedCallIsOffline() = runTest {
        val client = HttpClient(MockEngine { throw IllegalStateException("no network") }) {
            expectSuccess = false
        }
        val error = assertIs<ApiException>(
            failureOf(TowardsApi(client, ApiConfig("https://api.test"))),
        )
        assertEquals(ApiError.Offline, error.error)
        assertFalse(error.userMessage().contains("no network"))
    }

    private suspend fun failure(status: HttpStatusCode): ApiException =
        assertIs(failureOf(api(status, """{"code":"X","message":"secret"}""")))

    private suspend fun failureOf(api: TowardsApi): Throwable? =
        runCatching { api.get<HealthResponseDto>("/health") }.exceptionOrNull()

    private fun api(status: HttpStatusCode, body: String): TowardsApi {
        val client = HttpClient(MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }) {
            expectSuccess = false
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        return TowardsApi(client, ApiConfig("https://api.test"))
    }
}
