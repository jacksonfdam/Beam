package com.jacksonfdam.beam.remote

import com.jacksonfdam.beam.protocol.DeckInfo

/** Timer as last pushed by the host (the host owns the clock). */
data class TimerView(
    val elapsedMs: Long = 0,
    val running: Boolean = false,
)

/** The remote's view of the presentation, rebuilt from the host's pushes. */
data class Presentation(
    val decks: List<DeckInfo> = emptyList(),
    val selectedDeckId: String? = null,
    val index: Int = 0,
    val total: Int = 0,
    val notes: String? = null,
    val hasNotes: Boolean = false,
    val timer: TimerView = TimerView(),
    val lastError: String? = null,
)
