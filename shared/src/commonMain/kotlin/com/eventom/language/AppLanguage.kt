package com.eventom.language

/** User-selectable language preference (settings options locked by the owner). */
enum class AppLanguage {
    /** Follow the device/OS language. */
    SYSTEM,

    /** Force German. */
    DE,

    /** Force English. */
    EN,
}

/** The language that actually governs rendered content — always resolved, never SYSTEM. */
enum class EffectiveLanguage {
    DE,
    EN,
}