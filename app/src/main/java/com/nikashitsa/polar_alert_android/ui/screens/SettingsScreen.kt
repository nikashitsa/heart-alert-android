package com.nikashitsa.polar_alert_android.ui.screens

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikashitsa.polar_alert_android.lib.BatteryStatusFeature
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.lib.SettingsDefaults
import com.nikashitsa.polar_alert_android.lib.SettingsViewModel
import com.nikashitsa.polar_alert_android.lib.SoundType
import com.nikashitsa.polar_alert_android.lib.SoundViewModel
import com.nikashitsa.polar_alert_android.ui.components.AppSlider
import com.nikashitsa.polar_alert_android.ui.components.AppSwitch
import com.nikashitsa.polar_alert_android.ui.components.DevicePicker
import com.nikashitsa.polar_alert_android.ui.components.DropdownMenuSelector

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
        hrMin = hrMin,
        setHrMin = settings::setHrMin,
        hrMax = hrMax,
        setHrMax = settings::setHrMax,
        playSound = sound::play,
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
    hrMin: Int = SettingsDefaults.HR_MIN,
    setHrMin: (Int) -> Unit = {},
    hrMax: Int = SettingsDefaults.HR_MAX,
    setHrMax: (Int) -> Unit = {},
    playSound: (SoundType) -> Unit = {},
    onNext: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                    progression = 40..hrMax,
                    setValue = { it -> setHrMin(it) }
                )
            }
            SettingRow(label = "Max") {
                DropdownMenuSelector(
                    hrMax,
                    progression = hrMin..240,
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
            SettingRow(label = "Vibration") {
                AppSwitch(vibrate) {
                    setVibrate(it)
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

        AppButton("Start") { onNext() }

        if (showPicker) {
            DevicePicker(sheetState) {
                showPicker = false
            }
        }
    }
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
