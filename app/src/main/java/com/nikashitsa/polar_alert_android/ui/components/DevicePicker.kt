package com.nikashitsa.polar_alert_android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.nikashitsa.polar_alert_android.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.Fonts
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import com.polar.sdk.api.model.PolarDeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

enum class DevicePickerState {
    Searching,
    Connecting,
    NotFound
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePicker(
    sheetState: SheetState,
    bluetooth: BluetoothViewModel = hiltViewModel(),
    onDismissRequest: () -> Unit = {},
) {
    val isBluetoothOn = bluetooth.isBluetoothOn.collectAsState()
    val foundDevices = bluetooth.foundDevices.collectAsState()
    val demoEnabled = bluetooth.demoEnabled.collectAsState()

    DevicePickerContent(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        // The demo strap needs no Bluetooth, so don't block the list on it.
        isBluetoothOn = isBluetoothOn.value || demoEnabled.value,
        foundDevices = foundDevices.value,
        searchForDevice = bluetooth::searchForDevice,
        stopDevicesSearch = bluetooth::stopDevicesSearch,
        connectToDevice = bluetooth::connectToDevice,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePickerContent(
    sheetState: SheetState,
    onDismissRequest: () -> Unit = {},
    isBluetoothOn: Boolean,
    foundDevices: List<PolarDeviceInfo>,
    searchForDevice: () -> Unit = {},
    stopDevicesSearch: () -> Unit = {},
    connectToDevice: (PolarDeviceInfo, () -> Unit) -> Unit = { _, _ -> },
    initialState: DevicePickerState = DevicePickerState.Searching,
) {
    var state by rememberSaveable { mutableStateOf(initialState) }
    var timeoutTask by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Colors.Gray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isBluetoothOn) {
                when (state) {
                    DevicePickerState.Searching -> {
                        if (foundDevices.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.choose_device),
                                style = Fonts.textLgBold
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 20.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                foundDevices.forEach { device ->
                                    DeviceListItem(device.name) {
                                        state = DevicePickerState.Connecting
                                        connectToDevice(device) {
                                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                                if (!sheetState.isVisible) {
                                                    onDismissRequest()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(R.string.searching_for_devices),
                                style = Fonts.textLgBold
                            )
                            AppLoader(Modifier.weight(1f))
                            LaunchedEffect(Unit) {
                                searchForDevice()
                                timeoutTask = launch {
                                    try {
                                        delay(10_000L.milliseconds) // 10s
                                        if (foundDevices.isEmpty()) {
                                            stopDevicesSearch()
                                            state = DevicePickerState.NotFound
                                        }
                                    } catch (_: CancellationException) {}
                                }
                            }
                        }
                    }

                    DevicePickerState.Connecting -> {
                        Text(
                            text = stringResource(R.string.connecting),
                            style = Fonts.textLgBold,
                        )
                        AppLoader(Modifier.weight(1f))
                    }

                    DevicePickerState.NotFound -> {
                        Text(
                            text = stringResource(R.string.devices_not_found),
                            style = Fonts.textLgBold,
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.devices_not_found_hint),
                                style = Fonts.textMd,
                            )
                        }
                        AppButton(stringResource(R.string.try_again)) {
                            state = DevicePickerState.Searching
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.bluetooth_off),
                    style = Fonts.textLgBold,
                )
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.bluetooth_off_hint),
                        style = Fonts.textMd,
                    )
                }
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                stopDevicesSearch()
                timeoutTask?.cancel()
            }
        }
    }
}

@Composable
fun DeviceListItem(name: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        modifier = Modifier
            .clickable(
                onClick = onClick
            ),
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun DevicePickerContentPreview() {
    val expandedState = SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = LocalDensity.current,
    )
    HeartAlertTheme {
        DevicePickerContent(
            sheetState = expandedState,
            isBluetoothOn = true,
            foundDevices = emptyList(),
        )
    }
}