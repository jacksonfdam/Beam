package com.jacksonfdam.beam.protocol

import kotlinx.serialization.Serializable

/** Metadata for one loadable deck, advertised by the host in [HelloAck]. */
@Serializable
data class DeckInfo(
    val id: String,
    val title: String,
    val slideCount: Int,
    val hasNotes: Boolean,
)
