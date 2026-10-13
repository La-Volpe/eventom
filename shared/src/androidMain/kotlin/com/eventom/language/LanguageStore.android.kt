package com.eventom.language

import android.content.Context
import android.content.SharedPreferences

/**
 * Android-side app context holder. The Application class attaches at
 * startup (com.eventom.android.EvenTomApplication, minimal wiring required
 * by the shared language core in this task; app UX wiring follows in task 2).
 */
object LanguageContextHost {
    @Volatile
    private var context: Context? = null

    fun attach(context: Context) {
        this.context = context
    }

    fun require(): Context =
        context ?: error("LanguageContextHost not attached — EvenTomApplication must call attach() in onCreate")
}

actual fun defaultLanguageStore(): LanguageStore {
    val sharedPrefs: SharedPreferences =
        LanguageContextHost.require().getSharedPreferences("eventom_prefs", Context.MODE_PRIVATE)
    return object : LanguageStore {
        override fun readStoredLanguage(): String = sharedPrefs.getString("app_language", "") ?: ""

        override fun writeStoredLanguage(value: String) {
            sharedPrefs.edit().putString("app_language", value).apply()
        }
    }
}