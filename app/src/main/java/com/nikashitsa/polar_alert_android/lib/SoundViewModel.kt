package com.nikashitsa.polar_alert_android.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoundViewModel @Inject constructor(
    private val soundManager: SoundManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun play(type: SoundType) {
        viewModelScope.launch {
            val volume = settingsRepository.volumeFlow.first()
            soundManager.play(type, volume)
        }
    }
}