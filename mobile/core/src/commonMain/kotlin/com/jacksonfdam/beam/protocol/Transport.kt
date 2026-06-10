package com.jacksonfdam.beam.protocol

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/*
 * Transport contracts. UI and session logic depend on these abstractions, never
 * on a concrete engine (Ktor today, something else tomorrow). The split into a
 * client-side and a host-side interface keeps each focused (ISP).
 */

/** Phone / web panel side. */
interface PresenterClient {
    val state: StateFlow<ConnectionState>
    val incoming: Flow<HostMessage>

    suspend fun connect(endpoint: HostEndpoint, clientName: String)
    suspend fun send(msg: ClientMessage)
    suspend fun disconnect()
}

/** Desktop presenter side. */
interface PresenterServer {
    val clientCount: StateFlow<Int>
    val incoming: Flow<ClientMessage>

    /** Binds the socket and returns the endpoint to render as a QR / show as text. */
    suspend fun start(port: Int = DEFAULT_PORT, pin: String? = null): HostEndpoint
    suspend fun broadcast(msg: HostMessage)
    suspend fun stop()
}
