package app.purrhunt.game.creatures

import app.purrhunt.game.engine.Action
import app.purrhunt.game.engine.Creature
import app.purrhunt.game.engine.CreatureState
import app.purrhunt.game.engine.SoundCue
import app.purrhunt.game.engine.World
import kotlin.math.cos
import kotlin.math.sin

/**
 * Butterflies: fluttering flight (a steered path plus sine and noise wobble), landing on flowers
 * where they slowly fold and open their wings.
 */
object ButterflyBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        placeAtEdge(c, w)
        c.flying = true
        c.altitude = 0.7f
        startWander(c, w)
    }

    override fun startWander(c: Creature, w: World) {
        val flowers = w.layout.flowers
        if (flowers.isNotEmpty() && w.random.chance(0.55f)) {
            val f = w.random.pick(flowers)
            c.spot = f
            c.setTarget(f.x, f.y)
        } else {
            c.spot = null
            w.layout.randomPoint(c.species.medium, w.random, w.scratch)
            c.setTarget(w.scratch[0], w.scratch[1])
        }
        c.flying = true
        c.enter(CreatureState.WANDER, c.config.wanderTime.sample(w.random) * 2f)
        c.targetSpeed = c.config.walkSpeed * w.dp * w.difficulty.speedMultiplier
    }

    override fun startIdle(c: Creature, w: World) {
        // Land where it is: wings spread flat in the sun, or on a flower with slow folding.
        c.enter(CreatureState.IDLE, c.config.idleTime.sample(w.random) * w.difficulty.freezeMultiplier)
        c.action = if (c.spot != null) Action.FOLD_WINGS else Action.PERCHED
        c.actionTime = 0f
        c.targetSpeed = 0f
    }

    override fun startFreeze(c: Creature, w: World) {
        startIdle(c, w)
        c.action = Action.PERCHED
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> startWander(c, w)
            CreatureState.WANDER, CreatureState.DART, CreatureState.FLEE -> {
                // Flutter: the heading wobbles with the wing beat and a slower drift.
                c.heading = wrap(c.heading + (sin(c.age * 11f) * 0.9f + sin(c.age * 2.3f + c.seed) * 0.6f) * dt * 3f)
                val arrived = fly(c, w, dt, landing = c.spot != null && c.state == CreatureState.WANDER, cruise = 0.75f + 0.1f * sin(c.age * 3f))
                if (c.state == CreatureState.FLEE && isOutside(c, w)) c.active = false
                when {
                    arrived && c.spot != null && c.state == CreatureState.WANDER -> startIdle(c, w)
                    arrived || c.stateDone -> if (w.random.chance(0.35f)) startIdle(c, w) else startWander(c, w)
                }
            }
            CreatureState.IDLE, CreatureState.FREEZE -> {
                brake(c, dt, 8f)
                c.altitude += (0.08f - c.altitude) * (dt * 3f).coerceAtMost(1f)
                if (c.stateDone) {
                    c.spot = null
                    if (w.random.chance(0.3f)) startDart(c, w) else startWander(c, w)
                }
            }
            CreatureState.CAUGHT -> {
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                fly(c, w, dt, landing = false)
                if (isOutside(c, w) || c.stateDone) c.active = false
            }
            else -> startWander(c, w)
        }
    }

    override fun startDart(c: Creature, w: World) {
        super.startDart(c, w)
        c.spot = null
        c.flying = true
    }

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        c.spot = null
        c.flying = true
        pointAwayFrom(c, w, fx, fy, w.width * 0.6f)
        c.enter(CreatureState.FLEE, 2.5f)
        c.targetSpeed = c.config.fleeSpeed * w.dp
        w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.4f)
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        nearestEdge(c, w)
        c.spot = null
        c.flying = true
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun ambientSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.2f)

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.7f)
}

/**
 * House fly: fast chaotic flight with sharp turns, lands and rubs its front legs. The buzz loop volume
 * follows its speed (handled by the audio layer for flying creatures).
 */
object FlyBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        placeAtEdge(c, w)
        c.flying = true
        c.altitude = 0.6f
        startWander(c, w)
    }

    override fun idleAction(c: Creature, w: World): Action = Action.RUB_LEGS

    override fun startWander(c: Creature, w: World) {
        // Short buzzing loops around a nearby point.
        val r = w.random.range(60f, 160f) * w.dp
        val a = w.random.range(0f, 6.283f)
        val m = w.layout.margin
        c.setTarget((c.x + cos(a) * r).coerceIn(m, w.width - m), (c.y + sin(a) * r).coerceIn(m, w.height - m))
        c.flying = true
        c.enter(CreatureState.WANDER, w.random.range(0.25f, 0.6f))
        c.targetSpeed = c.config.dartSpeed * 0.55f * w.dp * w.difficulty.speedMultiplier
    }

    override fun startDart(c: Creature, w: World) {
        super.startDart(c, w)
        c.flying = true
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        when (c.state) {
            CreatureState.SPAWN -> startWander(c, w)
            CreatureState.WANDER -> {
                c.heading = wrap(c.heading + sin(c.age * 17f + c.seed) * dt * 6f)
                val arrived = fly(c, w, dt, landing = false, cruise = 0.6f)
                if (arrived || c.stateDone) {
                    // Many quick loops, then land or dart.
                    c.aux1 += 1f
                    if (c.aux1 > w.random.range(4f, 9f)) {
                        c.aux1 = 0f
                        decide(c, w)
                    } else {
                        startWander(c, w)
                    }
                }
            }
            CreatureState.DART, CreatureState.FLEE -> {
                val arrived = fly(c, w, dt, landing = false, cruise = 0.6f)
                if (c.state == CreatureState.FLEE && isOutside(c, w)) c.active = false
                if (arrived || c.stateDone) startWander(c, w)
            }
            CreatureState.IDLE, CreatureState.FREEZE -> {
                c.flying = false
                brake(c, dt, 20f)
                c.altitude += (0f - c.altitude) * (dt * 8f).coerceAtMost(1f)
                if (c.stateDone) startWander(c, w)
            }
            CreatureState.CAUGHT -> {
                c.flying = false
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                fly(c, w, dt, landing = false)
                if (isOutside(c, w) || c.stateDone) c.active = false
            }
            else -> startWander(c, w)
        }
    }

    override fun startHide(c: Creature, w: World) = startIdle(c, w)

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        c.flying = true
        pointAwayFrom(c, w, fx, fy, w.width * 0.4f)
        c.enter(CreatureState.FLEE, 1.5f)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        nearestEdge(c, w)
        c.flying = true
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun ambientSound(c: Creature, w: World) {
        if (!c.flying) w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.15f)
    }

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.INSECT_RUSTLE, c.x, 0.6f)
}

/** Fireflies: slow drifting flight in the dark with a pulsing glow ([Creature.aux0] is the glow 0..1). */
object FireflyBehavior : FlightBehavior() {
    override fun spawn(c: Creature, w: World) {
        w.layout.randomPoint(c.species.medium, w.random, w.scratch)
        c.x = w.scratch[0]
        c.y = w.scratch[1]
        c.heading = w.random.range(-3.14f, 3.14f)
        c.flying = true
        c.altitude = 0.6f
        c.visibility = 0f
        startWander(c, w)
    }

    override fun update(c: Creature, w: World, dt: Float) {
        tick(c, w, dt)
        // Glow: slow breathing with occasional bright flashes.
        val flash = if (sin(c.age * 0.7f + c.seed) > 0.85f) 1f else 0f
        val target = 0.35f + 0.35f * (0.5f + 0.5f * sin(c.age * 2.2f + c.seed)) + 0.3f * flash
        c.aux0 += (target - c.aux0) * (dt * 6f).coerceAtMost(1f)
        if (c.state != CreatureState.EXIT) c.visibility += (1f - c.visibility) * (dt * 1.5f).coerceAtMost(1f)
        when (c.state) {
            CreatureState.SPAWN -> startWander(c, w)
            CreatureState.WANDER, CreatureState.DART, CreatureState.FLEE -> {
                c.heading = wrap(c.heading + sin(c.age * 1.3f + c.seed) * dt * 0.8f)
                val arrived = fly(c, w, dt, landing = false, cruise = 0.5f + 0.15f * sin(c.age))
                if (c.state == CreatureState.FLEE && isOutside(c, w)) c.active = false
                if (arrived || c.stateDone) decide(c, w)
            }
            CreatureState.IDLE, CreatureState.FREEZE -> {
                // Hover in place with a lazy drift.
                c.x += sin(c.age * 1.1f) * 6f * w.dp * dt
                c.y += cos(c.age * 0.9f) * 6f * w.dp * dt
                brake(c, dt, 3f)
                if (c.stateDone) startWander(c, w)
            }
            CreatureState.CAUGHT -> {
                c.aux0 = 1f
                brake(c, dt)
                updateCaught(c, w, dt)
            }
            CreatureState.EXIT -> {
                fly(c, w, dt, landing = false)
                c.visibility = (c.visibility - dt * 0.4f).coerceAtLeast(0f)
                if (isOutside(c, w) || c.stateDone || c.visibility <= 0f) c.active = false
            }
            else -> startWander(c, w)
        }
    }

    override fun startHide(c: Creature, w: World) = startFreeze(c, w)

    override fun startFlee(c: Creature, w: World, fx: Float, fy: Float) {
        pointAwayFrom(c, w, fx, fy, w.width * 0.4f)
        c.enter(CreatureState.FLEE, 3f)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun startExit(c: Creature, w: World, preferShelter: Boolean) {
        nearestEdge(c, w)
        c.enter(CreatureState.EXIT, EXIT_MAX_TIME)
        c.targetSpeed = c.config.fleeSpeed * w.dp
    }

    override fun ambientSound(c: Creature, w: World) = w.sound(SoundCue.FIREFLY_TWINKLE, c.x, 0.2f)

    override fun caughtSound(c: Creature, w: World) = w.sound(SoundCue.FIREFLY_TWINKLE, c.x, 0.8f)
}
