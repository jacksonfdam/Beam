package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable

// The JVM target of :app:shared is not a camera host; manual entry only.
@Composable
actual fun rememberQrScanLauncher(onResult: (String) -> Unit): (() -> Unit)? = null
