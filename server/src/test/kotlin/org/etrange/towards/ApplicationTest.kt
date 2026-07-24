package org.etrange.towards

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    @Test
    fun healthAndMetricsAreExposed() = testApplication {
        environment { config = testConfig() }
        application {
            module()
        }

        val health = client.get("/health")
        assertEquals(HttpStatusCode.OK, health.status)
        assertEquals("""{"status":"UP"}""", health.bodyAsText())

        val metrics = client.get("/metrics")
        assertEquals(HttpStatusCode.OK, metrics.status)
        assertTrue(metrics.bodyAsText().contains("# HELP"))
    }

    @Test
    fun invalidRequestUsesStandardErrorAndCorrelationId() = testApplication {
        environment { config = testConfig() }
        application {
            module()
        }

        val response = client.get("/api/v1/trips/plan") {
            parameter("from", "50.8453,4.3570")
            header(HttpHeaders.XRequestId, "belgium-test-1")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("belgium-test-1", response.headers[HttpHeaders.XRequestId])
        assertTrue(response.bodyAsText().contains("INVALID_REQUEST"))
        assertTrue(response.bodyAsText().contains("belgium-test-1"))
    }

    @Test
    fun rateLimiterReturnsJsonTooManyRequests() = testApplication {
        environment {
            config = testConfig(
                "towards.rateLimit.requests" to "2",
                "towards.rateLimit.periodSeconds" to "60",
            )
        }
        application {
            module()
        }

        repeat(2) {
            val allowed = client.get("/api/v1/geocode") {
                parameter("text", "Bruxelles-Central")
            }
            assertTrue(
                allowed.status == HttpStatusCode.OK ||
                    allowed.status == HttpStatusCode.BadGateway ||
                    allowed.status == HttpStatusCode.ServiceUnavailable,
                "expected success or upstream failure, got ${allowed.status}",
            )
        }

        val limited = client.get("/api/v1/geocode") {
            parameter("text", "Bruxelles-Central")
            header(HttpHeaders.XRequestId, "belgium-rate-limit")
        }

        assertEquals(HttpStatusCode.TooManyRequests, limited.status)
        assertTrue(limited.bodyAsText().contains("RATE_LIMIT_EXCEEDED"))
        assertTrue(limited.bodyAsText().contains("belgium-rate-limit"))
        assertNotNull(limited.headers[HttpHeaders.RetryAfter])
    }
}

private fun testConfig(vararg overrides: Pair<String, String>) = MapApplicationConfig(
    "towards.motis.baseUrl" to "http://127.0.0.1:1",
    "towards.motis.requestTimeoutMillis" to "100",
    "towards.database.enabled" to "false",
    "towards.database.url" to "jdbc:postgresql://localhost:5432/towards",
    "towards.database.user" to "towards",
    "towards.database.password" to "towards",
    "towards.database.pool.maximumPoolSize" to "10",
    "towards.database.pool.minimumIdle" to "2",
    "towards.database.pool.connectionTimeoutMillis" to "30000",
    "towards.database.pool.idleTimeoutMillis" to "600000",
    "towards.database.pool.maxLifetimeMillis" to "1800000",
    "towards.rateLimit.requests" to "120",
    "towards.rateLimit.periodSeconds" to "60",
    "towards.authentication.dummyUserId" to "00000000-0000-0000-0000-000000000001",
    *overrides,
)