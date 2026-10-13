package com.eventom.language

/**
 * Device locale code ("de", "en", "fr"…). App-layer UI-test hooks are wired
 * through the [LanguageSetting] testOverride parameter (Android passes an
 * intent extra through the app layer); the iOS actual additionally honors
 * the runner-injected UITEST_LANG key directly.
 */
expect fun deviceLanguageCode(): String