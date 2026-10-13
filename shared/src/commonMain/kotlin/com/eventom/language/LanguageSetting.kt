package com.eventom.language

import com.eventom.core.UiObservation
import com.eventom.strings.Strings
import com.eventom.strings.strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Single language authority for BOTH apps (locked decision 3: strings +
 * language state live in shared).
 *
 * - [persistedChoice] reflects the [LanguageStore] ("", "de", "en") and is
 *   written through on [choose].
 * - [effective] is the RESOLVED content language: test hook > persisted >
 *   device, resolved to [EffectiveLanguage] (never SYSTEM).
 *
 * Fallback rule (pinned): any device code other than "de" resolves to EN —
 * the demo fleet is German-first, everything else gets English.
 *
 * The [testOverride] hook exists only for machine UI tests (task 2 wires an
 * Android intent extra; the iOS actual reads its own env/default keys) —
 * it wins over both the persisted choice and the device locale.
 */
class LanguageSetting(
    store: LanguageStore = defaultLanguageStore(),
    private val testOverride: (() -> String?)? = null,
    // Injectable scope (house gotcha #9); production leaves the Main default.
    mainScope: CoroutineScope? = null,
) {
    private val observeScope: CoroutineScope = mainScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val storeRef = store

    private fun parseStored(stored: String): AppLanguage = when (stored) {
        "de" -> AppLanguage.DE
        "en" -> AppLanguage.EN
        else -> AppLanguage.SYSTEM
    }

    private fun mapDeviceCode(code: String): EffectiveLanguage =
        if (code.take(2).lowercase() == "de") EffectiveLanguage.DE else EffectiveLanguage.EN

    private fun resolve(persisted: AppLanguage): EffectiveLanguage {
        testOverride?.invoke()?.let { return mapDeviceCode(it) }
        return when (persisted) {
            AppLanguage.DE -> EffectiveLanguage.DE
            AppLanguage.EN -> EffectiveLanguage.EN
            AppLanguage.SYSTEM -> mapDeviceCode(deviceLanguageCode())
        }
    }

    private val persistFlow = MutableStateFlow(parseStored(store.readStoredLanguage()))

    private val effectiveFlow = MutableStateFlow(resolve(persistFlow.value))

    /** The user's stored choice (SYSTEM/DE/EN) — collectable by settings UIs. */
    val persistedChoice: StateFlow<AppLanguage> = persistFlow.asStateFlow()

    /** The resolved DE/EN the rest of the app renders against. */
    val effective: StateFlow<EffectiveLanguage> = effectiveFlow.asStateFlow()

    /** Current effective value for non-reactive readers (view models). */
    val current: EffectiveLanguage
        get() = effectiveFlow.value

    /** Changes the persisted choice, writes through, and re-resolves effective. */
    fun choose(language: AppLanguage) {
        persistFlow.value = language
        storeRef.writeStoredLanguage(persistCode(language))
        effectiveFlow.value = resolve(language)
    }

    private fun persistCode(language: AppLanguage): String = when (language) {
        AppLanguage.DE -> "de"
        AppLanguage.EN -> "en"
        AppLanguage.SYSTEM -> LANGUAGE_STORE_EMPTY
    }

    fun observeEffective(onChange: (EffectiveLanguage) -> Unit): UiObservation = observe(effectiveFlow, onChange)

    fun observePersistedChoice(onChange: (AppLanguage) -> Unit): UiObservation = observe(persistFlow, onChange)

    /** Convenience for view models: the string table for the current effective language. */
    fun strings(): Strings = strings(effectiveFlow.value)

    private fun <T> observe(flow: StateFlow<T>, onChange: (T) -> Unit): UiObservation {
        val job: Job = observeScope.launch {
            flow.collect { value -> onChange(value) }
        }
        return object : UiObservation {
            override fun stop() {
                job.cancel()
            }
        }
    }
}
