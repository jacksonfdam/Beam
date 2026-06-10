package com.jacksonfdam.beam.transport

import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.protocol.DeckInfo
import com.jacksonfdam.beam.protocol.Hello
import com.jacksonfdam.beam.protocol.HelloAck
import com.jacksonfdam.beam.protocol.HelloReject
import com.jacksonfdam.beam.protocol.HostMessage
import com.jacksonfdam.beam.protocol.PROTOCOL_VERSION
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandshakeTest {

    private val server = KtorPresenterServer {
        HelloAck("Test Host", "1.0.0", listOf(DeckInfo("d1", "Talk", slideCount = 3, hasNotes = false)))
    }

    @AfterTest
    fun tearDown() = runBlocking { server.stop() }

    @Test
    fun acceptsCorrectPin() = runBlocking {
        val bound = server.start(port = 0, pin = "4821")
        val client = KtorPresenterClient(this)
        client.connect(bound.copy(host = "127.0.0.1"), "Phone")

        val state = client.state.first { it is ConnectionState.Connected || it is ConnectionState.Failed }
        assertTrue(state is ConnectionState.Connected, "expected Connected, was $state")
        assertEquals("Test Host", (state as ConnectionState.Connected).session.sessionName)
        client.disconnect()
    }

    @Test
    fun rejectsBadPin() = runBlocking {
        val bound = server.start(port = 0, pin = "4821")
        val client = KtorPresenterClient(this)
        client.connect(bound.copy(host = "127.0.0.1", pin = "0000"), "Phone")

        val state = client.state.first { it is ConnectionState.Connected || it is ConnectionState.Failed }
        assertTrue(state is ConnectionState.Failed, "expected Failed, was $state")
        client.disconnect()
    }

    @Test
    fun locksOutAfterRepeatedBadPins() = runBlocking {
        val bound = server.start(port = 0, pin = "4821")
        val raw = HttpClient(CIO) { install(WebSockets) }

        suspend fun attempt(pin: String): HostMessage? {
            var reply: HostMessage? = null
            raw.webSocket(host = "127.0.0.1", port = bound.port, path = CUE_PATH) {
                send(Frame.Text(Hello("Phone", protocolVersion = PROTOCOL_VERSION, pin = pin).toJson()))
                reply = decodeHostMessage((incoming.receive() as Frame.Text).readText())
            }
            return reply
        }

        // Five wrong PINs from the same host trip the lockout.
        repeat(5) {
            val r = attempt("0000")
            assertTrue(r is HelloReject, "expected HelloReject on bad pin, was $r")
        }
        // While locked out, even the CORRECT PIN is refused.
        val locked = attempt("4821")
        raw.close()
        assertTrue(
            locked is HelloReject && locked.reason.contains("too many", ignoreCase = true),
            "expected lockout reject, was $locked",
        )
    }

    @Test
    fun rejectsVersionMismatch() = runBlocking {
        val bound = server.start(port = 0, pin = null)
        // A raw client lets us forge an incompatible protocol version.
        val raw = HttpClient(CIO) { install(WebSockets) }
        var reply: HostMessage? = null
        raw.webSocket(host = "127.0.0.1", port = bound.port, path = CUE_PATH) {
            send(Frame.Text(Hello("Old client", protocolVersion = PROTOCOL_VERSION + 1).toJson()))
            val frame = incoming.receive()
            reply = decodeHostMessage((frame as Frame.Text).readText())
        }
        raw.close()
        assertTrue(reply is HelloReject, "expected HelloReject, was $reply")
    }
}
