package com.jacksonfdam.beam.remote

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.darwin.dispatch_get_main_queue

/**
 * iOS QR scanner via AVFoundation. Presents a full-screen camera that dismisses
 * on the first QR code and returns its text. Requires NSCameraUsageDescription
 * in the app's Info.plist.
 *
 * NOTE: AVFoundation interop must be verified on a device/simulator build — it
 * cannot be compiled in the authoring environment.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberQrScanLauncher(onResult: (String) -> Unit): (() -> Unit)? = {
    val controller = QrScannerController(onResult)
    rootViewController()?.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun rootViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow?.rootViewController

@OptIn(ExperimentalForeignApi::class)
private class QrScannerController(
    private val onResult: (String) -> Unit,
) : UIViewController(nibName = null, bundle = null), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return
        if (session.canAddInput(input)) session.addInput(input)

        val output = AVCaptureMetadataOutput()
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(this, dispatch_get_main_queue())
            output.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
        }

        val layer = AVCaptureVideoPreviewLayer(session = session)
        layer.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(layer)
        previewLayer = layer
        session.startRunning()
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        previewLayer?.setFrame(view.bounds)
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val code = didOutputMetadataObjects.firstOrNull() as? AVMetadataMachineReadableCodeObject
        val value = code?.stringValue
        if (value != null) {
            session.stopRunning()
            dismissViewControllerAnimated(true, null)
            onResult(value)
        }
    }
}
