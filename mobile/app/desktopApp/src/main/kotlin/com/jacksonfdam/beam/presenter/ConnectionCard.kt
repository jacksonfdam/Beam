package com.jacksonfdam.beam.presenter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.i18n.LocalStrings
import com.jacksonfdam.beam.i18n.connectionQrText
import com.jacksonfdam.beam.protocol.HostEndpoint
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

/** Shows the QR for [HostEndpoint.toUri], the raw host:port, and the PIN. */
@Composable
fun ConnectionCard(endpoint: HostEndpoint, pin: String?, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val strings = LocalStrings.current
            Text(strings.scanToConnect, style = MaterialTheme.typography.titleMedium)
            Image(
                painter = rememberQrCodePainter(endpoint.toUri()),
                contentDescription = strings.connectionQrText(endpoint.host),
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                "${endpoint.host}:${endpoint.port}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!pin.isNullOrEmpty()) {
                Text(
                    "${strings.pin} $pin",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                strings.sameWifi,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )
        }
    }
}
