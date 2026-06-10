package com.jacksonfdam.beam.protocol

/** Default LAN port the desktop host binds; also the QR / manual-entry default. */
const val DEFAULT_PORT = 53317

/** What the QR encodes and what manual entry parses into. */
data class HostEndpoint(
    val host: String,
    val port: Int = DEFAULT_PORT,
    val pin: String? = null,
) {
    fun toUri(): String = buildString {
        append("beam://connect?host=$host&port=$port")
        pin?.let { append("&pin=$it") }
    }

    companion object {
        // beam://connect?host=192.168.1.42&port=53317&pin=4821
        fun parse(uri: String): HostEndpoint? = runCatching {
            val query = uri.substringAfter('?', "")
                .split('&')
                .mapNotNull { pair ->
                    pair.split('=').takeIf { it.size == 2 }?.let { (k, v) -> k to v }
                }
                .toMap()
            HostEndpoint(
                host = query.getValue("host"),
                port = query["port"]?.toInt() ?: DEFAULT_PORT,
                pin = query["pin"],
            )
        }.getOrNull()
    }
}

/** Lifecycle of a remote's connection to the host. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Handshaking : ConnectionState
    data class Connected(val session: HelloAck) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}
