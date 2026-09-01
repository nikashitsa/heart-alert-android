package com.nikashitsa.polar_alert_android.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikashitsa.polar_alert_android.ui.components.AppButton
import com.nikashitsa.polar_alert_android.ui.components.AppTextButton
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.Fonts
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikashitsa.polar_alert_android.lib.BatteryStatusFeature
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.lib.SettingsDefaults
import com.nikashitsa.polar_alert_android.lib.SettingsOptions
import com.nikashitsa.polar_alert_android.lib.SettingsViewModel
import com.nikashitsa.polar_alert_android.lib.SoundType
import com.nikashitsa.polar_alert_android.lib.SoundViewModel
import com.nikashitsa.polar_alert_android.ui.components.Accordion
import com.nikashitsa.polar_alert_android.ui.components.AppSlider
import com.nikashitsa.polar_alert_android.ui.components.AppSwitch
import com.nikashitsa.polar_alert_android.ui.components.DevicePicker
import com.nikashitsa.polar_alert_android.ui.components.DropdownMenuSelector
import com.nikashitsa.polar_alert_android.ui.components.Paywall

@Composable
fun SettingsScreen(
    bluetooth: BluetoothViewModel = hiltViewModel(),
    settings: SettingsViewModel = hiltViewModel(),
    sound: SoundViewModel = hiltViewModel(),
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val deviceName = bluetooth.deviceName.collectAsState()
    val batteryStatusFeature = bluetooth.batteryStatusFeature.collectAsState()
    val volume by settings.volume.collectAsState()
    val vibrate by settings.vibrate.collectAsState()
    val hrMin by settings.hrMin.collectAsState()
    val hrMax by settings.hrMax.collectAsState()
    val alertInterval by settings.alertInterval.collectAsState()
    val outOfRangeFor by settings.outOfRangeFor.collectAsState()
    val initialDelay by settings.initialDelay.collectAsState()
    val hasAccess by settings.hasAccess.collectAsState()
    val freeSessionsLeft by settings.freeSessionsLeft.collectAsState()

    BackHandler {
        onBack()
    }

    SettingsScreenContent(
        deviceName = deviceName.value,
        batteryStatusFeature = batteryStatusFeature.value,
        volume = volume,
        setVolume = settings::setVolume,
        vibrate = vibrate,
        setVibrate = settings::setVibrate,
        alertInterval = alertInterval,
        setAlertInterval = settings::setAlertInterval,
        outOfRangeFor = outOfRangeFor,
        setOutOfRangeFor = settings::setOutOfRangeFor,
        initialDelay = initialDelay,
        setInitialDelay = settings::setInitialDelay,
        hrMin = hrMin,
        setHrMin = settings::setHrMin,
        hrMax = hrMax,
        setHrMax = settings::setHrMax,
        playSound = sound::play,
        hasAccess = hasAccess,
        freeSessionsLeft = freeSessionsLeft,
        onNext = onNext,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    deviceName: String = "HRM BELT",
    batteryStatusFeature: BatteryStatusFeature = BatteryStatusFeature(true),
    volume: Int = SettingsDefaults.VOLUME,
    setVolume: (Int) -> Unit = {},
    vibrate: Boolean = SettingsDefaults.VIBRATE,
    setVibrate: (Boolean) -> Unit = {},
    alertInterval: Int = SettingsDefaults.ALERT_INTERVAL,
    setAlertInterval: (Int) -> Unit = {},
    outOfRangeFor: Int = SettingsDefaults.OUT_OF_RANGE_FOR,
    setOutOfRangeFor: (Int) -> Unit = {},
    initialDelay: Int = SettingsDefaults.INITIAL_DELAY,
    setInitialDelay: (Int) -> Unit = {},
    hrMin: Int = SettingsDefaults.HR_MIN,
    setHrMin: (Int) -> Unit = {},
    hrMax: Int = SettingsDefaults.HR_MAX,
    setHrMax: (Int) -> Unit = {},
    playSound: (SoundType) -> Unit = {},
    hasAccess: Boolean = true,
    freeSessionsLeft: Int = 0,
    onNext: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    val paywallSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Read out here rather than inside the sheet: a ModalBottomSheet renders in its own
    // window, where LocalContext is not necessarily the Activity that billing needs.
    val activity = LocalContext.current as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Black)
            .padding(16.dp, 40.dp, 16.dp, 16.dp)
            .verticalScroll(scrollState),
    ) {
        Text(text = "Settings", style = Fonts.textXlBold)

        Spacer(modifier = Modifier.height(40.dp))

        SettingSection(title = "Heart rate") {
            SettingRow(label = "Min") {
                DropdownMenuSelector(
                    hrMin,
                    label = { "$it BPM" },
                    options = 30..hrMax,
                    setValue = { it -> setHrMin(it) }
                )
            }
            SettingRow(label = "Max") {
                DropdownMenuSelector(
                    hrMax,
                    label = { "$it BPM" },
                    options = hrMin..240,
                    setValue = { it -> setHrMax(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        SettingSection(title = "Alert") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                    contentDescription = "Volume down",
                )
                AppSlider(
                    value = volume,
                    onValueChange = {
                        setVolume(it)
                        playSound(SoundType.LOW_BEEP)
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume up",
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Accordion(label = "Advanced") {
                SettingRow(label = "Vibration") {
                    AppSwitch(vibrate) {
                        setVibrate(it)
                    }
                }
                SettingRow(label = "Interval") {
                    DropdownMenuSelector(
                        alertInterval,
                        options = SettingsOptions.ALERT_INTERVAL,
                        label = { "$it sec" },
                        setValue = { it -> setAlertInterval(it) }
                    )
                }
                SettingRow(label = "Out of range for") {
                    DropdownMenuSelector(
                        outOfRangeFor,
                        options = SettingsOptions.OUT_OF_RANGE_FOR,
                        label = { formatDuration(it) },
                        itemLabel = { formatDuration(it) },
                        setValue = { it -> setOutOfRangeFor(it) }
                    )
                }
                SettingRow(label = "Initial delay") {
                    DropdownMenuSelector(
                        initialDelay,
                        options = SettingsOptions.INITIAL_DELAY,
                        label = { formatInitialDelay(it) },
                        itemLabel = { formatInitialDelay(it) },
                        setValue = { it -> setInitialDelay(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        SettingSection(title = "Connection") {
            SettingRow(label = "Device") {
                AppTextButton(onClick = {
                    showPicker = true
                }) {
                    Text(text = deviceName)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Chevron")
                }
            }
            if (batteryStatusFeature.isSupported) {
                SettingRow(label = "Battery") { Text("${batteryStatusFeature.batteryLevel}%") }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Spacer(modifier = Modifier.weight(1f))

        // The count is only advertised while there are free sessions to advertise: an entitled
        // user, or one who has used them all, just gets "Start".
        val startLabel = if (freeSessionsLeft > 0) "Start for free ($freeSessionsLeft)" else "Start"
        AppButton(startLabel) { if (hasAccess) onNext() else showPaywall = true }

        if (showPicker) {
            DevicePicker(sheetState) {
                showPicker = false
            }
        }

        if (showPaywall) {
            Paywall(
                sheetState = paywallSheetState,
                activity = activity,
                onDismissRequest = { showPaywall = false },
                onContinue = {
                    showPaywall = false
                    onNext()
                },
            )
        }
    }
}

private fun formatDuration(seconds: Int): String =
    if (seconds < 60) "$seconds sec" else "${seconds / 60} min"

private fun formatInitialDelay(seconds: Int): String = when (seconds) {
    0 -> "off"
    SettingsOptions.UNTIL_IN_RANGE -> "until in range"
    else -> formatDuration(seconds)
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(text = title, style = Fonts.textLgBold)
        Column(content = content)
    }
}

@Composable
fun SettingRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    HeartAlertTheme {
        SettingsScreenContent()
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenFreeSessionsPreview() {
    HeartAlertTheme {
        SettingsScreenContent(freeSessionsLeft = 5)
    }
}
