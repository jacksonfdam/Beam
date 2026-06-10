package com.jacksonfdam.beam.presenter

import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

/**
 * Captures the host's screen to a downscaled JPEG (Base64) for SCREEN mode, so
 * the remote can see the live demo. On macOS this needs Screen Recording
 * permission for the running process.
 */
object ScreenCapture {
    private val robot: Robot? by lazy { runCatching { Robot() }.getOrNull() }

    fun capture(bounds: Rectangle, targetWidth: Int = 1280): String? = runCatching {
        val r = robot ?: return null
        val shot = r.createScreenCapture(bounds)
        val w = targetWidth.coerceAtMost(shot.width).coerceAtLeast(1)
        val h = (shot.height.toDouble() * w / shot.width).toInt().coerceAtLeast(1)
        val scaled = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.drawImage(shot, 0, 0, w, h, null)
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(scaled, "jpg", out)
        Base64.getEncoder().encodeToString(out.toByteArray())
    }.getOrNull()
}
