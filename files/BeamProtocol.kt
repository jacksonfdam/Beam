/*
 * Beam — shared presentation protocol  (commonMain)
 *
 * Transport: JSON over WebSocket.
 *   - Desktop (JVM) hosts a Ktor server WebSocket and shows its IP:port as a QR.
 *   - Phone / web panel are Ktor client WebSockets that connect to it.
 *
 * Source of truth = the HOST. Current slide, timer and speaker notes live on the
 * desktop. The remote is a thin client: it sends commands and renders what the
 * host pushes back. A phone that drops and reconnects mid-talk loses nothing.
 */

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val PROTOCOL_VERSION = 1
const val DEFAULT_PORT = 53317

// ---------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------

/**
 * Ink coordinates are NORMALIZED (0f..1f) relative to the slide CONTENT rect —
 * not the screen. A finger on the phone lands on the same spot on the projector
 * regardless of resolution or letterboxing. The host maps norm -> pixels using
 * the rendered page rect, so aspect-ratio differences never skew the stroke.
 */
@Serializable
data class NormPoint(val x: Float, val y: Float, val pressure: Float = 1f)

// ---------------------------------------------------------------------------
// Decks
// ---------------------------------------------------------------------------

@Serializable
data class DeckInfo(
    val id: String,
    val title: String,
    val slideCount: Int,
    val hasNotes: Boolean,
)

// ---------------------------------------------------------------------------
// Client -> Host   (the phone / panel drives)
// ---------------------------------------------------------------------------

@Serializable
sealed interface ClientMessage

@Serializable @SerialName("hello")
data class Hello(
    val clientName: String,
    val protocolVersion: Int = PROTOCOL_VERSION,
    val pin: String? = null,            // short code shown on the presenter screen
) : ClientMessage

@Serializable @SerialName("select_deck")
data class SelectDeck(val deckId: String) : ClientMessage

@Serializable @SerialName("nav")
data class Nav(val action: NavAction) : ClientMessage

@Serializable
enum class NavAction { NEXT, PREV, FIRST, LAST }

@Serializable @SerialName("goto")
data class GoTo(val index: Int) : ClientMessage

// Live ink: start -> point* -> end. strokeId groups points and enables undo/clear.
@Serializable @SerialName("stroke_start")
data class StrokeStart(
    val strokeId: Long,
    val colorArgb: Long,
    val widthDp: Float,
    val point: NormPoint,
) : ClientMessage

@Serializable @SerialName("stroke_point")
data class StrokePoint(val strokeId: Long, val point: NormPoint) : ClientMessage

@Serializable @SerialName("stroke_end")
data class StrokeEnd(val strokeId: Long) : ClientMessage

@Serializable @SerialName("clear_ink")
data object ClearInk : ClientMessage

@Serializable @SerialName("timer")
data class TimerCmd(val action: TimerAction) : ClientMessage

@Serializable
enum class TimerAction { START, PAUSE, RESET }

@Serializable @SerialName("ping")
data object Ping : ClientMessage

// ---------------------------------------------------------------------------
// Host -> Client
// ---------------------------------------------------------------------------

@Serializable
sealed interface HostMessage

@Serializable @SerialName("hello_ack")
data class HelloAck(
    val sessionName: String,
    val hostVersion: String,
    val decks: List<DeckInfo>,
) : HostMessage

@Serializable @SerialName("hello_reject")
data class HelloReject(val reason: String) : HostMessage   // bad pin / version mismatch

@Serializable @SerialName("deck_selected")
data class DeckSelected(val deckId: String, val slideCount: Int, val hasNotes: Boolean) : HostMessage

// Host owns the current slide and pushes it. Notes ride along so the phone's
// presenter view shows them — the PDF itself usually carries no notes, so they
// come from a sidecar the host owns.
@Serializable @SerialName("slide_changed")
data class SlideChanged(val index: Int, val total: Int, val notes: String? = null) : HostMessage

// Host owns the clock, so the timer survives a phone reconnect.
@Serializable @SerialName("timer_state")
data class TimerState(val elapsedMs: Long, val running: Boolean) : HostMessage

@Serializable @SerialName("pong")
data object Pong : HostMessage

@Serializable @SerialName("error")
data class HostError(val message: String) : HostMessage

// ---------------------------------------------------------------------------
// Connection
// ---------------------------------------------------------------------------

/** What the QR encodes and what manual entry parses into. */
data class HostEndpoint(val host: String, val port: Int = DEFAULT_PORT, val pin: String? = null) {
    fun toUri(): String = buildString {
        append("beam://connect?host=$host&port=$port")
        pin?.let { append("&pin=$it") }
    }
    companion object {
        fun parse(uri: String): HostEndpoint? = runCatching {
            // beam://connect?host=192.168.1.42&port=53317&pin=4821
            val q = uri.substringAfter('?', "").split('&')
                .mapNotNull { it.split('=').takeIf { p -> p.size == 2 }?.let { (k, v) -> k to v } }
                .toMap()
            HostEndpoint(
                host = q.getValue("host"),
                port = q["port"]?.toInt() ?: DEFAULT_PORT,
                pin = q["pin"],
            )
        }.getOrNull()
    }
}

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Handshaking : ConnectionState
    data class Connected(val session: HelloAck) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}

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

// ---------------------------------------------------------------------------
// Serialization (shared by both sides)
// ---------------------------------------------------------------------------

val BeamJson: Json = Json {
    classDiscriminator = "type"   // matches the @SerialName tags above
    ignoreUnknownKeys = true      // forward-compat: newer host can add fields
    encodeDefaults = true
}

// ---------------------------------------------------------------------------
// Platform glue (expect/actual)
// ---------------------------------------------------------------------------

/** Desktop picks the LAN NIC address to put in the QR; null if offline. */
expect fun localIpAddress(): String?

/*
 * --- Implementation outline (not in commonMain) -------------------------------
 *
 * jvmMain — PresenterServer with Ktor:
 *
 *   embeddedServer(CIO, port = port) {
 *       install(WebSockets)
 *       routing {
 *           webSocket("/cue") {
 *               for (frame in incoming) {
 *                   val msg = BeamJson.decodeFromString<ClientMessage>((frame as Frame.Text).readText())
 *                   clientMessages.emit(msg)          // -> your session logic
 *               }
 *           }
 *       }
 *   }.start()
 *   // broadcast(): session holds the active DefaultWebSocketSession and does
 *   //   session.send(Frame.Text(BeamJson.encodeToString<HostMessage>(msg)))
 *
 * jvmMain — localIpAddress():
 *
 *   NetworkInterface.getNetworkInterfaces().asSequence()
 *       .filter { it.isUp && !it.isLoopback }
 *       .flatMap { it.inetAddresses.asSequence() }
 *       .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
 *
 * androidMain / iosMain / wasmJsMain — PresenterClient with Ktor client:
 *
 *   HttpClient(<OkHttp | Darwin | Js>) { install(WebSockets) }
 *       .webSocket(host = endpoint.host, port = endpoint.port, path = "/cue") {
 *           send(Frame.Text(BeamJson.encodeToString<ClientMessage>(Hello(name, pin = endpoint.pin))))
 *           for (frame in incoming) {
 *               hostMessages.emit(BeamJson.decodeFromString<HostMessage>((frame as Frame.Text).readText()))
 *           }
 *       }
 *
 * QR rendering is a UI-layer concern — a CMP QR composable (e.g. QRose) draws
 * HostEndpoint.toUri() on the presenter screen.
 * -----------------------------------------------------------------------------
 */
