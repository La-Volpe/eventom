// EvenTom — root project settings.
// Two Gradle modules: `:shared` (all non-UI logic, Kotlin Multiplatform)
// and `:androidApp` (Jetpack Compose presentation). The iOS presentation
// lives in `iosApp/` and consumes `:shared` through the Gradle↔Xcode
// integration instead of being included here.

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "EvenTom"

include(":shared")
include(":androidApp")