package com.jacksonfdam.beam.protocol

import java.net.Inet4Address
import java.net.NetworkInterface

actual fun localIpAddress(): String? =
    NetworkInterface.getNetworkInterfaces()
        ?.asSequence()
        ?.filter { it.isUp && !it.isLoopback }
        ?.flatMap { it.inetAddresses.asSequence() }
        ?.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
        ?.hostAddress
