import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.maplibre.compose)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.etrange.towards.MainKt"
        val towardsVersion = providers.gradleProperty("towards.version").get()
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        jvmArgs += "-Dtowards.version=$towardsVersion"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.etrange.towards"
            packageVersion = desktopPackageVersion(towardsVersion)
            windows {
                // WiX/MSI major must be 0–255; calendar years like 2026 are invalid.
                val msiVersion = msiCompatibleVersion(towardsVersion)
                packageVersion = msiVersion
                msiPackageVersion = msiVersion
            }
        }
    }
}

private fun desktopPackageVersion(version: String): String {
    val parts = version.split('.')
    return when (parts.size) {
        1 -> "$version.0.0"
        2 -> "$version.0"
        else -> version
    }
}

/** Maps a display version such as 2026.9.0 to MAJOR.MINOR.BUILD with MAJOR ≤ 255. */
private fun msiCompatibleVersion(version: String): String {
    val parts = desktopPackageVersion(version).split('.')
    val major = parts[0].toInt()
    val minor = parts.getOrElse(1) { "0" }.toInt()
    val build = parts.getOrElse(2) { "0" }.toInt()
    val msiMajor = if (major > 255) major % 100 else major
    return "$msiMajor.$minor.$build"
}
