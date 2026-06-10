package com.jacksonfdam.beam.host

import com.jacksonfdam.beam.protocol.ClearInk
import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.DeckSelected
import com.jacksonfdam.beam.protocol.GoTo
import com.jacksonfdam.beam.protocol.Hello
import com.jacksonfdam.beam.protocol.HelloAck
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.protocol.Nav
import com.jacksonfdam.beam.protocol.NavAction
import com.jacksonfdam.beam.protocol.Ping
import com.jacksonfdam.beam.protocol.Pong
import com.jacksonfdam.beam.protocol.PresenterServer
import com.jacksonfdam.beam.protocol.SelectDeck
import com.jacksonfdam.beam.protocol.SlideChanged
import com.jacksonfdam.beam.protocol.StrokeEnd
import com.jacksonfdam.beam.protocol.StrokePoint
import com.jacksonfdam.beam.protocol.StrokeStart
import com.jacksonfdam.beam.protocol.TimerAction
import com.jacksonfdam.beam.protocol.TimerCmd
import com.jacksonfdam.beam.protocol.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val HOST_VERSION = "1.0.0"

/**
 * The host's single source of truth. It binds the [PresenterServer], reacts to
 * remote [ClientMessage]s, mutates slide / timer / notes / ink, and broadcasts
 * the authoritative [com.jacksonfdam.beam.protocol.HostMessage]s back. A remote
 * that drops and reconnects is brought fully back up to date on its next Hello.
 */
class HostSession(
    private val server: PresenterServer,
    private val scope: CoroutineScope,
    val sessionName: String,
) {
    private val decks = LinkedHashMap<String, HostDeck>()
    private val strokes = LinkedHashMap<Long, InkStroke>()

    private val _state = MutableStateFlow(HostState())
    val state: StateFlow<HostState> = _state.asStateFlow()

    private var current: HostDeck? = null

    // Host-owned clock.
    private var timerBaseMs = 0L
    private var timerStartedAt = 0L
    private var timerRunning = false
    private var timerJob: Job? = null

    fun deck(id: String?): HostDeck? = id?.let { decks[it] }

    suspend fun start(pin: String?, port: Int): HostEndpoint {
        val endpoint = server.start(port, pin)
        _state.update { it.copy(endpoint = endpoint, pin = pin) }
        scope.launch { server.clientCount.collect { c -> _state.update { s -> s.copy(clientCount = c) } } }
        scope.launch { server.incoming.collect { handle(it) } }
        return endpoint
    }

    fun addDeck(deck: HostDeck) {
        decks[deck.info.id] = deck
        _state.update { it.copy(decks = decks.values.map { d -> d.info }) }
    }

    /** Host-side convenience: register a freshly opened deck and project it immediately. */
    suspend fun openDeck(deck: HostDeck) {
        addDeck(deck)
        selectDeck(deck.info.id)
    }

    private suspend fun handle(msg: ClientMessage) {
        when (msg) {
            is Hello -> replayState()
            is SelectDeck -> selectDeck(msg.deckId)
            is Nav -> navigate(msg.action)
            is GoTo -> goTo(msg.index)
            is TimerCmd -> timer(msg.action)
            is StrokeStart -> {
                strokes[msg.strokeId] = InkStroke(msg.strokeId, msg.colorArgb, msg.widthDp, listOf(msg.point))
                publishStrokes()
            }
            is StrokePoint -> {
                strokes[msg.strokeId]?.let { s ->
                    strokes[msg.strokeId] = s.copy(points = s.points + msg.point)
                    publishStrokes()
                }
            }
            is StrokeEnd -> Unit // points already captured; nothing more to do
            is ClearInk -> {
                strokes.clear()
                publishStrokes()
            }
            is Ping -> server.broadcast(Pong)
        }
    }

    private suspend fun selectDeck(id: String) {
        val deck = decks[id] ?: return
        current = deck
        strokes.clear()
        _state.update {
            it.copy(
                currentDeckId = id,
                slideTotal = deck.info.slideCount,
                slideIndex = 0,
                currentNotes = deck.notes.notesFor(0),
                strokes = emptyList(),
            )
        }
        server.broadcast(DeckSelected(deck.info.id, deck.info.slideCount, deck.info.hasNotes))
        broadcastSlide()
    }

    private suspend fun navigate(action: NavAction) {
        val total = _state.value.slideTotal
        if (total == 0) return
        val cur = _state.value.slideIndex
        val next = when (action) {
            NavAction.NEXT -> (cur + 1).coerceAtMost(total - 1)
            NavAction.PREV -> (cur - 1).coerceAtLeast(0)
            NavAction.FIRST -> 0
            NavAction.LAST -> total - 1
        }
        if (next != cur) setSlide(next)
    }

    private suspend fun goTo(index: Int) {
        val total = _state.value.slideTotal
        if (total == 0) return
        val clamped = index.coerceIn(0, total - 1)
        if (clamped != _state.value.slideIndex) setSlide(clamped)
    }

    private suspend fun setSlide(index: Int) {
        strokes.clear() // ink is per-slide
        _state.update { it.copy(slideIndex = index, strokes = emptyList()) }
        broadcastSlide()
    }

    private suspend fun broadcastSlide() {
        val deck = current ?: return
        val index = _state.value.slideIndex
        val notes = deck.notes.notesFor(index)
        _state.update { it.copy(currentNotes = notes) }
        server.broadcast(SlideChanged(index, deck.info.slideCount, notes))
    }

    private fun publishStrokes() {
        _state.update { it.copy(strokes = strokes.values.toList()) }
    }

    // --- timer ---------------------------------------------------------------

    private fun elapsedMs(): Long =
        timerBaseMs + if (timerRunning) System.currentTimeMillis() - timerStartedAt else 0L

    private suspend fun timer(action: TimerAction) {
        when (action) {
            TimerAction.START -> if (!timerRunning) {
                timerStartedAt = System.currentTimeMillis()
                timerRunning = true
                startTicking()
            }
            TimerAction.PAUSE -> if (timerRunning) {
                timerBaseMs = elapsedMs()
                timerRunning = false
                timerJob?.cancel()
                timerJob = null
            }
            TimerAction.RESET -> {
                timerBaseMs = 0
                timerStartedAt = System.currentTimeMillis()
            }
        }
        pushTimer()
    }

    private fun startTicking() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (timerRunning) {
                pushTimer()
                delay(1000)
            }
        }
    }

    private suspend fun pushTimer() {
        val elapsed = elapsedMs()
        _state.update { it.copy(timerElapsedMs = elapsed, timerRunning = timerRunning) }
        server.broadcast(TimerState(elapsed, timerRunning))
    }

    /** On (re)connect, push the current deck/slide/timer so the remote loses nothing. */
    private suspend fun replayState() {
        current?.let { deck ->
            server.broadcast(DeckSelected(deck.info.id, deck.info.slideCount, deck.info.hasNotes))
            server.broadcast(SlideChanged(_state.value.slideIndex, deck.info.slideCount, _state.value.currentNotes))
        }
        server.broadcast(TimerState(elapsedMs(), timerRunning))
    }

    /** Build the HelloAck the transport sends to each newly accepted client. */
    fun helloAck(): HelloAck = HelloAck(sessionName, HOST_VERSION, _state.value.decks)
}
