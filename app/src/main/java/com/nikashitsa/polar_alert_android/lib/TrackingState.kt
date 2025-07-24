package com.nikashitsa.polar_alert_android.lib

import androidx.compose.ui.graphics.Color
import com.nikashitsa.polar_alert_android.ui.theme.Colors

enum class TrackingState {
    GOOD,
    LOW,
    HIGH;

    val heartBeatDuration: Double
        get() = when (this) {
            GOOD -> 30.0 / 80
            LOW -> 30.0 / 40
            HIGH -> 30.0 / 200
        }

    val heartBeatDescription: String
        get() = when (this) {
            GOOD -> "Good"
            LOW -> "Too low!"
            HIGH -> "Too high!"
        }

    val heartBeatColor: Color
        get() = when (this) {
            GOOD -> Colors.White
            LOW, HIGH -> Colors.Red
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