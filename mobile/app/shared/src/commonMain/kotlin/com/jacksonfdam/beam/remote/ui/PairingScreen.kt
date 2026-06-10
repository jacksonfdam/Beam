package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.protocol.ConnectionState
import com.jacksonfdam.beam.protocol.DEFAULT_PORT
import com.jacksonfdam.beam.protocol.HostEndpoint
import com.jacksonfdam.beam.remote.Presentation
import com.jacksonfdam.beam.remote.RemoteController
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun PairingScreen(
    connection: ConnectionState,
    presentation: Presentation,
    controller: RemoteController,
) {
    var host by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    val busy = connection is ConnectionState.Connecting || connection is ConnectionState.Handshaking
    val error = (connection as? ConnectionState.Failed)?.reason ?: presentation.lastError

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Connect to a host", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter the host's IP and PIN shown on the presenter screen.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host IP or beam:// link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your name (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                buildEndpoint(host, pin)?.let { controller.connect(it, name.ifBlank { "Phone" }) }
            },
            enabled = !busy && host.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Connecting…" else "Connect")
        }
    }
}

/** Accept a raw `beam://` link, an `ip:port`, or a bare IP (default port). */
private fun buildEndpoint(host: String, pin: String): HostEndpoint? {
    val trimmed = host.trim()
    if (trimmed.isEmpty()) return null
    if (trimmed.contains("://")) return HostEndpoint.parse(trimmed)
    val cleanPin = pin.trim().ifBlank { null }
    val parts = trimmed.split(':')
    return if (parts.size == 2) {
        val port = parts[1].toIntOrNull() ?: return null
        HostEndpoint(parts[0], port, cleanPin)
    } else {
        HostEndpoint(trimmed, DEFAULT_PORT, cleanPin)
    }
}
