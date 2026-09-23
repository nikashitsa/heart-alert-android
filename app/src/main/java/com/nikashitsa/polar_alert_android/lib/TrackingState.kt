package com.nikashitsa.polar_alert_android.lib

import androidx.annotation.StringRes
import com.nikashitsa.polar_alert_android.R

enum class TrackingState {
    GOOD,
    LOW,
    HIGH;

    companion object {
        fun of(bpm: Int, hrMin: Int, hrMax: Int): TrackingState = when {
            bpm > hrMax -> HIGH
            bpm < hrMin -> LOW
            else -> GOOD
        }
    }

    val heartBeatDuration: Double
        get() = when (this) {
            GOOD -> 30.0 / 80
            LOW -> 30.0 / 40
            HIGH -> 30.0 / 200
        }

    @get:StringRes
    val heartBeatDescription: Int
        get() = when (this) {
            GOOD -> R.string.state_good
            LOW -> R.string.state_too_low
            HIGH -> R.string.state_too_high
        }

    val sound: SoundType?
        get() = when (this) {
            GOOD -> null
            LOW -> SoundType.LOW_BEEP
            HIGH -> SoundType.HIGH_BEEP
        }

    val soundState: SoundType
        get() = when (this) {
            GOOD -> SoundType.GOOD
            LOW -> SoundType.TOO_LOW
            HIGH -> SoundType.TOO_HIGH
        }
    val vibration: VibrationType?
        get() = when (this) {
            GOOD -> null
            LOW -> VibrationType.LOW
            HIGH -> VibrationType.HIGH
        }
}