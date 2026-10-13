package com.eventom.language

/**
 * Tiny persistence for the selected language (plan: bilingual 1/4). Values:
 * "" (no explicit choice — SYSTEM), "de", "en". Storage details differ per
 * platform: Android = SharedPreferences("eventom_prefs")/key "app_language",
 * iOS = NSUserDefaults key "app_language".
 */
interface LanguageStore {
    fun readStoredLanguage(): String
    fun writeStoredLanguage(value: String)
}

/** The platform default store; app code never constructs alternatives by hand. */
expect fun defaultLanguageStore(): LanguageStore

/** Stored-code constant: everything else (malformed, missing) falls back to SYSTEM at parse time. */
const val LANGUAGE_STORE_EMPTY: String = ""