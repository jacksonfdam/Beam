package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * QR scanning seam. The desktop shows a `beam://connect?...` QR; a remote may
 * scan it as a convenience. Manual entry ALWAYS works and is the fallback, so
 * the camera actuals can be filled in per platform later (Android CameraX +
 * ML Kit Barcode; iOS AVCaptureMetadataOutput) without touching the UI.
 */

/** Whether this platform currently provides camera-based QR scanning. */
expect fun isQrScanSupported(): Boolean

/** Camera scanner surface; only shown when [isQrScanSupported]. Emits the decoded text. */
@Composable
expect fun QrScannerSurface(modifier: Modifier, onScanned: (String) -> Unit)
