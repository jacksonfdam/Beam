package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.remote.ConnectionStore
import com.jacksonfdam.beam.remote.NoopConnectionStore
import com.jacksonfdam.beam.remote.RemoteController
import com.jacksonfdam.beam.transport.KtorPresenterClient

/**
 * Root of the Beam remote, shared by Android and iOS. Connection drives which
 * screen shows: pair → pick a deck → drive the deck. The host is authoritative,
 * so a reconnect restores everything.
 */
@Composable
fun BeamRemoteApp(store: ConnectionStore = NoopConnectionStore) {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val controller = remember { RemoteController(scope, KtorPresenterClient(scope), store) }
        val connection by controller.connection.collectAsState()
        val presentation by controller.presentation.collectAsState()

        // Try to reconnect to the last host on launch (the pairing screen shows
        // pre-filled if it fails or is cancelled).
        LaunchedEffect(Unit) {
            controller.lastSaved()?.let { saved ->
                controller.connect(HostEndpoint(saved.host, saved.port, saved.pin), saved.name)
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            // Keep content clear of the status bar and the gesture/nav bar.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
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
    }
}
