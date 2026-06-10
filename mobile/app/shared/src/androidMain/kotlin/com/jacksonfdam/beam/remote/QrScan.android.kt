package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Uses Google's on-device code scanner (Play Services): a full-screen scanner
 * UI with no camera permission and no CameraX wiring on our side. The scanner
 * module is fetched on first use automatically.
 */
@Composable
actual fun rememberQrScanLauncher(onResult: (String) -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    return {
        GmsBarcodeScanning.getClient(context).startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(onResult) }
            .addOnCanceledListener { }
            .addOnFailureListener { }
        Unit
    }
}
