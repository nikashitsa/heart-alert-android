package com.nikashitsa.polar_alert_android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nikashitsa.polar_alert_android.R
import com.nikashitsa.polar_alert_android.ui.components.AppButton
import com.nikashitsa.polar_alert_android.ui.theme.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.lib.DeviceConnectionState
import com.nikashitsa.polar_alert_android.ui.components.DevicePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    bluetooth: BluetoothViewModel = hiltViewModel(),
    onNext: () -> Unit = {}
) {
    val deviceConnectionState = bluetooth.deviceConnectionState.collectAsState()

    ConnectScreenContent(
        deviceConnectionState = deviceConnectionState.value,
        onNext = onNext,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreenContent(
    deviceConnectionState: DeviceConnectionState = DeviceConnectionState.Disconnected(),
    onNext: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var opacity by rememberSaveable { mutableFloatStateOf(1f) }
    val animatedOpacity by animateFloatAsState(
        targetValue = opacity,
        animationSpec = tween(durationMillis = 400),
        label = "OpacityAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Black)
            .padding(16.dp)
            .alpha(animatedOpacity),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = "Heart",
            modifier = Modifier.padding(bottom = 20.dp)
        )
        Text(
            text = "Heart Alert",
            style = Fonts.textXlBold,
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton("Connect") {
            showPicker = true
            opacity = 0f
        }
    }

    LaunchedEffect(deviceConnectionState) {
        if (deviceConnectionState is DeviceConnectionState.Connected) {
            onNext()
        }
    }
    if (showPicker) {
        DevicePicker(sheetState) {
            showPicker = false
            opacity = 1f
        }
    }
}

@Preview
@Composable
fun ConnectScreenPreview() {
    HeartAlertTheme {
        ConnectScreenContent()
    }
}
