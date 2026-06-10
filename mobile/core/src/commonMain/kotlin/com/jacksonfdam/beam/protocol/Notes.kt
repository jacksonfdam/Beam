package com.jacksonfdam.beam.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * Host-side speaker-notes sidecar. A plain PDF carries no notes, so the user
 * pairs a deck with a small JSON file the host owns and reads; the notes for the
 * current slide then ride along on [SlideChanged].
 *
 * Format — a paired `<deck>.notes.json` next to the PDF:
 * ```json
 * { "version": 1, "notes": { "0": "Open warm", "3": "Pause for the demo" } }
 * ```
 * Keys are zero-based slide indices; missing indices simply have no notes.
 */
@Serializable
data class DeckNotes(
    val version: Int = 1,
    val notes: Map<Int, String> = emptyMap(),
) {
    fun notesFor(index: Int): String? = notes[index]

    companion object {
        val EMPTY = DeckNotes()

        /** Parse a sidecar; returns null on malformed input so loading can degrade gracefully. */
        fun parse(json: String): DeckNotes? =
            runCatching { BeamJson.decodeFromString<DeckNotes>(json) }.getOrNull()
    }
}
