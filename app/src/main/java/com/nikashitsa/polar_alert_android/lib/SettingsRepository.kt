package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, "settings"))
    }
)

object SettingsDefaults {
    const val VOLUME = 90
    const val HR_MIN = 110
    const val HR_MAX = 140
    const val VIBRATE = false
    const val ALERT_INTERVAL = 1
    const val OUT_OF_RANGE_FOR = 0
}

object SettingsOptions {
    val ALERT_INTERVAL = listOf(1, 3, 5, 10)
    val OUT_OF_RANGE_FOR = listOf(0, 5, 10, 30, 60, 300, 600)
}

object SettingsKeys {
    val volume = intPreferencesKey("volume")
    val hrMin = intPreferencesKey("hrMin")
    val hrMax = intPreferencesKey("hrMax")
    val vibrate = booleanPreferencesKey("vibrate")
    val alertInterval = intPreferencesKey("alertInterval")
    val outOfRangeFor = intPreferencesKey("outOfRangeFor")
}

@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    val volumeFlow: Flow<Int> = get(SettingsKeys.volume, SettingsDefaults.VOLUME)
    suspend fun setVolume(value: Int) = set(SettingsKeys.volume, value.coerceIn(0, 100))

    val hrMinFlow: Flow<Int> = get(SettingsKeys.hrMin, SettingsDefaults.HR_MIN)
    suspend fun setHrMin(value: Int) = set(SettingsKeys.hrMin, value)

    val hrMaxFlow: Flow<Int> = get(SettingsKeys.hrMax, SettingsDefaults.HR_MAX)
    suspend fun setHrMax(value: Int) = set(SettingsKeys.hrMax, value)

    val vibrateFlow: Flow<Boolean> = get(SettingsKeys.vibrate, SettingsDefaults.VIBRATE)
    suspend fun setVibrate(value: Boolean) = set(SettingsKeys.vibrate, value)

    val alertIntervalFlow: Flow<Int> = get(SettingsKeys.alertInterval, SettingsDefaults.ALERT_INTERVAL)
    suspend fun setAlertInterval(value: Int) = set(SettingsKeys.alertInterval, value)

    val outOfRangeForFlow: Flow<Int> = get(SettingsKeys.outOfRangeFor, SettingsDefaults.OUT_OF_RANGE_FOR)
    suspend fun setOutOfRangeFor(value: Int) = set(SettingsKeys.outOfRangeFor, value)

    private fun <T> get(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { prefs -> prefs[key] ?: default }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }
}