package com.jacksonfdam.beam.presenter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.host.HostDeck
import com.jacksonfdam.beam.host.HostState
import com.jacksonfdam.beam.i18n.LocalStrings
import com.jacksonfdam.beam.i18n.portHeldText
import com.jacksonfdam.beam.i18n.remotesConnectedText
import com.jacksonfdam.beam.i18n.slideXofYText
import com.jacksonfdam.beam.protocol.DEFAULT_PORT
import com.jacksonfdam.beam.protocol.PresentMode

/** The presenter's control window: live preview, next slide, notes, timer, and the connection card. */
@Composable
fun PresenterControlScreen(
    state: HostState,
    deck: HostDeck?,
    startError: String? = null,
    onOpenDeck: () -> Unit,
    onSetMode: (PresentMode) -> Unit = {},
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Beam", style = MaterialTheme.typography.headlineSmall)
            ModeToggle(state.presentMode, onSetMode)
            com.jacksonfdam.beam.i18n.LanguageSelector()
            Box(Modifier.weight(1f))
            Text(
                strings.remotesConnectedText(state.clientCount),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.clientCount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
            )
            Button(onClick = onOpenDeck) { Text(if (deck == null) strings.openAPdf else strings.openAnotherPdf) }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: current slide + next.
            Column(
                modifier = Modifier.weight(2f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CurrentSlide(state, deck, modifier = Modifier.weight(1f))
                NextSlide(state, deck)
            }

            // Right: connection, timer, notes.
            Column(
                modifier = Modifier.width(280.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val endpoint = state.endpoint
                if (endpoint != null) {
                    ConnectionCard(endpoint, state.pin, modifier = Modifier.fillMaxWidth())
                } else {
                    ServerStatusCard(startError)
                }
                TimerCard(state.timerElapsedMs, state.timerRunning)
                NotesCard(state.currentNotes, deck != null)
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: PresentMode, onSetMode: (PresentMode) -> Unit) {
    val strings = LocalStrings.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton(strings.slidesMode, mode == PresentMode.SLIDES) { onSetMode(PresentMode.SLIDES) }
        ModeButton(strings.screenMode, mode == PresentMode.SCREEN) { onSetMode(PresentMode.SCREEN) }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun CurrentSlide(state: HostState, deck: HostDeck?, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (state.hasDeck) strings.slideXofYText(state.slideIndex + 1, state.slideTotal) else strings.noDeckLoaded,
                style = MaterialTheme.typography.titleMedium,
            )
            val preview = remember(deck?.info?.id, state.slideIndex) {
                deck?.let {
                    runCatching {
                        SlideImages.render(
                            it.document,
                            state.slideIndex,
                            1280
                        )
                    }.getOrNull()
                }
            }
            Box(
                Modifier.fillMaxSize().background(Color.Black).clip(RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        preview,
                        contentDescription = strings.currentSlideCd,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(strings.openPdfPressStart, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun NextSlide(state: HostState, deck: HostDeck?) {
    val hasNext = state.slideIndex + 1 < state.slideTotal
    val next = remember(deck?.info?.id, state.slideIndex) {
        deck?.takeIf { hasNext }?.let {
            runCatching {
                SlideImages.render(
                    it.document,
                    state.slideIndex + 1,
                    640
                )
            }.getOrNull()
        }
    }
    val strings = LocalStrings.current
    Card {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(strings.nextLabel, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Box(
                Modifier.width(160.dp).aspectRatio(16f / 9f).background(Color.Black)
                    .clip(RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center
            ) {
                if (next != null) {
                    Image(
                        next,
                        contentDescription = strings.nextSlideCd,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        if (hasNext) "…" else strings.end,
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerStatusCard(error: String?) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.connectionTitle, style = MaterialTheme.typography.titleMedium)
            if (error == null) {
                Text(strings.startingServer, color = Color.Gray)
            } else {
                Text(strings.couldntStartServer, color = MaterialTheme.colorScheme.error)
                Text(error, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(
                    strings.portHeldText(DEFAULT_PORT),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun TimerCard(elapsedMs: Long, running: Boolean) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.timer, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Text(formatElapsed(elapsedMs), style = MaterialTheme.typography.headlineMedium)
            Text(
                if (running) strings.running else strings.paused,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun NotesCard(notes: String?, hasDeck: Boolean) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.speakerNotes, style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            Text(
                notes ?: if (hasDeck) strings.noNotesForSlide else strings.loadDeckToSeeNotes,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
