package org.etrange.towards.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.etrange.towards.domain.model.Coordinate

class JvmLocationProvider : LocationProvider {
    override fun hasPermission(): Boolean = false

    override fun lastKnownCoordinate(): Coordinate? = null

    override suspend fun currentCoordinate(): Coordinate? = null
}

@Composable
actual fun rememberLocationProvider(): LocationProvider =
    remember { JvmLocationProvider() }

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit =
    { onResult(false) }
