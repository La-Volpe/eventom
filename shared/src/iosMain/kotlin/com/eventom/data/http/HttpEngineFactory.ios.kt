package com.eventom.data.http

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun httpClientEngine(): HttpClientEngine = Darwin.create()