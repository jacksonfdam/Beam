package com.jacksonfdam.beam.remote

import androidx.compose.ui.graphics.ImageBitmap
import com.jacksonfdam.beam.protocol.DeckInfo
import com.jacksonfdam.beam.protocol.PresentMode

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
    /** A preview of the current slide, pushed by the host; null until it arrives. */
    val slideImage: ImageBitmap? = null,
    val presentMode: PresentMode = PresentMode.SLIDES,
    val interacting: Boolean = false,
    val screenAspect: Float = 16f / 9f,
    /** Live screenshot of the host, pushed while in SCREEN mode. */
    val screenImage: ImageBitmap? = null,
)
