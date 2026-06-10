package com.jacksonfdam.beam.presenter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.host.InkStroke

/**
 * A fully transparent ink layer painted over the live screen in SCREEN mode, so
 * annotations show on top of a demo being screen-shared. Strokes map to the full
 * screen (there's no slide rect here). Used inside a transparent, always-on-top
 * window that is only shown while there are strokes.
 */
@Composable
fun InkOverlayScreen(strokes: List<InkStroke>) {
    Canvas(Modifier.fillMaxSize()) {
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
