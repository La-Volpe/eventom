package com.evantom.data.sha

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(input: String): String {
    val inputBytes = input.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    inputBytes.usePinned { pinnedInput ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(pinnedInput.addressOf(0), inputBytes.size.toUInt(), pinnedDigest.addressOf(0))
        }
    }
    return digest.joinToString(separator = "") { it.toString(16).padStart(2, '0') }
}