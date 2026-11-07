package app.purrhunt.game.engine

import app.purrhunt.domain.Species
import app.purrhunt.game.scenes.HidingSpot
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One pooled creature. All fields are mutable and reused between spawns so the game loop never allocates.
 * Coordinates are screen pixels, [heading] is in radians (0 = facing right).
 */
class Creature(val id: Int) {
    var active = false
    var species: Species = Species.GRAY_MOUSE
        private set
    var config: BehaviorConfig = SpeciesConfigs.of(Species.GRAY_MOUSE)
        private set

    var x = 0f
    var y = 0f
    var heading = 0f
    var speed = 0f
    var targetSpeed = 0f
    var targetX = 0f
    var targetY = 0f
    /** Body length in pixels. */
    var length = 0f
    /** Hit radius in pixels without the finger slop. */
    var radius = 0f

    var state = CreatureState.SPAWN
        private set
    var stateTime = 0f
    var stateDuration = 0f
    var action = Action.NONE
    var actionTime = 0f

    /** 0 = on the ground / in water, 1 = high in the air. Drives shadow size and jump arcs. */
    var altitude = 0f
    /** Phase of steps, wing beats or tail beats; renderers derive limb poses from it. */
    var gait = 0f
    /** Free running time since spawn, used for breathing, blinking and twitching. */
    var age = 0f
    var blink = 0f
    var nextBlink = 2f
    /** 0..1, how much of the body is shown: 0 while hidden, partial while peeking. */
    var visibility = 1f
    /** Creatures peeking from a burrow are clipped above this line (NaN = no clip). */
    var clipAboveY = Float.NaN

    var spot: HidingSpot? = null
    var nextSound = 2f
    var catchable = true
    /** Seed for stable per-creature rendering variation (fur strokes, spots). */
    var seed = 0

    /** Generic per-family scratch values (jump start, flight arc, flutter phase...). */
    var aux0 = 0f
    var aux1 = 0f
    var aux2 = 0f
    var aux3 = 0f
    var flying = false

    /** Accumulated stats for "favourite prey". */
    var exposure = 0f

    val tail = TailChain(TAIL_SEGMENTS)

    fun reset(newSpecies: Species, length: Float, seed: Int) {
        species = newSpecies
        config = SpeciesConfigs.of(newSpecies)
        this.length = length
        this.radius = length * 0.5f
        this.seed = seed
        active = true
        state = CreatureState.SPAWN
        stateTime = 0f
        stateDuration = 0f
        action = Action.NONE
        actionTime = 0f
        speed = 0f
        targetSpeed = 0f
        altitude = 0f
        gait = 0f
        age = 0f
        blink = 0f
        nextBlink = 1.5f
        visibility = 1f
        clipAboveY = Float.NaN
        spot = null
        nextSound = 1f
        catchable = true
        aux0 = 0f; aux1 = 0f; aux2 = 0f; aux3 = 0f
        flying = false
        exposure = 0f
    }

    fun enter(newState: CreatureState, duration: Float = 0f) {
        state = newState
        stateTime = 0f
        stateDuration = duration
        if (newState != CreatureState.IDLE) action = Action.NONE
    }

    val stateDone: Boolean get() = stateTime >= stateDuration

    fun setTarget(tx: Float, ty: Float) {
        targetX = tx
        targetY = ty
    }

    fun distanceTo(px: Float, py: Float): Float {
        val dx = px - x
        val dy = py - y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceToTarget(): Float = distanceTo(targetX, targetY)

    fun angleTo(px: Float, py: Float): Float = atan2(py - y, px - x)

    val dirX: Float get() = cos(heading)
    val dirY: Float get() = sin(heading)

    val isHidden: Boolean get() = state == CreatureState.HIDE
    val isLeaving: Boolean get() = state == CreatureState.CAUGHT || state == CreatureState.EXIT

    companion object {
        const val TAIL_SEGMENTS = 10
    }
}

/** A chain of points that trails behind its anchor with springy inertia (mouse tails, fish fins). */
class TailChain(val count: Int) {
    val xs = FloatArray(count)
    val ys = FloatArray(count)
    private val pxs = FloatArray(count)
    private val pys = FloatArray(count)
    var segment = 1f

    fun snap(ax: Float, ay: Float, dirX: Float, dirY: Float, segment: Float) {
        this.segment = segment
        for (i in 0 until count) {
            xs[i] = ax - dirX * segment * i
            ys[i] = ay - dirY * segment * i
            pxs[i] = xs[i]
            pys[i] = ys[i]
        }
    }

    /**
     * Verlet step: each point keeps part of its velocity, then the chain is pulled back to fixed
     * segment lengths. [wiggle] adds a sideways sine motion that travels down the tail.
     */
    fun update(ax: Float, ay: Float, dirX: Float, dirY: Float, time: Float, wiggle: Float) {
        xs[0] = ax
        ys[0] = ay
        for (i in 1 until count) {
            val vx = (xs[i] - pxs[i]) * DAMPING
            val vy = (ys[i] - pys[i]) * DAMPING
            pxs[i] = xs[i]
            pys[i] = ys[i]
            val side = sin(time * 7f - i * 0.6f) * wiggle * i / count
            xs[i] += vx - dirY * side - dirX * GRAVITY * segment
            ys[i] += vy + dirX * side - dirY * GRAVITY * segment
        }
        for (iteration in 0 until 2) {
            for (i in 1 until count) {
                val dx = xs[i] - xs[i - 1]
                val dy = ys[i] - ys[i - 1]
                val d = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
                val k = segment / d
                xs[i] = xs[i - 1] + dx * k
                ys[i] = ys[i - 1] + dy * k
            }
        }
    }

    private companion object {
        const val DAMPING = 0.86f
        /** A slight backwards pull so a resting tail lies straight behind the body. */
        const val GRAVITY = 0.04f
    }
}
