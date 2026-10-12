package com.evantom.data.sha

import java.security.MessageDigest

actual fun sha256Hex(input: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(input.encodeToByteArray())
        .joinToString(separator = "") { "%02x".format(it) }