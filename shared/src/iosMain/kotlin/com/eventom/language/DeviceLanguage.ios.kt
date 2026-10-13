package com.eventom.language

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

/**
 * Device language: UI-test hook first (defaults key or the runner-provided
 * environment), then the user's preferred system languages ("AppleLanguages"
 * is the standard defaults entry mirroring NSLocale.preferredLanguages).
 * "de…" maps to DE by LanguageSetting; anything else falls back to EN —
 * the pinned fallback.
 */
actual fun deviceLanguageCode(): String {
    NSUserDefaults.standardUserDefaults.stringForKey("UITEST_LANG")?.let { return it }
    (NSProcessInfo.processInfo.environment["UITEST_LANG"] as? String)?.let { return it }
    val preferred = NSUserDefaults.standardUserDefaults
        .arrayForKey("AppleLanguages")?.firstOrNull() as? String
    return preferred?.take(3)?.takeWhile { it != '-' }?.toString() ?: "en"
}
