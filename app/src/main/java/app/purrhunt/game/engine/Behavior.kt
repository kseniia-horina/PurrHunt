package app.purrhunt.game.engine

import app.purrhunt.domain.Medium
import app.purrhunt.game.scenes.HidingSpot
import app.purrhunt.game.scenes.SceneLayout.Companion.dist
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * State machine of a body plan. Implementations are stateless: everything lives in [Creature],
 * so one behaviour instance serves all creatures of a family.
 */
abstract class Behavior {
    /** Places a fresh creature (in a burrow, at a screen edge, in the water...). */
    abstract fun spawn(c: Creature, w: World)

    abstract fun update(c: Creature, w: World, dt: Float)

    /** A touch landed close but missed: run away from (fx, fy). */
    open fun scare(c: Creature, w: World, fx: Float, fy: Float) {
        if (c.isLeaving || c.state == CreatureState.FLEE) return
        startFlee(c, w, fx, fy)
    }

    /** Peaceful catch: a fright jump and tumble, then the creature runs off unharmed. */
    open fun caught(c: Creature, w: World) {
        c.catchable = false
        c.enter(CreatureState.CAUGHT, CAUGHT_TIME)
        c.speed = 0f
        c.targetSpeed = 0f
        c.aux3 = w.random.sign()
        caughtSound(c, w)
    }

    /** The session is over: go home. */
    open fun bedtime(c: Creature, w: World) {
        if (c.isLeaving) return
        startExit(c, w, preferShelter = true)
    }

    protected open fun caughtSound(c: Creature, w: World) = Unit

    // ---------------------------------------------------------------- helpers shared by all families

    protected fun medium(c: Creature): Medium = c.species.medium

    /** Picks the next calm state from the weighted table, scaled by adaptive difficulty. */
    protected fun decide(c: Creature, w: World) {
        val cfg = c.config
        val d = w.difficulty
        val wIdle = cfg.weightIdle
        val wWander = cfg.weightWander
        val wDart = cfg.weightDart * d.dartMultiplier
        val wFreeze = cfg.weightFreeze * d.freezeMultiplier
        val wHide = if (w.layout.spotsFor(medium(c)).isEmpty()) 0f else cfg.weightHide
        val total = wIdle + wWander + wDart + wFreeze + wHide
        var r = w.random.float() * total
        r -= wIdle
        if (r < 0) return startIdle(c, w)
        r -= wWander
        if (r < 0) return startWander(c, w)
        r -= wDart
        if (r < 0) return startDart(c, w)
        r -= wFreeze
        if (r < 0) return startFreeze(c, w)
        startHide(c, w)
    }

    open fun startIdle(c: Creature, w: World) {
        c.enter(CreatureState.IDLE, c.config.idleTime.sample(w.random))
        c.targetSpeed = 0f
        c.action = if (w.random.chance(c.config.actionChance)) idleAction(c, w) else Action.NONE
        c.actionTime = 0f
    }

    protected open fun idleAction(c: Creature, w: World): Action = Action.SNIFF

    open fun startWander(c: Creature, w: World) {
        w.layout.randomPoint(medium(c), w.random, w.scratch, c.config.edgeAffinity)
        c.setTarget(w.scratch[0], w.scratch[1])
        c.enter(CreatureState.WANDER, c.config.wanderTime.sample(w.random))
        c.targetSpeed = c.config.walkSpeed * w.dp * w.difficulty.speedMultiplier
    }

    open fun startDart(c: Creature, w: World) {
        // A dart goes somewhere new, usually turning sharply.
        w.layout.randomPoint(medium(c), w.random, w.scratch, c.config.edgeAffinity * 0.5f)
        c.setTarget(w.scratch[0], w.scratch[1])
        c.enter(CreatureState.DART, c.config.dartTime.sample(w.random))
        c.targetSpeed = c.config.dartSpeed * w.dp * w.difficulty.speedMultiplier
    }

    open fun startFreeze(c: Creature, w: World) {
        c.enter(CreatureState.FREEZE, c.config.freezeTime.sample(w.random) * w.difficulty.freezeMultiplier)
        c.targetSpeed = 0f
    }

    open fun startHide(c: Creature, w: World) {
        val spot = w.layout.nearestShelter(medium(c), c.x, c.y) ?: return startWander(c, w)
        goToSpot(c, w, spot, c.config.walkSpeed * 2.2f)
    }

    protected fun goToSpot(c: Creature, w: World, spot: HidingSpot, speedDp: Float) {
        c.spot = spot
        c.setTarget(spot.x, spot.y)
        // HIDE with a zero duration means "on the way"; the timer starts once inside.
        c.enter(CreatureState.HIDE, 0f)
        c.aux2 = 0f
        c.targetSpeed = speedDp * w.dp * w.difficulty.speedMultiplier
    }

    open fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        val m = medium(c)
        val spot = if (m == Medium.AIR || m == Medium.WATER_SURFACE) null else w.layout.shelterAwayFrom(m, c.x, c.y, fx, fy)
        if (spot != null && dist(c.x, c.y, spot.x, spot.y) < w.width * 0.6f) {
            c.spot = spot
            c.setTarget(spot.x, spot.y)
        } else {
            // Run straight away from the paw, a bit sideways so it is not perfectly predictable.
            val a = atan2(c.y - fy, c.x - fx) + w.random.range(-0.5f, 0.5f)
            val dist = w.width * 0.35f
            var tx = c.x + cos(a) * dist
            var ty = c.y + sin(a) * dist
            if (!w.layout.allows(m, tx, ty)) {
                w.layout.randomPoint(m, w.random, w.scratch)
                tx = w.scratch[0]
                ty = w.scratch[1]
            }
            c.spot = null
            c.setTarget(tx, ty)
        }
        c.enter(CreatureState.FLEE, FLEE_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp * (0.6f + 0.4f * w.difficulty.speedMultiplier)
    }

    /** Leave the stage through the nearest edge, or into a shelter. */
    open fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        val spot = if (preferShelter) w.layout.nearestShelter(medium(c), c.x, c.y) else null
        if (spot != null) {
            c.spot = spot
            c.setTarget(spot.x, spot.y)
        } else {
            c.spot = null
            nearestEdge(c, w)
        }
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    protected fun nearestEdge(c: Creature, w: World) {
        val pad = c.length * 1.5f
        val toLeft = c.x
        val toRight = w.width - c.x
        val toTop = c.y - w.layout.floorTop
        val toBottom = w.height - c.y
        val minD = minOf(minOf(toLeft, toRight), minOf(toTop, toBottom))
        when (minD) {
            toLeft -> c.setTarget(-pad, c.y)
            toRight -> c.setTarget(w.width + pad, c.y)
            toBottom -> c.setTarget(c.x, w.height + pad)
            else -> c.setTarget(c.x, -pad)
        }
    }

    /** Turns towards the target and moves. Returns true once within [arrive] pixels. */
    protected fun steer(c: Creature, w: World, dt: Float, arrive: Float = c.length * 0.3f, accel: Float = 8f): Boolean {
        val d = c.distanceToTarget()
        if (d <= arrive) {
            c.speed += (0f - c.speed) * (accel * dt).coerceAtMost(1f)
            return true
        }
        val desired = c.angleTo(c.targetX, c.targetY)
        turnTowards(c, desired, c.config.turnRate * dt * (if (c.state == CreatureState.FLEE) 1.6f else 1f))
        // Slow down in tight turns so paths curve naturally.
        val misalign = abs(angleDiff(desired, c.heading))
        val wanted = c.targetSpeed * (1f - (misalign / PI.toFloat()).coerceIn(0f, 0.85f))
        c.speed += (wanted - c.speed) * (accel * dt).coerceAtMost(1f)
        c.x += cos(c.heading) * c.speed * dt
        c.y += sin(c.heading) * c.speed * dt
        return false
    }

    protected fun turnTowards(c: Creature, desired: Float, maxStep: Float) {
        val diff = angleDiff(desired, c.heading)
        c.heading = wrap(c.heading + diff.coerceIn(-maxStep, maxStep))
    }

    protected fun brake(c: Creature, dt: Float, rate: Float = 10f) {
        c.speed += (0f - c.speed) * (rate * dt).coerceAtMost(1f)
        c.x += cos(c.heading) * c.speed * dt
        c.y += sin(c.heading) * c.speed * dt
    }

    protected fun isOutside(c: Creature, w: World): Boolean {
        val m = c.length
        return c.x < -m || c.x > w.width + m || c.y < -m || c.y > w.height + m
    }

    /** Common bookkeeping each step: timers, blinking, gait, ambient sounds. */
    protected fun tick(c: Creature, w: World, dt: Float) {
        c.stateTime += dt
        c.actionTime += dt
        c.age += dt
        c.nextBlink -= dt
        if (c.nextBlink <= 0f) {
            c.blink = BLINK_TIME
            c.nextBlink = w.random.range(2f, 6f)
        }
        if (c.blink > 0f) c.blink = (c.blink - dt).coerceAtLeast(0f)
        val walkSpeed = c.config.walkSpeed * w.dp
        c.gait += dt * c.config.gaitRate * (0.25f + c.speed / walkSpeed.coerceAtLeast(1f)).coerceAtMost(4f)
        if (c.visibility > 0.2f && !c.isLeaving && !w.bedtime) {
            c.nextSound -= dt
            if (c.nextSound <= 0f) {
                c.nextSound = c.config.soundInterval.sample(w.random)
                ambientSound(c, w)
            }
        }
    }

    protected open fun ambientSound(c: Creature, w: World) = Unit

    /** Shared caught → exit sequence: hop, tumble, then dash away. */
    protected fun updateCaught(c: Creature, w: World, dt: Float) {
        val t = c.stateTime / CAUGHT_TIME
        c.altitude = sin(t.coerceIn(0f, 1f) * PI.toFloat()) * 0.6f
        c.heading = wrap(c.heading + c.aux3 * dt * 9f * (1f - t))
        if (c.stateDone) startExit(c, w, preferShelter = w.random.chance(0.5f))
    }

    /** Moves to the exit target and deactivates once outside or inside the shelter. */
    protected fun updateExit(c: Creature, w: World, dt: Float) {
        c.altitude = (c.altitude - dt * 2f).coerceAtLeast(0f)
        val arrived = steer(c, w, dt, arrive = c.length * 0.25f)
        val spot = c.spot
        if (spot != null) {
            c.visibility = (dist(c.x, c.y, spot.x, spot.y) / (spot.radius + c.length)).coerceIn(0f, 1f)
            if (!spot.clipLine.isNaN()) c.clipAboveY = spot.clipLine
        }
        if (arrived || isOutside(c, w) || c.stateDone) c.active = false
    }

    companion object {
        const val CAUGHT_TIME = 0.8f
        const val FLEE_MAX_TIME = 2.5f
        const val EXIT_MAX_TIME = 6f
        const val BLINK_TIME = 0.15f

        fun angleDiff(a: Float, b: Float): Float = wrap(a - b)

        fun wrap(a: Float): Float {
            var r = a
            val twoPi = (2 * PI).toFloat()
            while (r > PI) r -= twoPi
            while (r < -PI) r += twoPi
            return r
        }
    }
}
