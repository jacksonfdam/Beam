package com.jacksonfdam.beam

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jacksonfdam.beam.host.DeckLoader
import com.jacksonfdam.beam.host.HostSession
import com.jacksonfdam.beam.presenter.PresenterControlScreen
import com.jacksonfdam.beam.presenter.ProjectorScreen
import com.jacksonfdam.beam.presenter.SlideImages
import com.jacksonfdam.beam.protocol.DEFAULT_PORT
import com.jacksonfdam.beam.transport.KtorPresenterServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.io.File
import java.net.InetAddress
import kotlin.random.Random

fun main() = application {
    val appScope = remember { CoroutineScope(Dispatchers.Default + SupervisorJob()) }
    val pin = remember { (1000 + Random.nextInt(9000)).toString() }

    // The server greets each client with the session's current decks; the session
    // owns all state. (lateinit breaks the constructor cycle between the two.)
    val session = remember {
        lateinit var s: HostSession
        val server = KtorPresenterServer { s.helloAck() }
        s = HostSession(server, appScope, hostName())
        s
    }

    LaunchedEffect(Unit) { runCatching { session.start(pin = pin, port = DEFAULT_PORT) } }
    DisposableEffect(Unit) { onDispose { appScope.cancel() } }

    val state by session.state.collectAsState()
    val deck = session.deck(state.currentDeckId)

    Window(onCloseRequest = ::exitApplication, title = "Beam — Presenter") {
        MaterialTheme(colorScheme = darkColorScheme()) {
            PresenterControlScreen(
                state = state,
                deck = deck,
                onOpenDeck = {
                    pickPdf(window)?.let { file ->
                        appScope.launch {
                            val loaded = withContext(Dispatchers.IO) { DeckLoader.load(file) }
                            session.openDeck(loaded)
                        }
                    }
                },
            )
        }
    }

    // Fullscreen projector — on the external display when one is present.
    val projectorState = rememberWindowState(
        placement = WindowPlacement.Fullscreen,
        position = projectorPosition(),
    )
    Window(
        onCloseRequest = {},
        state = projectorState,
        title = "Beam — Projector",
        undecorated = true,
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            val slide = remember(state.currentDeckId, state.slideIndex) {
                deck?.let { runCatching { SlideImages.render(it.document, state.slideIndex, 1920) }.getOrNull() }
            }
            val aspect = remember(state.currentDeckId, state.slideIndex) {
                deck?.let { runCatching { it.document.pageAspectRatio(state.slideIndex) }.getOrNull() } ?: (16f / 9f)
            }
            ProjectorScreen(slide = slide, slideAspect = aspect, strokes = state.strokes)
        }
    }
}

private fun hostName(): String =
    runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("Beam Host")

private fun pickPdf(parent: java.awt.Window?): File? {
    val dialog = FileDialog(parent as? Frame, "Open a PDF", FileDialog.LOAD)
    dialog.setFilenameFilter { _, name -> name.endsWith(".pdf", ignoreCase = true) }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val file = dialog.file ?: return null
    return File(dir, file)
}

private fun projectorPosition(): WindowPosition {
    val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    val target = if (screens.size > 1) screens.last() else screens.first()
    val bounds = target.defaultConfiguration.bounds
    return WindowPosition(bounds.x.dp, bounds.y.dp)
}
