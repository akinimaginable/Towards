package org.etrange.towards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppVersion(): String = remember {
    System.getProperty("towards.version")
        ?.takeIf { it.isNotBlank() }
        ?: AppVersionJvm::class.java.`package`?.implementationVersion
            ?.takeIf { it.isNotBlank() }
        ?: "unknown"
}

private object AppVersionJvm
