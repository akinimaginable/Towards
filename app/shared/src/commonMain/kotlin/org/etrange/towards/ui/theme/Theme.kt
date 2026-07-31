package org.etrange.towards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Apple HIG-based schemes — default for iOS, desktop, and previews. */
private val LightColorScheme = lightColorScheme(
    primary = Hig.Light.systemBlue,
    onPrimary = Color.White,
    primaryContainer = Hig.Light.primaryContainer,
    onPrimaryContainer = Hig.Light.onPrimaryContainer,
    secondary = Hig.Light.systemIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E4FF),
    onSecondaryContainer = Color(0xFF1A1870),
    tertiary = Hig.Light.systemTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F0F8),
    onTertiaryContainer = Color(0xFF00363D),
    error = Hig.Light.systemRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Hig.Light.systemBackground,
    onBackground = Hig.Light.label,
    surface = Hig.Light.systemBackground,
    onSurface = Hig.Light.label,
    surfaceVariant = Hig.Light.systemGray5,
    onSurfaceVariant = Hig.Light.secondaryLabel,
    outline = Hig.Light.systemGray,
    outlineVariant = Hig.Light.opaqueSeparator,
    inverseSurface = Hig.Dark.secondarySystemBackground,
    inverseOnSurface = Hig.Dark.label,
    inversePrimary = Hig.Dark.systemBlue,
    surfaceContainerLowest = Hig.Light.systemBackground,
    surfaceContainerLow = Hig.Light.systemGray6,
    surfaceContainer = Hig.Light.secondarySystemBackground,
    surfaceContainerHigh = Hig.Light.systemGray5,
    surfaceContainerHighest = Hig.Light.systemGray4,
)

private val DarkColorScheme = darkColorScheme(
    primary = Hig.Dark.systemBlue,
    onPrimary = Color.White,
    primaryContainer = Hig.Dark.primaryContainer,
    onPrimaryContainer = Hig.Dark.onPrimaryContainer,
    secondary = Hig.Dark.systemIndigo,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E2C7A),
    onSecondaryContainer = Color(0xFFE5E4FF),
    tertiary = Hig.Dark.systemTeal,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFFB8F0F8),
    error = Hig.Dark.systemRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Hig.Dark.systemBackground,
    onBackground = Hig.Dark.label,
    surface = Hig.Dark.systemBackground,
    onSurface = Hig.Dark.label,
    surfaceVariant = Hig.Dark.systemGray5,
    onSurfaceVariant = Hig.Dark.secondaryLabel,
    outline = Hig.Dark.systemGray,
    outlineVariant = Hig.Dark.opaqueSeparator,
    inverseSurface = Hig.Light.systemGray6,
    inverseOnSurface = Hig.Light.label,
    inversePrimary = Hig.Light.systemBlue,
    surfaceContainerLowest = Hig.Dark.systemBackground,
    surfaceContainerLow = Hig.Dark.systemGray6,
    surfaceContainer = Hig.Dark.secondarySystemBackground,
    surfaceContainerHigh = Hig.Dark.tertiarySystemBackground,
    surfaceContainerHighest = Hig.Dark.systemGray4,
)

@Composable
fun TowardsTheme(
    themeMode: ThemeMode = ThemeMode.System,
    /**
     * When true, prefer a platform scheme (Material You / Android tinted fallback).
     * When false, always use the shared HIG palette (previews, desktop-stable).
     */
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colorScheme = when {
        dynamicColor -> rememberPlatformColorScheme(darkTheme) ?: if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    PlatformThemeEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TowardsTypography,
        content = content,
    )
}
