package app.purrhunt.game

import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.domain.Medium
import app.purrhunt.domain.SceneSpec
import app.purrhunt.domain.Season
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.EngineSpec
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.engine.GameEvents
import app.purrhunt.game.engine.GameRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllScenesTest {
    private fun run(location: Location, night: Boolean, seconds: Int, width: Float = 1080f, height: Float = 2160f): Set<Species> {
        val e = GameEngine(
            EngineSpec(SceneSpec(location, night, Season.AUTUMN), GameMode.HUNT, creatureCount = 5),
            GameRandom(location.ordinal + 100L), GameEvents.None, 0L,
        )
        e.resize(width, height, 2.75f)
        val seen = HashSet<Species>()
        val r = GameRandom(9)
        repeat(seconds * 60) { step ->
            e.step(GameEngine.STEP)
            // A cat paw every half second somewhere random.
            if (step % 30 == 0) {
                e.pointerDown(step, r.range(0f, width), r.range(0f, height))
                e.pointerUp(step)
            }
            for (c in e.world.creatures) if (c.active) {
                seen += c.species
                assertFalse("${c.species} x NaN", c.x.isNaN() || c.y.isNaN() || c.heading.isNaN())
                assertTrue("${c.species} wandered off: ${c.x},${c.y} ${c.state}", c.x in -width..(2 * width) && c.y in -height..(2 * height))
                if (c.species.medium == Medium.WATER_SURFACE && c.state == CreatureState.FREEZE) {
                    assertTrue("strider on land", e.world.layout.isWater(c.x, c.y, -40f))
                }
            }
        }
        assertTrue("${location} caught nothing in $seconds s", e.tracker.catches > 0)
        return seen
    }

    @Test
    fun `every location runs and catches`() {
        for (loc in Location.entries) {
            val seen = run(loc, night = false, seconds = 120)
            assertTrue("$loc spawned ${seen}", seen.containsAll(loc.common.filter { !it.nightOnly }.take(2)))
        }
    }

    @Test
    fun `night meadow has fireflies`() {
        val seen = run(Location.MEADOW, night = true, seconds = 90)
        assertTrue(Species.FIREFLY in seen)
    }

    @Test
    fun `landscape tablet layout works`() {
        run(Location.MIX, night = false, seconds = 60, width = 2560f, height = 1600f)
    }

    @Test
    fun `bedtime empties every scene`() {
        for (loc in Location.entries) {
            val e = GameEngine(
                EngineSpec(SceneSpec(loc, false, Season.SUMMER), GameMode.HUNT, creatureCount = 5, sessionSeconds = 20),
                GameRandom(3), GameEvents.None, 0L,
            )
            e.resize(1080f, 2160f, 2.75f)
            repeat((20 + GameEngine.BEDTIME_MAX.toInt() + 1) * 60) { e.step(GameEngine.STEP) }
            assertEquals(GameEngine.Phase.FINISHED, e.phase)
        }
    }

    @Test
    fun `toy modes count catches`() {
        for (mode in listOf(GameMode.LASER, GameMode.FEATHER)) {
            val e = GameEngine(EngineSpec(SceneSpec(Location.MEADOW, false, Season.SUMMER), mode), GameRandom(1), GameEvents.None, 0L)
            e.resize(1080f, 2160f, 2.75f)
            repeat(120) { e.step(GameEngine.STEP) }
            if (mode == GameMode.FEATHER) {
                // Owner's hand away from the feather, then the cat's paw on the feather.
                e.pointerDown(1, 100f, 1900f)
                repeat(60) { e.step(GameEngine.STEP) }
                e.pointerDown(2, e.feather.featherX, e.feather.featherY)
            } else {
                e.pointerDown(2, e.laser.x, e.laser.y)
            }
            assertEquals(mode.name, 1, e.tracker.catches)
            assertEquals(null, e.tracker.caughtList().single().species)
        }
    }
}
