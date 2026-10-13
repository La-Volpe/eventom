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
}