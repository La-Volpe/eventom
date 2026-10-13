package com.eventom.language

import platform.Foundation.NSString
import platform.Foundation.NSUserDefaults
import platform.Foundation.create

actual fun defaultLanguageStore(): LanguageStore {
    val defaults = NSUserDefaults.standardUserDefaults
    return object : LanguageStore {
        override fun readStoredLanguage(): String = defaults.stringForKey("app_language") ?: ""

        override fun writeStoredLanguage(value: String) {
            defaults.setObject(NSString.create(string = value), forKey = "app_language")
        }
    }
}