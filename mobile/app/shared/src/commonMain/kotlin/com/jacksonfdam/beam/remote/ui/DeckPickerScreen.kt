package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.i18n.LocalStrings
import com.jacksonfdam.beam.i18n.deckSubtitleText
import com.jacksonfdam.beam.protocol.DeckInfo
import com.jacksonfdam.beam.remote.Presentation
import com.jacksonfdam.beam.remote.RemoteController

@Composable
fun DeckPickerScreen(presentation: Presentation, controller: RemoteController) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ConnectedHeader(controller)
        Text(LocalStrings.current.chooseADeck, style = MaterialTheme.typography.titleLarge)

        if (presentation.decks.isEmpty()) {
            Text(
                LocalStrings.current.noDecksAvailable,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(presentation.decks) { deck ->
                    DeckRow(deck) { controller.selectDeck(deck.id) }
                }
            }
        }
    }
}

@Composable
private fun DeckRow(deck: DeckInfo, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(deck.title, style = MaterialTheme.typography.titleMedium)
            Text(
                LocalStrings.current.deckSubtitleText(deck.slideCount, deck.hasNotes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
