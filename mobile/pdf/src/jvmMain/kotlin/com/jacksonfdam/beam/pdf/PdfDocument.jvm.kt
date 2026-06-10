package com.jacksonfdam.beam.pdf

import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer

private class PdfBoxDocument(private val doc: PDDocument) : PdfDocument {
    private val renderer = PDFRenderer(doc)

    override val pageCount: Int get() = doc.numberOfPages

    override fun pageAspectRatio(index: Int): Float {
        val box = doc.getPage(index).cropBox
        return box.width / box.height
    }

    override fun renderPage(index: Int, targetWidthPx: Int): PdfRaster {
        val box = doc.getPage(index).cropBox
        val scale = targetWidthPx / box.width
        val image = renderer.renderImage(index, scale, ImageType.RGB)
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)
        return PdfRaster(width, height, pixels)
    }

    override fun close() = doc.close()
}

actual fun openPdfDocument(bytes: ByteArray): PdfDocument =
    PdfBoxDocument(Loader.loadPDF(bytes))
