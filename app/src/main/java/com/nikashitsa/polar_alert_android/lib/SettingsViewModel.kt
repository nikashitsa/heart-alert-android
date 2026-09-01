package com.nikashitsa.polar_alert_android.lib

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
): ViewModel() {

    private val _volume = MutableStateFlow<Int>(SettingsDefaults.VOLUME)
    val volume: StateFlow<Int> = _volume
    private val _hrMin = MutableStateFlow<Int>(SettingsDefaults.HR_MIN)
    val hrMin: StateFlow<Int> = _hrMin
    private val _hrMax = MutableStateFlow<Int>(SettingsDefaults.HR_MAX)
    val hrMax: StateFlow<Int> = _hrMax
    private val _vibrate = MutableStateFlow<Boolean>(SettingsDefaults.VIBRATE)
    val vibrate: StateFlow<Boolean> = _vibrate
    private val _alertInterval = MutableStateFlow<Int>(SettingsDefaults.ALERT_INTERVAL)
    val alertInterval: StateFlow<Int> = _alertInterval
    private val _outOfRangeFor = MutableStateFlow<Int>(SettingsDefaults.OUT_OF_RANGE_FOR)
    val outOfRangeFor: StateFlow<Int> = _outOfRangeFor
    private val _initialDelay = MutableStateFlow<Int>(SettingsDefaults.INITIAL_DELAY)
    val initialDelay: StateFlow<Int> = _initialDelay
    private val _unlimitedAccess = MutableStateFlow<Boolean>(SettingsDefaults.UNLIMITED_ACCESS)
    val unlimitedAccess: StateFlow<Boolean> = _unlimitedAccess

    // Seeded true, unlike every other setting, because DataStore loads asynchronously and a
    // false seed would flash the paywall at an entitled user who taps Start immediately.
    private val _hasAccess = MutableStateFlow<Boolean>(true)
    val hasAccess: StateFlow<Boolean> = _hasAccess

    // Seeded 0 for the same reason: until DataStore loads the button reads a plain "Start",
    // so an entitled user is never briefly offered free sessions.
    private val _freeSessionsLeft = MutableStateFlow<Int>(0)
    val freeSessionsLeft: StateFlow<Int> = _freeSessionsLeft

    init {
        observe(repository.volumeFlow, _volume)
        observe(repository.hrMinFlow, _hrMin)
        observe(repository.hrMaxFlow, _hrMax)
        observe(repository.vibrateFlow, _vibrate)
        observe(repository.alertIntervalFlow, _alertInterval)
        observe(repository.outOfRangeForFlow, _outOfRangeFor)
        observe(repository.initialDelayFlow, _initialDelay)
        observe(repository.unlimitedAccessFlow, _unlimitedAccess)
        observe(repository.hasAccessFlow, _hasAccess)
        observe(repository.freeSessionsLeftFlow, _freeSessionsLeft)
    }

    fun setVolume(value: Int) = update { repository.setVolume(value) }
    fun setHrMin(value: Int) = update { repository.setHrMin(value) }
    fun setHrMax(value: Int) = update { repository.setHrMax(value) }
    fun setVibrate(value: Boolean) = update { repository.setVibrate(value) }
    fun setAlertInterval(value: Int) = update { repository.setAlertInterval(value) }
    fun setOutOfRangeFor(value: Int) = update { repository.setOutOfRangeFor(value) }
    fun setInitialDelay(value: Int) = update { repository.setInitialDelay(value) }
    fun countTrackedSession() = update { repository.countTrackedSession() }

    private fun <T> observe(flow: Flow<T>, state: MutableStateFlow<T>) {
        viewModelScope.launch {
            flow.collect { state.value = it }
        }
    }
    private fun update(action: suspend () -> Unit) {
        viewModelScope.launch { action() }
    }
}