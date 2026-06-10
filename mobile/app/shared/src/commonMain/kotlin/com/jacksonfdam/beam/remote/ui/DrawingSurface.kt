package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.protocol.NormPoint
import com.jacksonfdam.beam.remote.HIGHLIGHT_COLOR_ARGB
import com.jacksonfdam.beam.remote.HIGHLIGHT_WIDTH_DP
import com.jacksonfdam.beam.remote.INK_COLOR_ARGB
import com.jacksonfdam.beam.remote.INK_WIDTH_DP
import com.jacksonfdam.beam.remote.RemoteController
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class DrawTool { PEN, HIGHLIGHTER, SPOTLIGHT }

private data class StrokePreview(val points: List<Offset>, val color: Color, val widthDp: Float)

/**
 * Pointer surface emitting NORMALIZED (0..1) coordinates. One finger draws (PEN/
 * HIGHLIGHTER) or sweeps a SPOTLIGHT rect; two fingers pinch-zoom and pan so you
 * can mark precisely. Points are tracked in content space and the canvas is
 * visually scaled, so a touch maps to the right spot on the projector/screen.
 */
@Composable
fun DrawingSurface(
    controller: RemoteController,
    tool: DrawTool,
    slide: ImageBitmap?,
    slideKey: Any?,
    fallbackAspect: Float,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val completed = remember { mutableStateListOf<StrokePreview>() }
    val active = remember { mutableStateListOf<Offset>() }
    var spotStart by remember { mutableStateOf<Offset?>(null) }
    var spotCurrent by remember { mutableStateOf<Offset?>(null) }

    fun resetView() {
        scale = 1f
        pan = Offset.Zero
    }

    LaunchedEffect(slideKey) {
        active.clear()
        completed.clear()
        spotStart = null
        spotCurrent = null
    }

    // Screen point -> content point (inverse of the visual scale/translate).
    fun content(p: Offset): Offset = Offset((p.x - pan.x) / scale, (p.y - pan.y) / scale)
    fun norm(p: Offset): NormPoint {
        val c = content(p)
        val w = size.width.toFloat().coerceAtLeast(1f)
        val h = size.height.toFloat().coerceAtLeast(1f)
        return NormPoint((c.x / w).coerceIn(0f, 1f), (c.y / h).coerceIn(0f, 1f))
    }

    val penColor = Color(INK_COLOR_ARGB.toInt())
    val markerColor = Color(HIGHLIGHT_COLOR_ARGB.toInt())
    val strokeColorArgb = if (tool == DrawTool.HIGHLIGHTER) HIGHLIGHT_COLOR_ARGB else INK_COLOR_ARGB
    val strokeWidthDp = if (tool == DrawTool.HIGHLIGHTER) HIGHLIGHT_WIDTH_DP else INK_WIDTH_DP
    val previewColor = if (tool == DrawTool.HIGHLIGHTER) markerColor else penColor

    val aspect = slide?.let { it.width.toFloat() / it.height.toFloat() } ?: fallbackAspect

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .onSizeChanged { size = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = pan.x
                    translationY = pan.y
                    transformOrigin = TransformOrigin(0f, 0f)
                    clip = true
                }
                .pointerInput(slideKey, tool) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var drawing = false
                        var transforming = false
                        var strokeId = -1L
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                if (drawing) { controller.endStroke(strokeId); active.clear(); drawing = false }
                                spotStart = null
                                spotCurrent = null
                                transforming = true
                                val zoom = event.calculateZoom()
                                if (zoom != 1f) scale = (scale * zoom).coerceIn(1f, 6f)
                                pan = if (scale <= 1f) Offset.Zero else pan + event.calculatePan()
                                event.changes.forEach { it.consume() }
                            } else if (!transforming) {
                                val pos = pressed.first().position
                                if (tool == DrawTool.SPOTLIGHT) {
                                    if (spotStart == null) spotStart = pos
                                    spotCurrent = pos
                                } else if (!drawing) {
                                    drawing = true
                                    active.clear()
                                    active.add(content(pos))
                                    strokeId = controller.beginStroke(norm(pos), strokeColorArgb, strokeWidthDp)
                                } else {
                                    active.add(content(pos))
                                    controller.extendStroke(strokeId, norm(pos))
                                }
                                pressed.first().consume()
                            }
                        }
                        if (drawing) {
                            if (active.isNotEmpty()) completed.add(StrokePreview(active.toList(), previewColor, strokeWidthDp))
                            active.clear()
                            controller.endStroke(strokeId)
                        }
                        if (tool == DrawTool.SPOTLIGHT && !transforming) {
                            val s = spotStart
                            val c = spotCurrent
                            if (s != null && c != null) {
                                val a = norm(s)
                                val b = norm(c)
                                controller.spotlight(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y))
                            }
                        }
                        spotStart = null
                        spotCurrent = null
                    }
                },
        ) {
            if (slide != null) {
                drawImage(
                    image = slide,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(slide.width, slide.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(size.width, size.height),
                )
            } else {
                drawRect(Color(0xFF13161D))
            }

            fun drawStroke(points: List<Offset>, color: Color, widthDp: Float) {
                if (points.isEmpty()) return
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                }
                drawPath(
                    path,
                    color,
                    style = Stroke(width = widthDp.dp.toPx() / scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            completed.forEach { drawStroke(it.points, it.color, it.widthDp) }
            if (tool != DrawTool.SPOTLIGHT) drawStroke(active, previewColor, strokeWidthDp)

            val s = spotStart
            val c = spotCurrent
            if (tool == DrawTool.SPOTLIGHT && s != null && c != null) {
                val a = content(s)
                val b = content(c)
                drawRect(
                    color = Color(0xFFFFC107),
                    topLeft = Offset(min(a.x, b.x), min(a.y, b.y)),
                    size = Size(abs(b.x - a.x), abs(b.y - a.y)),
                    style = Stroke(width = 2.dp.toPx() / scale),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    active.clear()
                    completed.clear()
                    controller.clearInk()
                },
            ) { Text("Clear ink") }
            if (scale > 1f) {
                OutlinedButton(onClick = { resetView() }) { Text("Reset zoom") }
            }
        }
    }
}
