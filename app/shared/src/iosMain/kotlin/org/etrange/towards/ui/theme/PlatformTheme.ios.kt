package org.etrange.towards.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberPlatformColorScheme(darkTheme: Boolean): ColorScheme? = null

@Composable
internal actual fun PlatformThemeEffect(darkTheme: Boolean) = Unit
