package org.etrange.towards.api.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import org.etrange.towards.domain.model.LocationKind

class DtoMappingResilienceTest {
    @Test
    fun unknownLocationKindBecomesAPlace() {
        val result = GeocodeResultDto(
            id = "pad",
            kind = "SPACESHIP",
            name = "Pad",
            coordinate = CoordinateDto(1.0, 2.0),
        ).toDomain()

        assertEquals(LocationKind.PLACE, result.kind)
        assertEquals("pad", result.id)
    }

    @Test
    fun oneInvalidGeocodeCoordinateDoesNotDropTheList() {
        val results = listOf(
            GeocodeResultDto("ok", "STOP", "Ok", CoordinateDto(50.0, 4.0)),
            GeocodeResultDto("bad", "STOP", "Bad", CoordinateDto(999.0, 4.0)),
        ).toGeocodeResults()

        assertEquals(listOf("ok"), results.map { it.id })
    }

    @Test
    fun oneInvalidStopEventDoesNotDropTheBoard() {
        val times = StopTimesDto(
            place = PlaceDto(name = "A", coordinate = CoordinateDto(50.0, 4.0)),
            events = listOf(
                stopTime(tripId = "good", latitude = 50.0),
                stopTime(tripId = "bad", latitude = 999.0),
            ),
        ).toDomain()

        assertEquals(listOf("good"), times.events.map { it.tripId })
    }

    private fun stopTime(tripId: String, latitude: Double) = StopTimeDto(
        place = PlaceDto(name = tripId, coordinate = CoordinateDto(latitude, 4.0)),
        mode = "BUS",
        realTime = true,
        tripId = tripId,
        cancelled = false,
    )
}
