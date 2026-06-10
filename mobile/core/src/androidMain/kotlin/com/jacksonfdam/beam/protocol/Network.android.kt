package com.jacksonfdam.beam.protocol

// The Android app is a remote: it connects outward and never advertises an
// endpoint, so it has no LAN address to surface.
actual fun localIpAddress(): String? = null
