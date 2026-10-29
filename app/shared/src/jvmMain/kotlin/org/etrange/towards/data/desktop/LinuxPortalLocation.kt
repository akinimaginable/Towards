package org.etrange.towards.data.desktop

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.etrange.towards.domain.model.Coordinate

internal class LinuxPortalLocation : DesktopLocation {
    private val lock = Any()
    private val sessionMutex = Mutex()
    private var authorizationState = LocationAuthorization.NotDetermined
    private var cached: Coordinate? = null
    private var sessionPath: String? = null
    private var expectedRequestPath: String? = null
    private var requestToken: String? = null
    private var connection: DBusConnection? = null
    private var portal: LocationPortal? = null
    private var permissionWaiter: CompletableDeferred<Boolean>? = null
    private var fixWaiter: CompletableDeferred<Coordinate?>? = null

    override fun authorization(): LocationAuthorization = synchronized(lock) { authorizationState }

    override fun lastKnown(): Coordinate? = synchronized(lock) { cached }

    override suspend fun requestPermission(): Boolean = sessionMutex.withLock {
        when (authorization()) {
            LocationAuthorization.Granted -> return@withLock true
            LocationAuthorization.Denied,
            LocationAuthorization.Unavailable,
            -> return@withLock false
            LocationAuthorization.NotDetermined -> Unit
        }
        openSession()
    }

    override suspend fun current(): Coordinate? {
        lastKnown()?.let { return it }
        val waiter = CompletableDeferred<Coordinate?>()
        val existing = synchronized(lock) {
            val known = cached
            if (known == null) {
                fixWaiter = waiter
            }
            known
        }
        if (existing != null) return existing
        return try {
            waiter.await()
        } finally {
            synchronized(lock) {
                if (fixWaiter == waiter) {
                    fixWaiter = null
                }
            }
        }
    }

    override fun cancel() {
        synchronized(lock) {
            fixWaiter?.cancel()
            fixWaiter = null
        }
    }

    private suspend fun openSession(): Boolean {
        val locationPortal = try {
            ensureConnection()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            markUnavailable()
            return false
        } catch (_: LinkageError) {
            markUnavailable()
            return false
        } ?: return false
        val bus = connection ?: return false
        val sender = bus.uniqueName.removePrefix(":").replace('.', '_')
        if (sender.isEmpty()) {
            markUnavailable()
            return false
        }
        val sessionToken = pathToken("towards_s")
        val token = pathToken("towards_r")
        val expectedSession = "/org/freedesktop/portal/desktop/session/$sender/$sessionToken"
        val expectedRequest = "/org/freedesktop/portal/desktop/request/$sender/$token"
        val waiter = CompletableDeferred<Boolean>()
        synchronized(lock) {
            sessionPath = expectedSession
            expectedRequestPath = expectedRequest
            requestToken = token
            permissionWaiter = waiter
        }
        try {
            val created = locationPortal.CreateSession(PortalOptions.createSession(sessionToken))
            synchronized(lock) { sessionPath = created.path }
            locationPortal.Start(created, "", PortalOptions.start(token))
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            markUnavailable()
            return false
        } catch (_: LinkageError) {
            markUnavailable()
            return false
        }
        return waiter.await()
    }

    private fun ensureConnection(): LocationPortal? {
        portal?.let { return it }
        val bus = DBusConnectionBuilder.forSessionBus().build()
        val locationPortal = bus.getRemoteObject(
            BUS_NAME,
            PORTAL_PATH,
            LocationPortal::class.java,
        )
        bus.addSigHandler(PortalRequest.Response::class.java) { signal -> onResponse(signal) }
        bus.addSigHandler(LocationPortal.LocationUpdated::class.java) { signal -> onLocation(signal) }
        connection = bus
        portal = locationPortal
        return locationPortal
    }

    private fun onResponse(signal: PortalRequest.Response) {
        val token = synchronized(lock) { requestToken } ?: return
        val expected = synchronized(lock) { expectedRequestPath }
        val path = signal.path
        if (path != expected && !path.endsWith("/$token")) return
        val granted = signal.response.toLong() == 0L
        synchronized(lock) {
            authorizationState = if (granted) {
                LocationAuthorization.Granted
            } else {
                LocationAuthorization.Denied
            }
            if (!granted) {
                cached = null
            }
            permissionWaiter?.complete(granted)
            permissionWaiter = null
        }
    }

    private fun onLocation(signal: LocationPortal.LocationUpdated) {
        val session = signal.sessionHandle.path
        val active = synchronized(lock) { sessionPath }
        if (active != null && session != active) return
        val latitude = PortalOptions.variantDouble(signal.location, "Latitude") ?: return
        val longitude = PortalOptions.variantDouble(signal.location, "Longitude") ?: return
        val coordinate = coordinateOrNull(latitude, longitude) ?: return
        synchronized(lock) {
            cached = coordinate
            fixWaiter?.complete(coordinate)
            fixWaiter = null
        }
    }

    private fun markUnavailable() {
        synchronized(lock) {
            authorizationState = LocationAuthorization.Unavailable
            permissionWaiter = null
        }
    }

    private fun pathToken(prefix: String): String =
        prefix + java.lang.Long.toUnsignedString(System.nanoTime())

    private companion object {
        const val BUS_NAME = "org.freedesktop.portal.Desktop"
        const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
    }
}

internal fun linuxPortalLocation(): DesktopLocation {
    if (System.getenv("DBUS_SESSION_BUS_ADDRESS").isNullOrBlank()) {
        return UnavailableLocation
    }
    return LinuxPortalLocation()
}
