package com.eventom.android

import android.app.Application
import com.eventom.di.SharedKoin
import com.eventom.language.LanguageContextHost

/**
 * The only boot-level android class: hands over to the shared graph. All
 * logic (repositories, view models) comes from `:shared` via Koin.
 */
class EvenTomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Bilingual 1/4: the shared language core persists via
        // SharedPreferences, which needs an Application context.
        LanguageContextHost.attach(this)
        SharedKoin.start()
    }
}