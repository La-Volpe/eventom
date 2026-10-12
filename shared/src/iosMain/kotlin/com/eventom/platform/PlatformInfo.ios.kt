package com.eventom.platform

import platform.UIKit.UIDevice

actual fun platformInfo(): PlatformInfo = PlatformInfo(
    osName = UIDevice.currentDevice.systemName,
    osVersion = UIDevice.currentDevice.systemVersion,
    deviceModel = UIDevice.currentDevice.model,
)