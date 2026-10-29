package org.etrange.towards.data.desktop

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.ptr.DoubleByReference
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import org.etrange.towards.domain.model.Coordinate

private const val CODE_SUCCESS = 0

private const val STATUS_NOT_DETERMINED = 0
private const val STATUS_DENIED = 1
private const val STATUS_GRANTED = 2

internal interface LocationCallback : Callback {
    fun callback(code: Int, latitude: Double, longitude: Double)
}

internal class RetainedLocationCallback(
    private val onResult: (Int, Double, Double) -> Unit,
) : LocationCallback {
    override fun callback(code: Int, latitude: Double, longitude: Double) {
        onResult(code, latitude, longitude)
    }
}

internal interface TowardsLocationLib : Library {
    fun location_host_declares_location_usage(): Int

    fun location_authorization_status(): Int

    fun location_last_known(latitude: DoubleByReference, longitude: DoubleByReference): Int

    fun location_request_permission(callback: LocationCallback)

    fun location_request_current(callback: LocationCallback)

    fun location_cancel()
}

internal class NativeDesktopLocation(
    private val lib: TowardsLocationLib,
) : DesktopLocation {
    private val liveCallbacks = Collections.newSetFromMap(IdentityHashMap<LocationCallback, Boolean>())
    private var activePermission: CancellableContinuation<Boolean>? = null

    override fun authorization(): LocationAuthorization =
        when (lib.location_authorization_status()) {
            STATUS_NOT_DETERMINED -> LocationAuthorization.NotDetermined
            STATUS_DENIED -> LocationAuthorization.Denied
            STATUS_GRANTED -> LocationAuthorization.Granted
            else -> LocationAuthorization.Unavailable
        }

    override fun lastKnown(): Coordinate? {
        val latitude = DoubleByReference()
        val longitude = DoubleByReference()
        if (lib.location_last_known(latitude, longitude) == 0) return null
        return coordinateOrNull(latitude.value, longitude.value)
    }

    override suspend fun requestPermission(): Boolean {
        when (authorization()) {
            LocationAuthorization.Granted -> return true
            LocationAuthorization.Denied,
            LocationAuthorization.Unavailable,
            -> return false
            LocationAuthorization.NotDetermined -> Unit
        }
        return suspendCancellableCoroutine { continuation ->
            val previous = synchronized(liveCallbacks) {
                val current = activePermission
                activePermission = continuation
                current
            }
            previous?.cancel()
            continuation.invokeOnCancellation {
                synchronized(liveCallbacks) {
                    if (activePermission === continuation) activePermission = null
                }
            }
            val callback = retain { delivered, code, _, _ ->
                val stillCurrent = synchronized(liveCallbacks) {
                    if (activePermission !== continuation) {
                        false
                    } else {
                        activePermission = null
                        true
                    }
                }
                if (stillCurrent) {
                    resumeNative(continuation, code == CODE_SUCCESS, delivered)
                } else {
                    synchronized(liveCallbacks) { liveCallbacks.remove(delivered) }
                }
            }
            lib.location_request_permission(callback)
        }
    }

    fun hostDeclaresLocationUsage(): Boolean =
        try {
            lib.location_host_declares_location_usage() != 0
        } catch (_: UnsatisfiedLinkError) {
            false
        }

    override suspend fun current(): Coordinate? = suspendCancellableCoroutine { continuation ->
        val callback = retain { delivered, code, latitude, longitude ->
            val coordinate = if (code == CODE_SUCCESS) coordinateOrNull(latitude, longitude) else null
            resumeNative(continuation, coordinate, delivered)
        }
        continuation.invokeOnCancellation { lib.location_cancel() }
        lib.location_request_current(callback)
    }

    override fun cancel() {
        lib.location_cancel()
    }

    private fun retain(
        onResult: (LocationCallback, Int, Double, Double) -> Unit,
    ): LocationCallback {
        lateinit var callback: RetainedLocationCallback
        callback = RetainedLocationCallback { code, latitude, longitude ->
            onResult(callback, code, latitude, longitude)
        }
        synchronized(liveCallbacks) {
            liveCallbacks.add(callback)
        }
        return callback
    }

    private fun <T> resumeNative(
        continuation: CancellableContinuation<T>,
        value: T,
        callback: LocationCallback,
    ) {
        Thread(
            {
                synchronized(liveCallbacks) {
                    liveCallbacks.remove(callback)
                }
                try {
                    continuation.resume(value)
                } catch (_: IllegalStateException) {
                    // The coroutine was cancelled before this native callback arrived.
                }
            },
            "towards-location",
        ).apply { isDaemon = true }.start()
    }
}

internal fun loadNativeDesktopLocation(libraryFileName: String): DesktopLocation =
    try {
        val file = NativeLibraryExtractor.extract(libraryFileName)
        if (file == null) {
            System.err.println("Towards location is unavailable: native library $libraryFileName was not found")
            UnavailableLocation
        } else {
            NativeDesktopLocation(Native.load(file.absolutePath, TowardsLocationLib::class.java))
        }
    } catch (error: UnsatisfiedLinkError) {
        System.err.println("Towards location is unavailable: ${error.message}")
        UnavailableLocation
    } catch (error: Exception) {
        System.err.println("Towards location is unavailable: ${error.message}")
        UnavailableLocation
    }

internal object NativeLibraryExtractor {
    fun extract(libraryFileName: String): File? {
        val folder = nativeFolderName() ?: return null
        val resource = "/natives/$folder/$libraryFileName"
        val bytes = NativeLibraryExtractor::class.java.getResourceAsStream(resource)?.use { it.readBytes() }
            ?: return null
        val hash = sha256Prefix(bytes)
        val base = libraryFileName.substringBeforeLast('.')
        val extension = libraryFileName.substringAfterLast('.', "bin")
        val dir = File(System.getProperty("java.io.tmpdir"), "towards-location/$folder")
        dir.mkdirs()
        val out = File(dir, "$base-$hash.$extension")
        if (!out.isFile) {
            out.writeBytes(bytes)
        }
        return out
    }

    fun nativeFolderName(): String? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val arch = when (System.getProperty("os.arch").orEmpty().lowercase()) {
            "x86_64", "amd64" -> "x64"
            "aarch64", "arm64" -> "arm64"
            else -> return null
        }
        val osName = when {
            os.startsWith("mac") -> "macos"
            os.startsWith("windows") -> "windows"
            else -> return null
        }
        return "$osName-$arch"
    }

    private fun sha256Prefix(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }
}
