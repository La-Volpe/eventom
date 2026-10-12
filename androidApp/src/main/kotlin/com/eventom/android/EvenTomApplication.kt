package com.eventom.android

import android.app.Application
import com.eventom.di.SharedKoin

/**
 * The only boot-level android class: hands over to the shared graph. All
 * logic (repositories, view models) comes from `:shared` via Koin.
 */
class EvenTomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SharedKoin.start()
    }
}