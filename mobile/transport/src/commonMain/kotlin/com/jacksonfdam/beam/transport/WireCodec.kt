package com.jacksonfdam.beam.transport

import com.jacksonfdam.beam.protocol.BeamJson
import com.jacksonfdam.beam.protocol.ClientMessage
import com.jacksonfdam.beam.protocol.HostMessage
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/** WebSocket route both sides agree on. */
internal const val CUE_PATH = "/cue"

internal fun ClientMessage.toJson(): String = BeamJson.encodeToString<ClientMessage>(this)
internal fun HostMessage.toJson(): String = BeamJson.encodeToString<HostMessage>(this)
internal fun decodeHostMessage(text: String): HostMessage = BeamJson.decodeFromString(text)
internal fun decodeClientMessage(text: String): ClientMessage = BeamJson.decodeFromString(text)
