package com.jacksonfdam.beam.protocol

/**
 * The desktop host picks its LAN IPv4 to put in the QR; null if offline or not
 * applicable. Remotes (Android / iOS) connect outward and don't advertise an
 * endpoint, so their actual returns null.
 */
expect fun localIpAddress(): String?
