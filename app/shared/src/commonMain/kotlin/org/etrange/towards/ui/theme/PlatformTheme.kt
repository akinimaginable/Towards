package org.etrange.towards.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Platform color scheme override:
 * - Android: Material You (12+) or tinted brand fallback
 * - iOS / desktop: null → shared Apple HIG schemes in [TowardsTheme]
 */
@Composable
internal expect fun rememberPlatformColorScheme(darkTheme: Boolean): ColorScheme?

@Composable
internal expect fun PlatformThemeEffect(darkTheme: Boolean)
