package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.i18n.LocalStrings
import com.jacksonfdam.beam.i18n.slideXofYText
import com.jacksonfdam.beam.protocol.NavAction
import com.jacksonfdam.beam.protocol.PresentMode
import com.jacksonfdam.beam.protocol.TimerAction
import com.jacksonfdam.beam.remote.Presentation
import com.jacksonfdam.beam.remote.RemoteController

@Composable
fun ControlScreen(presentation: Presentation, controller: RemoteController) {
    var showDrawing by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(DrawTool.PEN) }
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConnectedHeader(controller)

        ModeToggle(presentation.presentMode) { controller.setMode(it) }

        val slidesMode = presentation.presentMode == PresentMode.SLIDES
        if (slidesMode) {
            presentation.slideImage?.let { SlidePreview(it) }
        } else {
            val shot = presentation.screenImage
            if (shot != null) SlidePreview(shot) else ScreenModeNote()
            InteractToggle(presentation.interacting) { controller.setInteracting(it) }
        }

        SlideIndicator(presentation.index, presentation.total)

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { controller.nav(NavAction.PREV) },
                modifier = Modifier.weight(1f)
            ) { Text(strings.prev) }
            Button(
                onClick = { controller.nav(NavAction.NEXT) },
                modifier = Modifier.weight(1f)
            ) { Text(strings.next) }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = { controller.nav(NavAction.FIRST) },
                modifier = Modifier.weight(1f)
            ) { Text(strings.first) }
            OutlinedButton(
                onClick = { controller.nav(NavAction.LAST) },
                modifier = Modifier.weight(1f)
            ) { Text(strings.last) }
        }

        NotesCard(presentation.notes, presentation.hasNotes)
        TimerCard(presentation.timer.elapsedMs, presentation.timer.running, controller)

        TextButton(
            onClick = {
                if (showDrawing) controller.clearInk() // hiding also clears the projected ink
                showDrawing = !showDrawing
            },
        ) {
            Text(if (showDrawing) strings.hideDrawing else if (slidesMode) strings.drawOnSlide else strings.drawSpotlight)
        }
        if (showDrawing) {
            val effectiveTool = if (slidesMode && tool == DrawTool.SPOTLIGHT) DrawTool.PEN else tool
            ToolSelector(effectiveTool, allowSpotlight = !slidesMode) { tool = it }
            DrawingSurface(
                controller = controller,
                tool = effectiveTool,
                // In SCREEN mode you draw over the live screen snapshot.
                slide = if (slidesMode) presentation.slideImage else presentation.screenImage,
                slideKey = if (slidesMode) presentation.index else "screen",
                fallbackAspect = presentation.screenAspect,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ModeToggle(mode: PresentMode, onSetMode: (PresentMode) -> Unit) {
    val strings = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeButton(strings.slidesMode, mode == PresentMode.SLIDES, Modifier.weight(1f)) { onSetMode(PresentMode.SLIDES) }
        ModeButton(strings.screenMode, mode == PresentMode.SCREEN, Modifier.weight(1f)) { onSetMode(PresentMode.SCREEN) }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val padding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    val text = @Composable {
        Text(label, maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
    }
    if (selected) {
        Button(onClick = onClick, modifier = modifier, contentPadding = padding) { text() }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = padding) { text() }
    }
}

@Composable
private fun InteractToggle(interacting: Boolean, onSet: (Boolean) -> Unit) {
    val strings = LocalStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeButton(strings.annotate, !interacting, Modifier.weight(1f)) { onSet(false) }
        ModeButton(strings.interact, interacting, Modifier.weight(1f)) { onSet(true) }
    }
}

@Composable
private fun ToolSelector(tool: DrawTool, allowSpotlight: Boolean, onSelect: (DrawTool) -> Unit) {
    val strings = LocalStrings.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModeButton(strings.pen, tool == DrawTool.PEN, Modifier.weight(1f)) { onSelect(DrawTool.PEN) }
        ModeButton(strings.marker, tool == DrawTool.HIGHLIGHTER, Modifier.weight(1f)) { onSelect(DrawTool.HIGHLIGHTER) }
        if (allowSpotlight) {
            ModeButton(strings.spotlight, tool == DrawTool.SPOTLIGHT, Modifier.weight(1f)) { onSelect(DrawTool.SPOTLIGHT) }
        }
    }
}

@Composable
private fun ScreenModeNote() {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(strings.projectingYourScreen, style = MaterialTheme.typography.titleMedium)
            Text(
                strings.screenModeBody,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SlidePreview(image: ImageBitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Card(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(image.width.toFloat() / image.height.toFloat())
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale <= 1f) Offset.Zero else offset + pan
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = image,
                contentDescription = LocalStrings.current.slidePreviewCd,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun SlideIndicator(index: Int, total: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (total > 0) "${index + 1} / $total" else "0 / 0",
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center,
            )
            Text(
                LocalStrings.current.slideLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotesCard(notes: String?, hasNotes: Boolean) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                strings.speakerNotes,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                notes
                    ?: if (hasNotes) strings.noNotesForSlide else strings.noNotesSidecar,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TimerCard(elapsedMs: Long, running: Boolean, controller: RemoteController) {
    val strings = LocalStrings.current
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    strings.timer,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { controller.timer(if (running) TimerAction.PAUSE else TimerAction.START) },
                    modifier = Modifier.weight(1f),
                ) { Text(if (running) strings.pause else strings.start) }
                OutlinedButton(
                    onClick = { controller.timer(TimerAction.RESET) },
                    modifier = Modifier.weight(1f)
                ) { Text(strings.reset) }
            }
        }
    }
}
