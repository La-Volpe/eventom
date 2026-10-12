package com.eventom.presentation

import com.eventom.core.UiObservation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Minimal, framework-agnostic view model. Android Compose collects
 * [state] directly; SwiftUI uses [observeState].
 *
 * A production app may replace this with `androidx.lifecycle.ViewModel`
 * (multiplatform since lifecycle 2.8+) — kept hand-rolled here so the
 * boilerplate has no hidden dependency choices baked in.
 */
abstract class BaseViewModel<S>(initialState: S, mainScope: CoroutineScope? = null) {

    /** Main-dispatcher scope; all mutations of [state] happen on it.
     *  Tests inject their own scope instead (JVM tests have no Main —
     *  repo gotcha #9); production leaves the parameter unset. */
    val scope: CoroutineScope = mainScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val mutableState = MutableStateFlow(initialState)

    /** The single source of truth the UI renders. */
    val state: StateFlow<S> = mutableState.asStateFlow()

    protected fun updateState(transform: (S) -> S) {
        mutableState.value = transform(mutableState.value)
    }

    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(block = block)

    /**
     * Delivers every state to [onChange]; the returned handle stops it.
     * Concrete view models expose this with a typed signature (the generic
     * one would lose its type over the Swift/ObjC boundary).
     */
    protected fun observeState(onChange: (S) -> Unit): UiObservation {
        val job = scope.launch {
            state.collect { value -> onChange(value) }
        }
        return object : UiObservation {
            override fun stop() {
                job.cancel()
            }
        }
    }

    /** Cancels all work; UIs should call this when the screen goes away for good. */
    fun dispose() {
        scope.cancel()
    }
}