package com.jacksonfdam.beam.protocol

import kotlinx.serialization.Serializable

/**
 * Ink coordinates are NORMALIZED (0f..1f) relative to the slide CONTENT rect —
 * not the screen. A finger on the phone lands on the same spot on the projector
 * regardless of resolution or letterboxing. The host maps norm -> pixels using
 * the rendered page rect, so aspect-ratio differences never skew the stroke.
 */
@Serializable
data class NormPoint(val x: Float, val y: Float, val pressure: Float = 1f)
