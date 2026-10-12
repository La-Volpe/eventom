package com.evantom.data.fake

/**
 * The one switch between the offline fake backend (this milestone) and the
 * real network engines. Flip to `false` once the real EvenTom API exists:
 * `dataModule` then wires the platform engines (OkHttp on Android, Darwin on
 * iOS) via `HttpEngineFactory` again, and the fake payload/engine files plus
 * the JSON fixtures can be deleted.
 */
object BackendConfig {
    const val useFakeBackend: Boolean = true
}