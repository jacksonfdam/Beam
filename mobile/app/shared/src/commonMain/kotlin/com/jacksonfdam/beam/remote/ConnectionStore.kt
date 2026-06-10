package com.jacksonfdam.beam.remote

/** The last host a remote paired with, for auto-reconnect on next launch. */
data class SavedEndpoint(
    val host: String,
    val port: Int,
    val pin: String?,
    val name: String,
)

/** Small persistent store for the last connection. Platform-backed. */
interface ConnectionStore {
    fun load(): SavedEndpoint?
    fun save(endpoint: SavedEndpoint)
    fun clear()
}

/** Default used in previews / the JVM target, where nothing is persisted. */
object NoopConnectionStore : ConnectionStore {
    override fun load(): SavedEndpoint? = null
    override fun save(endpoint: SavedEndpoint) {}
    override fun clear() {}
}
