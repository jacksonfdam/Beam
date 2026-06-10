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
import io.ktor.server.plugins.origin
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
import java.util.concurrent.ConcurrentHashMap

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

    // Brute-force defence: a 4-digit PIN is only ~10k combinations, so an
    // external agent on the LAN could otherwise grind it. We track failed PIN
    // attempts per remote host and lock that host out for a cooldown once it
    // trips the threshold. Successful auth clears the counter.
    private val failures = ConcurrentHashMap<String, Failures>()

    private data class Failures(val count: Int, val blockedUntil: Long)

    override suspend fun start(port: Int, pin: String?): HostEndpoint {
        this.pin = pin
        val engine = embeddedServer(CIO, port = port) {
            install(WebSockets) {
                // Slide-image frames (Base64 PNG) are larger than control messages.
                maxFrameSize = MAX_FRAME_SIZE
            }
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
        val remoteHost = runCatching { call.request.origin.remoteHost }.getOrDefault("unknown")

        // Refuse a host that has tripped the brute-force lockout, before reading
        // anything: it cannot keep guessing the PIN during the cooldown.
        if (isLockedOut(remoteHost)) {
            send(Frame.Text(HelloReject("too many attempts; try again later").toJson()))
            close()
            return
        }

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
        if (!required.isNullOrEmpty() && !constantTimeEquals(hello.pin, required)) {
            registerFailure(remoteHost)
            send(Frame.Text(HelloReject("bad pin").toJson()))
            close()
            return
        }
        clearFailures(remoteHost)

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

    // --- brute-force lockout -------------------------------------------------

    private fun isLockedOut(host: String): Boolean {
        val f = failures[host] ?: return false
        return f.blockedUntil > System.currentTimeMillis()
    }

    private fun registerFailure(host: String) {
        val now = System.currentTimeMillis()
        failures.compute(host) { _, prev ->
            val count = (prev?.count ?: 0) + 1
            if (count >= MAX_PIN_ATTEMPTS) Failures(0, now + LOCKOUT_MS) else Failures(count, 0L)
        }
    }

    private fun clearFailures(host: String) {
        failures.remove(host)
    }

    /**
     * Length-independent (the PIN length is not secret) but value-comparison is
     * constant-time, so a network attacker can't learn the PIN digit-by-digit
     * from response timing.
     */
    private fun constantTimeEquals(provided: String?, expected: String): Boolean {
        val a = provided ?: ""
        if (a.length != expected.length) return false
        var diff = 0
        for (i in expected.indices) diff = diff or (a[i].code xor expected[i].code)
        return diff == 0
    }

    private companion object {
        const val MAX_PIN_ATTEMPTS = 5
        const val LOCKOUT_MS = 30_000L
    }
}
