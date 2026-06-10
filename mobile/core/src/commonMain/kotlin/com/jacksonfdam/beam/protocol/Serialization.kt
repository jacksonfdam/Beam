package com.jacksonfdam.beam.protocol

import kotlinx.serialization.json.Json

/**
 * The one JSON configuration shared by host and every client. The polymorphic
 * sealed hierarchies are discriminated by a "type" key that matches the
 * @SerialName tags on each message.
 */
val BeamJson: Json = Json {
    classDiscriminator = "type" // matches the @SerialName tags on the messages
    ignoreUnknownKeys = true // forward-compat: a newer host can add fields
    encodeDefaults = true
}
