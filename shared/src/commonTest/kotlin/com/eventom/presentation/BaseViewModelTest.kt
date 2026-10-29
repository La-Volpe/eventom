package com.eventom.presentation

import com.eventom.core.UiObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The observation contract every concrete view model forwards to its UI:
 * `observeState` delivers the current state immediately, then every change,
 * and `stop()` ends delivery. Both apps rely on it (Compose collects `state`
 * directly; SwiftUI uses the typed `observe(onChange:)` wrappers — gotcha #14).
 */
class BaseViewModelTest {
    /** Minimal subclass: the members under test are `protected`. */
    private class ProbeViewModel(
        mainScope: CoroutineScope?,
    ) : BaseViewModel<String>(initialState = "initial", mainScope = mainScope) {
        fun observe(onChange: (String) -> Unit): UiObservation = observeState(onChange)

        fun set(value: String) = updateState { value }
    }

    private fun unconfined() = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Test
    fun observeDeliversCurrentStateThenEveryChange() {
        val viewModel = ProbeViewModel(unconfined())
        val delivered = mutableListOf<String>()

        viewModel.observe { delivered.add(it) }
        viewModel.set("second")
        viewModel.set("third")

        // Unconfined collection is synchronous: no polling, and a dispatcher
        // regression (someone hardwiring Main — gotcha #9) fails here.
        assertEquals(listOf("initial", "second", "third"), delivered)
    }

    @Test
    fun stopEndsDelivery() {
        val viewModel = ProbeViewModel(unconfined())
        val delivered = mutableListOf<String>()
        val observation = viewModel.observe { delivered.add(it) }

        observation.stop()
        viewModel.set("after-stop")

        assertEquals(listOf("initial"), delivered, "delivery must end after stop()")
        assertEquals("after-stop", viewModel.state.value, "state itself keeps changing")
    }

    /**
     * Production leaves `mainScope` unset (Main dispatcher). Constructing with
     * the default must not throw, and a state write must still be visible —
     * this is the branch the injected-scope tests never touch.
     */
    @Test
    fun defaultScopeIsUsableForConstructionAndStateWrites() {
        val viewModel = ProbeViewModel(mainScope = null)

        assertEquals("initial", viewModel.state.value)
        viewModel.set("written")
        assertEquals("written", viewModel.state.value)
    }
}
