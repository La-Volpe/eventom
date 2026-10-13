// `androidApp` — Jetpack Compose presentation layer. Plain Android app
// module (Kotlin JVM-style): no business logic here, it renders
// `EventsViewModel.state` from `:shared` and forwards user actions back
// into the shared view model.
// AGP 9 brings built-in Kotlin support, so there is no separate
// `org.jetbrains.kotlin.android` plugin here — only the Compose compiler
// plugin is needed on top of `com.android.application`.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.eventom.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.eventom.android"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // T18 (D4): typed nav routes; version aligned with the compose BOM era.
    implementation(libs.androidx.navigation.compose)

    // Compose (versions come from the BoM)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    // .connected Android walkthrough (plan T13): Compose test APIs + runner.
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

// Style gate: same baseline-first policy as `shared` (see its comment).
ktlint {
    baseline = rootProject.file("config/ktlint/baseline.xml")
}
