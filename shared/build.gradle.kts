// `shared` — Everything except the UI lives here: domain models & use cases,
// data layer (Ktor remote + fallback), view models exposing StateFlow, and
// Koin wiring. Both apps (`androidApp`, `iosApp`) render these states natively.
//
// AGP 9's KMP library plugin owns the Android target below (AGP 9 no longer
// accepts com.android.library directly inside the multiplatform plugin).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
}

// T17: measured coverage is the gate, not a number chase. Integration-only
// seams stay out of the counting: Koin wiring (di/), the offline fake
// backend (data/fake/) and expect/actual platform plumbing (platform/) are
// exercised end-to-end by walkthroughs, not unit assertions. (Kover 0.9:
// package filters live on the report configs.)
kover {
    reports {
        filters {
            excludes {
                packages(
                    "com.eventom.di",
                    "com.eventom.data.fake",
                    "com.eventom.platform",
                )
            }
        }
    }
}

kotlin {
    // AGP 9 KMP plugin: the Android target is declared `android { }` inside
    // `kotlin { }` (there is no top-level `android { }` with this plugin).
    android {
        namespace = "com.eventom.shared"
        compileSdk = 37
        minSdk = 26
        // Run the common tests also as JVM unit tests (./gradlew :shared:testAndroidHostTest)
        withHostTestBuilder {}.configure {}
    }
    iosArm64()
    iosSimulatorArm64()
    // T9: iosX64 removed from the default target set (Intel-Mac sim builds no
    // longer exercised). Intel-Mac users: uncomment the line below — the
    // Xcode" embedAndSign" phase (`:shared:embedAndSignAppleFrameworkForXcode`)
    // covers it automatically once present.
    // iosX64()

    // The framework the Xcode app embeds (see iosApp build phase:
    // `:shared:embedAndSignAppleFrameworkForXcode`).
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
        binaries {
            framework {
                baseName = "Shared"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
    }
}

// Style gate: baseline carries pre-existing findings (parity raw-string fixtures, use-case file naming)
// so ktlintCheck only flags NEW violations. Refresh with `./gradlew :shared:ktlintBaseline`.
ktlint {
    baseline = rootProject.file("config/ktlint/baseline.xml")
}
