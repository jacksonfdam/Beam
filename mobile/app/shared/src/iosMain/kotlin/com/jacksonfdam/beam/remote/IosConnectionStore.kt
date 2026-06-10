package com.jacksonfdam.beam.remote

import com.jacksonfdam.beam.protocol.DEFAULT_PORT
import platform.Foundation.NSUserDefaults

/** NSUserDefaults-backed [ConnectionStore]. */
class IosConnectionStore : ConnectionStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun load(): SavedEndpoint? {
        val host = defaults.stringForKey("beam_host") ?: return null
        val port = defaults.integerForKey("beam_port").toInt().let { if (it == 0) DEFAULT_PORT else it }
        return SavedEndpoint(
            host = host,
            port = port,
            pin = defaults.stringForKey("beam_pin"),
            name = defaults.stringForKey("beam_name") ?: "",
        )
    }

    override fun save(endpoint: SavedEndpoint) {
        defaults.setObject(endpoint.host, "beam_host")
        defaults.setInteger(endpoint.port.toLong(), "beam_port")
        if (endpoint.pin != null) {
            defaults.setObject(endpoint.pin, "beam_pin")
        } else {
            defaults.removeObjectForKey("beam_pin")
        }
        defaults.setObject(endpoint.name, "beam_name")
    }

    override fun clear() {
        listOf("beam_host", "beam_port", "beam_pin", "beam_name").forEach {
            defaults.removeObjectForKey(it)
        }
    }
}
