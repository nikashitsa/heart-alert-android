package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.nikashitsa.polar_alert_android.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Spoken sounds have per-language versions in `res/raw-<tag>/`; the beeps are shared. */
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
    @param:ApplicationContext private val context: Context,
    settings: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private val soundPool: SoundPool

    // Swapped whole rather than mutated, since it is loaded off the main thread.
    @Volatile
    private var soundMap: Map<SoundType, Int> = emptyMap()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        // The application context ignores the in-app language, so load through a context
        // localized to it, and reload whenever the user picks another language.
        scope.launch {
            settings.languageFlow.distinctUntilChanged().collect { tag ->
                load(context.withLanguage(tag))
            }
        }
    }

    private fun load(localizedContext: Context) {
        val old = soundMap
        soundMap = SoundType.entries.associateWith { soundPool.load(localizedContext, it.resId, 1) }
        old.values.forEach(soundPool::unload)
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