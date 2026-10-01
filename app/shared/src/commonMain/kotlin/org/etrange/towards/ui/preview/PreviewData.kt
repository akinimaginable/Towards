package org.etrange.towards.ui.preview

import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.GeocodeResult
import org.etrange.towards.domain.model.Itinerary
import org.etrange.towards.domain.model.JourneyLeg
import org.etrange.towards.domain.model.LocationKind
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.TransportMode
import org.etrange.towards.ui.home.NearbyDeparture
import org.etrange.towards.ui.home.NearbyStop
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

internal fun previewSuggestions() = listOf(
    GeocodeResult(
        id = "stop:bru",
        kind = LocationKind.STOP,
        name = "Bruxelles-Central",
        coordinate = Coordinate(50.8453, 4.3570),
        country = "Belgium",
    ),
    GeocodeResult(
        id = "place:gp",
        kind = LocationKind.PLACE,
        name = "Grand Place",
        coordinate = Coordinate(50.8467, 4.3525),
        street = "Grand Place",
        country = "Belgium",
    ),
)

internal fun previewNearbyStops(): List<NearbyStop> {
    val now = Clock.System.now()
    fun departure(
        id: String,
        line: String,
        headsign: String,
        mode: TransportMode,
        color: String,
        textColor: String,
        inMinutes: Int,
        realTime: Boolean,
        later: List<Int> = emptyList(),
    ) = NearbyDeparture(
        id = id,
        lineName = line,
        headsign = headsign,
        mode = mode,
        routeColor = color,
        routeTextColor = textColor,
        time = now + inMinutes.minutes,
        realTime = realTime,
        laterTimes = later.map { now + it.minutes },
    )
    return listOf(
        NearbyStop(
            id = "stop:debrouckere",
            name = "De Brouckère",
            coordinate = Coordinate(50.8510, 4.3525),
            distanceMeters = 34,
            departures = listOf(
                departure("1", "1", "GARE DE L'OUEST", TransportMode.SUBWAY, "B4378B", "FFFFFF", 0, true, listOf(6, 13)),
                departure("2", "29", "HOF TEN BERG", TransportMode.BUS, "ED7807", "000000", 0, false, listOf(12)),
                departure("3", "10", "GARE DU MIDI", TransportMode.TRAM, "8F4199", "FFFFFF", 1, true, listOf(9)),
                departure("4", "1", "STOCKEL", TransportMode.SUBWAY, "B4378B", "FFFFFF", 2, true, listOf(9, 16)),
                departure("5", "5", "HERRMANN-DEBROUX", TransportMode.SUBWAY, "F5A90B", "000000", 7, false),
                departure("6", "4", "WIELS", TransportMode.TRAM, "EA4F7F", "FFFFFF", 8, false, listOf(16)),
            ),
        ),
        NearbyStop(
            id = "stop:bourse",
            name = "Bourse",
            coordinate = Coordinate(50.8481, 4.3497),
            distanceMeters = 280,
            departures = listOf(
                departure("7", "3", "CHURCHILL", TransportMode.TRAM, "FFDD00", "000000", 5, true, listOf(12)),
            ),
        ),
    )
}

internal fun previewItineraries(): List<Itinerary> {
    val bourse = Place("stop:bourse", "Bourse", Coordinate(50.848, 4.35))
    val grandPlace = Place("place:gp", "Grand Place", Coordinate(50.8467, 4.3525))
    val origin = Place(null, "My location", Coordinate(50.8503, 4.3517))
    return listOf(
        Itinerary(
            id = "opt-1",
            durationSeconds = 12 * 60,
            startTime = "2026-09-07T10:04:00+02:00",
            endTime = "2026-09-07T10:16:00+02:00",
            transfers = 0,
            legs = listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = bourse,
                    startTime = "2026-09-07T10:04:00+02:00",
                    endTime = "2026-09-07T10:08:00+02:00",
                    scheduledStartTime = "2026-09-07T10:04:00+02:00",
                    scheduledEndTime = "2026-09-07T10:08:00+02:00",
                    durationSeconds = 240,
                    realTime = false,
                    distanceMeters = 280.0,
                ),
                JourneyLeg(
                    mode = TransportMode.TRAM,
                    from = bourse,
                    to = grandPlace,
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
            ),
        ),
        Itinerary(
            id = "opt-2",
            durationSeconds = 18 * 60,
            startTime = "2026-09-07T10:06:00+02:00",
            endTime = "2026-09-07T10:24:00+02:00",
            transfers = 0,
            legs = listOf(
                JourneyLeg(
                    mode = TransportMode.WALK,
                    from = origin,
                    to = grandPlace,
                    startTime = "2026-09-07T10:06:00+02:00",
                    endTime = "2026-09-07T10:24:00+02:00",
                    scheduledStartTime = "2026-09-07T10:06:00+02:00",
                    scheduledEndTime = "2026-09-07T10:24:00+02:00",
                    durationSeconds = 18 * 60,
                    realTime = false,
                    distanceMeters = 1_200.0,
                ),
            ),
        ),
    )
}
