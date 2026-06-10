package com.jacksonfdam.beam.presenter

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jacksonfdam.beam.host.InkStroke
import com.jacksonfdam.beam.i18n.LocalStrings
import kotlin.math.roundToInt

/**
 * Fullscreen projector output: the slide rendered exactly as designed, fitted
 * (letterboxed) into the display, with the live ink overlay mapped from
 * normalized coordinates onto the displayed slide rect.
 */
@Composable
fun ProjectorScreen(
    slide: ImageBitmap?,
    strokes: List<InkStroke>,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (slide == null) {
            Text(
                LocalStrings.current.waitingForDeck,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()

            // Fit using the rendered image's own aspect (matches the pixels drawn,
            // regardless of any page rotation/cropbox quirk).
            val slideAspect = slide.width.toFloat() / slide.height.toFloat()
            var dw = cw
            var dh = cw / slideAspect
            if (dh > ch) {
                dh = ch
                dw = ch * slideAspect
            }
            val left = (cw - dw) / 2f
            val top = (ch - dh) / 2f

            Canvas(Modifier.fillMaxSize()) {
                drawImage(
                    image = slide,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(slide.width, slide.height),
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                )
                for (stroke in strokes) {
                    if (stroke.points.isEmpty()) continue
                    val path = Path()
                    stroke.points.forEachIndexed { i, p ->
                        val x = left + p.x * dw
                        val y = top + p.y * dh
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
    }
}
