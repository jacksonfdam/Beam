package com.jacksonfdam.beam.remote

import androidx.compose.ui.graphics.ImageBitmap

/** Decode encoded image bytes (PNG/JPEG) into a Compose [ImageBitmap], or null. */
expect fun decodeImageBytes(bytes: ByteArray): ImageBitmap?
