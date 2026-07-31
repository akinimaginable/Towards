package org.etrange.towards.ui.theme

import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Stable theme wrapper for Compose previews — shared Apple HIG palette.
 */
@Composable
fun TowardsPreview(
    themeMode: ThemeMode = ThemeMode.Light,
    content: @Composable () -> Unit,
) {
    TowardsTheme(
        themeMode = themeMode,
        dynamicColor = false,
        content = content,
    )
}

@Preview(name = "Theme · Light")
@Composable
private fun TowardsThemeLightPreview() {
    TowardsPreview(themeMode = ThemeMode.Light) {
        Surface {
            Text("Towards")
        }
    }
}

@Preview(name = "Theme · Dark")
@Composable
private fun TowardsThemeDarkPreview() {
    TowardsPreview(themeMode = ThemeMode.Dark) {
        Surface {
            Text("Towards")
        }
    }
}
