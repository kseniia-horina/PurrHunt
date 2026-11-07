package app.purrhunt.game.engine

import app.purrhunt.domain.GameMode
import app.purrhunt.domain.GameSettings
import app.purrhunt.domain.SceneSpec
import app.purrhunt.domain.Species
import app.purrhunt.game.scenes.SceneLayout

/** Everything the engine needs to know about a session up front. */
data class EngineSpec(
    val scene: SceneSpec,
    val mode: GameMode,
    val speedLevel: Int = 3,
    val adaptive: Boolean = true,
    val creatureCount: Int = 3,
    val favorite: Species? = null,
    /** 0 = unlimited. */
    val sessionSeconds: Int = 0,
)

/**
 * The simulation: fixed 60 Hz logic steps fed by an accumulator, independent of the frame rate.
 * Pure Kotlin so it runs in unit tests; rendering and audio observe it from the outside.
 */
class GameEngine(
    val spec: EngineSpec,
    val random: GameRandom,
    events: GameEvents,
    wallStart: Long,
) {
    enum class Phase { PLAYING, BEDTIME, FINISHED }

    val difficulty = AdaptiveDifficulty(GameSettings.speedMultiplier(spec.speedLevel), spec.adaptive)
    val world = World(random, events, difficulty, SceneLayout.build(spec.scene.location, 1080f, 1920f, 2.75f))
    val tracker = SessionTracker(wallStart)
    val exitDetector = OwnerExitDetector(1080f, 220f)
    val spawner = Spawner(spec.scene.common, spec.scene.rare, spec.creatureCount, spec.favorite)
    val laser = LaserDot()
    val feather = FeatherToy()

    var phase = Phase.PLAYING
        private set
    var paused = false
    /** Bedtime dusk: 0 = day, 1 = lights out. */
    var dusk = 0f
        private set
    /** Seconds of actual play (pauses excluded). */
    var playTime = 0.0
        private set
    var sized = false
        private set

    /** Play time at which bedtime starts; 0 = unlimited. Grows when the owner adds time. */
    var sessionLimit = spec.sessionSeconds.toDouble()
        private set

    /** Seconds left until bedtime; null for unlimited sessions. */
    val remainingSeconds: Double?
        get() = if (sessionLimit > 0) (sessionLimit - playTime).coerceAtLeast(0.0) else null

    /** 1 at the start of a timed session, 0 at bedtime; 1 for unlimited sessions. */
    val remainingFraction: Float
        get() = remainingSeconds?.let { (it / sessionLimit).toFloat() } ?: 1f

    /** Whether the owner can still add time: a timed session that hasn't reached bedtime. */
    val canExtend: Boolean get() = sessionLimit > 0 && phase == Phase.PLAYING

    /** Adds [seconds] to a timed session before bedtime starts. */
    fun extend(seconds: Int) {
        if (canExtend) sessionLimit += seconds
    }

    private var accumulator = 0f
    private var lastFrameNanos = -1L
    private var bedtimeElapsed = 0f

    private val pointerIds = IntArray(MAX_POINTERS) { -1 }
    private val pointerCaught = BooleanArray(MAX_POINTERS)
    private val pointerLastX = FloatArray(MAX_POINTERS)
    private val pointerLastY = FloatArray(MAX_POINTERS)

    val pawSize: Float get() = PAW_DP * world.dp
    val slop: Float get() = SLOP_DP * world.dp

    fun resize(width: Float, height: Float, dp: Float) {
        if (width <= 0f || height <= 0f) return
        val old = world.layout
        if (sized && old.width == width && old.height == height) return
        val layout = SceneLayout.build(spec.scene.location, width, height, dp)
        if (sized) {
            val sx = width / old.width
            val sy = height / old.height
            for (c in world.creatures) if (c.active) {
                c.x *= sx; c.y *= sy; c.targetX *= sx; c.targetY *= sy
                c.spot = null
            }
            laser.rescale(sx, sy)
            feather.rescale(sx, sy)
        }
        world.layout = layout
        exitDetector.resize(width, CORNER_DP * dp)
        if (!sized) {
            laser.place(width / 2, height / 2)
            feather.place(width / 2, height * 0.4f, dp)
        }
        sized = true
    }

    /** Feed with the frame timestamp; runs as many fixed steps as the elapsed time requires. */
    fun frame(frameNanos: Long) {
        if (lastFrameNanos < 0 || paused) {
            lastFrameNanos = frameNanos
            return
        }
        val elapsed = ((frameNanos - lastFrameNanos) / 1e9f).coerceIn(0f, MAX_FRAME)
        lastFrameNanos = frameNanos
        accumulator += elapsed
        while (accumulator >= STEP) {
            step(STEP)
            accumulator -= STEP
        }
    }

    /** Call when the frame clock was interrupted (pause, background). */
    fun resetClock() {
        lastFrameNanos = -1L
        accumulator = 0f
    }

    fun step(dt: Float) {
        if (!sized || phase == Phase.FINISHED) return
        playTime += dt
        exitDetector.update(world.time)
        when (spec.mode) {
            GameMode.HUNT -> {
                spawner.update(world, dt)
                for (c in world.creatures) {
                    if (c.active && c.visibility > 0.3f && !c.isLeaving) tracker.expose(c.species, dt)
                }
            }
            GameMode.LASER -> laser.update(world, dt)
            GameMode.FEATHER -> feather.update(world, dt)
        }
        world.step(dt)

        if (phase == Phase.PLAYING && sessionLimit > 0 && playTime >= sessionLimit) startBedtime()
        if (phase == Phase.BEDTIME) {
            bedtimeElapsed += dt
            dusk = (bedtimeElapsed / DUSK_TIME).coerceAtMost(1f)
            val everyoneHome = world.activeCount() == 0
            if ((everyoneHome && bedtimeElapsed >= DUSK_TIME) || bedtimeElapsed >= BEDTIME_MAX) phase = Phase.FINISHED
        }
    }

    /** Prey goes to sleep: everyone hides, the lights dim, then the session finishes. */
    fun startBedtime() {
        if (phase != Phase.PLAYING) return
        phase = Phase.BEDTIME
        bedtimeElapsed = 0f
        world.startBedtime()
        laser.sleep()
        feather.sleep()
    }

    /** Ends the session right away (owner chose "finish" in the pause menu). */
    fun finishNow() {
        phase = Phase.FINISHED
    }

    // ---------------------------------------------------------------- touches

    fun pointerDown(id: Int, x: Float, y: Float) {
        if (!sized) return
        exitDetector.down(id, x, y, world.time)
        val slot = pointerIds.indexOf(-1)
        if (slot >= 0) {
            pointerIds[slot] = id
            pointerCaught[slot] = false
            pointerLastX[slot] = x
            pointerLastY[slot] = y
        }
        if (paused || phase != Phase.PLAYING) return

        if (spec.mode == GameMode.FEATHER && feather.claimOwner(id, x, y, world)) return
        world.marks.add(x, y, 1f, MARK_LIFETIME, random.range(-0.4f, 0.4f))
        tracker.touch(x / world.width, y / world.height)

        val hit = when (spec.mode) {
            GameMode.HUNT -> huntTouch(x, y, isMove = false)
            GameMode.LASER -> laser.touch(world, x, y, slop).also { if (it) toyCaught(x, y) }
            GameMode.FEATHER -> feather.touch(world, x, y, slop).also { if (it) toyCaught(x, y) }
        }
        if (slot >= 0) pointerCaught[slot] = hit
        difficulty.recordAttempt(world.time, hit)
        if (!hit) {
            tracker.missed()
            world.events.onMiss(x, y)
        }
        if (world.layout.isWater(x, y)) world.ripple(x, y, 30f * world.dp)
    }

    fun pointerMove(id: Int, x: Float, y: Float) {
        if (!sized) return
        exitDetector.move(id, x, y)
        if (spec.mode == GameMode.FEATHER && feather.isOwner(id)) {
            feather.moveOwner(x, y)
            return
        }
        val slot = pointerIds.indexOf(id)
        if (slot < 0 || paused || phase != Phase.PLAYING) return
        val dx = x - pointerLastX[slot]
        val dy = y - pointerLastY[slot]
        if (dx * dx + dy * dy < (MARK_SPACING_DP * world.dp) * (MARK_SPACING_DP * world.dp)) return
        pointerLastX[slot] = x
        pointerLastY[slot] = y
        world.marks.add(x, y, 0.8f, MARK_LIFETIME, random.range(-0.4f, 0.4f))
        // A swipe across prey counts once per press, but never as a miss.
        if (!pointerCaught[slot] && spec.mode == GameMode.HUNT && huntTouch(x, y, isMove = true)) {
            pointerCaught[slot] = true
            difficulty.recordAttempt(world.time, true)
        }
    }

    fun pointerUp(id: Int) {
        exitDetector.up(id)
        if (spec.mode == GameMode.FEATHER) feather.releaseOwner(id)
        val slot = pointerIds.indexOf(id)
        if (slot >= 0) pointerIds[slot] = -1
    }

    fun pointersCancelled() {
        for (i in pointerIds.indices) if (pointerIds[i] != -1) pointerUp(pointerIds[i])
    }

    private fun toyCaught(x: Float, y: Float) {
        tracker.caught(null, world.time)
        world.events.onToyCaught(x, y)
    }

    /** Finds the closest catchable creature under the paw; otherwise frightens creatures nearby. */
    private fun huntTouch(x: Float, y: Float, isMove: Boolean): Boolean {
        var best: Creature? = null
        var bestGap = Float.MAX_VALUE
        val reach = if (isMove) slop * 0.5f else slop
        for (c in world.creatures) {
            if (!c.active) continue
            val d = c.distanceTo(x, y)
            if (!isMove && d <= c.radius + slop + NEAR_PAWS * pawSize) tracker.near(c.species)
            if (!c.catchable || c.isLeaving || c.visibility < 0.3f) continue
            val gap = d - (c.radius + reach)
            if (gap <= 0f && gap < bestGap) {
                bestGap = gap
                best = c
            }
        }
        if (best != null) {
            world.behaviorOf(best).caught(best, world)
            tracker.caught(best.species, world.time)
            world.events.onCatch(best)
            return true
        }
        if (!isMove) {
            for (c in world.creatures) {
                if (!c.active || c.isLeaving) continue
                if (c.distanceTo(x, y) <= c.radius + slop + SCARE_PAWS * pawSize) world.behaviorOf(c).scare(c, world, x, y)
            }
        }
        return false
    }

    companion object {
        const val STEP = 1f / 60f
        private const val MAX_FRAME = 0.1f
        const val PAW_DP = 36f
        /** Generous hitbox: creature radius + 24 dp. */
        const val SLOP_DP = 24f
        /** Misses within 1.5 paw sizes of a creature scare it. */
        const val SCARE_PAWS = 1.5f
        /** Touches within 3 paws count as "interest" for the favourite-prey statistics. */
        const val NEAR_PAWS = 3f
        const val CORNER_DP = 80f
        const val MARK_LIFETIME = 0.5f
        const val MARK_SPACING_DP = 40f
        const val DUSK_TIME = 4f
        const val BEDTIME_MAX = 8f
        private const val MAX_POINTERS = 10
    }
}
