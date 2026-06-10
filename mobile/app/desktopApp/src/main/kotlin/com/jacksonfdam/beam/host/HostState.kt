package com.jacksonfdam.beam.host

import com.jacksonfdam.beam.protocol.DeckInfo
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.protocol.PresentMode

/** Everything the presenter UI renders. The host is the single source of truth. */
data class HostState(
    val endpoint: HostEndpoint? = null,
    val pin: String? = null,
    val decks: List<DeckInfo> = emptyList(),
    val currentDeckId: String? = null,
    val slideIndex: Int = 0,
    val slideTotal: Int = 0,
    val currentNotes: String? = null,
    val clientCount: Int = 0,
    val timerElapsedMs: Long = 0,
    val timerRunning: Boolean = false,
    val strokes: List<InkStroke> = emptyList(),
    val presentMode: PresentMode = PresentMode.SLIDES,
) {
    val hasDeck: Boolean get() = currentDeckId != null && slideTotal > 0
}
