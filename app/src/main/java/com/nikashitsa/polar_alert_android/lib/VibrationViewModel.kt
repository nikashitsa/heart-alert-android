package com.nikashitsa.polar_alert_android.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VibrationViewModel @Inject constructor(
    private val vibrationManager: VibrationManager,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    fun vibrate(type: VibrationType) {
        viewModelScope.launch {
            val vibrate = settingsRepository.vibrateFlow.first()
            if (vibrate) {
                vibrationManager.vibrate(type)
            }
        }
    }
}