package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class VibrationType {
    LOW, HIGH
}

@Singleton
class VibrationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator

    /**
     * Android drops vibrations from apps that are not in the foreground (e.g. while the screen is
     * locked) unless their usage is one the system allows in the background. Tagging them as an
     * alarm keeps the out-of-range alert vibrating on a locked phone, like the sound does.
     */
    private val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM)

    fun vibrate(type: VibrationType) {
        val timings = when (type) {
            VibrationType.LOW -> longArrayOf(0, 75)
            VibrationType.HIGH -> longArrayOf(0, 75, 150, 75)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1), attributes)
    }
}
