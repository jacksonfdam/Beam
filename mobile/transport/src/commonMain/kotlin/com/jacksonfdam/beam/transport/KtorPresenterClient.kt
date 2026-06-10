package com.jacksonfdam.beam.transport

import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.protocol.Hello
import com.jacksonfdam.beam.protocol.HelloAck
import com.jacksonfdam.beam.protocol.HelloReject
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.protocol.HostMessage
import com.jacksonfdam.beam.protocol.PresenterClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Ktor-backed [PresenterClient]. Opens a WebSocket to the host, sends [Hello]
 * (with the PIN from the endpoint), and turns the host's frames into a
 * [HostMessage] stream. Connection lifecycle is exposed through [state] so the
 * UI can render it; a drop simply moves back to Disconnected.
 */
class KtorPresenterClient(
    private val scope: CoroutineScope,
    engineFactory: HttpClientEngineFactory<*> = httpClientEngineFactory(),
) : PresenterClient {

    // Note: the client uses the engine default frame size — the OkHttp engine
    // rejects overriding maxFrameSize. Large slide-image frames are received fine.
    private val client = HttpClient(engineFactory) { install(WebSockets) }

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<HostMessage>(extraBufferCapacity = 64)
    override val incoming: Flow<HostMessage> = _incoming.asSharedFlow()

    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null

    override suspend fun connect(endpoint: HostEndpoint, clientName: String) {
        disconnect()
        _state.value = ConnectionState.Connecting
        job = scope.launch {
            try {
                client.webSocket(host = endpoint.host, port = endpoint.port, path = CUE_PATH) {
                    session = this
                    _state.value = ConnectionState.Handshaking
                    send(Frame.Text(Hello(clientName = clientName, pin = endpoint.pin).toJson()))
                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val msg = decodeHostMessage(frame.readText())
                        when (msg) {
                            is HelloAck -> _state.value = ConnectionState.Connected(msg)
                            is HelloReject -> _state.value = ConnectionState.Failed(msg.reason)
                            else -> {}
                        }
                        _incoming.emit(msg)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _state.value = ConnectionState.Failed(e.message ?: "connection error")
            } finally {
                session = null
                if (_state.value !is ConnectionState.Failed) {
                    _state.value = ConnectionState.Disconnected
                }
            }
        }
    }

    override suspend fun send(msg: ClientMessage) {
        session?.send(Frame.Text(msg.toJson()))
    }

    override suspend fun disconnect() {
        job?.cancel()
        job = null
        session = null
        _state.value = ConnectionState.Disconnected
    }
}
