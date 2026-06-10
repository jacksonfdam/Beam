package com.jacksonfdam.beam.host

import com.jacksonfdam.beam.protocol.NormPoint

/**
 * One live ink stroke painted by a remote, kept in NORMALIZED coordinates so
 * the projector maps it onto the slide rect at any resolution.
 */
data class InkStroke(
    val id: Long,
    val colorArgb: Long,
    val widthDp: Float,
    val points: List<NormPoint>,
)
