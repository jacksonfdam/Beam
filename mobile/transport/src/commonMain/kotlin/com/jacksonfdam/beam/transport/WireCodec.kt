package com.jacksonfdam.beam.transport

import com.jacksonfdam.beam.protocol.BeamJson
import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.HostMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** WebSocket route both sides agree on. */
internal const val CUE_PATH = "/cue"

/** Max WebSocket frame size — slide-image (Base64 PNG) frames exceed the default. */
internal const val MAX_FRAME_SIZE = 16L * 1024 * 1024

internal fun ClientMessage.toJson(): String = BeamJson.encodeToString<ClientMessage>(this)
internal fun HostMessage.toJson(): String = BeamJson.encodeToString<HostMessage>(this)
internal fun decodeHostMessage(text: String): HostMessage = BeamJson.decodeFromString(text)
internal fun decodeClientMessage(text: String): ClientMessage = BeamJson.decodeFromString(text)
