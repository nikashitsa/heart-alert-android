package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.os.Vibrator
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
    @Suppress("DEPRECATION")
    fun vibrate(type: VibrationType) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        when (type) {
            VibrationType.LOW -> {
                val pattern = longArrayOf(0, 75)
                vibrator.vibrate(pattern, -1)
            }
            VibrationType.HIGH -> {
                val pattern = longArrayOf(0, 75, 150, 75)
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
