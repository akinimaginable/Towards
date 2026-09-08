package org.etrange.towards.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.etrange.towards.domain.model.EncodedPath

class EncodedPolylineTest {
    @Test
    fun decodesGoogleSampleAtPrecision5() {
        val points = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, absoluteTolerance = 0.0001)
        assertEquals(-120.2, points[0].longitude, absoluteTolerance = 0.0001)
        assertEquals(40.7, points[1].latitude, absoluteTolerance = 0.0001)
        assertEquals(-120.95, points[1].longitude, absoluteTolerance = 0.0001)
        assertEquals(43.252, points[2].latitude, absoluteTolerance = 0.0001)
        assertEquals(-126.453, points[2].longitude, absoluteTolerance = 0.0001)
    }

    @Test
    fun emptyPolylineDecodesToNothing() {
        assertTrue(decodePolyline("", precision = 5).isEmpty())
        assertTrue(EncodedPath("", 5, 0).decodeCoordinates().isEmpty())
    }
}
