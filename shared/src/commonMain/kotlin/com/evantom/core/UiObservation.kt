package com.evantom.core

/**
 * Native-friendly handle for stopping a UI observation (e.g. SwiftUI's
 * `onDisappear`, the Compose equivalent). Shared by every view-model /
 * session observation consumer in both presentation layers.
 */
interface UiObservation {
    fun stop()
}