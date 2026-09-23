package com.nikashitsa.polar_alert_android.lib

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polar.androidcommunications.api.ble.model.DisInfo
import com.polar.sdk.api.PolarBleApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
    private var demoHrJob: Job? = null

    /** Unlocked from the Connect screen; adds the simulated strap to the device list. */
    private val _demoEnabled = MutableStateFlow(false)
    val demoEnabled = _demoEnabled.asStateFlow()

    /** A simulated strap is connected, so the app can be used without real hardware. */
    val isDemo: Boolean
        get() = (_deviceConnectionState.value as? DeviceConnectionState.Connected)?.address == DEMO_ADDRESS

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
        // Demo mode offers the demo strap alone, so there is nothing to scan for.
        if (_demoEnabled.value) {
            _foundDevices.value = listOf(DEMO_DEVICE)
            return
        }
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
                if (_demoEnabled.value && device.address != DEMO_ADDRESS) {
                    Log.w(tag, "Only the demo strap can connect in demo mode")
                    return@launch
                }

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

                if (device.address == DEMO_ADDRESS) {
                    startDemo()
                    onComplete()
                    return@launch
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
        if (address == DEMO_ADDRESS) {
            demoHrStreamStart(onNext)
            return
        }
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
        demoHrJob?.cancel()
    }

    /** Lasts until the process ends; from then on the demo strap is the only device offered. */
    fun enableDemo() {
        Log.d(tag, "enableDemo")
        _demoEnabled.value = true
        scanDisposable?.dispose()
        if (_foundDevices.value.isNotEmpty()) _foundDevices.value = listOf(DEMO_DEVICE)
    }

    /** Pretends a strap with HR and battery support has connected. */
    private fun startDemo() {
        Log.d(tag, "startDemo")
        _deviceName.value = DEMO_DEVICE_NAME
        _hrFeature.value = HrFeature(true)
        _batteryStatusFeature.value = BatteryStatusFeature(true, DEMO_BATTERY_LEVEL)
        _deviceConnectionState.value = DeviceConnectionState.Connected(DEMO_ADDRESS)
    }

    /**
     * One sample a second, like a real strap: a slow wave through 90..160 BPM with a little
     * jitter. With the default range a session starts good, then goes too high and too low.
     */
    private fun demoHrStreamStart(onNext: (Int) -> Unit) {
        if (demoHrJob?.isActive == true) return
        demoHrJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            while (true) {
                val t = (System.currentTimeMillis() - startedAt) / DEMO_HR_PERIOD_MS.toDouble()
                val hr = 125 + 35 * sin(2 * PI * t) + Random.nextInt(-2, 3)
                onNext(hr.roundToInt())
                delay(1000)
            }
        }
    }

    companion object {
        const val DEMO_ADDRESS = "demo"
        const val DEMO_DEVICE_NAME = "Heart Alert Demo"
        private const val DEMO_BATTERY_LEVEL = 80
        // Lowest possible signal, so real straps sort above it.
        private val DEMO_DEVICE = PolarDeviceInfo("", DEMO_ADDRESS, Int.MIN_VALUE, DEMO_DEVICE_NAME, true)
        private const val DEMO_HR_PERIOD_MS = 120_000L
    }
}