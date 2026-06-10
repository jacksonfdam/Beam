package com.jacksonfdam.beam.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HostEndpointTest {

    @Test
    fun roundTripsWithPin() {
        val endpoint = HostEndpoint(host = "192.168.1.42", port = 53317, pin = "4821")
        assertEquals(endpoint, HostEndpoint.parse(endpoint.toUri()))
    }

    @Test
    fun roundTripsWithoutPinAndDefaultsPort() {
        val uri = HostEndpoint(host = "10.0.0.5").toUri()
        assertEquals(HostEndpoint(host = "10.0.0.5", port = DEFAULT_PORT, pin = null), HostEndpoint.parse(uri))
    }

    @Test
    fun returnsNullForMalformedUri() {
        assertNull(HostEndpoint.parse("beam://connect?port=1"))
        assertNull(HostEndpoint.parse("garbage"))
    }
}
