package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class VibrationType {
    LOW, HIGH
}

/**
 * Android drops vibrations from apps that are not in the foreground (e.g. while the screen is
 * locked) unless their usage is one the system allows in the background. Tagging them as an
 * alarm keeps the out-of-range alert vibrating on a locked phone, like the sound does.
 */
@Singleton
class VibrationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    fun vibrate(type: VibrationType) {
        val timings = when (type) {
            VibrationType.LOW -> longArrayOf(0, 75)
            VibrationType.HIGH -> longArrayOf(0, 75, 150, 75)
        }
        val effect = VibrationEffect.createWaveform(timings, -1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            // Before API 33 the vibration usage is taken from AudioAttributes.
            @Suppress("DEPRECATION")
            vibrator.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }
}
