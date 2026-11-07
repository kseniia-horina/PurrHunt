package app.purrhunt.domain

enum class AppLanguage(val tag: String) { SYSTEM(""), RU("ru"), UK("uk"), EN("en") }

data class GameSettings(
    /** 1..5, 3 is normal. */
    val speedLevel: Int = 3,
    /** 1..5 creatures on screen at once. */
    val creatureCount: Int = 3,
    val adaptiveDifficulty: Boolean = true,
    val catContrast: Boolean = false,
    val effectsVolume: Float = 0.8f,
    val ambientVolume: Float = 0.5f,
    val stereoPanning: Boolean = true,
    val vibration: Boolean = true,
    /** 0 = unlimited. */
    val sessionMinutes: Int = 10,
    val treatReminder: Boolean = true,
    val nightMode: NightMode = NightMode.AUTO,
    val seasons: Boolean = true,
    val starUnlock: Boolean = false,
    val favoritePreyMoreOften: Boolean = false,
    val remindersEnabled: Boolean = false,
    /** ISO day numbers, Monday = 1 .. Sunday = 7. */
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7),
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val photoOnCatch: Boolean = false,
    val videoRecording: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
) {
    companion object {
        val SESSION_OPTIONS = listOf(5, 10, 15, 20, 0)
        const val MIN_LEVEL = 1
        const val MAX_LEVEL = 5

        /** Speed multiplier for the five base speed levels. */
        fun speedMultiplier(level: Int): Float = when (level.coerceIn(MIN_LEVEL, MAX_LEVEL)) {
            1 -> 0.6f
            2 -> 0.8f
            3 -> 1.0f
            4 -> 1.2f
            else -> 1.4f
        }
    }
}

data class AppState(
    val onboardingDone: Boolean = false,
    val currentCatId: Long? = null,
    val lockTaskHintShown: Boolean = false,
)
