plugins {
    // Docker builds only `:core` and `:server`, so Android/Compose plugins are unnecessary.
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
}
