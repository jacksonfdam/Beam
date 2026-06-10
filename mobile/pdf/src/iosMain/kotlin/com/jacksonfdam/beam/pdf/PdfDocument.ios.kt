package com.jacksonfdam.beam.pdf

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGDataProviderCreateWithCFData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGPDFBox
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGPDFDocumentRef
import platform.CoreGraphics.CGPDFDocumentRelease
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.CGRectMake
import platform.posix.UByteVar
import kotlin.math.roundToInt

/**
 * iOS renderer via CoreGraphics' CGPDF API. Builds a premultiplied-RGBA bitmap
 * context, draws the page top-down, then packs the pixels into ARGB ints.
 *
 * NOTE: the CoreGraphics interop here should be verified on a device/simulator
 * build — it cannot be compiled in the authoring environment.
 */
@OptIn(ExperimentalForeignApi::class)
private class CoreGraphicsPdfDocument(private val doc: CGPDFDocumentRef) : PdfDocument {

    override val pageCount: Int = CGPDFDocumentGetNumberOfPages(doc).toInt()

    private fun pageRect(index: Int) =
        CGPDFDocumentGetPage(doc, (index + 1).toULong())
            ?: error("no PDF page at index $index")

    override fun pageAspectRatio(index: Int): Float {
        val page = pageRect(index)
        return CGPDFPageGetBoxRect(page, CGPDFBox.kCGPDFCropBox).useContents {
            (size.width / size.height).toFloat()
        }
    }

    override fun renderPage(index: Int, targetWidthPx: Int): PdfRaster {
        val page = pageRect(index)
        val box = CGPDFPageGetBoxRect(page, CGPDFBox.kCGPDFCropBox)
        val (boxWidth, boxHeight) = box.useContents { size.width to size.height }
        val scale = targetWidthPx / boxWidth
        val height = (boxHeight * scale).roundToInt().coerceAtLeast(1)
        val width = targetWidthPx

        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val context = CGBitmapContextCreate(
            data = null,
            width = width.toULong(),
            height = height.toULong(),
            bitsPerComponent = 8u,
            bytesPerRow = (width * 4).toULong(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )

        // Opaque white background, then flip to a top-down raster and scale to fit.
        CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
        CGContextFillRect(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
        CGContextTranslateCTM(context, 0.0, height.toDouble())
        CGContextScaleCTM(context, scale.toDouble(), -scale.toDouble())
        CGContextDrawPDFPage(context, page)

        val pixels = IntArray(width * height)
        val raw = CGBitmapContextGetData(context)?.reinterpret<UByteVar>()
        if (raw != null) {
            for (i in 0 until width * height) {
                val r = raw[i * 4].toInt() and 0xFF
                val g = raw[i * 4 + 1].toInt() and 0xFF
                val b = raw[i * 4 + 2].toInt() and 0xFF
                val a = raw[i * 4 + 3].toInt() and 0xFF
                pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
        return PdfRaster(width, height, pixels)
    }

    override fun close() {
        CGPDFDocumentRelease(doc)
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun openPdfDocument(bytes: ByteArray): PdfDocument {
    val cfData = bytes.usePinned { pinned ->
        CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
    }
    val provider = CGDataProviderCreateWithCFData(cfData)
    val doc = CGPDFDocumentCreateWithProvider(provider) ?: error("could not open PDF")
    CGDataProviderRelease(provider)
    cfData?.let { CFRelease(it) }
    return CoreGraphicsPdfDocument(doc)
}
