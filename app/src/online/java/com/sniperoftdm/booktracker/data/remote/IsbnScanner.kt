package com.sniperoftdm.booktracker.data.remote

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Helper to launch Google Code Scanner for EAN-13 and EAN-8 barcode recognition without camera permissions.
 */
object IsbnScanner {

    /** Returns true if Google Play services are available on device for barcode scanning. */
    fun isAvailable(context: Context): Boolean {
        return try {
            val availability = GoogleApiAvailability.getInstance()
            availability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
        } catch (e: Throwable) {
            false
        }
    }

    /** Launch Google Code Scanner overlay dialog to scan an ISBN barcode. */
    fun startScan(
        context: Context,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                .build()

            val scanner = GmsBarcodeScanning.getClient(context, options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank()) {
                        onSuccess(raw)
                    }
                }
                .addOnFailureListener { exception ->
                    onFailure(exception)
                }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
