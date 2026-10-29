package com.eventom.language

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.test.Test
import kotlin.test.assertEquals

/** In-memory store used across language fakes (house rule: inject stores). */
class MemoryLanguageStore : LanguageStore {
    var stored: String = ""
    override fun readStoredLanguage(): String = stored
    override fun writeStoredLanguage(value: String) { stored = value }
}

/** The language core is synchronous under the Unconfined scope — no polling needed. */
private fun makeSetting(store: MemoryLanguageStore = MemoryLanguageStore(), testOverride: (() -> String?)? = null) =
    LanguageSetting(
        store = store,
        testOverride = testOverride,
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

class LanguageSettingTest {

    @Test
    fun persistedDERoundtripsToEffectiveDE() {
        val store = MemoryLanguageStore()
        val setting = makeSetting(store)
        setting.choose(AppLanguage.DE)
        assertEquals(AppLanguage.DE, setting.persistedChoice.value)
        assertEquals(EffectiveLanguage.DE, setting.effective.value)
        assertEquals("de", store.stored)
    }

    @Test
    fun persistedENRoundtripsToEffectiveEN() {
        val store = MemoryLanguageStore()
        val setting = makeSetting(store)
        setting.choose(AppLanguage.EN)
        assertEquals(EffectiveLanguage.EN, setting.effective.value)
        assertEquals("en", store.stored)
    }

    @Test
    fun systemResolvesDeviceCode() {
        val setting = makeSetting(testOverride = { "de" })
        assertEquals(EffectiveLanguage.DE, setting.effective.value)
    }

    @Test
    fun systemResolvesAnyNonGermanCodeToEnglish() {
        // Fallback rule pinned: non-"de" → EN (demo fleet is German-first).
        assertEquals(EffectiveLanguage.EN, makeSetting(testOverride = { "en" }).effective.value)
        assertEquals(EffectiveLanguage.EN, makeSetting(testOverride = { "fr" }).effective.value)
    }

    @Test
    fun testOverrideWinsOverPersistedChoice() {
        val store = MemoryLanguageStore()
        store.writeStoredLanguage("de")
        val setting = makeSetting(store, testOverride = { "en" })
        assertEquals(EffectiveLanguage.EN, setting.effective.value)
    }

    @Test
    fun persistedChoiceFlowsProvideConcreteObservation() {
        val setting = makeSetting()
        var last: EffectiveLanguage? = null
        val observation = setting.observeEffective { last = it }
        // Initial emission arrives at attach (Unconfined): the resolved default.
        assertEquals(EffectiveLanguage.EN, last)
        setting.choose(AppLanguage.DE)
        assertEquals(EffectiveLanguage.DE, last)
        observation.stop()
    }

    /** A store that already holds "en" (a relaunch after choosing EN). */
    @Test
    fun storedEnglishCodeRoundtripsToEffectiveEN() {
        val store = MemoryLanguageStore()
        store.writeStoredLanguage("en")

        val setting = makeSetting(store)

        assertEquals(AppLanguage.EN, setting.persistedChoice.value)
        assertEquals(EffectiveLanguage.EN, setting.effective.value)
    }

    /** The UI-test hook is set and CLEARED at runtime (WalkthroughTest does
     *  exactly this); clearing must fall back to the persisted choice, not to
     *  the device locale. */
    @Test
    fun setTestOverrideWinsThenClearingRestoresThePersistedChoice() {
        val store = MemoryLanguageStore()
        val setting = makeSetting(store, testOverride = { "en" })
        setting.choose(AppLanguage.DE)
        assertEquals(EffectiveLanguage.EN, setting.effective.value, "the override wins over the persisted DE")

        setting.setTestOverride("de")
        assertEquals(EffectiveLanguage.DE, setting.effective.value)

        setting.setTestOverride(null)
        assertEquals(EffectiveLanguage.DE, setting.effective.value, "cleared override must fall back to the persisted DE")
    }

    /** Choosing "System" clears the stored code instead of persisting a
     *  sentinel — the store contract is "de"/"en"/"" only. */
    @Test
    fun chooseSystemClearsTheStoredChoice() {
        val store = MemoryLanguageStore()
        val setting = makeSetting(store)

        setting.choose(AppLanguage.DE)
        assertEquals("de", store.stored)

        setting.choose(AppLanguage.SYSTEM)

        assertEquals(LANGUAGE_STORE_EMPTY, store.stored)
        assertEquals(AppLanguage.SYSTEM, setting.persistedChoice.value)
    }

    /** The Settings screen observes the raw choice (not the resolved DE/EN),
     *  so the radio row can show "System default" as selected. */
    @Test
    fun observePersistedChoiceDeliversTheRawChoiceAndStops() {
        val setting = makeSetting()
        val seen = mutableListOf<AppLanguage>()

        val observation = setting.observePersistedChoice { seen.add(it) }
        setting.choose(AppLanguage.EN)
        setting.choose(AppLanguage.SYSTEM)

        assertEquals(
            listOf(AppLanguage.SYSTEM, AppLanguage.EN, AppLanguage.SYSTEM),
            seen,
        )

        observation.stop()
        val delivered = seen.size
        setting.choose(AppLanguage.DE)
        assertEquals(delivered, seen.size, "delivery must end after stop()")
    }
}
