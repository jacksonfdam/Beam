package com.jacksonfdam.beam.remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.ImageBitmap
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
import kotlin.math.max
import kotlin.math.min

enum class DrawTool { PEN, HIGHLIGHTER, SPOTLIGHT }

private data class StrokePreview(val points: List<Offset>, val color: Color, val widthDp: Float)

/**
 * Pointer surface emitting NORMALIZED (0..1) coordinates. PEN/HIGHLIGHTER stream
 * strokes (the marker is a translucent yellow); SPOTLIGHT drags a rectangle and,
 * on release, asks the host to dim everything outside it. The surface takes the
 * slide's aspect in SLIDES mode and [fallbackAspect] (the screen) in SCREEN mode,
 * so a touch maps to the right spot. Changing [slideKey] clears the local ink.
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
    val completed = remember { mutableStateListOf<StrokePreview>() }
    val active = remember { mutableStateListOf<Offset>() }
    var activeId by remember { mutableStateOf(-1L) }
    var spotStart by remember { mutableStateOf<Offset?>(null) }
    var spotCurrent by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(slideKey) {
        active.clear()
        completed.clear()
        spotStart = null
        spotCurrent = null
    }

    fun norm(o: Offset): NormPoint {
        val w = size.width.toFloat().coerceAtLeast(1f)
        val h = size.height.toFloat().coerceAtLeast(1f)
        return NormPoint((o.x / w).coerceIn(0f, 1f), (o.y / h).coerceIn(0f, 1f))
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
            .pointerInput(slideKey, tool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (tool == DrawTool.SPOTLIGHT) {
                            spotStart = offset
                            spotCurrent = offset
                        } else {
                            active.clear()
                            active.add(offset)
                            activeId = controller.beginStroke(norm(offset), strokeColorArgb, strokeWidthDp)
                        }
                    },
                    onDrag = { change, _ ->
                        if (tool == DrawTool.SPOTLIGHT) {
                            spotCurrent = change.position
                        } else {
                            active.add(change.position)
                            controller.extendStroke(activeId, norm(change.position))
                        }
                    },
                    onDragEnd = {
                        if (tool == DrawTool.SPOTLIGHT) {
                            val s = spotStart
                            val c = spotCurrent
                            if (s != null && c != null) {
                                val a = norm(s)
                                val b = norm(c)
                                controller.spotlight(
                                    min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y),
                                )
                            }
                            spotStart = null
                            spotCurrent = null
                        } else {
                            if (active.isNotEmpty()) completed.add(StrokePreview(active.toList(), previewColor, strokeWidthDp))
                            active.clear()
                            controller.endStroke(activeId)
                        }
                    },
                    onDragCancel = {
                        spotStart = null
                        spotCurrent = null
                        active.clear()
                        controller.endStroke(activeId)
                    },
                )
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
                style = Stroke(width = widthDp.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        completed.forEach { drawStroke(it.points, it.color, it.widthDp) }
        if (tool != DrawTool.SPOTLIGHT) drawStroke(active, previewColor, strokeWidthDp)

        // Spotlight rectangle preview while dragging.
        val s = spotStart
        val c = spotCurrent
        if (tool == DrawTool.SPOTLIGHT && s != null && c != null) {
            val left = min(s.x, c.x)
            val top = min(s.y, c.y)
            drawRect(
                color = Color(0xFFFFC107),
                topLeft = Offset(left, top),
                size = Size(kotlin.math.abs(c.x - s.x), kotlin.math.abs(c.y - s.y)),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
        }

        OutlinedButton(
            onClick = {
                active.clear()
                completed.clear()
                controller.clearInk()
            },
        ) {
            Text("Clear ink")
        }
    }
}
