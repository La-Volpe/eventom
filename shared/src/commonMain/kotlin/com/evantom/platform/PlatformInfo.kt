package com.evantom.platform

/**
 * Demonstration of expect/actual in this boilerplate: the About screen in
 * each app renders this, and the actuals live in `androidMain` / `iosMain`.
 */
data class PlatformInfo(
    val osName: String,
    val osVersion: String,
    val deviceModel: String,
)

expect fun platformInfo(): PlatformInfo