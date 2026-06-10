package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable

// TODO: implement with AVCaptureMetadataOutput. Manual entry is the fallback.
@Composable
actual fun rememberQrScanLauncher(onResult: (String) -> Unit): (() -> Unit)? = null
