package com.jacksonfdam.beam.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import kotlin.math.roundToInt

private class AndroidPdfDocument(
    private val renderer: PdfRenderer,
    private val pfd: ParcelFileDescriptor,
    private val backingFile: File,
) : PdfDocument {

    override val pageCount: Int get() = renderer.pageCount

    override fun pageAspectRatio(index: Int): Float {
        val page = renderer.openPage(index)
        try {
            return page.width.toFloat() / page.height.toFloat()
        } finally {
            page.close()
        }
    }

    override fun renderPage(index: Int, targetWidthPx: Int): PdfRaster {
        val page = renderer.openPage(index)
        try {
            val scale = targetWidthPx.toFloat() / page.width.toFloat()
            val height = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(targetWidthPx, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE) // slides are opaque; avoid transparent gaps
            page.render(
                bitmap,
                null,
                Matrix().apply { setScale(scale, scale) },
                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
            )
            val pixels = IntArray(targetWidthPx * height)
            bitmap.getPixels(pixels, 0, targetWidthPx, 0, 0, targetWidthPx, height)
            bitmap.recycle()
            return PdfRaster(targetWidthPx, height, pixels)
        } finally {
            page.close()
        }
    }

    override fun close() {
        renderer.close()
        pfd.close()
        backingFile.delete()
    }
}

actual fun openPdfDocument(bytes: ByteArray): PdfDocument {
    // PdfRenderer needs a seekable file descriptor, so spool the bytes to a temp file.
    val file = File.createTempFile("beam-deck", ".pdf").apply { writeBytes(bytes) }
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    return AndroidPdfDocument(PdfRenderer(pfd), pfd, file)
}
