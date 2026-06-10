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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.protocol.NormPoint
import com.jacksonfdam.beam.remote.RemoteController
import kotlin.math.roundToInt

/**
 * A pointer surface that emits NORMALIZED (0..1) coordinates relative to its own
 * size. When the current [slide] is available it is drawn as the background and
 * the surface takes the slide's aspect ratio, so a touch maps to the exact spot
 * on the projected slide. Changing slide ([slideKey]) clears the local preview.
 */
@Composable
fun DrawingSurface(
    controller: RemoteController,
    slide: ImageBitmap?,
    slideKey: Any?,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val completed = remember { mutableStateListOf<List<Offset>>() }
    val active = remember { mutableStateListOf<Offset>() }
    var activeId by remember { mutableStateOf(-1L) }

    // Clear the local ink whenever the slide changes (the host clears its own).
    LaunchedEffect(slideKey) {
        active.clear()
        completed.clear()
    }

    fun norm(o: Offset): NormPoint {
        val w = size.width.toFloat().coerceAtLeast(1f)
        val h = size.height.toFloat().coerceAtLeast(1f)
        return NormPoint((o.x / w).coerceIn(0f, 1f), (o.y / h).coerceIn(0f, 1f))
    }

    val aspect = if (slide != null) slide.width.toFloat() / slide.height.toFloat() else 16f / 9f

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .onSizeChanged { size = it }
                .pointerInput(slideKey) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            active.clear()
                            active.add(offset)
                            activeId = controller.beginStroke(norm(offset))
                        },
                        onDrag = { change, _ ->
                            active.add(change.position)
                            controller.extendStroke(activeId, norm(change.position))
                        },
                        onDragEnd = {
                            if (active.isNotEmpty()) completed.add(active.toList())
                            active.clear()
                            controller.endStroke(activeId)
                        },
                        onDragCancel = {
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
                    dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                )
            } else {
                drawRect(Color(0xFF13161D))
            }
            val ink = Color(0xFFEF4444)
            val style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            fun draw(points: List<Offset>) {
                if (points.isEmpty()) return
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                }
                drawPath(path, ink, style = style)
            }
            completed.forEach(::draw)
            draw(active)
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
