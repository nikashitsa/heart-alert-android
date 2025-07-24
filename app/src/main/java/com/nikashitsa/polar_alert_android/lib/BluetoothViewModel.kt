package com.nikashitsa.polar_alert_android.lib

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHealthThermometerData
import com.polar.sdk.api.model.PolarHrData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val api: PolarBleApi
): ViewModel() {

    private val tag = "BluetoothViewModel"
    private val _isBluetoothOn = MutableStateFlow(false)
    val isBluetoothOn = _isBluetoothOn.asStateFlow()

    private val _foundDevices = MutableStateFlow<List<PolarDeviceInfo>>(emptyList())
    val foundDevices = _foundDevices.asStateFlow()

    private val _deviceConnectionState = MutableStateFlow<DeviceConnectionState>(DeviceConnectionState.Disconnected())
    val deviceConnectionState = _deviceConnectionState.asStateFlow()

    private val _deviceName = MutableStateFlow("")
    val deviceName = _deviceName.asStateFlow()

    private val _batteryStatusFeature = MutableStateFlow(BatteryStatusFeature())
    val batteryStatusFeature = _batteryStatusFeature.asStateFlow()

    private val _hrFeature = MutableStateFlow(HrFeature())
    val hrFeature = _hrFeature.asStateFlow()

    private var scanDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null

    init {
        api.setPolarFilter(false)
        api.setApiCallback(object: PolarBleApiCallback() {

            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(tag, "BLE power: $powered")
                _isBluetoothOn.value = powered
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "CONNECTED: ${polarDeviceInfo.address}")
                _deviceConnectionState.value = DeviceConnectionState.Connected(polarDeviceInfo.address)
                _deviceName.value = polarDeviceInfo.name
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "CONNECTING: ${polarDeviceInfo.address}")
                _deviceConnectionState.value = DeviceConnectionState.Connecting(polarDeviceInfo.address)
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(tag, "DISCONNECTED: ${polarDeviceInfo.address}")
                _deviceConnectionState.value = DeviceConnectionState.Disconnected(polarDeviceInfo.address)
                _hrFeature.value = HrFeature()
                _batteryStatusFeature.value = BatteryStatusFeature()
            }

            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
                Log.d(tag, "Polar BLE SDK feature $feature is ready")
                when (feature) {
                    PolarBleApi.PolarBleSdkFeature.FEATURE_HR -> {
                        _hrFeature.value = HrFeature(true)
                    }
                    PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO -> {
                        _batteryStatusFeature.value = BatteryStatusFeature(true)
                    }
                    else -> {}
                }
            }

            override fun disInformationReceived(identifier: String, disInfo: DisInfo) {}

            override fun htsNotificationReceived(identifier: String, data: PolarHealthThermometerData) {}

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(tag, "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(tag, "BATTERY LEVEL: $level")
                _batteryStatusFeature.value = BatteryStatusFeature(true, level)
            }
        })
    }

    fun searchForDevice() {
        Log.d(tag, "searchForDevice")
        val state = _deviceConnectionState.value
        if (state is DeviceConnectionState.Connected) {
            _foundDevices.value = listOf(
                PolarDeviceInfo("", state.address, 0,_deviceName.value, true,)
            )
        }
        scanDisposable = api.searchForDevice()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { polarDeviceInfo: PolarDeviceInfo ->
                    Log.d(tag, "found ${polarDeviceInfo.name} ${polarDeviceInfo.address}")
                    val currentList = _foundDevices.value
                    if (currentList.none { it.name == polarDeviceInfo.name }) {
                        _foundDevices.value = (currentList + polarDeviceInfo).sortedByDescending { it.rssi }
                    }
                },
                { error: Throwable ->
                    Log.e(tag, "Device scan failed. Reason $error")
                },
                {
                    Log.d(tag, "complete")
                }
            )
    }

    fun stopDevicesSearch() {
        Log.d(tag, "stopDevicesSearch")
        scanDisposable?.dispose()
        _foundDevices.value = emptyList()
    }

    fun connectToDevice(device: PolarDeviceInfo, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(tag, "connectToDevice")
                val state = _deviceConnectionState.value
                if (state is DeviceConnectionState.Connected && state.address == device.address) {
                    onComplete()
                    return@launch
                }

                if (state is DeviceConnectionState.Connected) {
                    api.disconnectFromDevice(state.address)
                    _deviceConnectionState
                        .filterIsInstance<DeviceConnectionState.Disconnected>()
                        .filter { it.address == state.address }
                        .first()
                }

                api.connectToDevice(device.address)

                _deviceConnectionState
                    .filterIsInstance<DeviceConnectionState.Connected>()
                    .filter { it.address == device.address }
                    .first()
                onComplete()
            } catch (polarInvalidArgument: PolarInvalidArgument) {
                Log.e(tag, "Failed to connect. Reason $polarInvalidArgument ")
            }
        }
    }

    fun hrStreamStart(address: String, onNext: (Int) -> Unit) {
        if (hrDisposable?.isDisposed == false) return
        hrDisposable = api.startHrStreaming(address)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { hrData: PolarHrData ->
                    for (sample in hrData.samples) {
                        onNext(sample.hr)
                    }
                },
                { error: Throwable ->
                    Log.e(tag, "HR stream failed. Reason $error")
                },
                { Log.d(tag, "HR stream complete") }
            )
    }

    fun hrStreamStop() {
        hrDisposable?.dispose()
    }
}