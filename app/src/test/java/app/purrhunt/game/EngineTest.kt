package app.purrhunt.game

import app.purrhunt.domain.GameMode
import app.purrhunt.domain.Location
import app.purrhunt.domain.SceneSpec
import app.purrhunt.domain.Season
import app.purrhunt.domain.Species
import app.purrhunt.game.engine.AdaptiveDifficulty
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.EngineSpec
import app.purrhunt.game.engine.GameEngine
import app.purrhunt.game.engine.GameEvents
import app.purrhunt.game.engine.GameRandom
import app.purrhunt.game.engine.OwnerExitDetector
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.Spawner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
    private class RecordingEvents : GameEvents {
        val sounds = mutableListOf<SoundCue>()
        val catches = mutableListOf<Species>()
        var misses = 0
        override fun onSound(cue: SoundCue, pan: Float, volume: Float) {
            sounds += cue
            assertTrue(pan in 0f..1f)
        }
        override fun onCatch(creature: Creature) { catches += creature.species }
        override fun onMiss(x: Float, y: Float) { misses++ }
    }

    private fun engine(
        location: Location = Location.BASEMENT,
        mode: GameMode = GameMode.HUNT,
        count: Int = 3,
        seconds: Int = 0,
        seed: Long = 42,
        events: GameEvents = GameEvents.None,
        adaptive: Boolean = true,
    ) = GameEngine(
        EngineSpec(SceneSpec(location, false, Season.SUMMER), mode, creatureCount = count, sessionSeconds = seconds, adaptive = adaptive),
        GameRandom(seed),
        events,
        wallStart = 0L,
    ).also { it.resize(1080f, 2160f, 2.75f) }

    private fun GameEngine.run(seconds: Float) {
        repeat((seconds / GameEngine.STEP).toInt()) { step(GameEngine.STEP) }
    }

    private fun GameEngine.firstVisible(): Creature? =
        world.creatures.firstOrNull { it.active && it.catchable && it.visibility > 0.5f && !it.isLeaving }

    @Test
    fun `spawner keeps the requested number of creatures`() {
        val e = engine(count = 4)
        e.run(8f)
        assertEquals(4, e.world.activeCount())
    }

    @Test
    fun `simulation is deterministic for a seed`() {
        val a = engine(seed = 7).apply { run(12f) }
        val b = engine(seed = 7).apply { run(12f) }
        for (i in a.world.creatures.indices) {
            assertEquals(a.world.creatures[i].x, b.world.creatures[i].x, 0.0001f)
            assertEquals(a.world.creatures[i].state, b.world.creatures[i].state)
        }
    }

    @Test
    fun `creatures go through calm states and never leave the screen while calm`() {
        val e = engine(count = 5)
        val seen = mutableSetOf<CreatureState>()
        repeat(60 * 60) {
            e.step(GameEngine.STEP)
            for (c in e.world.creatures) if (c.active) {
                seen += c.state
                if (c.state in setOf(CreatureState.IDLE, CreatureState.FREEZE, CreatureState.PEEK)) {
                    assertTrue("x=${c.x}", c.x in -c.length..(1080f + c.length))
                    assertTrue("y=${c.y}", c.y in -c.length..(2160f + c.length))
                }
            }
        }
        assertTrue(seen.containsAll(listOf(CreatureState.IDLE, CreatureState.WANDER, CreatureState.DART, CreatureState.FREEZE, CreatureState.HIDE, CreatureState.PEEK)))
    }

    @Test
    fun `touching a creature catches it and it runs away unharmed`() {
        val events = RecordingEvents()
        val e = engine(events = events)
        var target: Creature? = null
        repeat(600) {
            if (target == null) {
                e.step(GameEngine.STEP)
                target = e.firstVisible()
            }
        }
        val c = assertNotNull(target).let { target!! }
        e.pointerDown(1, c.x, c.y)
        e.pointerUp(1)
        assertEquals(CreatureState.CAUGHT, c.state)
        assertEquals(1, e.tracker.catches)
        assertEquals(listOf(c.species), events.catches)
        assertTrue(events.sounds.any { it == SoundCue.MOUSE_CAUGHT || it == SoundCue.RAT_CAUGHT })
        var leftStage = false
        repeat(60) {
            e.step(GameEngine.STEP)
            if (c.state == CreatureState.EXIT || !c.active) leftStage = true
        }
        assertTrue(leftStage)
        e.run(8f)
        assertEquals("a replacement appears", 3, e.world.activeCount())
    }

    @Test
    fun `hitbox is the creature radius plus 24 dp`() {
        val e = engine(count = 1)
        e.run(0.05f)
        val c = e.world.spawn(Species.GRAY_MOUSE)!!
        c.enter(CreatureState.FREEZE, 5f)
        c.x = 500f; c.y = 1000f; c.visibility = 1f; c.clipAboveY = Float.NaN
        val edge = c.radius + GameEngine.SLOP_DP * e.world.dp
        e.pointerDown(1, c.x + edge + 2f, c.y)
        assertEquals(CreatureState.FLEE, c.state)
        c.enter(CreatureState.FREEZE, 5f)
        c.x = 500f; c.y = 1000f
        e.pointerDown(2, c.x + edge - 2f, c.y)
        assertEquals(CreatureState.CAUGHT, c.state)
    }

    @Test
    fun `a miss far away does not scare anyone and counts as a miss`() {
        val events = RecordingEvents()
        val e = engine(count = 1, events = events)
        e.run(0.05f)
        val c = e.world.spawn(Species.RAT)!!
        c.enter(CreatureState.FREEZE, 5f)
        c.x = 200f; c.y = 1500f; c.visibility = 1f
        e.pointerDown(1, 900f, 300f)
        assertEquals(CreatureState.FREEZE, c.state)
        assertEquals(1, events.misses)
        assertEquals(1, e.tracker.misses)
    }

    @Test
    fun `session timer sends everyone to sleep and finishes`() {
        val e = engine(seconds = 5)
        e.run(5.1f)
        assertEquals(GameEngine.Phase.BEDTIME, e.phase)
        e.run(GameEngine.BEDTIME_MAX + 0.5f)
        assertEquals(GameEngine.Phase.FINISHED, e.phase)
        assertEquals(1f, e.dusk, 0.001f)
    }

    @Test
    fun `fixed step is independent of the frame rate`() {
        val a = engine(seed = 3)
        val b = engine(seed = 3)
        var t = 0L
        a.frame(t); b.frame(t)
        repeat(300) { t += 16_666_667L; a.frame(t) }        // 60 fps
        var t2 = 0L
        repeat(150) { t2 += 33_333_334L; b.frame(t2) }      // 30 fps
        assertEquals(a.world.time, b.world.time, 0.05)
    }

    @Test
    fun `rare prey appears at most once`() {
        val spawner = Spawner(listOf(Species.GRAY_MOUSE), listOf(Species.WHITE_MOUSE), 3, null)
        val r = GameRandom(1)
        val picks = List(5000) { spawner.pick(r) }
        assertEquals(1, picks.count { it == Species.WHITE_MOUSE })
    }

    @Test
    fun `favourite prey is picked more often`() {
        val spawner = Spawner(listOf(Species.GRAY_MOUSE, Species.RAT), emptyList(), 3, Species.RAT)
        val r = GameRandom(5)
        val rats = List(4000) { spawner.pick(r) }.count { it == Species.RAT }
        assertTrue("rats=$rats", rats > 2600)
    }

    @Test
    fun `extending a timed session postpones bedtime`() {
        val e = engine(seconds = 10)
        e.run(8f)
        e.extend(300)
        e.run(5f)
        assertEquals(GameEngine.Phase.PLAYING, e.phase)
        assertEquals(297.0, e.remainingSeconds!!, 0.1)
        e.run(300f)
        assertTrue(e.phase != GameEngine.Phase.PLAYING)
        assertTrue(!e.canExtend)
    }

    @Test
    fun `unlimited sessions cannot be extended`() {
        val e = engine(seconds = 0)
        assertTrue(!e.canExtend)
        e.extend(300)
        assertEquals(null, e.remainingSeconds)
    }
}

class DifficultyTest {
    @Test
    fun `low accuracy slows prey down to minus 40 percent`() {
        val d = AdaptiveDifficulty(baseSpeed = 1f, enabled = true)
        var t = 0.0
        repeat(120) {
            d.recordAttempt(t, hit = it % 10 == 0) // 10 %
            d.update(t, 0.5f)
            t += 0.5
        }
        assertEquals(AdaptiveDifficulty.MIN_LEVEL, d.level, 0.001f)
        assertEquals(0.6f, d.speedMultiplier, 0.001f)
        assertTrue(d.freezeMultiplier > 1.6f)
    }

    @Test
    fun `high accuracy speeds prey up to plus 30 percent`() {
        val d = AdaptiveDifficulty(baseSpeed = 1.2f, enabled = true)
        var t = 0.0
        repeat(200) {
            d.recordAttempt(t, hit = it % 5 != 0) // 80 %
            d.update(t, 0.5f)
            t += 0.5
        }
        assertEquals(AdaptiveDifficulty.MAX_LEVEL, d.level, 0.001f)
        assertEquals(1.2f * 1.3f, d.speedMultiplier, 0.001f)
    }

    @Test
    fun `middle accuracy keeps the level and change is gradual`() {
        val d = AdaptiveDifficulty(baseSpeed = 1f, enabled = true)
        var t = 0.0
        repeat(40) {
            d.recordAttempt(t, hit = it % 5 < 2) // 40 %
            d.update(t, 0.5f)
            t += 0.5
        }
        assertEquals(1f, d.level, 0.001f)
        val easing = AdaptiveDifficulty(1f, true)
        repeat(10) { easing.recordAttempt(it.toDouble(), false) }
        easing.update(10.0, 1f)
        assertTrue(easing.level > 0.95f)
    }

    @Test
    fun `only the last 30 seconds count`() {
        val d = AdaptiveDifficulty(1f, true)
        repeat(20) { d.recordAttempt(it.toDouble(), false) }
        repeat(10) { d.recordAttempt(100.0 + it, true) }
        assertEquals(1f, d.accuracy(110.0), 0.001f)
    }

    @Test
    fun `disabled difficulty stays neutral`() {
        val d = AdaptiveDifficulty(1f, enabled = false)
        repeat(50) { d.recordAttempt(it.toDouble(), false); d.update(it.toDouble(), 1f) }
        assertEquals(1f, d.level, 0.0001f)
    }
}

class ExitGestureTest {
    private val corner = 220f

    @Test
    fun `holding the top right corner for three seconds exits`() {
        val d = OwnerExitDetector(1080f, corner)
        d.down(1, 1050f, 30f, 0.0)
        d.update(0.9)
        assertEquals(0f, d.cornerProgress, 0.0001f) // ring hidden during the first second
        d.update(2.0)
        assertEquals(0.5f, d.cornerProgress, 0.01f)
        assertFalse(d.triggered)
        d.update(3.0)
        assertTrue(d.triggered)
    }

    @Test
    fun `leaving the corner or lifting resets the hold`() {
        val d = OwnerExitDetector(1080f, corner)
        d.down(1, 1050f, 30f, 0.0)
        d.move(1, 500f, 500f)
        d.update(3.5)
        assertFalse(d.triggered)
        d.down(2, 1060f, 20f, 4.0)
        d.up(2)
        d.update(8.0)
        assertFalse(d.triggered)
    }

    @Test
    fun `touch elsewhere never exits`() {
        val d = OwnerExitDetector(1080f, corner)
        d.down(1, 100f, 100f, 0.0)
        d.update(10.0)
        assertFalse(d.triggered)
    }

    @Test
    fun `three fingers for two seconds exit`() {
        val d = OwnerExitDetector(1080f, corner)
        d.down(1, 300f, 900f, 0.0)
        d.down(2, 400f, 900f, 0.1)
        d.update(3.0)
        assertFalse(d.triggered)
        d.down(3, 500f, 900f, 3.0)
        d.update(4.5)
        assertFalse(d.triggered)
        d.update(5.05)
        assertTrue(d.triggered)
    }

    @Test
    fun `engine pauses only through the gesture`() {
        val e = GameEngine(
            EngineSpec(SceneSpec(Location.BASEMENT, false, Season.SUMMER), GameMode.HUNT),
            GameRandom(1), GameEvents.None, 0L,
        )
        e.resize(1080f, 2160f, 2.75f)
        e.pointerDown(9, 1070f, 10f)
        repeat((2.9f / GameEngine.STEP).toInt()) { e.step(GameEngine.STEP) }
        assertFalse(e.exitDetector.triggered)
        repeat((0.3f / GameEngine.STEP).toInt()) { e.step(GameEngine.STEP) }
        assertTrue(e.exitDetector.triggered)
    }
}
