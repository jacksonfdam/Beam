package com.jacksonfdam.beam.presenter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jacksonfdam.beam.pdf.PdfDocument
import java.awt.image.BufferedImage

/** Renders a PDF page to a Compose [ImageBitmap] for display on this JVM host. */
object SlideImages {
    fun render(document: PdfDocument, index: Int, targetWidthPx: Int): ImageBitmap {
        val raster = document.renderPage(index, targetWidthPx)
        val image = BufferedImage(raster.width, raster.height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, raster.width, raster.height, raster.argbPixels, 0, raster.width)
        return image.toComposeImageBitmap()
    }
}
