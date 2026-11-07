package app.purrhunt.domain

/** Aggregated statistics of one cat, the input for ranks, stars and achievements. */
data class CatStats(
    val totalCatches: Int = 0,
    val totalMisses: Int = 0,
    val perSpecies: Map<Species, Int> = emptyMap(),
    val rareCatches: Int = 0,
    val bestStreak: Int = 0,
    val bestSessionCatches: Int = 0,
    val nightCatches: Int = 0,
    val featherCatches: Int = 0,
    val laserCatches: Int = 0,
    val sessions: Int = 0,
    val totalPlayMs: Long = 0,
    /** Days (epoch day numbers) on which the cat played. */
    val playDays: Set<Long> = emptySet(),
    val locationsPlayed: Set<Location> = emptySet(),
) {
    fun caught(species: Species): Int = perSpecies[species] ?: 0
    fun caught(group: Collection<Species>): Int = group.sumOf { caught(it) }
    fun caughtAll(group: Collection<Species>): Boolean = group.all { caught(it) > 0 }
}

enum class Rank(val threshold: Int) {
    KITTEN(0), MOUSER(50), YARD_TERROR(200), HUNTER(500), BASEMENT_LEGEND(1000);

    companion object {
        fun forCatches(catches: Int): Rank = entries.last { catches >= it.threshold }

        /** The next rank and how many catches remain, or null at the top rank. */
        fun next(catches: Int): Pair<Rank, Int>? =
            entries.firstOrNull { it.threshold > catches }?.let { it to (it.threshold - catches) }
    }
}

object Stars {
    const val CATCHES_PER_STAR = 10
    const val RARE_BONUS = 3

    fun total(catches: Int, rareCatches: Int): Int = catches / CATCHES_PER_STAR + rareCatches * RARE_BONUS

    /** Stars earned by a session given the totals before it. */
    fun earned(catchesBefore: Int, rareBefore: Int, sessionCatches: Int, sessionRare: Int): Int =
        total(catchesBefore + sessionCatches, rareBefore + sessionRare) - total(catchesBefore, rareBefore)

    fun isUnlocked(location: Location, night: Boolean, stars: Int, starUnlockEnabled: Boolean): Boolean {
        if (!starUnlockEnabled) return true
        val cost = location.starCost + if (night) Location.NIGHT_STAR_COST else 0
        return stars >= cost
    }
}

object SpeciesGroups {
    val MICE = listOf(Species.GRAY_MOUSE, Species.MOUSELET, Species.WHITE_MOUSE)
    val BIRDS = Species.entries.filter { it.family == Family.BIRD }
    val INSECTS = Species.entries.filter {
        it.family in setOf(Family.BUTTERFLY, Family.FLY, Family.CRAWLER, Family.CRICKET, Family.FIREFLY, Family.STRIDER)
    }
    val POND = Location.POND.common + Location.POND.rare
    val GARDEN = Location.GARDEN.common
    val MEADOW = Location.MEADOW.common + Location.MEADOW.rare
    val RARE = Species.entries.filter { it.rare }
}

enum class Achievement(val target: Long, val progress: (CatStats) -> Long) {
    FIRST_CATCH(1, { it.totalCatches.toLong() }),
    TEN_CATCHES(10, { it.totalCatches.toLong() }),
    HUNDRED_CATCHES(100, { it.totalCatches.toLong() }),
    THOUSAND_CATCHES(1000, { it.totalCatches.toLong() }),
    HUNDRED_MICE(100, { it.caught(SpeciesGroups.MICE).toLong() }),
    RAT_CATCHER(25, { it.caught(Species.RAT).toLong() }),
    BIRD_WATCHER(50, { it.caught(SpeciesGroups.BIRDS).toLong() }),
    BUG_HUNTER(100, { it.caught(SpeciesGroups.INSECTS).toLong() }),
    GARDEN_COMPLETE(SpeciesGroups.GARDEN.size.toLong(), { s -> SpeciesGroups.GARDEN.count { s.caught(it) > 0 }.toLong() }),
    MEADOW_COMPLETE(SpeciesGroups.MEADOW.size.toLong(), { s -> SpeciesGroups.MEADOW.count { s.caught(it) > 0 }.toLong() }),
    POND_COMPLETE(SpeciesGroups.POND.size.toLong(), { s -> SpeciesGroups.POND.count { s.caught(it) > 0 }.toLong() }),
    PERFECT_TEN(10, { it.bestStreak.toLong() }),
    SESSION_THIRTY(30, { it.bestSessionCatches.toLong() }),
    NIGHT_HUNTER(25, { it.nightCatches.toLong() }),
    RARE_FIRST(1, { it.rareCatches.toLong() }),
    RARE_COLLECTOR(SpeciesGroups.RARE.size.toLong(), { s -> SpeciesGroups.RARE.count { s.caught(it) > 0 }.toLong() }),
    WEEK_STREAK(7, { longestDayRun(it.playDays).toLong() }),
    FEATHER_FAN(50, { it.featherCatches.toLong() }),
    LASER_CHASER(50, { it.laserCatches.toLong() }),
    EXPLORER(Location.entries.size.toLong(), { it.locationsPlayed.size.toLong() }),
    MARATHON(5L * 60 * 60 * 1000, { it.totalPlayMs }),
    BESTIARY_COMPLETE(Species.entries.size.toLong(), { s -> Species.entries.count { s.caught(it) > 0 }.toLong() });

    fun fraction(stats: CatStats): Float = (progress(stats).coerceAtMost(target).toFloat() / target).coerceIn(0f, 1f)
    fun isUnlocked(stats: CatStats): Boolean = progress(stats) >= target

    companion object {
        fun unlocked(stats: CatStats): Set<Achievement> = entries.filterTo(mutableSetOf()) { it.isUnlocked(stats) }

        fun longestDayRun(days: Set<Long>): Int {
            if (days.isEmpty()) return 0
            val sorted = days.sorted()
            var best = 1
            var run = 1
            for (i in 1 until sorted.size) {
                run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
                if (run > best) best = run
            }
            return best
        }
    }
}

/** Exposure of a cat to one species: how long it was on screen and how many touches landed near it. */
data class Interaction(val species: Species, val exposureMs: Long, val nearTouches: Int)

object FavoritePrey {
    /** Species seen for less than this are ignored: too little data. */
    const val MIN_EXPOSURE_MS = 30_000L

    fun touchesPerMinute(i: Interaction): Float = i.nearTouches / (i.exposureMs / 60_000f)

    fun find(interactions: List<Interaction>): Species? = interactions
        .groupBy { it.species }
        .map { (species, list) -> Interaction(species, list.sumOf { it.exposureMs }, list.sumOf { it.nearTouches }) }
        .filter { it.exposureMs >= MIN_EXPOSURE_MS && it.nearTouches > 0 }
        .maxByOrNull { touchesPerMinute(it) }
        ?.species

    /** The location to suggest for a favourite species. */
    fun suggestedLocation(species: Species): Location =
        Location.entries.firstOrNull { species in it.common || species in it.rare } ?: Location.MEADOW
}
