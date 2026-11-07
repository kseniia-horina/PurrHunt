package app.purrhunt.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import app.purrhunt.domain.AppLanguage
import app.purrhunt.domain.AppState
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.NightMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<GameSettings> = store.data.map { it.toSettings() }
    val appState: Flow<AppState> = store.data.map { it.toAppState() }

    suspend fun current(): GameSettings = settings.first()

    suspend fun update(transform: (GameSettings) -> GameSettings) {
        store.edit { prefs -> prefs.write(transform(prefs.toSettings())) }
    }

    suspend fun setOnboardingDone() = store.edit { it[Keys.ONBOARDING] = true }
    suspend fun setLockTaskHintShown() = store.edit { it[Keys.LOCK_HINT] = true }
    suspend fun setCurrentCat(id: Long?) = store.edit {
        if (id == null) it.remove(Keys.CURRENT_CAT) else it[Keys.CURRENT_CAT] = id
    }

    private object Keys {
        val SPEED = intPreferencesKey("speed")
        val COUNT = intPreferencesKey("count")
        val ADAPTIVE = booleanPreferencesKey("adaptive")
        val CONTRAST = booleanPreferencesKey("contrast")
        val FX_VOLUME = floatPreferencesKey("fx_volume")
        val AMBIENT_VOLUME = floatPreferencesKey("ambient_volume")
        val STEREO = booleanPreferencesKey("stereo")
        val VIBRATION = booleanPreferencesKey("vibration")
        val SESSION = intPreferencesKey("session_minutes")
        val TREAT = booleanPreferencesKey("treat_reminder")
        val NIGHT = stringPreferencesKey("night_mode")
        val SEASONS = booleanPreferencesKey("seasons")
        val STAR_UNLOCK = booleanPreferencesKey("star_unlock")
        val FAVORITE = booleanPreferencesKey("favorite_more_often")
        val REMINDERS = booleanPreferencesKey("reminders")
        val REMINDER_DAYS = stringSetPreferencesKey("reminder_days")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val PHOTO = booleanPreferencesKey("photo_on_catch")
        val VIDEO = booleanPreferencesKey("video")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LOCK_HINT = booleanPreferencesKey("lock_hint_shown")
        val CURRENT_CAT = longPreferencesKey("current_cat")
    }

    private fun Preferences.toSettings(): GameSettings {
        val d = GameSettings()
        return GameSettings(
            speedLevel = this[Keys.SPEED] ?: d.speedLevel,
            creatureCount = this[Keys.COUNT] ?: d.creatureCount,
            adaptiveDifficulty = this[Keys.ADAPTIVE] ?: d.adaptiveDifficulty,
            catContrast = this[Keys.CONTRAST] ?: d.catContrast,
            effectsVolume = this[Keys.FX_VOLUME] ?: d.effectsVolume,
            ambientVolume = this[Keys.AMBIENT_VOLUME] ?: d.ambientVolume,
            stereoPanning = this[Keys.STEREO] ?: d.stereoPanning,
            vibration = this[Keys.VIBRATION] ?: d.vibration,
            sessionMinutes = this[Keys.SESSION] ?: d.sessionMinutes,
            treatReminder = this[Keys.TREAT] ?: d.treatReminder,
            nightMode = this[Keys.NIGHT]?.let { v -> NightMode.entries.firstOrNull { it.name == v } } ?: d.nightMode,
            seasons = this[Keys.SEASONS] ?: d.seasons,
            starUnlock = this[Keys.STAR_UNLOCK] ?: d.starUnlock,
            favoritePreyMoreOften = this[Keys.FAVORITE] ?: d.favoritePreyMoreOften,
            remindersEnabled = this[Keys.REMINDERS] ?: d.remindersEnabled,
            reminderDays = this[Keys.REMINDER_DAYS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: d.reminderDays,
            reminderHour = this[Keys.REMINDER_HOUR] ?: d.reminderHour,
            reminderMinute = this[Keys.REMINDER_MINUTE] ?: d.reminderMinute,
            photoOnCatch = this[Keys.PHOTO] ?: d.photoOnCatch,
            videoRecording = this[Keys.VIDEO] ?: d.videoRecording,
            language = this[Keys.LANGUAGE]?.let { v -> AppLanguage.entries.firstOrNull { it.name == v } } ?: d.language,
        )
    }

    private fun MutablePreferences.write(s: GameSettings) {
        this[Keys.SPEED] = s.speedLevel.coerceIn(GameSettings.MIN_LEVEL, GameSettings.MAX_LEVEL)
        this[Keys.COUNT] = s.creatureCount.coerceIn(1, 5)
        this[Keys.ADAPTIVE] = s.adaptiveDifficulty
        this[Keys.CONTRAST] = s.catContrast
        this[Keys.FX_VOLUME] = s.effectsVolume.coerceIn(0f, 1f)
        this[Keys.AMBIENT_VOLUME] = s.ambientVolume.coerceIn(0f, 1f)
        this[Keys.STEREO] = s.stereoPanning
        this[Keys.VIBRATION] = s.vibration
        this[Keys.SESSION] = s.sessionMinutes
        this[Keys.TREAT] = s.treatReminder
        this[Keys.NIGHT] = s.nightMode.name
        this[Keys.SEASONS] = s.seasons
        this[Keys.STAR_UNLOCK] = s.starUnlock
        this[Keys.FAVORITE] = s.favoritePreyMoreOften
        this[Keys.REMINDERS] = s.remindersEnabled
        this[Keys.REMINDER_DAYS] = s.reminderDays.map { it.toString() }.toSet()
        this[Keys.REMINDER_HOUR] = s.reminderHour
        this[Keys.REMINDER_MINUTE] = s.reminderMinute
        this[Keys.PHOTO] = s.photoOnCatch
        this[Keys.VIDEO] = s.videoRecording
        this[Keys.LANGUAGE] = s.language.name
    }

    private fun Preferences.toAppState() = AppState(
        onboardingDone = this[Keys.ONBOARDING] ?: false,
        currentCatId = this[Keys.CURRENT_CAT],
        lockTaskHintShown = this[Keys.LOCK_HINT] ?: false,
    )
}
