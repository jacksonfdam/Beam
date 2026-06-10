package com.jacksonfdam.beam.transport

import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Each platform supplies its documented Ktor client engine: CIO on the JVM,
 * OkHttp on Android, Darwin on iOS. The client logic above is identical.
 */
internal expect fun httpClientEngineFactory(): HttpClientEngineFactory<*>
