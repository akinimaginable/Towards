package org.etrange.towards.data.desktop

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.etrange.towards.domain.model.Coordinate
import kotlin.coroutines.coroutineContext

internal fun parseHelperAuthorization(output: String): LocationAuthorization =
    when (output.trim()) {
        "0" -> LocationAuthorization.NotDetermined
        "1" -> LocationAuthorization.Denied
        "2" -> LocationAuthorization.Granted
        else -> LocationAuthorization.Unavailable
    }

internal fun parseHelperPermission(output: String): Boolean = output.trim() == "granted"

internal fun parseHelperLocation(output: String): Coordinate? {
    val parts = output.trim().split(" ")
    if (parts.size != 2) return null
    val latitude = parts[0].toDoubleOrNull() ?: return null
    val longitude = parts[1].toDoubleOrNull() ?: return null
    return coordinateOrNull(latitude, longitude)
}

/**
 * macOS only shows the location prompt for an app bundle that declares a usage string.
 * A desktop launch through the JVM has no such bundle, so permission and fixes run in a
 * small helper app.
 */
internal class MacOsBundleLocation(
    private val app: File,
) : DesktopLocation {
    private val gate = Mutex()
    private var cached = LocationAuthorization.NotDetermined
    private var latest: Coordinate? = null
    private var freshFix: Coordinate? = null

    override fun authorization(): LocationAuthorization = cached

    override fun lastKnown(): Coordinate? = latest

    override suspend fun requestPermission(): Boolean = gate.withLock {
        if (cached == LocationAuthorization.Granted) return@withLock true
        if (cached == LocationAuthorization.Denied || cached == LocationAuthorization.Unavailable) {
            return@withLock false
        }
        val answer = execute("request") ?: return@withLock false
        parseHelperLocation(answer)?.let { coordinate ->
            latest = coordinate
            freshFix = coordinate
            cached = LocationAuthorization.Granted
            return@withLock true
        }
        val granted = parseHelperPermission(answer)
        if (answer == "granted" || answer == "denied") {
            cached = if (granted) LocationAuthorization.Granted else LocationAuthorization.Denied
        }
        granted
    }

    override suspend fun current(): Coordinate? = gate.withLock {
        freshFix?.let { coordinate ->
            freshFix = null
            return@withLock coordinate
        }
        val coordinate = parseHelperLocation(execute("current") ?: return@withLock null)
        if (coordinate != null) latest = coordinate
        coordinate
    }

    override fun cancel() = Unit

    private suspend fun execute(command: String): String? = withContext(Dispatchers.IO) {
        val result = File.createTempFile("towards-location-", ".txt")
        val process = ProcessBuilder(
            "open",
            "-W",
            "-n",
            app.absolutePath,
            "--args",
            command,
            result.absolutePath,
        ).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        coroutineContext[Job]?.invokeOnCompletion { cause ->
            if (cause != null && process.isAlive) process.destroy()
        }
        try {
            process.waitFor()
            if (result.isFile) result.readText().trim().ifEmpty { null } else null
        } finally {
            if (process.isAlive) process.destroy()
            result.delete()
        }
    }

    companion object {
        fun install(): MacOsBundleLocation? {
            val folder = NativeLibraryExtractor.nativeFolderName() ?: return null
            val plist = resourceBytes("/natives/$folder/TowardsLocation.app/Contents/Info.plist") ?: return null
            val binary = resourceBytes("/natives/$folder/TowardsLocation.app/Contents/MacOS/location-helper")
                ?: return null
            val root = File(
                System.getProperty("user.home"),
                "Library/Application Support/org.etrange.towards/TowardsLocation.app",
            )
            val executable = File(root, "Contents/MacOS/location-helper")
            val info = File(root, "Contents/Info.plist")
            executable.parentFile.mkdirs()
            if (!info.isFile || !info.readBytes().contentEquals(plist)) {
                info.writeBytes(plist)
            }
            if (!executable.isFile || !executable.readBytes().contentEquals(binary)) {
                executable.writeBytes(binary)
                executable.setExecutable(true)
            }
            return MacOsBundleLocation(root)
        }

        private fun resourceBytes(path: String): ByteArray? =
            MacOsBundleLocation::class.java.getResourceAsStream(path)?.use { it.readBytes() }
    }
}
