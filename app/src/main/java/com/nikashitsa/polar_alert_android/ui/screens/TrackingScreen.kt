package com.nikashitsa.polar_alert_android.ui.screens

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.play.core.review.ReviewManagerFactory
import com.nikashitsa.polar_alert_android.R
import com.nikashitsa.polar_alert_android.lib.BluetoothViewModel
import com.nikashitsa.polar_alert_android.lib.DeviceConnectionState
import com.nikashitsa.polar_alert_android.lib.HrFeature
import com.nikashitsa.polar_alert_android.lib.SettingsDefaults
import com.nikashitsa.polar_alert_android.lib.SettingsViewModel
import com.nikashitsa.polar_alert_android.lib.SoundType
import com.nikashitsa.polar_alert_android.lib.SoundViewModel
import com.nikashitsa.polar_alert_android.lib.TrackingState
import com.nikashitsa.polar_alert_android.lib.VibrationType
import com.nikashitsa.polar_alert_android.lib.VibrationViewModel
import com.nikashitsa.polar_alert_android.ui.components.AppButton
import com.nikashitsa.polar_alert_android.ui.theme.Colors
import com.nikashitsa.polar_alert_android.ui.theme.Fonts
import com.nikashitsa.polar_alert_android.ui.theme.HeartAlertTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    bluetooth: BluetoothViewModel = hiltViewModel(),
    settings: SettingsViewModel = hiltViewModel(),
    sound: SoundViewModel = hiltViewModel(),
    vibration: VibrationViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val deviceConnectionState = bluetooth.deviceConnectionState.collectAsState()
    val hrFeature = bluetooth.hrFeature.collectAsState()
    val hrMin by settings.hrMin.collectAsState()
    val hrMax by settings.hrMax.collectAsState()
    val alertInterval by settings.alertInterval.collectAsState()
    val outOfRangeFor by settings.outOfRangeFor.collectAsState()

    BackHandler {
        bluetooth.hrStreamStop()
        onBack()
    }

    TrackingScreenContent(
        deviceConnectionState = deviceConnectionState.value,
        hrFeature = hrFeature.value,
        hrStreamStart = bluetooth::hrStreamStart,
        hrStreamStop = bluetooth::hrStreamStop,
        playSound = sound::play,
        hrMin = hrMin,
        hrMax = hrMax,
        alertInterval = alertInterval,
        outOfRangeFor = outOfRangeFor,
        vibrate = vibration::vibrate,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreenContent(
    deviceConnectionState: DeviceConnectionState,
    hrFeature: HrFeature = HrFeature(),
    hrStreamStart: (String, (Int) -> Unit) -> Unit = {_, _ ->},
    hrStreamStop: () -> Unit = {},
    playSound: (SoundType) -> Unit = {},
    hrMin: Int = SettingsDefaults.HR_MIN,
    hrMax: Int = SettingsDefaults.HR_MAX,
    alertInterval: Int = SettingsDefaults.ALERT_INTERVAL,
    outOfRangeFor: Int = SettingsDefaults.OUT_OF_RANGE_FOR,
    vibrate: (VibrationType) -> Unit = {},
    initialBpm: Int = -1,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Colors.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Range $hrMin-$hrMax BPM")
        }
        Spacer(modifier = Modifier.weight(1f))

        BpmView(
            deviceConnectionState = deviceConnectionState,
            hrFeature = hrFeature,
            hrStreamStart = hrStreamStart,
            playSound = playSound,
            hrMin = hrMin,
            hrMax = hrMax,
            alertInterval = alertInterval,
            outOfRangeFor = outOfRangeFor,
            vibrate = vibrate,
            initialBpm = initialBpm,
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton("Stop") {
            hrStreamStop()
            requestAppReview(context, activity)
            onBack()
        }
    }
}

private fun requestAppReview(context: Context, activity: Activity?) {
    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful && activity != null) {
            val reviewInfo = task.result
            manager.launchReviewFlow(activity, reviewInfo)
        }
    }
}

@Composable
fun BpmView(
    deviceConnectionState: DeviceConnectionState,
    hrFeature: HrFeature,
    hrStreamStart: (String, (Int) -> Unit) -> Unit = {_, _ ->},
    playSound: (SoundType) -> Unit = {},
    hrMin: Int = SettingsDefaults.HR_MIN,
    hrMax: Int = SettingsDefaults.HR_MAX,
    alertInterval: Int = SettingsDefaults.ALERT_INTERVAL,
    outOfRangeFor: Int = SettingsDefaults.OUT_OF_RANGE_FOR,
    vibrate: (VibrationType) -> Unit = {},
    initialBpm: Int = -1,
) {
    var state by rememberSaveable { mutableStateOf(TrackingState.GOOD) }
    var bpm by rememberSaveable { mutableIntStateOf(initialBpm) }
    var prevConnectionState by rememberSaveable(
        stateSaver = DeviceConnectionState.Saver
    ) { mutableStateOf(DeviceConnectionState.Connected()) }
    // timestamps in ms, null when they haven't happened yet
    var lastTriggerTime by rememberSaveable { mutableStateOf<Long?>(null) }
    // start of the current uninterrupted out-of-range stretch, null while in range
    var outOfRangeSince by rememberSaveable { mutableStateOf<Long?>(null) }
    // whether the stretch has already lasted long enough for alerts to start
    var alerting by rememberSaveable { mutableStateOf(false) }
    val outOfRangeForInterval = outOfRangeFor * 1000 // ms
    val throttleInterval = alertInterval * 1000 - 310 // ms

    when (val connectionState = deviceConnectionState) {
        is DeviceConnectionState.Disconnected -> {
            Text("Disconnected", style = Fonts.textLg)
            PlaySoundRepeatedly(playSound, SoundType.DISCONNECTED) {
                prevConnectionState = DeviceConnectionState.Disconnected()
            }
        }
        is DeviceConnectionState.Connecting -> {
            Text("Reconnecting...", style = Fonts.textLg)
            PlaySoundRepeatedly(playSound, SoundType.RECONNECTING) {
                prevConnectionState = DeviceConnectionState.Disconnected()
            }
        }
        is DeviceConnectionState.Connected -> {
            if (hrFeature.isSupported) {
                val bpmColor = if (alerting) Colors.Red else Colors.White
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier
                            .height(80.dp),
                    ) {
                        val bpmLabel = if (bpm > -1) "$bpm" else "--"
                        Text(
                            text = bpmLabel,
                            style = Fonts.text2XlBold,
                            overflow = TextOverflow.Visible,
                            modifier = Modifier.offset(y = (-12).dp),
                            color = bpmColor,
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            HeartIcon(state)
                            Text(
                                text = "BPM",
                                style = Fonts.textLg,
                                color = bpmColor,
                            )
                            LaunchedEffect(Unit) {
                                if (prevConnectionState is DeviceConnectionState.Disconnected) {
                                    playSound(SoundType.CONNECTED)
                                }

                                hrStreamStart(connectionState.address) { hr ->
                                    bpm = hr
                                    val prevState = state
                                    state = TrackingState.of(hr, hrMin, hrMax)
                                    val now = System.currentTimeMillis()

                                    // back in range: forget the stretch, announce recovery
                                    // only if we actually raised an alert during it
                                    if (state == TrackingState.GOOD) {
                                        outOfRangeSince = null
                                        lastTriggerTime = null
                                        if (alerting) {
                                            alerting = false
                                            playSound(state.soundState)
                                        }
                                        return@hrStreamStart
                                    }

                                    // out of range: stay quiet until the stretch is long enough
                                    val since = outOfRangeSince ?: now
                                    outOfRangeSince = since
                                    if (now - since < outOfRangeForInterval) return@hrStreamStart

                                    // speak up when alerts begin, and again on a low <-> high flip
                                    if (!alerting || state != prevState) {
                                        alerting = true
                                        playSound(state.soundState)
                                    }

                                    // repeat the beep/vibration at the chosen interval
                                    val lastTrigger = lastTriggerTime
                                    if (lastTrigger == null || now - lastTrigger > throttleInterval) {
                                        lastTriggerTime = now
                                        state.sound?.let(playSound)
                                        state.vibration?.let(vibrate)
                                    }
                                }
                            }
                        }
                    }
                    // out of range, but not alerting yet: count down the sustained period
                    val waitingSince = outOfRangeSince.takeIf { !alerting && outOfRangeForInterval > 0 }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(30.dp),
                    ) {
                        when {
                            alerting -> Text(
                                text = state.heartBeatDescription,
                                style = Fonts.textLg,
                            )
                            waitingSince != null -> AlertCountdown(
                                since = waitingSince,
                                duration = outOfRangeForInterval,
                            )
                        }
                    }
                }
            } else {
                Text("Reconnecting...", style = Fonts.textLg)
            }
        }
    }

}

@Composable
fun PlaySoundRepeatedly(playSound: (SoundType) -> Unit = {}, soundType: SoundType, onStart: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        onStart()
        while (true) {
            playSound(soundType)
            delay(5000.milliseconds)
        }
    }
}

/**
 * Fills up as the current out-of-range stretch approaches [duration] ms, at which
 * point alerts start. [since] is the start of the stretch, in epoch ms.
 */
@Composable
fun AlertCountdown(since: Long, duration: Int) {
    var progress by remember(since, duration) { mutableFloatStateOf(0f) }

    LaunchedEffect(since, duration) {
        // one step per ~1% of the wait, so a 10 min countdown isn't redrawn 20x a second
        val step = (duration / 100).coerceIn(50, 1000).toLong()
        while (progress < 1f) {
            progress = ((System.currentTimeMillis() - since).toFloat() / duration).coerceIn(0f, 1f)
            delay(step.milliseconds)
        }
    }

    CircularProgressIndicator(
        progress = { progress },
        modifier = Modifier.size(20.dp),
        color = Colors.Red,
        trackColor = Colors.White,
        strokeWidth = 2.dp,
        gapSize = 0.dp,
    )
}

@Composable
fun HeartIcon(state: TrackingState) {
    key(state.heartBeatDuration) {
        val infiniteTransition = rememberInfiniteTransition()
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween((state.heartBeatDuration * 1000).toInt(), easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = "Heart",
            modifier = Modifier
                .height(32.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
        )
    }
}

@Preview
@Composable
fun TrackingScreenConnectedPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Connected(),
            hrFeature = HrFeature(true),
            initialBpm = 117,
        )
    }
}
@Preview
@Composable
fun TrackingScreenDisconnectedPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Disconnected()
        )
    }
}
@Preview
@Composable
fun TrackingScreenConnectingPreview() {
    HeartAlertTheme {
        TrackingScreenContent(
            deviceConnectionState = DeviceConnectionState.Connecting()
        )
    }
}
