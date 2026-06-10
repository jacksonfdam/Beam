package com.jacksonfdam.beam.protocol

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProtocolRoundTripTest {

    private val clientMessages: List<ClientMessage> = listOf(
        Hello(clientName = "Phone", pin = "4821"),
        Hello(clientName = "Phone"),
        SelectDeck(deckId = "deck-1"),
        Nav(NavAction.NEXT),
        Nav(NavAction.PREV),
        Nav(NavAction.FIRST),
        Nav(NavAction.LAST),
        GoTo(index = 7),
        StrokeStart(strokeId = 1L, colorArgb = 0xFFEF4444L, widthDp = 4f, point = NormPoint(0.1f, 0.2f, 0.5f)),
        StrokePoint(strokeId = 1L, point = NormPoint(0.3f, 0.4f)),
        StrokeEnd(strokeId = 1L),
        ClearInk,
        TimerCmd(TimerAction.START),
        TimerCmd(TimerAction.PAUSE),
        TimerCmd(TimerAction.RESET),
        Ping,
        SetMode(PresentMode.SLIDES),
        SetMode(PresentMode.SCREEN),
    )

    private val hostMessages: List<HostMessage> = listOf(
        HelloAck(
            sessionName = "Jackson's Mac",
            hostVersion = "1.0.0",
            decks = listOf(DeckInfo("d1", "Talk", slideCount = 12, hasNotes = true)),
        ),
        HelloReject(reason = "bad pin"),
        DeckSelected(deckId = "d1", slideCount = 12, hasNotes = false),
        SlideChanged(index = 3, total = 12, notes = "Smile."),
        SlideChanged(index = 0, total = 12),
        TimerState(elapsedMs = 4200L, running = true),
        Pong,
        HostError(message = "boom"),
        SlideImage(index = 2, pngBase64 = "iVBORw0KGgo="),
        ModeChanged(PresentMode.SCREEN),
    )

    @Test
    fun everyClientMessageRoundTrips() {
        for (msg in clientMessages) {
            val json = BeamJson.encodeToString<ClientMessage>(msg)
            val decoded = BeamJson.decodeFromString<ClientMessage>(json)
            assertEquals(msg, decoded, "round-trip failed for $msg")
        }
    }

    @Test
    fun everyHostMessageRoundTrips() {
        for (msg in hostMessages) {
            val json = BeamJson.encodeToString<HostMessage>(msg)
            val decoded = BeamJson.decodeFromString<HostMessage>(json)
            assertEquals(msg, decoded, "round-trip failed for $msg")
        }
    }

    @Test
    fun discriminatorIsTheSnakeCaseType() {
        assertTrue(BeamJson.encodeToString<ClientMessage>(SelectDeck("x")).contains("\"type\":\"select_deck\""))
        assertTrue(BeamJson.encodeToString<ClientMessage>(ClearInk).contains("\"type\":\"clear_ink\""))
        assertTrue(BeamJson.encodeToString<HostMessage>(Pong).contains("\"type\":\"pong\""))
    }

    @Test
    fun decodesAKnownWireString() {
        val decoded = BeamJson.decodeFromString<ClientMessage>("""{"type":"nav","action":"NEXT"}""")
        assertEquals(Nav(NavAction.NEXT), decoded)
    }
}
