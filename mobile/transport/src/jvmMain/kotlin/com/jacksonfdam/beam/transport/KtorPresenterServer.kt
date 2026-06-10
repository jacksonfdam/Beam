package com.jacksonfdam.beam.transport

import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.Hello
import com.jacksonfdam.beam.protocol.HelloAck
import com.jacksonfdam.beam.protocol.HelloReject
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.protocol.HostError
import com.jacksonfdam.beam.protocol.HostMessage
import com.jacksonfdam.beam.protocol.PROTOCOL_VERSION
import com.jacksonfdam.beam.protocol.PresenterServer
import com.jacksonfdam.beam.protocol.localIpAddress
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections

/**
 * Ktor CIO-backed [PresenterServer] — the desktop host's LAN socket.
 *
 * The transport owns *security only*: it verifies the [Hello] (protocol version
 * + PIN) and rejects mismatches with [HelloReject]. Application state (decks,
 * session name) is injected via [helloAck] so the transport stays free of
 * domain knowledge. Accepted clients are greeted, registered for [broadcast],
 * and their [Hello] is surfaced on [incoming] so the session layer can replay
 * the current slide/timer to a (re)connecting remote.
 */
class KtorPresenterServer(
    private val helloAck: () -> HelloAck,
) : PresenterServer {

    private val _clientCount = MutableStateFlow(0)
    override val clientCount: StateFlow<Int> = _clientCount.asStateFlow()

    private val _incoming = MutableSharedFlow<ClientMessage>(extraBufferCapacity = 128)
    override val incoming: Flow<ClientMessage> = _incoming.asSharedFlow()

    private val sessions = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketServerSession>())
    private var server: EmbeddedServer<*, *>? = null
    private var pin: String? = null

    override suspend fun start(port: Int, pin: String?): HostEndpoint {
        this.pin = pin
        val engine = embeddedServer(CIO, port = port) {
            install(WebSockets)
            routing {
                webSocket(CUE_PATH) { handleClient() }
            }
        }
        engine.start(wait = false)
        server = engine
        val boundPort = engine.engine.resolvedConnectors().firstOrNull()?.port ?: port
        return HostEndpoint(host = localIpAddress() ?: "127.0.0.1", port = boundPort, pin = pin)
    }

    private suspend fun DefaultWebSocketServerSession.handleClient() {
        // Never trust a bare open port: the first frame must be a valid Hello.
        val firstText = (incoming.receiveCatching().getOrNull() as? Frame.Text)?.readText()
        val hello = firstText?.let { runCatching { decodeClientMessage(it) }.getOrNull() } as? Hello
        if (hello == null) {
            send(Frame.Text(HostError("expected hello").toJson()))
            close()
            return
        }
        if (hello.protocolVersion != PROTOCOL_VERSION) {
            send(Frame.Text(HelloReject("protocol version mismatch").toJson()))
            close()
            return
        }
        val required = pin
        if (!required.isNullOrEmpty() && hello.pin != required) {
            send(Frame.Text(HelloReject("bad pin").toJson()))
            close()
            return
        }

        send(Frame.Text(helloAck().toJson()))
        sessions.add(this)
        _clientCount.value = sessions.size
        _incoming.emit(hello)
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                runCatching { decodeClientMessage(frame.readText()) }.getOrNull()?.let { _incoming.emit(it) }
            }
        } finally {
            sessions.remove(this)
            _clientCount.value = sessions.size
        }
    }

    override suspend fun broadcast(msg: HostMessage) {
        val text = msg.toJson()
        val snapshot = synchronized(sessions) { sessions.toList() }
        for (s in snapshot) {
            runCatching { s.send(Frame.Text(text)) }
        }
    }

    override suspend fun stop() {
        synchronized(sessions) { sessions.clear() }
        _clientCount.value = 0
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        server = null
    }
}
