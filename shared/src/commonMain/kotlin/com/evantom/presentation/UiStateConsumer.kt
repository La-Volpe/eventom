package com.evantom.presentation

/**
 * Native-friendly handle for stopping a UI observation (e.g. SwiftUI's
 * `onDisappear`). Kotlin-side callers can also just collect [BaseViewModel.state].
 */
interface UiObservation {
    fun stop()
}