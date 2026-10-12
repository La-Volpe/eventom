package com.evantom.platform

import android.os.Build

actual fun platformInfo(): PlatformInfo = PlatformInfo(
    osName = "Android",
    osVersion = Build.VERSION.RELEASE,
    deviceModel = Build.MODEL,
)