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
import androidx.compose.ui.graphics.Color
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
import com.nikashitsa.polar_alert_android.lib.SettingsLimits
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
    val initialDelay by settings.initialDelay.collectAsState()

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
        initialDelay = initialDelay,
        vibrate = vibration::vibrate,
        countTrackedSession = settings::countTrackedSession,
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
    initialDelay: Int = SettingsDefaults.INITIAL_DELAY,
    vibrate: (VibrationType) -> Unit = {},
    countTrackedSession: () -> Unit = {},
    initialBpm: Int = -1,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val sessionCompleted = rememberSessionCompleted(onComplete = countTrackedSession)

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
            initialDelay = initialDelay,
            vibrate = vibrate,
            initialBpm = initialBpm,
        )

        Spacer(modifier = Modifier.weight(1f))

        AppButton("Stop") {
            hrStreamStop()
            // Only worth asking for a review after a session that actually ran.
            if (sessionCompleted) requestAppReview(context, activity)
            onBack()
        }
    }
}

/**
 * Waits out the minimum session length, then reports the session as completed exactly once.
 * Returns whether that point has been reached. Both pieces of state survive rotation and
 * process death, and the remaining time is recomputed rather than restarted.
 */
@Composable
private fun rememberSessionCompleted(onComplete: () -> Unit): Boolean {
    val startedAt = rememberSaveable { System.currentTimeMillis() }
    var completed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (completed) return@LaunchedEffect
        val remaining = SettingsLimits.SESSION_MIN_DURATION_MS - (System.currentTimeMillis() - startedAt)
        if (remaining > 0) delay(remaining.milliseconds)
        completed = true
        onComplete()
    }
    return completed
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
    initialDelay: Int = SettingsDefaults.INITIAL_DELAY,
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
    // alerts are held back at the start of a session until HR first reaches the
    // range, or until the initial delay times out, whichever comes first
    var initialDelayPassed by rememberSaveable { mutableStateOf(false) }
    val trackingStartedAt = rememberSaveable { System.currentTimeMillis() }
    val initialDelayActive = initialDelay != 0 && !initialDelayPassed
    val outOfRangeForInterval = outOfRangeFor * 1000 // ms
    val throttleInterval = alertInterval * 1000 - 310 // ms

    // Called for every heart rate sample the strap sends, about once a second.
    fun onHeartRate(hr: Int) {
        bpm = hr
        val prevState = state
        state = TrackingState.of(hr, hrMin, hrMax)
        val now = System.currentTimeMillis()

        // Back in range. Announce the recovery only if we were really alerting.
        if (state == TrackingState.GOOD) {
            outOfRangeSince = null
            lastTriggerTime = null
            initialDelayPassed = true // reaching the range always ends the initial delay
            if (alerting) {
                alerting = false
                playSound(state.soundState)
            }
            return
        }

        // Out of range. Keep timing the stretch even while alerts are held back.
        val since = outOfRangeSince ?: now
        outOfRangeSince = since

        // Hold everything back until the initial delay times out. The "until in
        // range" option never times out, it only ends in the branch above.
        if (initialDelay != 0 && !initialDelayPassed) {
            val timedOut = initialDelay > 0 && now - trackingStartedAt >= initialDelay * 1000
            if (!timedOut) return
            initialDelayPassed = true
        }

        // Stay quiet until HR has been out of range long enough.
        if (now - since < outOfRangeForInterval) return

        // Say what is wrong when alerts start, and again on a too low <-> too high flip.
        if (!alerting || state != prevState) {
            alerting = true
            playSound(state.soundState)
        }

        // Repeat the beep and the vibration at the chosen interval.
        val lastTrigger = lastTriggerTime
        if (lastTrigger == null || now - lastTrigger > throttleInterval) {
            lastTriggerTime = now
            state.sound?.let(playSound)
            state.vibration?.let(vibrate)
        }
    }

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
                LaunchedEffect(Unit) {
                    if (prevConnectionState is DeviceConnectionState.Disconnected) {
                        playSound(SoundType.CONNECTED)
                    }
                    hrStreamStart(connectionState.address, ::onHeartRate)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BpmReadout(
                        bpm = bpm,
                        state = state,
                        color = if (alerting) Colors.Red else Colors.White,
                    )
                    AlertStatus(
                        state = state,
                        alerting = alerting,
                        initialDelay = initialDelay,
                        initialDelayActive = initialDelayActive,
                        trackingStartedAt = trackingStartedAt,
                        outOfRangeSince = outOfRangeSince,
                        outOfRangeForInterval = outOfRangeForInterval,
                    )
                }
            } else {
                Text("Reconnecting...", style = Fonts.textLg)
            }
        }
    }
}

/** The big BPM number with the beating heart next to it. */
@Composable
fun BpmReadout(bpm: Int, state: TrackingState, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(80.dp),
    ) {
        Text(
            text = if (bpm > -1) "$bpm" else "--",
            style = Fonts.text2XlBold,
            overflow = TextOverflow.Visible,
            modifier = Modifier.offset(y = (-12).dp),
            color = color,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeartIcon(state)
            Text(text = "BPM", style = Fonts.textLg, color = color)
        }
    }
}

/**
 * The line under the BPM number. It shows whichever of the three is happening:
 * the alert itself, the initial delay, or the wait for HR to stay out of range.
 * The fixed height keeps the screen from jumping as it switches between them.
 */
@Composable
fun AlertStatus(
    state: TrackingState,
    alerting: Boolean,
    initialDelay: Int,
    initialDelayActive: Boolean,
    trackingStartedAt: Long,
    outOfRangeSince: Long?,
    outOfRangeForInterval: Int,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.height(30.dp),
    ) {
        when {
            alerting -> Text(
                text = state.heartBeatDescription,
                style = Fonts.textLg,
            )
            initialDelayActive -> if (initialDelay > 0) {
                InitialDelayCountdown(
                    since = trackingStartedAt,
                    duration = initialDelay * 1000,
                )
            } else {
                Text(
                    text = "Initial delay until in range",
                    style = Fonts.textLg,
                )
            }
            outOfRangeSince != null && outOfRangeForInterval > 0 -> AlertCountdown(
                since = outOfRangeSince,
                duration = outOfRangeForInterval,
            )
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
 * Counts down the time left of the initial delay, during which alerts are held back.
 * [since] is when tracking started, in epoch ms, [duration] the delay in ms.
 */
@Composable
fun InitialDelayCountdown(since: Long, duration: Int) {
    var remaining by remember(since, duration) { mutableIntStateOf(duration) }

    LaunchedEffect(since, duration) {
        while (remaining > 0) {
            remaining = (duration - (System.currentTimeMillis() - since)).coerceAtLeast(0L).toInt()
            // faster than once a second, so the displayed second never lags behind
            delay(200.milliseconds)
        }
    }

    Text(
        text = "Initial delay %02d:%02d".format(remaining / 60_000, remaining / 1000 % 60),
        style = Fonts.textLg,
    )
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
