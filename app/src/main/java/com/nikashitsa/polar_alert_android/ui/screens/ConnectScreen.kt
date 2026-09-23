package com.nikashitsa.polar_alert_android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
    val demoEnabled by bluetooth.demoEnabled.collectAsState()

    ConnectScreenContent(
        deviceConnectionState = deviceConnectionState.value,
        demoEnabled = demoEnabled,
        onDemo = bluetooth::enableDemo,
        onNext = onNext,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreenContent(
    deviceConnectionState: DeviceConnectionState = DeviceConnectionState.Disconnected(),
    demoEnabled: Boolean = false,
    onDemo: () -> Unit = {},
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
            contentDescription = stringResource(R.string.heart),
            modifier = Modifier
                .padding(bottom = 20.dp)
                .demoTrigger(onDemo)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = Fonts.textXlBold,
        )
        // Always laid out, only hidden, so the logo and title don't shift when it appears.
        Text(
            text = stringResource(R.string.demo_mode),
            modifier = Modifier
                .padding(top = 8.dp)
                .then(if (demoEnabled) Modifier else Modifier.alpha(0f).clearAndSetSemantics {}),
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton(stringResource(R.string.connect)) {
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

/**
 * Hidden entry to demo mode: [DEMO_TAPS] taps in a row, each within [DEMO_TAP_GAP_MS] of the
 * last. No ripple, so the logo doesn't look tappable.
 */
@Composable
private fun Modifier.demoTrigger(onDemo: () -> Unit): Modifier {
    var taps by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
    ) {
        val now = System.currentTimeMillis()
        taps = if (now - lastTapAt <= DEMO_TAP_GAP_MS) taps + 1 else 1
        lastTapAt = now
        if (taps >= DEMO_TAPS) {
            taps = 0
            onDemo()
        }
    }
}

private const val DEMO_TAPS = 5
private const val DEMO_TAP_GAP_MS = 1000L

@Preview
@Composable
fun ConnectScreenPreview() {
    HeartAlertTheme {
        ConnectScreenContent()
    }
}

@Preview
@Composable
fun ConnectScreenDemoPreview() {
    HeartAlertTheme {
        ConnectScreenContent(demoEnabled = true)
    }
}
