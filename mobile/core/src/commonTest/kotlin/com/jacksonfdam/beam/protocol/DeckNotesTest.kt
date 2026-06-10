package com.jacksonfdam.beam.protocol

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeckNotesTest {

    @Test
    fun parsesAndLooksUpByIndex() {
        val notes = DeckNotes.parse("""{"version":1,"notes":{"0":"Open warm","3":"Pause"}}""")
        assertEquals("Open warm", notes?.notesFor(0))
        assertEquals("Pause", notes?.notesFor(3))
        assertNull(notes?.notesFor(1))
    }

    @Test
    fun roundTrips() {
        val original = DeckNotes(notes = mapOf(0 to "a", 2 to "b"))
        val decoded = DeckNotes.parse(BeamJson.encodeToString(original))
        assertEquals(original, decoded)
    }

    @Test
    fun returnsNullForMalformedJson() {
        assertNull(DeckNotes.parse("not json"))
    }
}
