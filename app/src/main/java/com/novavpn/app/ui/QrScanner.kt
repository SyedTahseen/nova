package com.novavpn.app.ui

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Opens Google's code scanner (part of Play services: no camera permission needed in the app).
 * [onResult] gets the raw text of the scanned QR code.
 */
fun scanQrCode(context: Context, onResult: (String) -> Unit, onError: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()
    GmsBarcodeScanning.getClient(context, options)
        .startScan()
        .addOnSuccessListener { code -> code.rawValue?.let(onResult) ?: onError("The QR code was empty") }
        .addOnFailureListener { onError("QR scanner isn't available on this device (needs Google Play services)") }
    // Cancelling the scanner just closes it; nothing to do.
}
