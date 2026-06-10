package com.jacksonfdam.beam

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jacksonfdam.beam.host.DeckLoader
import com.jacksonfdam.beam.host.HostSession
import com.jacksonfdam.beam.i18n.ProvideStrings
import com.jacksonfdam.beam.presenter.InkOverlayScreen
import com.jacksonfdam.beam.presenter.PresenterControlScreen
import com.jacksonfdam.beam.presenter.ProjectorScreen
import com.jacksonfdam.beam.presenter.ScreenCapture
import com.jacksonfdam.beam.presenter.SlideImages
import com.jacksonfdam.beam.presenter.SlidePng
import com.jacksonfdam.beam.protocol.DEFAULT_PORT
import com.jacksonfdam.beam.protocol.NavAction
import com.jacksonfdam.beam.protocol.PresentMode
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
        s = HostSession(
            server = server,
            scope = appScope,
            sessionName = hostName(),
            encodeSlide = { document, index -> SlidePng.encode(document, index) },
            screenAspect = screenAspect(),
            captureScreen = { ScreenCapture.capture(targetScreenBounds()) },
        )
        s
    }

    var startError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { session.start(pin = pin, port = DEFAULT_PORT) }
            .onFailure { startError = it.message ?: it.toString() }
    }
    DisposableEffect(Unit) { onDispose { appScope.cancel() } }

    val state by session.state.collectAsState()
    val deck = session.deck(state.currentDeckId)

    // Presenter keyboard navigation, shared by both windows: → / Enter / Space /
    // PageDown advance; ← / Backspace / PageUp go back.
    fun onNavKey(e: KeyEvent): Boolean {
        if (e.type != KeyEventType.KeyDown) return false
        return when (e.key) {
            Key.DirectionRight, Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.PageDown -> {
                session.nav(NavAction.NEXT); true
            }
            Key.DirectionLeft, Key.Backspace, Key.PageUp -> {
                session.nav(NavAction.PREV); true
            }
            else -> false
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Beam — Presenter",
        onPreviewKeyEvent = ::onNavKey,
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            ProvideStrings {
                PresenterControlScreen(
                    state = state,
                    deck = deck,
                    startError = startError,
                    onOpenDeck = {
                        pickPdf(window)?.let { file ->
                            appScope.launch {
                                val loaded = withContext(Dispatchers.IO) { DeckLoader.load(file) }
                                session.openDeck(loaded)
                            }
                        }
                    },
                    onSetMode = { mode -> appScope.launch { session.setMode(mode) } },
                )
            }
        }
    }

    // Projector — borderless window sized to fully cover the target display
    // (the external screen when one is present). On macOS, an `undecorated`
    // window with WindowPlacement.Fullscreen does NOT enter real fullscreen and
    // leaves the slide in a small default-size window, so we size and position
    // it explicitly to the screen bounds (the same approach the overlay uses).
    val projectorState = rememberWindowState(
        position = projectorPosition(),
        size = projectorSize(),
    )
    Window(
        onCloseRequest = {},
        state = projectorState,
        title = "Beam — Projector",
        undecorated = true,
        resizable = false,
        onPreviewKeyEvent = ::onNavKey,
        // In SCREEN mode the projector hides so the real desktop (a demo/code) shows.
        visible = state.presentMode == PresentMode.SLIDES,
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            ProvideStrings {
                val slide = remember(state.currentDeckId, state.slideIndex) {
                    deck?.let { runCatching { SlideImages.render(it.document, state.slideIndex, 1920) }.getOrNull() }
                }
                ProjectorScreen(
                    slide = slide,
                    strokes = state.strokes,
                    onAdvance = { session.nav(NavAction.NEXT) },
                )
            }
        }
    }

    // Transparent annotation overlay for SCREEN mode: paints the ink over the
    // live desktop (a demo being screen-shared). Shown only while strokes exist,
    // so an empty overlay never blocks interaction with the demo.
    // Transparent windows must be undecorated and NOT maximized/fullscreen, so
    // we size it explicitly to the target screen (floating placement).
    val overlayState = rememberWindowState(
        position = projectorPosition(),
        size = projectorSize(),
    )
    Window(
        onCloseRequest = {},
        state = overlayState,
        title = "Beam — Annotations",
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = true,
        visible = state.presentMode == PresentMode.SCREEN &&
            !state.interacting &&
            (state.strokes.isNotEmpty() || state.spotlight != null),
    ) {
        InkOverlayScreen(state.strokes, state.spotlight)
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

private fun targetScreenBounds(): java.awt.Rectangle {
    val screens = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    val target = if (screens.size > 1) screens.last() else screens.first()
    return target.defaultConfiguration.bounds
}

private fun projectorPosition(): WindowPosition {
    val bounds = targetScreenBounds()
    return WindowPosition(bounds.x.dp, bounds.y.dp)
}

private fun projectorSize(): DpSize {
    val bounds = targetScreenBounds()
    return DpSize(bounds.width.dp, bounds.height.dp)
}

private fun screenAspect(): Float {
    val bounds = targetScreenBounds()
    return if (bounds.height > 0) bounds.width.toFloat() / bounds.height.toFloat() else 16f / 9f
}
