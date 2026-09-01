package com.nikashitsa.polar_alert_android.lib

import android.content.Context
import androidx.datastore.core.DataMigration
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
        listOf(
            SharedPreferencesMigration(context, "settings"),
            EntitlementMigration(context),
        )
    }
)

object SettingsDefaults {
    const val VOLUME = 90
    const val HR_MIN = 110
    const val HR_MAX = 140
    const val VIBRATE = false
    const val ALERT_INTERVAL = 1
    const val OUT_OF_RANGE_FOR = 0
    const val INITIAL_DELAY = 0
    const val TRACKED_SESSIONS = 0
    const val UNLIMITED_ACCESS = false
}

object SettingsOptions {
    // initial delay never times out, it only ends once HR first reaches the range
    const val UNTIL_IN_RANGE = -1

    val ALERT_INTERVAL = listOf(1, 3, 5, 10)
    val OUT_OF_RANGE_FOR = listOf(0, 5, 10, 30, 60, 300, 600)
    val INITIAL_DELAY = listOf(0, UNTIL_IN_RANGE, 60, 300, 600, 900)
}

object SettingsLimits {
    /** Tracking sessions a new user gets before the paywall. */
    const val FREE_SESSIONS = 5

    /** A session only counts, and only earns a review prompt, once it passes this. */
    const val SESSION_MIN_DURATION_MS = 60_000L
}

object SettingsKeys {
    val volume = intPreferencesKey("volume")
    val hrMin = intPreferencesKey("hrMin")
    val hrMax = intPreferencesKey("hrMax")
    val vibrate = booleanPreferencesKey("vibrate")
    val alertInterval = intPreferencesKey("alertInterval")
    val outOfRangeFor = intPreferencesKey("outOfRangeFor")
    val initialDelay = intPreferencesKey("initialDelay")
    val trackedSessions = intPreferencesKey("trackedSessions")
    val unlimitedAccess = booleanPreferencesKey("unlimitedAccess")
    val entitlementResolved = booleanPreferencesKey("entitlementResolved")
}

/**
 * Settings that already existed before the paywall shipped. Their presence means the user
 * was here first. Frozen on purpose: a setting added later is not evidence of seniority.
 */
private val LEGACY_SETTING_KEYS = setOf(
    "volume", "hrMin", "hrMax", "vibrate", "alertInterval", "outOfRangeFor", "initialDelay"
)

/**
 * Grants free unlimited access to everyone who was already using the app when the paywall
 * shipped. Runs as a DataStore migration, which happens inside the store's own actor before
 * any read or write the app makes, so it cannot race with a settings write.
 */
private class EntitlementMigration(private val context: Context) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences) =
        currentData[SettingsKeys.entitlementResolved] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        // The install has been updated at least once, so it predates this release. Catches
        // long-time users who never opened Settings and therefore have nothing stored.
        val updatedOnce = runCatching {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.lastUpdateTime - info.firstInstallTime > 60_000L
        }.getOrDefault(false)

        val hasLegacySettings = currentData.asMap().keys.any { it.name in LEGACY_SETTING_KEYS }

        return currentData.toMutablePreferences().apply {
            this[SettingsKeys.entitlementResolved] = true
            if (updatedOnce || hasLegacySettings) {
                this[SettingsKeys.unlimitedAccess] = true
            }
        }.toPreferences()
    }

    override suspend fun cleanUp() {}
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

    val initialDelayFlow: Flow<Int> = get(SettingsKeys.initialDelay, SettingsDefaults.INITIAL_DELAY)
    suspend fun setInitialDelay(value: Int) = set(SettingsKeys.initialDelay, value)

    /**
     * Entitled, whether bought or granted for being an existing user. Only ever set to true:
     * a grandfathered user has no Play purchase, so "no purchase found" must never revoke it.
     */
    val unlimitedAccessFlow: Flow<Boolean> = get(SettingsKeys.unlimitedAccess, SettingsDefaults.UNLIMITED_ACCESS)
    suspend fun setUnlimitedAccess() = set(SettingsKeys.unlimitedAccess, true)

    val trackedSessionsFlow: Flow<Int> = get(SettingsKeys.trackedSessions, SettingsDefaults.TRACKED_SESSIONS)

    /**
     * Free sessions still on offer, for the Start button's label. Zero once they are used up
     * and also for an entitled user, who should not be told about free sessions at all.
     */
    val freeSessionsLeftFlow: Flow<Int> = dataStore.data.map { prefs ->
        if (prefs[SettingsKeys.unlimitedAccess] == true) {
            0
        } else {
            val used = prefs[SettingsKeys.trackedSessions] ?: SettingsDefaults.TRACKED_SESSIONS
            (SettingsLimits.FREE_SESSIONS - used).coerceAtLeast(0)
        }
    }

    /** Whether tracking may start: entitled, or still has free sessions left. */
    val hasAccessFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.unlimitedAccess] == true ||
            (prefs[SettingsKeys.trackedSessions] ?: SettingsDefaults.TRACKED_SESSIONS) < SettingsLimits.FREE_SESSIONS
    }

    /**
     * Counts one completed session. Read and write happen in a single transaction so two
     * sessions can never both read the same value, and the count stops at the free limit.
     */
    suspend fun countTrackedSession() {
        dataStore.edit { prefs ->
            if (prefs[SettingsKeys.unlimitedAccess] == true) return@edit
            val used = prefs[SettingsKeys.trackedSessions] ?: SettingsDefaults.TRACKED_SESSIONS
            if (used < SettingsLimits.FREE_SESSIONS) {
                prefs[SettingsKeys.trackedSessions] = used + 1
            }
        }
    }

    private fun <T> get(key: Preferences.Key<T>, default: T): Flow<T> =
        dataStore.data.map { prefs -> prefs[key] ?: default }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        dataStore.edit { prefs -> prefs[key] = value }
    }
}
