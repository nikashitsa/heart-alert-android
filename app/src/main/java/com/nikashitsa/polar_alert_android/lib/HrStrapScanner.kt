package com.nikashitsa.polar_alert_android.lib

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.polar.sdk.api.model.PolarDeviceInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Finds heart rate straps of any brand.
 *
 * Polar BLE SDK 7+ `searchForDevice()` drops every device without a Polar device id, even with
 * `setPolarFilter(false)`, so generic straps (Garmin, Wahoo, CooSpo…) never show up. The SDK still
 * connects to them by address, so only discovery is done here: a plain BLE scan for devices
 * advertising the standard Heart Rate service, plus Polar's manufacturer data (as the SDK does).
 */
class HrStrapScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // Permissions are requested up front in MainActivity; a denied scan fails via onScanFailed.
    @SuppressLint("MissingPermission")
    fun scan(): Flow<PolarDeviceInfo> = callbackFlow {
        val scanner = context.getSystemService(BluetoothManager::class.java)
            ?.adapter
            ?.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth LE scanner unavailable")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName ?: return
                if (name.isBlank()) return
                trySend(
                    PolarDeviceInfo(
                        deviceId = "",
                        address = result.device.address,
                        rssi = result.rssi,
                        name = name,
                        isConnectable = result.isConnectable,
                    )
                )
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: $errorCode"))
            }
        }

        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(HR_SERVICE).build(),
            ScanFilter.Builder().setManufacturerData(POLAR_MANUFACTURER_ID, byteArrayOf()).build(),
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(filters, settings, callback)
        awaitClose {
            try {
                scanner.stopScan(callback)
            } catch (_: IllegalStateException) {
                // Bluetooth was turned off mid-scan; the scan is already gone.
            }
        }
    }

    private companion object {
        val HR_SERVICE: ParcelUuid = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        const val POLAR_MANUFACTURER_ID = 0x006B
    }
}
