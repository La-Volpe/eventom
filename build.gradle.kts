// Root build script — declares which plugins are available; the subprojects
// apply their own. Nothing else should grow here; put shared config in
// `gradle/libs.versions.toml` and per-module files.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
}