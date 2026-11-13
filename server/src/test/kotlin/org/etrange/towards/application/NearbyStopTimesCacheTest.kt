package org.etrange.towards.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.etrange.towards.domain.model.Coordinate
import org.etrange.towards.domain.model.Place
import org.etrange.towards.domain.model.StopTimes

class NearbyStopTimesCacheTest {
    private val sample = StopTimes(
        place = Place(id = null, name = "Stop", coordinate = Coordinate(50.85, 4.35)),
        events = emptyList(),
        previousPageCursor = null,
        nextPageCursor = null,
    )

    @Test
    fun theSameCellAndMinuteLoadsOnce() = runBlocking {
        val cache = NearbyStopTimesCache()
        val cell = Coordinate(50.8503, 4.3517).nearbyCell()
        var loads = 0

        val first = cache.getOrLoad(cell, minute = 10) { loads += 1; sample }
        val second = cache.getOrLoad(cell, minute = 10) { loads += 1; sample }

        assertEquals(1, loads)
        assertSame(first, second)
    }

    @Test
    fun aNewMinuteLoadsAgain() = runBlocking {
        val cache = NearbyStopTimesCache()
        val cell = Coordinate(50.8503, 4.3517).nearbyCell()
        var loads = 0
        val load: suspend () -> StopTimes = { loads += 1; sample }

        cache.getOrLoad(cell, minute = 10, load)
        cache.getOrLoad(cell, minute = 11, load)

        assertEquals(2, loads)
    }

    @Test
    fun failedLoadsAreRetriedAndDoNotKeepLocks() = runBlocking {
        val cache = NearbyStopTimesCache()
        val cell = Coordinate(50.8503, 4.3517).nearbyCell()

        repeat(3) {
            assertFailsWith<IllegalStateException> {
                cache.getOrLoad(cell, minute = 10) { error("upstream") }
            }
        }
        repeat(20) { index ->
            val other = Coordinate(40.0 + index, 5.0).nearbyCell()
            assertFailsWith<IllegalStateException> {
                cache.getOrLoad(other, minute = 10) { error("upstream") }
            }
        }

        assertEquals(0, cache.retainedEntryCount())
        assertEquals(0, cache.retainedLockCount())

        var loads = 0
        cache.getOrLoad(cell, minute = 10) { loads += 1; sample }
        assertEquals(1, loads)
        assertEquals(0, cache.retainedLockCount())
    }

    @Test
    fun waitingRetryKeepsItsLockAfterLeaderFailureOrCancellation() = runBlocking {
        for (cancelLeader in listOf(false, true)) {
            val cache = NearbyStopTimesCache()
            val cell = Coordinate(50.8503, 4.3517).nearbyCell()
            val failLeader = CompletableDeferred<Unit>()
            val retryStarted = CompletableDeferred<Unit>()
            val finishRetry = CompletableDeferred<Unit>()
            var retryLoads = 0
            val leader = launch(start = CoroutineStart.UNDISPATCHED) {
                if (cancelLeader) {
                    cache.getOrLoad(cell, minute = 10) { awaitCancellation() }
                } else {
                    assertFailsWith<IllegalStateException> {
                        cache.getOrLoad(cell, minute = 10) {
                            failLeader.await()
                            error("upstream")
                        }
                    }
                }
            }
            val waiting = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrLoad(cell, minute = 10) {
                    retryLoads += 1
                    retryStarted.complete(Unit)
                    finishRetry.await()
                    sample
                }
            }

            if (cancelLeader) leader.cancelAndJoin() else {
                failLeader.complete(Unit)
                leader.join()
            }
            retryStarted.await()
            val third = async(start = CoroutineStart.UNDISPATCHED) {
                cache.getOrLoad(cell, minute = 10) { retryLoads += 1; sample }
            }
            assertEquals(1, retryLoads)
            assertEquals(1, cache.retainedLockCount())

            finishRetry.complete(Unit)
            assertSame(sample, waiting.await())
            assertSame(sample, third.await())
            assertEquals(1, retryLoads)
            assertEquals(0, cache.retainedLockCount())
        }
    }

    @Test
    fun distinctCellsStayWithinTheEntryCapAndReleaseLocks() = runBlocking {
        val cache = NearbyStopTimesCache(maxEntries = 2)
        val cells = List(10) { index -> Coordinate(40.0 + index, 4.0).nearbyCell() }
        var latestLoads = 0

        cells.dropLast(1).forEach { cell ->
            cache.getOrLoad(cell, minute = 10) { sample }
        }
        cache.getOrLoad(cells.last(), minute = 10) { latestLoads += 1; sample }
        cache.getOrLoad(cells.last(), minute = 10) { latestLoads += 1; sample }

        assertEquals(1, latestLoads)
        assertEquals(2, cache.retainedEntryCount())
        assertEquals(0, cache.retainedLockCount())

        val capped = NearbyStopTimesCache(maxEntries = 1)
        val older = Coordinate(48.0, 4.0).nearbyCell()
        val newer = Coordinate(49.0, 4.0).nearbyCell()
        var olderLoads = 0
        capped.getOrLoad(older, minute = 10) { olderLoads += 1; sample }
        capped.getOrLoad(newer, minute = 10) { sample }
        capped.getOrLoad(older, minute = 10) { olderLoads += 1; sample }

        assertEquals(2, olderLoads)
        assertEquals(1, capped.retainedEntryCount())
        assertEquals(0, capped.retainedLockCount())
    }

    @Test
    fun aNewMinuteDropsThePreviousEntries() = runBlocking {
        val cache = NearbyStopTimesCache(maxEntries = 8)
        val first = Coordinate(50.0, 4.0).nearbyCell()
        val second = Coordinate(51.0, 4.0).nearbyCell()

        cache.getOrLoad(first, minute = 10) { sample }
        cache.getOrLoad(second, minute = 11) { sample }

        assertEquals(1, cache.retainedEntryCount())
        assertEquals(0, cache.retainedLockCount())
    }

    @Test
    fun nearbyPointsShareACellAndItsCenterStaysInsideIt() {
        val here = Coordinate(50.8503, 4.3517)
        val tenMetersNorth = Coordinate(50.85039, 4.3517)
        val cell = here.nearbyCell()

        assertEquals(cell, tenMetersNorth.nearbyCell())
        assertEquals(cell, cell.center().nearbyCell())
    }
}
