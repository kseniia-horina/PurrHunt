package app.purrhunt.domain

enum class Location(
    val common: List<Species>,
    val rare: List<Species>,
    /** Stars needed when "unlock locations with stars" is enabled. */
    val starCost: Int,
    val supportsNight: Boolean = false,
) {
    BASEMENT(
        common = listOf(Species.GRAY_MOUSE, Species.MOUSELET, Species.RAT),
        rare = listOf(Species.WHITE_MOUSE),
        starCost = 0,
    ),
    GARDEN(
        common = listOf(Species.SPARROW, Species.TIT, Species.BULLFINCH),
        rare = emptyList(),
        starCost = 3,
    ),
    MEADOW(
        common = listOf(Species.BUTTERFLY, Species.FLY, Species.LADYBUG, Species.CRICKET, Species.BEETLE),
        rare = listOf(Species.SWALLOWTAIL),
        starCost = 6,
        supportsNight = true,
    ),
    POND(
        common = listOf(Species.CRUCIAN, Species.TADPOLE, Species.WATER_STRIDER),
        rare = listOf(Species.GOLDFISH),
        starCost = 10,
    ),
    MIX(
        common = listOf(
            Species.GRAY_MOUSE, Species.MOUSELET, Species.SPARROW, Species.BUTTERFLY,
            Species.FLY, Species.LADYBUG, Species.CRICKET, Species.CRUCIAN, Species.WATER_STRIDER,
        ),
        rare = listOf(Species.WHITE_MOUSE, Species.GOLDFISH, Species.SWALLOWTAIL),
        starCost = 15,
    );

    companion object {
        /** The night meadow costs extra stars on top of the meadow itself. */
        const val NIGHT_STAR_COST = 8
        fun byName(name: String): Location = entries.firstOrNull { it.name == name } ?: BASEMENT
    }
}

/** A concrete playable scene: a location plus time of day and season. */
data class SceneSpec(
    val location: Location,
    val night: Boolean,
    val season: Season,
) {
    val common: List<Species>
        get() = if (night) NIGHT_COMMON else location.common
    val rare: List<Species>
        get() = if (night) NIGHT_RARE else location.rare

    companion object {
        val NIGHT_COMMON = listOf(Species.FIREFLY, Species.CRICKET, Species.BEETLE, Species.FLY)
        val NIGHT_RARE = listOf(Species.GIANT_FIREFLY)
    }
}

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    companion object {
        /** [month] is 1-based (January = 1). */
        fun ofMonth(month: Int): Season = when (month) {
            12, 1, 2 -> WINTER
            3, 4, 5 -> SPRING
            6, 7, 8 -> SUMMER
            else -> AUTUMN
        }
    }
}

enum class GameMode { HUNT, FEATHER, LASER }

enum class NightMode { AUTO, ALWAYS, NEVER }

object NightClock {
    /** Night is 21:00–05:59. */
    fun isNight(hour: Int): Boolean = hour >= 21 || hour < 6

    fun resolveNight(location: Location, requestedNight: Boolean, mode: NightMode, hour: Int): Boolean {
        if (!location.supportsNight) return false
        return when (mode) {
            NightMode.NEVER -> false
            NightMode.ALWAYS -> true
            NightMode.AUTO -> requestedNight || isNight(hour)
        }
    }
}
