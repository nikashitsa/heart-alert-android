package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.nikashitsa.polar_alert_android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SoundType(val resId: Int) {
    HIGH_BEEP(R.raw.high_beep),
    LOW_BEEP(R.raw.low_beep),
    CONNECTED(R.raw.connected),
    DISCONNECTED(R.raw.disconnected),
    RECONNECTING(R.raw.reconnecting),
    GOOD(R.raw.good),
    TOO_HIGH(R.raw.too_high),
    TOO_LOW(R.raw.too_low),
}

@Singleton
class SoundManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundType, Int>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        for (type in SoundType.entries) {
            val soundId = soundPool.load(context, type.resId, 1)
            soundMap[type] = soundId
        }
    }

    fun play(type: SoundType, volume: Int) {
        soundMap[type]?.let { soundId ->
            val volumeFloat = volume / 100f
            soundPool.play(soundId, volumeFloat, volumeFloat, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}