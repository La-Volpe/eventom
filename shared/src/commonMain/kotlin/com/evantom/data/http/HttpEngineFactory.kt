package com.evantom.data.http

import io.ktor.client.engine.HttpClientEngine

/**
 * Ktor has no common engine: each platform provides one via expect/actual.
 * OkHttp on Android, Darwin (NSURLSession) on iOS.
 */
expect fun httpClientEngine(): HttpClientEngine