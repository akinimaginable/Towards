package org.etrange.towards.data

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.etrange.towards.data.desktop.DesktopLocation
import org.etrange.towards.data.desktop.LocationAuthorization
import org.etrange.towards.data.desktop.awaitLocationPermission
import org.etrange.towards.data.desktop.coordinateOrNull
import org.etrange.towards.data.desktop.parseHelperAuthorization
import org.etrange.towards.data.desktop.parseHelperLocation
import org.etrange.towards.data.desktop.parseHelperPermission
import org.etrange.towards.domain.model.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmLocationProviderTest {

    @Test
    fun helperOutputMapsToAuthorizationAndCoordinates() {
        assertEquals(LocationAuthorization.NotDetermined, parseHelperAuthorization("0\n"))
        assertEquals(LocationAuthorization.Denied, parseHelperAuthorization("1"))
        assertEquals(LocationAuthorization.Granted, parseHelperAuthorization("2"))
        assertEquals(LocationAuthorization.Unavailable, parseHelperAuthorization("3"))
        assertTrue(parseHelperPermission("granted"))
        assertFalse(parseHelperPermission("denied"))
        assertEquals(Coordinate(latitude = 50.85, longitude = 4.35), parseHelperLocation("50.85 4.35"))
        assertNull(parseHelperLocation("granted"))
        assertNull(parseHelperLocation("120 4"))
    }

    @Test
    fun permissionFollowsAuthorization() {
        val location = FakeDesktopLocation()
        val provider = JvmLocationProvider(location)

        location.authorizationState = LocationAuthorization.NotDetermined
        assertFalse(provider.hasPermission())
        location.authorizationState = LocationAuthorization.Denied
        assertFalse(provider.hasPermission())
        location.authorizationState = LocationAuthorization.Unavailable
        assertFalse(provider.hasPermission())
        location.authorizationState = LocationAuthorization.Granted
        assertTrue(provider.hasPermission())
    }

    @Test
    fun lastKnownIsHiddenUntilGranted() {
        val location = FakeDesktopLocation()
        location.known = Coordinate(latitude = 50.85, longitude = 4.35)
        val provider = JvmLocationProvider(location)

        assertNull(provider.lastKnownCoordinate())

        location.authorizationState = LocationAuthorization.Granted
        assertEquals(location.known, provider.lastKnownCoordinate())
    }

    @Test
    fun currentFixRequiresPermission() = runBlocking {
        val location = FakeDesktopLocation()
        location.fix = Coordinate(latitude = 48.86, longitude = 2.35)
        val provider = JvmLocationProvider(location)

        assertNull(provider.currentCoordinate())
        assertEquals(0, location.currentCalls)

        location.authorizationState = LocationAuthorization.Granted
        assertEquals(location.fix, provider.currentCoordinate())
        assertEquals(1, location.currentCalls)
    }

    @Test
    fun currentFixCanBeMissing() = runBlocking {
        val location = FakeDesktopLocation()
        location.authorizationState = LocationAuthorization.Granted
        val provider = JvmLocationProvider(location)

        assertNull(provider.currentCoordinate())
    }

    @Test
    fun currentFixTimesOutAndCancels() = runBlocking {
        val location = FakeDesktopLocation()
        location.authorizationState = LocationAuthorization.Granted
        location.suspendCurrent = true
        val provider = JvmLocationProvider(location, fixTimeout = 200.milliseconds)

        assertNull(provider.currentCoordinate())
        assertTrue(location.cancelled)
    }

    @Test
    fun permissionResultIsReportedOnlyAfterThePlatformAnswers() = runBlocking {
        val location = FakeDesktopLocation()
        val results = mutableListOf<Boolean>()
        val job = launch {
            awaitLocationPermission(location) { granted ->
                results += granted
            }
        }

        location.permissionEntered.await()
        assertEquals(emptyList(), results)

        location.permissionResult.complete(true)
        job.join()

        assertEquals(listOf(true), results)
        assertTrue(JvmLocationProvider(location).hasPermission())
    }

    @Test
    fun deniedPermissionIsReported() = runBlocking {
        val location = FakeDesktopLocation()
        val results = mutableListOf<Boolean>()
        val job = launch {
            awaitLocationPermission(location) { granted ->
                results += granted
            }
        }

        location.permissionEntered.await()
        location.permissionResult.complete(false)
        job.join()

        assertEquals(listOf(false), results)
        assertFalse(JvmLocationProvider(location).hasPermission())
    }

    @Test
    fun permissionFailureIsReportedAsDenied() = runBlocking {
        val location = object : DesktopLocation {
            override fun authorization(): LocationAuthorization = LocationAuthorization.NotDetermined

            override fun lastKnown(): Coordinate? = null

            override suspend fun requestPermission(): Boolean = error("location services unavailable")

            override suspend fun current(): Coordinate? = null

            override fun cancel() = Unit
        }
        val results = mutableListOf<Boolean>()

        awaitLocationPermission(location) { granted ->
            results += granted
        }

        assertEquals(listOf(false), results)
    }

    @Test
    fun rejectsInvalidNativeCoordinates() {
        assertNull(coordinateOrNull(latitude = 120.0, longitude = 0.0))
        assertEquals(Coordinate(latitude = 1.0, longitude = 2.0), coordinateOrNull(1.0, 2.0))
    }
}

private class FakeDesktopLocation : DesktopLocation {
    var authorizationState = LocationAuthorization.NotDetermined
    var known: Coordinate? = null
    var fix: Coordinate? = null
    var suspendCurrent = false
    var currentCalls = 0
    var cancelled = false
    val permissionEntered = CompletableDeferred<Unit>()
    val permissionResult = CompletableDeferred<Boolean>()

    override fun authorization(): LocationAuthorization = authorizationState

    override fun lastKnown(): Coordinate? = known

    override suspend fun requestPermission(): Boolean {
        when (authorizationState) {
            LocationAuthorization.Granted -> return true
            LocationAuthorization.Denied,
            LocationAuthorization.Unavailable,
            -> return false
            LocationAuthorization.NotDetermined -> Unit
        }
        if (!permissionEntered.isCompleted) {
            permissionEntered.complete(Unit)
        }
        val granted = permissionResult.await()
        authorizationState = if (granted) {
            LocationAuthorization.Granted
        } else {
            LocationAuthorization.Denied
        }
        return granted
    }

    override suspend fun current(): Coordinate? {
        currentCalls += 1
        if (!suspendCurrent) return fix
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancelled = true }
        }
    }

    override fun cancel() {
        cancelled = true
    }
}
