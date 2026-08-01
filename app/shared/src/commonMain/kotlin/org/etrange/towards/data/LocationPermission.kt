package org.etrange.towards.data

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLocationProvider(): LocationProvider

/**
 * Returns a launcher that requests location permission when invoked.
 * [onResult] receives whether access was granted.
 */
@Composable
expect fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit
