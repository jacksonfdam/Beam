package com.jacksonfdam.beam.presenter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.host.InkStroke
import com.jacksonfdam.beam.host.SpotlightRect

/**
 * A fully transparent layer painted over the live screen in SCREEN mode. Shows a
 * spotlight (dims everything outside a focus rect) and/or the live ink, on top of
 * a demo being screen-shared. Strokes/rects map to the full screen.
 */
@Composable
fun InkOverlayScreen(strokes: List<InkStroke>, spotlight: SpotlightRect?) {
    Canvas(Modifier.fillMaxSize()) {
        // Spotlight: dim the four bands around the focus rect, leaving it clear.
        if (spotlight != null) {
            val dim = Color.Black.copy(alpha = 0.6f)
            val l = (spotlight.left * size.width).coerceIn(0f, size.width)
            val t = (spotlight.top * size.height).coerceIn(0f, size.height)
            val r = (spotlight.right * size.width).coerceIn(0f, size.width)
            val b = (spotlight.bottom * size.height).coerceIn(0f, size.height)
            drawRect(dim, topLeft = Offset(0f, 0f), size = Size(size.width, t))
            drawRect(dim, topLeft = Offset(0f, b), size = Size(size.width, size.height - b))
            drawRect(dim, topLeft = Offset(0f, t), size = Size(l, b - t))
            drawRect(dim, topLeft = Offset(r, t), size = Size(size.width - r, b - t))
        }

        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            val path = Path()
            stroke.points.forEachIndexed { i, p ->
                val x = p.x * size.width
                val y = p.y * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = Color(stroke.colorArgb.toInt()),
                style = Stroke(
                    width = stroke.widthDp.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
