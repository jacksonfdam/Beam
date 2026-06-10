package com.jacksonfdam.beam.transport

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

internal actual fun httpClientEngineFactory(): HttpClientEngineFactory<*> = Darwin
