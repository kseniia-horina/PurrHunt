package app.purrhunt.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    @Test
    fun `rank thresholds`() {
        assertEquals(Rank.KITTEN, Rank.forCatches(0))
        assertEquals(Rank.KITTEN, Rank.forCatches(49))
        assertEquals(Rank.MOUSER, Rank.forCatches(50))
        assertEquals(Rank.YARD_TERROR, Rank.forCatches(200))
        assertEquals(Rank.HUNTER, Rank.forCatches(999))
        assertEquals(Rank.BASEMENT_LEGEND, Rank.forCatches(5000))
        assertEquals(Rank.MOUSER to 8, Rank.next(42))
        assertNull(Rank.next(1000))
    }

    @Test
    fun `one star per ten catches plus rare bonus`() {
        assertEquals(0, Stars.total(9, 0))
        assertEquals(1, Stars.total(10, 0))
        assertEquals(2 + Stars.RARE_BONUS, Stars.total(25, 1))
    }

    @Test
    fun `stars earned accumulate across sessions`() {
        // 7 caught before, 5 now -> crosses the 10 boundary once.
        assertEquals(1, Stars.earned(7, 0, 5, 0))
        assertEquals(0, Stars.earned(0, 0, 9, 0))
        assertEquals(Stars.RARE_BONUS, Stars.earned(0, 0, 1, 1))
    }

    @Test
    fun `locations lock only when star unlock is enabled`() {
        assertTrue(Stars.isUnlocked(Location.MIX, false, 0, starUnlockEnabled = false))
        assertFalse(Stars.isUnlocked(Location.MIX, false, 0, starUnlockEnabled = true))
        assertTrue(Stars.isUnlocked(Location.BASEMENT, false, 0, starUnlockEnabled = true))
        assertFalse(Stars.isUnlocked(Location.MEADOW, true, Location.MEADOW.starCost, starUnlockEnabled = true))
        assertTrue(Stars.isUnlocked(Location.MEADOW, true, Location.MEADOW.starCost + Location.NIGHT_STAR_COST, true))
    }

    @Test
    fun `at least fifteen achievements`() {
        assertTrue(Achievement.entries.size >= 15)
    }

    @Test
    fun `achievements unlock from stats`() {
        val stats = CatStats(
            totalCatches = 120,
            perSpecies = mapOf(Species.GRAY_MOUSE to 80, Species.MOUSELET to 20, Species.RAT to 20),
            bestStreak = 10,
        )
        val unlocked = Achievement.unlocked(stats)
        assertTrue(Achievement.FIRST_CATCH in unlocked)
        assertTrue(Achievement.HUNDRED_CATCHES in unlocked)
        assertTrue(Achievement.HUNDRED_MICE in unlocked)
        assertTrue(Achievement.PERFECT_TEN in unlocked)
        assertFalse(Achievement.RAT_CATCHER in unlocked)
        assertEquals(0.8f, Achievement.RAT_CATCHER.fraction(stats), 0.001f)
    }

    @Test
    fun `pond complete needs every pond species including the rare one`() {
        val almost = CatStats(perSpecies = mapOf(Species.CRUCIAN to 1, Species.TADPOLE to 1, Species.WATER_STRIDER to 1))
        assertFalse(Achievement.POND_COMPLETE.isUnlocked(almost))
        val all = almost.copy(perSpecies = almost.perSpecies + (Species.GOLDFISH to 1))
        assertTrue(Achievement.POND_COMPLETE.isUnlocked(all))
    }

    @Test
    fun `longest run of consecutive days`() {
        assertEquals(0, Achievement.longestDayRun(emptySet()))
        assertEquals(3, Achievement.longestDayRun(setOf(1, 2, 3, 5, 6)))
        assertTrue(Achievement.WEEK_STREAK.isUnlocked(CatStats(playDays = (100L..106L).toSet())))
    }

    @Test
    fun `favourite prey is the species with most touches per minute`() {
        val interactions = listOf(
            Interaction(Species.GRAY_MOUSE, 120_000, 20), // 10 / min
            Interaction(Species.FLY, 60_000, 15), // 15 / min
            Interaction(Species.RAT, 10_000, 30), // too short to count
        )
        assertEquals(Species.FLY, FavoritePrey.find(interactions))
        assertEquals(Location.MEADOW, FavoritePrey.suggestedLocation(Species.FLY))
    }

    @Test
    fun `favourite prey sums exposure across sessions`() {
        val interactions = listOf(
            Interaction(Species.RAT, 20_000, 10),
            Interaction(Species.RAT, 20_000, 10),
            Interaction(Species.FLY, 60_000, 5),
        )
        assertEquals(Species.RAT, FavoritePrey.find(interactions))
        assertNull(FavoritePrey.find(emptyList()))
    }
}
