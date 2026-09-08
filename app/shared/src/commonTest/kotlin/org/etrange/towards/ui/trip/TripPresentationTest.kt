package org.etrange.towards.ui.trip

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.domain.model.TripPlan

class TripPresentationTest {
    private val origin = Place(null, "Start", Coordinate(50.85, 4.35))
    private val stop = Place("stop:1", "Bourse", Coordinate(50.848, 4.349))
    private val dest = Place("place:gp", "Grand Place", Coordinate(50.8467, 4.3525))

    @Test
    fun formatsDurationAndClock() {
        assertEquals("12 min", formatDuration(12 * 60))
        assertEquals("1 h", formatDuration(60 * 60))
        assertEquals("1 h 12 min", formatDuration(72 * 60))
        assertEquals("10:04", formatClock("2026-09-07T10:04:00+02:00"))
    }

    @Test
    fun combinesTransitThenWalkAndDedupesIds() {
        val transit = itinerary("opt-1", walk = false)
        val walk = itinerary("opt-walk", walk = true)
        val duplicate = itinerary("opt-1", walk = false)
        val plan = TripPlan(
            from = origin,
            to = dest,
            direct = listOf(walk),
            itineraries = listOf(transit, duplicate),
            previousPageCursor = null,
            nextPageCursor = null,
        )

        val options = combineTripOptions(plan)
        assertEquals(listOf("opt-1", "opt-walk"), options.map { it.id })
    }

    @Test
    fun walkOnlySummaryAndStreetDetection() {
        val walk = itinerary("walk", walk = true)
        assertTrue(walk.isWalkOnly())
        assertEquals("Walk", walk.summaryLabel())
        assertEquals("Walk 12 min to Grand Place", walk.legs.single().detailTitle())
    }

    @Test
    fun transitSummaryIncludesWalkAndTransfers() {
        val transit = itinerary("tram", walk = false)
        assertEquals("Walk 4 min · Direct", transit.summaryLabel())
        assertEquals("3 Churchill", transit.legs.last().detailTitle())
    }

    @Test
    fun pickingOriginKeepsDestination() {
        val origin = TripEndpoint("My location", Coordinate(50.85, 4.35))
        val destination = TripEndpoint("Grand Place", Coordinate(50.8467, 4.3525))
        val picked = TripEndpoint("Bourse", Coordinate(50.848, 4.349), stopId = "stop:bourse")

        val (from, to) = tripEndpointsAfterPicking(
            editingOrigin = true,
            picked = picked,
            counterpart = destination,
        )
        assertEquals("Bourse", from.name)
        assertEquals("Grand Place", to.name)

        val (fromDestination, toDestination) = tripEndpointsAfterPicking(
            editingOrigin = false,
            picked = picked,
            counterpart = origin,
        )
        assertEquals("My location", fromDestination.name)
        assertEquals("Bourse", toDestination.name)
    }

    private fun itinerary(id: String, walk: Boolean): Itinerary {
        val legs = if (walk) {
            listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = dest,
                    startTime = "2026-09-07T10:06:00+02:00",
                    endTime = "2026-09-07T10:18:00+02:00",
                    scheduledStartTime = "2026-09-07T10:06:00+02:00",
                    scheduledEndTime = "2026-09-07T10:18:00+02:00",
                    durationSeconds = 12 * 60,
                    realTime = false,
                ),
            )
        } else {
            listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = stop,
                    startTime = "2026-09-07T10:04:00+02:00",
                    endTime = "2026-09-07T10:08:00+02:00",
                    scheduledStartTime = "2026-09-07T10:04:00+02:00",
                    scheduledEndTime = "2026-09-07T10:08:00+02:00",
                    durationSeconds = 240,
                    realTime = false,
                ),
                JourneyLeg(
                    mode = TransportMode.TRAM,
                    from = stop,
                    to = dest,
                    startTime = "2026-09-07T10:08:00+02:00",
                    endTime = "2026-09-07T10:16:00+02:00",
                    scheduledStartTime = "2026-09-07T10:08:00+02:00",
                    scheduledEndTime = "2026-09-07T10:16:00+02:00",
                    durationSeconds = 480,
                    realTime = true,
                    displayName = "3",
                    headsign = "Churchill",
                    routeColor = "FFDD00",
                    routeTextColor = "000000",
                ),
            )
        }
        return Itinerary(
            id = id,
            durationSeconds = legs.sumOf { it.durationSeconds },
            startTime = legs.first().startTime,
            endTime = legs.last().endTime,
            transfers = if (walk) 0 else 0,
            legs = legs,
        )
    }
}
