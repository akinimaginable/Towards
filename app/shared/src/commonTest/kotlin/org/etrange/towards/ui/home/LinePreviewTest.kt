package org.etrange.towards.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.ui.home.nearby.linePreviewOf
import org.etrange.towards.ui.home.nearby.splitLineAtStop

class LinePreviewTest {

    private val start = Coordinate(50.00, 4.00)
    private val middle = Coordinate(50.01, 4.00)
    private val end = Coordinate(50.02, 4.00)

    @Test
    fun stopInTheMiddleSharesTheSplitPoint() {
        val (passed, ahead) = splitLineAtStop(listOf(start, middle, end), middle)

        assertEquals(listOf(start, middle), passed)
        assertEquals(listOf(middle, end), ahead)
    }

    @Test
    fun stopAtTheStartLeavesThePassedPartEmpty() {
        val (passed, ahead) = splitLineAtStop(listOf(start, middle, end), start)

        assertEquals(emptyList(), passed)
        assertEquals(listOf(start, middle, end), ahead)
    }

    @Test
    fun missingGeometryFallsBackToTheStopSequence() {
        val itinerary = Itinerary(
            id = "trip",
            durationSeconds = 60,
            startTime = "2026-08-06T12:00:00Z",
            endTime = "2026-08-06T12:01:00Z",
            transfers = 0,
            legs = listOf(
                JourneyLeg(
                    mode = TransportMode.TRAM,
                    from = Place("a", "A", start, arrival = "2026-08-06T12:00:00Z"),
                    to = Place("c", "C", end, arrival = "2026-08-06T12:10:00Z"),
                    startTime = "2026-08-06T12:00:00Z",
                    endTime = "2026-08-06T12:01:00Z",
                    scheduledStartTime = "2026-08-06T12:00:00Z",
                    scheduledEndTime = "2026-08-06T12:01:00Z",
                    durationSeconds = 60,
                    realTime = false,
                    intermediateStops = listOf(
                        Place("b", "B", middle, arrival = "2026-08-06T12:04:00Z"),
                    ),
                ),
            ),
        )

        val preview = linePreviewOf(itinerary, middle, "FF0000")

        assertEquals(middle, preview.stop)
        assertEquals("FF0000", preview.routeColor)
        assertEquals(listOf(start, middle), preview.passed)
        assertEquals(listOf(middle, end), preview.ahead)
        assertEquals(listOf("A", "B", "C"), preview.stops.map { it.name })
        assertEquals(listOf(false, true, false), preview.stops.map { it.current })
        assertEquals(listOf(false, true, true), preview.stops.map { it.upcoming })
        assertEquals(listOf(false, false, true), preview.stops.map { it.terminus })
        assertEquals(Instant.parse("2026-08-06T12:04:00Z"), preview.stops[1].time)
    }

    @Test
    fun matchingTripIgnoresTheInterlinedReverseLeg() {
        val gare = Coordinate(47.00, -1.60)
        val plessis = Coordinate(47.02, -1.60)
        val haluchere = Coordinate(47.03, -1.60)
        fun place(name: String, at: Coordinate) = Place(name.lowercase(), name, at)
        fun leg(tripId: String, headsign: String, from: Place, to: Place, between: Place) = JourneyLeg(
            mode = TransportMode.BUS,
            from = from,
            to = to,
            startTime = "2026-08-06T12:00:00Z",
            endTime = "2026-08-06T12:30:00Z",
            scheduledStartTime = "2026-08-06T12:00:00Z",
            scheduledEndTime = "2026-08-06T12:30:00Z",
            durationSeconds = 1800,
            realTime = false,
            tripId = tripId,
            headsign = headsign,
            intermediateStops = listOf(between),
        )
        val preview = linePreviewOf(
            itinerary = Itinerary(
                id = "interlined",
                durationSeconds = 3600,
                startTime = "2026-08-06T12:00:00Z",
                endTime = "2026-08-06T13:00:00Z",
                transfers = 0,
                legs = listOf(
                    leg(
                        "outbound",
                        "Haluchere",
                        place("Gare", gare),
                        place("Haluchere", haluchere),
                        place("Plessis", plessis)
                    ),
                    leg(
                        "return",
                        "Gare",
                        place("Haluchere", haluchere),
                        place("Gare", gare),
                        place("Plessis", plessis)
                    ),
                ),
            ),
            stop = plessis,
            routeColor = "00A9E0",
            headsign = "Gare",
            tripId = "return",
        )

        assertEquals(listOf("Haluchere", "Plessis", "Gare"), preview.stops.map { it.name })
        assertEquals(1, preview.stops.count { !it.upcoming })
    }
}
