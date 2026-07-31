package org.etrange.towards.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AndroidLightColorScheme = lightColorScheme(
    primary = AndroidBrand.Teal40,
    onPrimary = Color.White,
    primaryContainer = AndroidBrand.Teal90,
    onPrimaryContainer = AndroidBrand.Teal10,
    secondary = AndroidBrand.Secondary40,
    onSecondary = Color.White,
    secondaryContainer = AndroidBrand.Secondary90,
    onSecondaryContainer = AndroidBrand.Teal10,
    tertiary = AndroidBrand.Tertiary40,
    onTertiary = Color.White,
    tertiaryContainer = AndroidBrand.Tertiary90,
    onTertiaryContainer = Color(0xFF041C35),
    error = AndroidBrand.Error40,
    onError = Color.White,
    errorContainer = AndroidBrand.Error90,
    onErrorContainer = Color(0xFF410002),
    background = AndroidBrand.SoftLightBackground,
    onBackground = AndroidBrand.SoftDarkBackground,
    surface = AndroidBrand.SoftLightBackground,
    onSurface = AndroidBrand.SoftDarkBackground,
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = AndroidBrand.Outline40,
    outlineVariant = Color(0xFFBEC9C8),
    inverseSurface = AndroidBrand.SoftDarkBackground,
    inverseOnSurface = AndroidBrand.SoftInverseOnSurface,
    inversePrimary = AndroidBrand.Teal80,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFEEF6F5),
    surfaceContainer = Color(0xFFE8F2F1),
    surfaceContainerHigh = Color(0xFFE2ECED),
    surfaceContainerHighest = AndroidBrand.SoftInverseOnSurface,
)

private val AndroidDarkColorScheme = darkColorScheme(
    primary = AndroidBrand.Teal80,
    onPrimary = AndroidBrand.Teal20,
    primaryContainer = AndroidBrand.Teal30,
    onPrimaryContainer = AndroidBrand.Teal90,
    secondary = AndroidBrand.Secondary80,
    onSecondary = Color(0xFF1C3535),
    secondaryContainer = Color(0xFF334B4B),
    onSecondaryContainer = AndroidBrand.Secondary90,
    tertiary = AndroidBrand.Tertiary80,
    onTertiary = Color(0xFF1C314B),
    tertiaryContainer = Color(0xFF334863),
    onTertiaryContainer = AndroidBrand.Tertiary90,
    error = AndroidBrand.Error80,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = AndroidBrand.Error90,
    background = AndroidBrand.SoftDarkBackground,
    onBackground = AndroidBrand.SoftDarkOnBackground,
    surface = AndroidBrand.SoftDarkBackground,
    onSurface = AndroidBrand.SoftDarkOnBackground,
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = AndroidBrand.Outline80,
    outlineVariant = Color(0xFF3F4948),
    inverseSurface = AndroidBrand.SoftDarkOnBackground,
    inverseOnSurface = AndroidBrand.SoftDarkBackground,
    inversePrimary = AndroidBrand.Teal40,
    surfaceContainerLowest = Color(0xFF0C0F0F),
    surfaceContainerLow = Color(0xFF191C1C),
    surfaceContainer = Color(0xFF1D2020),
    surfaceContainerHigh = Color(0xFF272B2B),
    surfaceContainerHighest = Color(0xFF323535),
)

@Composable
internal actual fun rememberPlatformColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
    }
    return if (darkTheme) AndroidDarkColorScheme else AndroidLightColorScheme
}

@Composable
internal actual fun PlatformThemeEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
}
