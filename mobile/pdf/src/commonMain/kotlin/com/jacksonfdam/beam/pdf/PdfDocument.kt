package com.jacksonfdam.beam.pdf

/**
 * A rendered page as raw ARGB_8888 pixels (row-major, [width] * [height]).
 * The UI layer turns this into a platform ImageBitmap; keeping it as plain
 * pixels means `:pdf` has no Compose/UI dependency (SRP).
 */
class PdfRaster(
    val width: Int,
    val height: Int,
    val argbPixels: IntArray,
)

/**
 * A loaded PDF. Pages render exactly as designed — Beam never re-flows or
 * substitutes fonts; it draws the page the way the exporter produced it.
 *
 * Implementations are not thread-safe; render from a single coroutine/thread
 * and call [close] when done to release native resources.
 */
interface PdfDocument {
    val pageCount: Int

    /** Page width / height, for letterboxing the projector and the remote preview. */
    fun pageAspectRatio(index: Int): Float

    /** Render [index] to ARGB pixels at [targetWidthPx]; height follows the aspect ratio. */
    fun renderPage(index: Int, targetWidthPx: Int): PdfRaster

    fun close()
}

/** Open a PDF from its raw bytes. Bytes are the one source every platform accepts. */
expect fun openPdfDocument(bytes: ByteArray): PdfDocument
