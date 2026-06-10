package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable

/*
 * QR scanning seam. The desktop shows a `beam://connect?...` QR; a remote may
 * scan it instead of typing. Manual entry always works as the fallback.
 *
 * [rememberQrScanLauncher] returns a function that opens the platform scanner
 * and calls [onResult] with the decoded text, or null when the platform has no
 * scanner wired up (iOS/desktop for now), so the UI can hide the scan button.
 */
@Composable
expect fun rememberQrScanLauncher(onResult: (String) -> Unit): (() -> Unit)?
