package com.jacksonfdam.beam.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// TODO: implement with CameraX + ML Kit Barcode. Manual entry is the fallback.
actual fun isQrScanSupported(): Boolean = false

@Composable
actual fun QrScannerSurface(modifier: Modifier, onScanned: (String) -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("Enter the host details manually for now.")
    }
}
