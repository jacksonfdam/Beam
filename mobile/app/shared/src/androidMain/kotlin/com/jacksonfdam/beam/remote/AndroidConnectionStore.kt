package com.jacksonfdam.beam.remote

import android.content.Context
import com.jacksonfdam.beam.protocol.DEFAULT_PORT

/** SharedPreferences-backed [ConnectionStore]. */
class AndroidConnectionStore(context: Context) : ConnectionStore {
    private val prefs = context.applicationContext
        .getSharedPreferences("beam_remote", Context.MODE_PRIVATE)

    override fun load(): SavedEndpoint? {
        val host = prefs.getString("host", null) ?: return null
        return SavedEndpoint(
            host = host,
            port = prefs.getInt("port", DEFAULT_PORT),
            pin = prefs.getString("pin", null),
            name = prefs.getString("name", "") ?: "",
        )
    }

    override fun save(endpoint: SavedEndpoint) {
        prefs.edit()
            .putString("host", endpoint.host)
            .putInt("port", endpoint.port)
            .putString("pin", endpoint.pin)
            .putString("name", endpoint.name)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
