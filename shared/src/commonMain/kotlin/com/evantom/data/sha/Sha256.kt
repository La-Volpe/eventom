package com.evantom.data.sha

/**
 * SHA-256 of [input] as lowercase hex — the demo-credential hashing contract
 * (see AGENT.md gotcha #18). Kept as an expect/actual pair so no new
 * dependency (Bouncy/Keychain wrappers) enters the build.
 */
expect fun sha256Hex(input: String): String