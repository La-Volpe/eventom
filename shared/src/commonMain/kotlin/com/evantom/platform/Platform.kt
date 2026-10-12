package com.evantom.platform

/**
 * Object wrapper so the expect/actual [platformInfo] is addressable from
 * Swift as `Platform.shared.info()` (top-level functions get a file-name
 * prefix in objective interop).
 */
object Platform {
    fun info(): PlatformInfo = platformInfo()
}