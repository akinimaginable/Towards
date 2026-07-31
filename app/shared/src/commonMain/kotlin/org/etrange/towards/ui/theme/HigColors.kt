package org.etrange.towards.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Apple HIG system colors (default contrast).
 * Shared by iOS, desktop, and Compose previews.
 * @see <a href="https://developer.apple.com/design/human-interface-guidelines/color">HIG Color</a>
 */
internal object Hig {
    object Light {
        val systemBlue = Color(0xFF007AFF)
        val systemIndigo = Color(0xFF5856D6)
        val systemTeal = Color(0xFF30B0C7)
        val systemRed = Color(0xFFFF3B30)

        val systemBackground = Color(0xFFFFFFFF)
        val secondarySystemBackground = Color(0xFFF2F2F7)
        val tertiarySystemBackground = Color(0xFFFFFFFF)

        val label = Color(0xFF000000)
        val secondaryLabel = Color(0x993C3C43)

        val systemGray = Color(0xFF8E8E93)
        val systemGray4 = Color(0xFFD1D1D6)
        val systemGray5 = Color(0xFFE5E5EA)
        val systemGray6 = Color(0xFFF2F2F7)
        val opaqueSeparator = Color(0xFFC6C6C8)

        val primaryContainer = Color(0xFFD6E8FF)
        val onPrimaryContainer = Color(0xFF002F6C)
    }

    object Dark {
        val systemBlue = Color(0xFF0A84FF)
        val systemIndigo = Color(0xFF5E5CE6)
        val systemTeal = Color(0xFF40CBE0)
        val systemRed = Color(0xFFFF453A)

        val systemBackground = Color(0xFF000000)
        val secondarySystemBackground = Color(0xFF1C1C1E)
        val tertiarySystemBackground = Color(0xFF2C2C2E)

        val label = Color(0xFFFFFFFF)
        val secondaryLabel = Color(0x99EBEBF5)

        val systemGray = Color(0xFF8E8E93)
        val systemGray4 = Color(0xFF3A3A3C)
        val systemGray5 = Color(0xFF2C2C2E)
        val systemGray6 = Color(0xFF1C1C1E)
        val opaqueSeparator = Color(0xFF38383A)

        val primaryContainer = Color(0xFF003A70)
        val onPrimaryContainer = Color(0xFFD6E8FF)
    }
}
