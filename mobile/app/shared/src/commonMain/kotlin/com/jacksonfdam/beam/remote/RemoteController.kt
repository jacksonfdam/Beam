package com.jacksonfdam.beam.remote

import com.jacksonfdam.beam.protocol.ClearInk
import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.protocol.DeckSelected
import com.jacksonfdam.beam.protocol.GoTo
import com.jacksonfdam.beam.protocol.HelloAck
import com.jacksonfdam.beam.protocol.HelloReject
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.protocol.HostError
import com.jacksonfdam.beam.protocol.HostMessage
import com.jacksonfdam.beam.protocol.ModeChanged
import com.jacksonfdam.beam.protocol.Nav
import com.jacksonfdam.beam.protocol.NavAction
import com.jacksonfdam.beam.protocol.PresentMode
import com.jacksonfdam.beam.protocol.NormPoint
import com.jacksonfdam.beam.protocol.Pong
import com.jacksonfdam.beam.protocol.PresenterClient
import com.jacksonfdam.beam.protocol.ScreenImage
import com.jacksonfdam.beam.protocol.SelectDeck
import com.jacksonfdam.beam.protocol.SetInteracting
import com.jacksonfdam.beam.protocol.SetMode
import com.jacksonfdam.beam.protocol.Spotlight
import com.jacksonfdam.beam.protocol.SlideChanged
import com.jacksonfdam.beam.protocol.SlideImage
import com.jacksonfdam.beam.protocol.StrokeEnd
import com.jacksonfdam.beam.protocol.StrokePoint
import com.jacksonfdam.beam.protocol.StrokeStart
import com.jacksonfdam.beam.protocol.TimerAction
import com.jacksonfdam.beam.protocol.TimerCmd
import com.jacksonfdam.beam.protocol.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

const val INK_COLOR_ARGB = 0xFFEF4444L
const val INK_WIDTH_DP = 4f
const val HIGHLIGHT_COLOR_ARGB = 0x55FFEB3BL // translucent yellow marker
const val HIGHLIGHT_WIDTH_DP = 18f

/**
 * Drives the remote: wraps a [PresenterClient], turns the host's pushes into a
 * [Presentation] the UI observes, and exposes intent functions for the controls.
 * The host is authoritative; this never guesses state locally.
 */
class RemoteController(
    private val scope: CoroutineScope,
    private val client: PresenterClient,
    private val store: ConnectionStore = NoopConnectionStore,
) {
    val connection: StateFlow<ConnectionState> = client.state

    private val _presentation = MutableStateFlow(Presentation())
    val presentation: StateFlow<Presentation> = _presentation.asStateFlow()

    private var strokeCounter = 0L

    init {
        scope.launch { client.incoming.collect { reduce(it) } }
    }

    /** The last host paired with, if any — used to pre-fill and auto-reconnect. */
    fun lastSaved(): SavedEndpoint? = store.load()

    fun connect(endpoint: HostEndpoint, clientName: String) {
        store.save(SavedEndpoint(endpoint.host, endpoint.port, endpoint.pin, clientName))
        _presentation.value = Presentation()
        scope.launch { client.connect(endpoint, clientName) }
    }

    fun disconnect() {
        scope.launch { client.disconnect() }
    }

    fun selectDeck(deckId: String) = send(SelectDeck(deckId))
    fun nav(action: NavAction) = send(Nav(action))
    fun goTo(index: Int) = send(GoTo(index))
    fun timer(action: TimerAction) = send(TimerCmd(action))
    fun setMode(mode: PresentMode) = send(SetMode(mode))
    fun clearInk() = send(ClearInk)

    /** Toggle the host's annotation overlay (interacting = overlay hidden, demo usable). */
    fun setInteracting(interacting: Boolean) {
        _presentation.update { it.copy(interacting = interacting) }
        send(SetInteracting(interacting))
    }

    fun spotlight(left: Float, top: Float, right: Float, bottom: Float) =
        send(Spotlight(left, top, right, bottom))

    fun beginStroke(point: NormPoint, colorArgb: Long = INK_COLOR_ARGB, widthDp: Float = INK_WIDTH_DP): Long {
        val id = ++strokeCounter
        send(StrokeStart(id, colorArgb, widthDp, point))
        return id
    }

    fun extendStroke(id: Long, point: NormPoint) = send(StrokePoint(id, point))
    fun endStroke(id: Long) = send(StrokeEnd(id))

    private fun send(msg: ClientMessage) {
        scope.launch { client.send(msg) }
    }

    private fun reduce(msg: HostMessage) {
        // Decode the slide preview outside the update lambda (which may retry).
        if (msg is SlideImage) {
            val image = runCatching { decodeImageBytes(Base64.Default.decode(msg.pngBase64)) }.getOrNull()
            _presentation.update { p -> if (msg.index == p.index) p.copy(slideImage = image) else p }
            return
        }
        if (msg is ScreenImage) {
            val image = runCatching { decodeImageBytes(Base64.Default.decode(msg.jpegBase64)) }.getOrNull()
            _presentation.update { p -> p.copy(screenImage = image) }
            return
        }
        _presentation.update { p ->
            when (msg) {
                is HelloAck -> p.copy(decks = msg.decks, screenAspect = msg.screenAspect, lastError = null)
                is HelloReject -> p.copy(lastError = msg.reason)
                is DeckSelected -> p.copy(
                    selectedDeckId = msg.deckId,
                    total = msg.slideCount,
                    hasNotes = msg.hasNotes,
                    index = 0,
                    notes = null,
                    slideImage = null,
                )

                // New slide: drop the stale preview until the fresh one arrives.
                is SlideChanged -> p.copy(index = msg.index, total = msg.total, notes = msg.notes, slideImage = null)
                is TimerState -> p.copy(timer = TimerView(msg.elapsedMs, msg.running))
                is HostError -> p.copy(lastError = msg.message)
                is Pong -> p
                is ModeChanged -> p.copy(presentMode = msg.mode)
                is SlideImage -> p // handled above
                is ScreenImage -> p // handled above
            }
        }
    }
}
