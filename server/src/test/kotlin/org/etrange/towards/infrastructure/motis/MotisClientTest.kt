package org.etrange.towards.infrastructure.motis

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.etrange.towards.config.MotisConfig
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.LocationReference
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.requests.GeocodeRequest
import org.etrange.towards.domain.model.requests.ReverseGeocodeRequest
import org.etrange.towards.domain.model.requests.StopTimesRequest
import org.etrange.towards.domain.model.requests.TripPlanningRequest
import kotlin.test.Test
import kotlin.test.assertEquals

class MotisClientTest {
    @Test
    fun planMapsProviderResponseToDomainAndForwardsParameters() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val engine = MockEngine { request ->
            assertEquals("/api/v6/plan", request.url.encodedPath)
            assertEquals("50.8453,4.357", request.url.parameters["fromPlace"])
            assertEquals("51.2172,4.4211", request.url.parameters["toPlace"])
            assertEquals("TRANSIT", request.url.parameters["transitModes"])

            respond(
                content = PLAN_RESPONSE,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val client = MotisClient(
            client = httpClient,
            config = MotisConfig("https://motis.test", 1_000),
            json = json,
        )

        val result = client.plan(
            TripPlanningRequest(
                from = LocationReference.Position(Coordinate(50.8453, 4.3570)),
                to = LocationReference.Position(Coordinate(51.2172, 4.4211)),
                transitModes = setOf(TransportMode.TRANSIT),
            ),
        )

        assertEquals("Bruxelles-Central", result.from.name)
        assertEquals("Antwerpen-Centraal", result.to.name)
        assertEquals(1, result.itineraries.size)
        assertEquals("IC 2031", result.itineraries.single().legs.single().displayName)
        httpClient.close()
    }

    @Test
    fun omittedCountsAreSentAsDefaults() = runBlocking {
        val json = Json { ignoreUnknownKeys = true }
        val engine = MockEngine { request ->
            val body = if (request.url.encodedPath == "/api/v6/stoptimes") "{}" else "[]"
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
        val client = MotisClient(
            client = httpClient,
            config = MotisConfig("https://motis.test", 1_000),
            json = json,
        )

        client.getStopTimes(StopTimesRequest(stopId = "stop-1"))
        client.geocode(GeocodeRequest(text = "Central"))
        client.reverseGeocode(ReverseGeocodeRequest(Coordinate(50.85, 4.35)))

        val requests = engine.requestHistory
        assertEquals(StopTimesRequest.DEFAULT_EVENT_COUNT.toString(), requests[0].url.parameters["n"])
        assertEquals(GeocodeRequest.DEFAULT_RESULT_COUNT.toString(), requests[1].url.parameters["numResults"])
        assertEquals(GeocodeRequest.DEFAULT_RESULT_COUNT.toString(), requests[2].url.parameters["numResults"])
        httpClient.close()
    }
}

private const val PLAN_RESPONSE = """
{
  "from": {"name":"Bruxelles-Central","lat":50.8453,"lon":4.3570,"stopId":"be:brussels"},
  "to": {"name":"Antwerpen-Centraal","lat":51.2172,"lon":4.4211,"stopId":"be:antwerp"},
  "direct": [],
  "itineraries": [{
    "duration": 2460,
    "startTime": "2026-07-24T08:00:00+02:00",
    "endTime": "2026-07-24T08:41:00+02:00",
    "transfers": 0,
    "id": "belgium-itinerary",
    "legs": [{
      "mode": "RAIL",
      "startTime": "2026-07-24T08:00:00+02:00",
      "endTime": "2026-07-24T08:41:00+02:00",
      "scheduledStartTime": "2026-07-24T08:00:00+02:00",
      "scheduledEndTime": "2026-07-24T08:41:00+02:00",
      "realTime": true,
      "duration": 2460,
      "from": {"name":"Bruxelles-Central","lat":50.8453,"lon":4.3570},
      "to": {"name":"Antwerpen-Centraal","lat":51.2172,"lon":4.4211},
      "displayName": "IC 2031"
    }]
  }],
  "previousPageCursor": "previous",
  "nextPageCursor": "next"
}
"""
