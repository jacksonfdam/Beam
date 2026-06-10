package com.jacksonfdam.beam.presenter

import com.jacksonfdam.beam.pdf.PdfDocument
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

/** Renders a slide to a Base64 PNG to push to remotes as a preview. */
object SlidePng {
    fun encode(document: PdfDocument, index: Int, targetWidthPx: Int = 1000): String? = runCatching {
        val raster = document.renderPage(index, targetWidthPx)
        val image = BufferedImage(raster.width, raster.height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, raster.width, raster.height, raster.argbPixels, 0, raster.width)
        val out = ByteArrayOutputStream()
        ImageIO.write(image, "png", out)
        Base64.getEncoder().encodeToString(out.toByteArray())
    }.getOrNull()
}
