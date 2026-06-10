package com.jacksonfdam.beam.remote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.remote.RemoteController
import com.jacksonfdam.beam.transport.KtorPresenterClient

/**
 * Root of the Beam remote, shared by Android and iOS. Connection drives which
 * screen shows: pair → pick a deck → drive the deck. The host is authoritative,
 * so a reconnect restores everything.
 */
@Composable
fun BeamRemoteApp() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val controller = remember { RemoteController(scope, KtorPresenterClient(scope)) }
        val connection by controller.connection.collectAsState()
        val presentation by controller.presentation.collectAsState()

        when (connection) {
            is ConnectionState.Connected ->
                if (presentation.selectedDeckId == null) {
                    DeckPickerScreen(presentation, controller)
                } else {
                    ControlScreen(presentation, controller)
                }
            else -> PairingScreen(connection, presentation, controller)
        }
    }
}
