package com.jacksonfdam.beam.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Bumped when the wire format changes incompatibly; verified in the handshake. */
const val PROTOCOL_VERSION = 1

// ---------------------------------------------------------------------------
// Client -> Host   (the phone / panel drives)
// ---------------------------------------------------------------------------

@Serializable
sealed interface ClientMessage

@Serializable
@SerialName("hello")
data class Hello(
    val clientName: String,
    val protocolVersion: Int = PROTOCOL_VERSION,
    val pin: String? = null, // short code shown on the presenter screen
) : ClientMessage

@Serializable
@SerialName("select_deck")
data class SelectDeck(val deckId: String) : ClientMessage

@Serializable
@SerialName("nav")
data class Nav(val action: NavAction) : ClientMessage

@Serializable
enum class NavAction { NEXT, PREV, FIRST, LAST }

@Serializable
@SerialName("goto")
data class GoTo(val index: Int) : ClientMessage

// Live ink: start -> point* -> end. strokeId groups points and enables undo/clear.
@Serializable
@SerialName("stroke_start")
data class StrokeStart(
    val strokeId: Long,
    val colorArgb: Long,
    val widthDp: Float,
    val point: NormPoint,
) : ClientMessage

@Serializable
@SerialName("stroke_point")
data class StrokePoint(val strokeId: Long, val point: NormPoint) : ClientMessage

@Serializable
@SerialName("stroke_end")
data class StrokeEnd(val strokeId: Long) : ClientMessage

@Serializable
@SerialName("clear_ink")
data object ClearInk : ClientMessage

@Serializable
@SerialName("timer")
data class TimerCmd(val action: TimerAction) : ClientMessage

@Serializable
enum class TimerAction { START, PAUSE, RESET }

@Serializable
@SerialName("ping")
data object Ping : ClientMessage

// What the projector shows: the deck, or the host's live screen (for a demo).
@Serializable
enum class PresentMode { SLIDES, SCREEN }

// Switch the projector between slides and the live screen — so the presenter
// can step into a code/demo (sharing the real screen) and back, all from the
// phone while still navigating the deck underneath.
@Serializable
@SerialName("set_mode")
data class SetMode(val mode: PresentMode) : ClientMessage

// In SCREEN mode, toggle whether the host's annotation overlay is shown. When
// interacting, the overlay hides so the presenter can click the live demo.
@Serializable
@SerialName("set_interacting")
data class SetInteracting(val interacting: Boolean) : ClientMessage

// Spotlight a region of the screen (normalized 0..1): the host dims everything
// outside the rect for a few seconds. A zero-area rect clears it.
@Serializable
@SerialName("spotlight")
data class Spotlight(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) : ClientMessage

// ---------------------------------------------------------------------------
// Host -> Client
// ---------------------------------------------------------------------------

@Serializable
sealed interface HostMessage

@Serializable
@SerialName("hello_ack")
data class HelloAck(
    val sessionName: String,
    val hostVersion: String,
    val decks: List<DeckInfo>,
    val screenAspect: Float = 16f / 9f,
) : HostMessage

@Serializable
@SerialName("hello_reject")
data class HelloReject(val reason: String) : HostMessage // bad pin / version mismatch

@Serializable
@SerialName("deck_selected")
data class DeckSelected(
    val deckId: String,
    val slideCount: Int,
    val hasNotes: Boolean,
) : HostMessage

// Host owns the current slide and pushes it. Notes ride along so the phone's
// presenter view shows them — the PDF itself usually carries no notes, so they
// come from a sidecar the host owns.
@Serializable
@SerialName("slide_changed")
data class SlideChanged(
    val index: Int,
    val total: Int,
    val notes: String? = null,
) : HostMessage

// Host owns the clock, so the timer survives a phone reconnect.
@Serializable
@SerialName("timer_state")
data class TimerState(val elapsedMs: Long, val running: Boolean) : HostMessage

@Serializable
@SerialName("pong")
data object Pong : HostMessage

@Serializable
@SerialName("error")
data class HostError(val message: String) : HostMessage

// A rendered preview of the current slide (PNG, Base64) so the remote can show
// what's projected — useful while drawing or when the host screen isn't visible.
@Serializable
@SerialName("slide_image")
data class SlideImage(val index: Int, val pngBase64: String) : HostMessage

// The host echoes the active projector mode so every remote stays in sync.
@Serializable
@SerialName("mode_changed")
data class ModeChanged(val mode: PresentMode) : HostMessage
