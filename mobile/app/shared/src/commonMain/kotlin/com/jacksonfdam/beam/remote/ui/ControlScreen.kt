package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.protocol.NavAction
import com.jacksonfdam.beam.protocol.TimerAction
import com.jacksonfdam.beam.remote.Presentation
import com.jacksonfdam.beam.remote.RemoteController

@Composable
fun ControlScreen(presentation: Presentation, controller: RemoteController) {
    var showDrawing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConnectedHeader(controller)

        SlideIndicator(presentation.index, presentation.total)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { controller.nav(NavAction.PREV) }, modifier = Modifier.weight(1f)) { Text("‹ Prev") }
            Button(onClick = { controller.nav(NavAction.NEXT) }, modifier = Modifier.weight(1f)) { Text("Next ›") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { controller.nav(NavAction.FIRST) }, modifier = Modifier.weight(1f)) { Text("First") }
            OutlinedButton(onClick = { controller.nav(NavAction.LAST) }, modifier = Modifier.weight(1f)) { Text("Last") }
        }

        NotesCard(presentation.notes, presentation.hasNotes)
        TimerCard(presentation.timer.elapsedMs, presentation.timer.running, controller)

        TextButton(onClick = { showDrawing = !showDrawing }) {
            Text(if (showDrawing) "Hide drawing" else "Draw on the slide")
        }
        if (showDrawing) {
            DrawingSurface(controller, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SlideIndicator(index: Int, total: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > 0) "${index + 1} / $total" else "0 / 0",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Text("Slide", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NotesCard(notes: String?, hasNotes: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Speaker notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                notes ?: if (hasNotes) "No notes for this slide." else "This deck has no notes sidecar.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimerCard(elapsedMs: Long, running: Boolean, controller: RemoteController) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Timer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { controller.timer(if (running) TimerAction.PAUSE else TimerAction.START) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (running) "Pause" else "Start") }
                OutlinedButton(onClick = { controller.timer(TimerAction.RESET) }, modifier = Modifier.weight(1f)) { Text("Reset") }
            }
        }
    }
}
